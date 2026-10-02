package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.domain.analysis.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Versioned provider contract, prepared for activation with the matching remote schema. */
record OpenAiTracedReport(String schemaVersion, String title, String context, String summary,
                         List<String> summarySourceIds, Verdict verdict, List<Evidence> sources,
                         Positions publicationPositions, List<String> limitations, LocalDate asOf) {
    AnalysisReport toReport() {
        require("2".equals(schemaVersion));
        var positions = publicationPositions;
        require(positions != null && positions.assessments() != null);
        var units = new ArrayList<PublicationAssessment>();
        var excluded = new ArrayList<ExcludedPublication>();
        var ids = new HashSet<String>();
        var assignedSources = new HashSet<String>();
        for (var assessment : positions.assessments()) {
            require(assessment != null && ids.add(assessment.id()));
            assessment.validate();
            for (String sourceId : assessment.sourceIds()) require(assignedSources.add(sourceId));
            if (assessment.decision() == Decision.COUNT) {
                units.add(new PublicationAssessment(assessment.id(), assessment.sourceIds(),
                        assessment.position(), assessment.explanation(), assessment.trace().toDomain()));
            } else {
                for (String sourceId : assessment.sourceIds()) {
                    excluded.add(new ExcludedPublication(sourceId, assessment.explanation(), assessment.trace().toDomain()));
                }
            }
        }
        var mapped = new PublicationPositions(positions.availability(), positions.reason(), positions.proposition(),
                null, null, positions.selectionCriteria(), units, excluded);
        return new OpenAiReport(title, context, summary, summarySourceIds, verdict, sources,
                mapped, limitations, asOf).toReport();
    }

    record Positions(ClassificationAvailability availability, String reason, String proposition,
                     String selectionCriteria, List<Assessment> assessments) {}
    enum Decision { COUNT, EXCLUDE }
    enum Owner { AUTHOR, THIRD_PARTY, NONE, UNDETERMINED }
    enum Match { MATCH, PARTIAL, OUT_OF_SCOPE, UNCERTAIN }
    enum TemporalRelation { REQUESTED_PERIOD, RETROSPECTIVE, BACKGROUND, UNKNOWN }
    enum Attribution { AUTHOR, THIRD_PARTY, DESCRIPTIVE }
    enum Relation { SUPPORTS, QUESTIONS, CONTEXT }
    record Scope(String outcome, String geography, String studyPeriod, String policy,
                 Match match, String explanation) {}
    record Argument(String sourceId, String locator, String paraphrase, Attribution attribution, Relation relation) {}
    record Trace(Owner stanceOwner, String stanceHolder, Scope scope,
                 TemporalRelation temporalRelation, List<Argument> arguments) {
        PublicationTrace toDomain() {
            return new PublicationTrace(PublicationTrace.Owner.valueOf(stanceOwner.name()), stanceHolder,
                    new PublicationTrace.Scope(scope.outcome(), scope.geography(), scope.studyPeriod(), scope.policy(),
                            PublicationTrace.Match.valueOf(scope.match().name()), scope.explanation()),
                    PublicationTrace.TemporalRelation.valueOf(temporalRelation.name()),
                    arguments.stream().map(argument -> new PublicationTrace.Argument(argument.sourceId(),
                            argument.locator(), argument.paraphrase(),
                            PublicationTrace.Attribution.valueOf(argument.attribution().name()),
                            PublicationTrace.Relation.valueOf(argument.relation().name()))).toList());
        }
    }
    record Assessment(String id, List<String> sourceIds, PublicationPosition position,
                      String explanation, Decision decision, Trace trace) {
        void validate() {
            text(id); text(explanation);
            require(sourceIds != null && !sourceIds.isEmpty() && decision != null && trace != null);
            sourceIds.forEach(OpenAiTracedReport::text);
            require(new HashSet<>(sourceIds).size() == sourceIds.size());
            require(trace.stanceOwner() != null && trace.temporalRelation() != null && trace.scope() != null);
            optionalText(trace.stanceHolder());
            var scope = trace.scope();
            text(scope.outcome()); text(scope.geography()); text(scope.explanation());
            optionalText(scope.studyPeriod()); optionalText(scope.policy()); require(scope.match() != null);
            require(trace.arguments() != null && !trace.arguments().isEmpty());
            boolean supports = false;
            boolean questions = false;
            for (var argument : trace.arguments()) {
                require(argument != null);
                text(argument.sourceId()); text(argument.paraphrase()); optionalText(argument.locator());
                require(sourceIds.contains(argument.sourceId()) && argument.attribution() != null && argument.relation() != null);
                if (argument.attribution() == Attribution.AUTHOR) {
                    supports |= argument.relation() == Relation.SUPPORTS;
                    questions |= argument.relation() == Relation.QUESTIONS;
                }
            }
            if (decision == Decision.EXCLUDE) {
                require(position == null);
                return;
            }
            require(position != null && scope.match() == Match.MATCH);
            require(trace.temporalRelation() == TemporalRelation.REQUESTED_PERIOD
                    || trace.temporalRelation() == TemporalRelation.RETROSPECTIVE);
            require(trace.stanceOwner() != Owner.UNDETERMINED);
            if (position == PublicationPosition.NO_EXPLICIT_POSITION) {
                require(!supports && !questions);
            } else {
                require(trace.stanceOwner() == Owner.AUTHOR);
                switch (position) {
                    case SUPPORTS -> require(supports && !questions);
                    case QUESTIONS -> require(questions && !supports);
                    case MIXED -> require(supports && questions);
                    default -> throw new InvalidAnalysisOutputException();
                }
            }
        }
    }
    private static void optionalText(String value) { if (value != null) text(value); }
    private static void text(String value) { require(value != null && !value.isBlank()); }
    private static void require(boolean condition) { if (!condition) throw new InvalidAnalysisOutputException(); }
}
