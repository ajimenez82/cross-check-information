package com.crosscheck.infrastructure.jobs;
import com.crosscheck.application.features.analysis.jobs.*;
import com.crosscheck.application.model.ConversationContext;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import tools.jackson.databind.json.JsonMapper;

/** File-backed single-instance POC store. Nested calls join the outer transaction. */
public final class JdbcJobStore implements JobStore, AutoCloseable {
    private final Connection connection;
    private final Clock clock;
    private final JsonMapper mapper=JsonMapper.builder().build();
    private boolean inside;
    public JdbcJobStore(Path path, Clock clock) {
        this.clock=clock;
        try {
            var absolute=path.toAbsolutePath().normalize();
            if (absolute.toString().contains(";")) throw new IllegalArgumentException("Invalid database path");
            Files.createDirectories(absolute.getParent());
            connection=DriverManager.getConnection("jdbc:h2:file:"+absolute.toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0", "sa", "");
            connection.setAutoCommit(false);
            try (var input=JdbcJobStore.class.getResourceAsStream("/db/jobs/V1__analysis_jobs.sql")) {
                String migration=new String(Objects.requireNonNull(input).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                try (var statement=connection.createStatement()) { for (String sql:migration.split(";")) if (!sql.isBlank()) statement.execute(sql); }
            }
            connection.commit();
            try (var statement=connection.createStatement(); var rows=statement.executeQuery("SELECT MAX(version) FROM job_schema_version")) {
                rows.next(); if (rows.getInt(1)!=1) throw new IllegalStateException("Unsupported database schema");
            }
        } catch (Exception failure) { throw new JobException(503,"ANALYSIS_STORAGE_UNAVAILABLE"); }
    }
    @Override public synchronized <T> T transact(Function<Transaction,T> operation) {
        if (inside) return operation.apply(new Tx());
        try {
            try (var statement=connection.createStatement();var rows=statement.executeQuery("SELECT id FROM job_mutex WHERE id=1 FOR UPDATE")) { rows.next(); }
            inside=true;
            T value=operation.apply(new Tx()); connection.commit(); return value;
        } catch (RuntimeException failure) { rollback(); throw failure;
        } catch (SQLException failure) { rollback(); throw storage();
        } finally { inside=false; }
    }
    private void rollback() { try { connection.rollback(); } catch (SQLException ignored) {} }
    private static JobException storage() { return new JobException(503,"ANALYSIS_STORAGE_UNAVAILABLE"); }
    private void execute(String sql,Object...values) {
        try (var statement=connection.prepareStatement(sql)) {
            for(int i=0;i<values.length;i++) statement.setObject(i+1,values[i]); statement.executeUpdate();
        } catch (SQLException failure) { throw storage(); }
    }
    private final class Tx implements Transaction {
        public List<AnalysisJob> jobs() {
            try (var statement=connection.createStatement();var rows=statement.executeQuery("SELECT payload FROM analysis_job")) {
                var jobs=new ArrayList<AnalysisJob>(); while(rows.next()) jobs.add(mapper.readValue(rows.getString(1),AnalysisJob.class)); return jobs;
            } catch (Exception failure) { throw storage(); }
        }
        private AnalysisJob query(String column,String value) {
            try(var statement=connection.prepareStatement("SELECT payload FROM analysis_job WHERE "+column+"=?")) {
                statement.setString(1,value);try(var rows=statement.executeQuery()) { return rows.next()?mapper.readValue(rows.getString(1),AnalysisJob.class):null; }
            } catch(Exception failure) { throw storage(); }
        }
        public AnalysisJob find(String id) { return query("id",id); }
        public AnalysisJob byKey(String key) { return query("idempotency_key",key); }
        public void save(AnalysisJob job) { execute("MERGE INTO analysis_job (id,idempotency_key,payload) KEY(id) VALUES (?,?,?)",job.id,job.idempotencyKey,mapper.writeValueAsString(job)); }
        public void delete(String id) { execute("DELETE FROM analysis_job WHERE id=?",id); }
        public String latestContextId(String session) {
            try(var statement=connection.prepareStatement("SELECT latest_context_id FROM conversation_state WHERE session_id=?")) {
                statement.setString(1,session);try(var rows=statement.executeQuery()) {return rows.next()?rows.getString(1):null;}
            } catch(SQLException failure) {throw storage();}
        }
        public void purgeContexts() { execute("DELETE FROM conversation_context WHERE expires_at<=?",clock.instant().toEpochMilli()); }
    }
    @Override public String put(String session,ConversationContext context,Instant expires) {
        return transact(tx -> {
            String id=UUID.randomUUID().toString();
            execute("INSERT INTO conversation_context (id,session_id,expires_at,payload) VALUES (?,?,?,?)",id,session,expires.toEpochMilli(),mapper.writeValueAsString(context));
            execute("MERGE INTO conversation_state (session_id,latest_context_id) KEY(session_id) VALUES (?,?)",session,id);return id;
        });
    }
    @Override public ConversationContext get(String id,String session) {
        return transact(tx -> {
            try(var statement=connection.prepareStatement("SELECT payload FROM conversation_context WHERE id=? AND session_id=? AND expires_at>?")) {
                statement.setString(1,id);statement.setString(2,session);statement.setLong(3,clock.instant().toEpochMilli());
                try(var rows=statement.executeQuery()) {return rows.next()?mapper.readValue(rows.getString(1),ConversationContext.class):null;}
            } catch(Exception failure) {throw storage();}
        });
    }
    @Override public synchronized void close() { try {connection.close();} catch(SQLException failure) {throw storage();} }
}
