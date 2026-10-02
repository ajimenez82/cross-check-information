package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.model.*;
import com.crosscheck.domain.analysis.*;
import com.crosscheck.domain.analysis.evidence.*;
import com.crosscheck.infrastructure.evidence.SourceCapture;
import com.crosscheck.infrastructure.evidence.PublicSourceCapture;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import static com.crosscheck.infrastructure.openai.OpenAiJson.MAPPER;
import static com.crosscheck.domain.analysis.evidence.EvidenceDossier.*;

/** Maps model drafts into an independently captured, immutable dossier and the existing public report. */
final class EvidencePipeline {
    static final String PUBLICATION_SELECTION_CRITERIA = "Se evalúan publicaciones de análisis o información periodística con hallazgos aceptados sobre la proposición y con alcance y periodo elegibles. "
            + "Pueden respaldarla, cuestionarla, combinar ambas posiciones o no expresar una posición explícita. "
            + "No se exige una postura propia ni una demostración causal para incluirlas; los documentos que solo aportan antecedentes fuera del periodo elegible quedan excluidos.";
    record Anchor(String id, int inputIndex, int start, int end, String text) {}
    record ProposedTarget(String id, String anchorId, String proposition) {}
    record ProposedFinding(String targetId, String quote, String locator, String paraphrase,
            PublicationTrace.Attribution attribution, Method method, PublicationTrace.Scope scope,
            PublicationTrace.TemporalRelation temporalRelation, PublicationTrace.Relation relation) {}
    enum SourceRole { ANALYSIS, REPORTING, LEGAL, RAW_DATA }
    record ProposedSource(String url, String title, String publisher, List<String> authors, SourceRole sourceRole,
            PublicationTrace.Owner stanceOwner, String stanceHolder, List<ProposedFinding> findings) {}
    record Research(String analysisTarget, ClaimAnalysis.Decomposition decomposition, List<ProposedTarget> targets,
            List<ProposedSource> sources, List<String> limitations, LocalDate asOf) {}
    record ResearchResponse(String schemaVersion, Research research, Clarification clarification) {}
    record VerdictDraft(String targetId, VerdictStatus status, String explanation, String documentarySupport, List<String> findingIds) {}
    record AssessmentDraft(String sourceId, OpenAiTracedReport.Decision decision, PublicationPosition position,
            String explanation, List<String> findingIds) {}
    record PositionsDraft(ClassificationAvailability availability, String reason, String selectionCriteria, List<AssessmentDraft> assessments) {}
    record Synthesis(String schemaVersion, String title, String context, String summary, List<String> summaryFindingIds,
            List<VerdictDraft> verdicts, PositionsDraft publicationPositions, List<String> limitations) {}

    static final class State {
        public Research research;
        public List<String> anchorTexts;
        public List<Target> targets;
        public List<Source> sources = new ArrayList<>();
        public List<Finding> findings = new ArrayList<>();
        public List<String> issues = new ArrayList<>();
        public int nextSource;
        public String researchUsage;
        public EvidenceDossier dossier() { return new EvidenceDossier(VERSION, anchorTexts, targets, sources, findings); }
    }

    static List<Anchor> anchors(AiAnalysisInput input) {
        var result = new ArrayList<Anchor>();
        var texts = input.anchorTexts();
        for (int index = 0; index < texts.size(); index++) {
            String text = texts.get(index);
            result.add(new Anchor("A" + index, index, 0, text.length(), text));
            var matcher = java.util.regex.Pattern.compile("[^.!?;\\n]+[.!?;]?").matcher(text);
            int part = 0;
            while (matcher.find()) {
                int start = matcher.start();
                while (start < matcher.end() && Character.isWhitespace(text.charAt(start))) start++;
                if (start < matcher.end()) result.add(new Anchor("A" + index + "P" + part++, index, start, matcher.end(), text.substring(start, matcher.end())));
            }
        }
        return result;
    }

    static State start(Research research, AiAnalysisInput input) {
        require(research != null && research.sources() != null && !research.sources().isEmpty() && research.sources().size() <= 6);
        require(research.targets() != null && !research.targets().isEmpty() && research.targets().size() <= 8);
        var state = new State(); state.research = research; state.anchorTexts = input.anchorTexts();
        var options = anchors(input);
        var targets = new ArrayList<Target>();
        for (int index = 0; index < research.targets().size(); index++) {
            var proposed = research.targets().get(index);
            require(proposed.id().equals("C" + (index + 1)));
            var anchor = options.stream().filter(value -> value.id().equals(proposed.anchorId())).findFirst().orElseThrow();
            targets.add(new Target(proposed.id(), proposed.proposition(), anchor.inputIndex(), anchor.start(), anchor.end(), anchor.text()));
        }
        state.targets = List.copyOf(targets);
        state.dossier();
        var claims = targets.stream().map(target -> new ClaimAnalysis.Claim(target.id(), target.excerpt(), target.proposition(),
                new Verdict(VerdictStatus.INSUFFICIENT_EVIDENCE, "Pending synthesis", "Pending synthesis", List.of()))).toList();
        new ClaimAnalysis(research.analysisTarget(), research.decomposition(), claims).validateInputs(input.anchorTexts());
        var uniqueSources = new LinkedHashMap<String, ProposedSource>();
        for (var source : research.sources()) {
            require(source.findings() != null && source.findings().size() <= 4
                    && source.sourceRole() != null && source.stanceOwner() != null);
            var previous = uniqueSources.get(source.url());
            if (previous == null) uniqueSources.put(source.url(), source);
            else {
                // One URL is captured once; inconsistent stance/role metadata is not silently reconciled.
                require(previous.sourceRole() == source.sourceRole() && previous.stanceOwner() == source.stanceOwner()
                        && Objects.equals(previous.stanceHolder(), source.stanceHolder()) && Objects.equals(previous.publisher(), source.publisher())
                        && Objects.equals(previous.title(), source.title()));
                var combined = new ArrayList<>(previous.findings()); combined.addAll(source.findings());
                boolean sameAuthors = Objects.equals(previous.authors(), source.authors());
                uniqueSources.put(source.url(), new ProposedSource(source.url(), source.title(), source.publisher(),
                        sameAuthors ? source.authors() : List.of(), source.sourceRole(), source.stanceOwner(), source.stanceHolder(),
                        combined.stream().distinct().toList()));
                state.issues.add("Se agruparon entradas de una misma URL para evitar contar o capturar dos veces la publicación."
                        + (sameAuthors ? "" : " La lista de autores era discrepante y no se ha dado por confirmada."));
            }
        }
        state.research = new Research(research.analysisTarget(), research.decomposition(), research.targets(),
                List.copyOf(uniqueSources.values()), research.limitations(), research.asOf());
        require(research.limitations() != null && research.limitations().size() <= 12);
        return state;
    }

    static void captureNext(State state, SourceCapture acquisition) {
        int index = state.nextSource;
        var proposed = state.research.sources().get(index);
        String sourceId = "S" + (index + 1);
        URI uri = URI.create(proposed.url());
        Source source;
        var accepted = new ArrayList<Finding>();
        try {
            var document = acquisition.read(uri);
            source = new Source(sourceId, uri, document.title() == null ? proposed.title() : document.title(),
                    proposed.publisher(), proposed.authors(), Access.CAPTURED, document.capture(), document.date(), null);
            if (state.research.asOf() != null && (document.date().publishedAt() == null
                    || document.date().publishedAt().isAfter(state.research.asOf()))) {
                state.issues.add(sourceId + ": no se ha confirmado disponibilidad antes de la fecha de corte; se omiten sus hallazgos.");
            } else {
                for (int findingIndex = 0; findingIndex < proposed.findings().size(); findingIndex++) {
                    var finding = proposed.findings().get(findingIndex);
                    String quote = PublicSourceCapture.normalize(finding.quote());
                    int start = source.capture().content().indexOf(quote);
                    if (quote.length() < 20 || quote.length() > 1200 || start < 0) {
                        state.issues.add(sourceId + ": se descartó un pasaje que no pudo cotejarse literalmente con el documento capturado.");
                        continue;
                    }
                    accepted.add(new Finding("F" + (index + 1) + "_" + (findingIndex + 1), sourceId, finding.targetId(),
                            new Fragment(start, start + quote.length(), quote, finding.locator()), finding.paraphrase(),
                            finding.attribution(), finding.method(), finding.scope(), finding.temporalRelation(), finding.relation()));
                }
            }
            if (accepted.isEmpty()) source = new Source(source.id(), source.requestedUrl(), source.title(), source.publisher(),
                    source.authors(), source.access(), source.capture(), source.publicationDate(), "Documento capturado sin hallazgos aceptados para esta consulta.");
        } catch (RuntimeException unavailable) {
            source = new Source(sourceId, uri, proposed.title(), proposed.publisher(), proposed.authors(), Access.INACCESSIBLE, null,
                    new PublicationDate(null, DatePrecision.UNKNOWN, DateKind.UNKNOWN, DateReview.DECLARED, "Fecha no verificada", null),
                    "No se pudo capturar independientemente el contenido; no se utiliza como evidencia.");
            accepted.clear();
            state.issues.add(sourceId + ": no se pudo acceder al documento de forma independiente.");
        }
        state.sources.add(source); state.findings.addAll(accepted); state.nextSource++;
        state.dossier();
    }

    static Object synthesisInput(State state) {
        var dossier = state.dossier();
        var packet = new LinkedHashMap<String, Object>();
        packet.put("analysisTarget", state.research.analysisTarget());
        packet.put("decomposition", state.research.decomposition()); packet.put("targets", dossier.targets());
        var sources = new ArrayList<Object>();
        var candidates = new ArrayList<Object>();
        var omittedSources = new ArrayList<Object>();
        for (int index = 0; index < dossier.sources().size(); index++) {
            var source = dossier.sources().get(index); var proposed = state.research.sources().get(index);
            var findings = dossier.findings().stream().filter(value -> value.sourceId().equals(source.id())).toList();
            if (findings.isEmpty()) {
                omittedSources.add(Map.of("sourceId", source.id(), "title", source.title(),
                        "url", source.requestedUrl().toString(), "metadataVerified", false, "assessmentAllowed", false,
                        "reason", "No accepted findings. Mention only as a research limitation; do not create an assessment."));
                continue;
            }
            var item = new LinkedHashMap<String, Object>();
            item.put("source", EvidenceAssembler.source(dossier, source.id()));
            item.put("sourceRole", proposed.sourceRole()); item.put("stanceOwner", proposed.stanceOwner());
            item.put("stanceHolder", proposed.stanceHolder()); item.put("authors", source.authors());
            item.put("access", source.access()); item.put("dateStatus", source.publicationDate().label());
            item.put("contentHash", source.capture() == null ? null : source.capture().contentHash());
            sources.add(item);
            if (dossier.targets().size() == 1) {
                var targetFindings = findings.stream().filter(value -> value.targetId().equals(dossier.targets().getFirst().id())).toList();
                var eligible = targetFindings.stream().filter(EvidencePipeline::countEligible).map(Finding::id).toList();
                var directions = targetFindings.stream().filter(value -> value.attribution() == PublicationTrace.Attribution.AUTHOR
                        && value.relation() != PublicationTrace.Relation.CONTEXT).map(Finding::id).toList();
                boolean editorial = proposed.sourceRole() == SourceRole.ANALYSIS || proposed.sourceRole() == SourceRole.REPORTING;
                boolean mayCount = editorial && !eligible.isEmpty() && eligible.containsAll(directions);
                var restrictions = new ArrayList<String>();
                if (!editorial) restrictions.add("Non-editorial source role: EXCLUDE only.");
                for (var finding : targetFindings) {
                    if (!countEligible(finding)) restrictions.add(finding.id() + ": scope=" + finding.scope().match()
                            + ", temporalRelation=" + finding.temporalRelation() + "; cannot be used for COUNT, including NO_EXPLICIT_POSITION.");
                }
                candidates.add(Map.of("sourceId", source.id(), "targetId", dossier.targets().getFirst().id(),
                        "allowedFindingIds", targetFindings.stream().map(Finding::id).toList(),
                        "countEligibleFindingIds", eligible, "requiredDirectionalFindingIds", directions,
                        "allowedDecisions", mayCount ? List.of("COUNT", "EXCLUDE") : List.of("EXCLUDE"),
                        "restrictions", restrictions));
            }
        }
        packet.put("sources", sources); packet.put("findings", dossier.findings());
        packet.put("allowedFindingIds", dossier.findings().stream().map(Finding::id).toList());
        packet.put("publicationCandidates", candidates); packet.put("omittedSources", omittedSources);
        packet.put("unverifiedResearchNotes", state.research.limitations());
        packet.put("limitations", limitations(state)); packet.put("asOf", state.research.asOf());
        return Map.of("acceptedEvidence", packet);
    }

    private static boolean countEligible(Finding finding) {
        return finding.scope().match() == PublicationTrace.Match.MATCH
                && Set.of(PublicationTrace.TemporalRelation.REQUESTED_PERIOD, PublicationTrace.TemporalRelation.RETROSPECTIVE)
                        .contains(finding.temporalRelation());
    }

    static List<String> limitations(State state) {
        var limits = new ArrayList<String>();
        limits.add("La muestra examinada es limitada y no representa todas las publicaciones disponibles.");
        for (String issue : state.issues) {
            String description = issue;
            for (var source : state.sources) {
                if (issue.startsWith(source.id() + ":")) {
                    description = "Documento localizado «" + source.title() + "» (" + source.requestedUrl().getHost() + ")"
                            + issue.substring(source.id().length());
                    break;
                }
            }
            limits.add(description);
        }
        if (state.sources.stream().anyMatch(source -> source.publicationDate().publishedAt() == null))
            limits.add("La fecha exacta de publicación de algunas fuentes no pudo verificarse; no se ha completado artificialmente.");
        return limits.stream().distinct().toList();
    }

    static AnalysisReport assemble(State state, Synthesis synthesis) {
        require("1".equals(synthesis.schemaVersion()));
        var dossier = state.dossier();
        require(synthesis.verdicts().size() == dossier.targets().size());
        var claims = new ArrayList<ClaimAnalysis.Claim>(); var seen = new HashSet<String>();
        for (var verdict : synthesis.verdicts()) {
            require(seen.add(verdict.targetId()));
            var target = dossier.targets().stream().filter(value -> value.id().equals(verdict.targetId())).findFirst().orElseThrow();
            require(Set.of(VerdictStatus.INSUFFICIENT_EVIDENCE, VerdictStatus.OPINION).contains(verdict.status()) || !verdict.findingIds().isEmpty());
            claims.add(new ClaimAnalysis.Claim(target.id(), target.excerpt(), target.proposition(), new Verdict(verdict.status(),
                    verdict.explanation(), verdict.documentarySupport(), references(dossier, verdict.targetId(), verdict.findingIds()))));
        }
        var claimAnalysis = new ClaimAnalysis(state.research.analysisTarget(), state.research.decomposition(), claims);
        claimAnalysis.validateInputs(dossier.anchorTexts());
        var assessments = new ArrayList<OpenAiTracedReport.Assessment>();
        var positions = synthesis.publicationPositions();
        require(positions != null && positions.reason() != null && !positions.reason().isBlank());
        require(claims.size() == 1 || positions.assessments().isEmpty());
        if (claims.size() == 1) {
            var assessed = positions.assessments().stream().map(AssessmentDraft::sourceId).collect(java.util.stream.Collectors.toSet());
            for (int index = 0; index < dossier.sources().size(); index++) {
                var source = dossier.sources().get(index); var role = state.research.sources().get(index).sourceRole();
                if ((role == SourceRole.ANALYSIS || role == SourceRole.REPORTING) && dossier.findings().stream().anyMatch(finding -> finding.sourceId().equals(source.id())))
                    require(positions.availability() == ClassificationAvailability.AVAILABLE && assessed.contains(source.id()));
            }
        }
        for (var proposed : positions.assessments()) {
            var source = dossier.sources().stream().filter(value -> value.id().equals(proposed.sourceId())).findFirst().orElseThrow();
            int index = dossier.sources().indexOf(source); var origin = state.research.sources().get(index);
            var findings = selected(dossier, proposed.findingIds());
            require(!findings.isEmpty() && findings.stream().allMatch(value -> value.sourceId().equals(source.id())
                    && value.targetId().equals(claims.getFirst().id())));
            require(proposed.decision() != OpenAiTracedReport.Decision.COUNT
                    || origin.sourceRole() == SourceRole.ANALYSIS || origin.sourceRole() == SourceRole.REPORTING);
            if (proposed.decision() == OpenAiTracedReport.Decision.COUNT) {
                require(findings.stream().allMatch(EvidencePipeline::countEligible));
                var ownDirections = dossier.findings().stream().filter(value -> value.sourceId().equals(source.id())
                        && value.targetId().equals(claims.getFirst().id()) && value.attribution() == PublicationTrace.Attribution.AUTHOR
                        && value.relation() != PublicationTrace.Relation.CONTEXT).map(Finding::id).toList();
                require(proposed.findingIds().containsAll(ownDirections));
            }
            var first = findings.getFirst();
            var arguments = findings.stream().map(value -> new OpenAiTracedReport.Argument(value.sourceId(), value.excerpt().locator(),
                    value.paraphrase(), OpenAiTracedReport.Attribution.valueOf(value.attribution().name()),
                    OpenAiTracedReport.Relation.valueOf(value.relation().name()))).toList();
            var scope = first.scope();
            var trace = new OpenAiTracedReport.Trace(OpenAiTracedReport.Owner.valueOf(origin.stanceOwner().name()), origin.stanceHolder(),
                    new OpenAiTracedReport.Scope(scope.outcome(), scope.geography(), scope.studyPeriod(), scope.policy(),
                            OpenAiTracedReport.Match.valueOf(scope.match().name()), scope.explanation()),
                    OpenAiTracedReport.TemporalRelation.valueOf(first.temporalRelation().name()), arguments);
            assessments.add(new OpenAiTracedReport.Assessment("A" + (assessments.size() + 1), List.of(source.id()),
                    proposed.position(), proposed.explanation(), proposed.decision(), trace));
        }
        var mappedPositions = new OpenAiTracedReport.Positions(positions.availability(), positions.reason(),
                claims.size() == 1 && positions.availability() == ClassificationAvailability.AVAILABLE ? claims.getFirst().proposition() : null,
                positions.availability() == ClassificationAvailability.AVAILABLE ? PUBLICATION_SELECTION_CRITERIA : null, assessments);
        var limits = new ArrayList<>(limitations(state)); limits.addAll(synthesis.limitations());
        var acceptedSources = dossier.sources().stream().filter(source -> dossier.findings().stream().anyMatch(finding -> finding.sourceId().equals(source.id())))
                .map(source -> EvidenceAssembler.source(dossier, source.id())).toList();
        var report = new OpenAiClaimReport("3", synthesis.title(), synthesis.context(), synthesis.summary(),
                selected(dossier, synthesis.summaryFindingIds()).stream().map(Finding::sourceId).distinct().toList(), claimAnalysis,
                acceptedSources, mappedPositions,
                limits.stream().distinct().toList(), state.research.asOf());
        var validated = report.toReport(dossier.anchorTexts());
        return new AnalysisReport(validated.title(), validated.context(), validated.summary(), validated.summarySourceIds(),
                validated.verdict(), acceptedSources, validated.publicationPositions(), validated.limitations(), validated.asOf(), validated.claimAnalysis());
    }

    private static List<String> references(EvidenceDossier dossier, String targetId, List<String> ids) {
        return EvidenceAssembler.sourceIds(dossier, targetId, selected(dossier, ids).stream()
                .map(finding -> new EvidenceAssembler.Reference(finding.sourceId(), finding.id())).toList());
    }

    private static List<Finding> selected(EvidenceDossier dossier, List<String> ids) {
        require(ids != null && ids.size() == new HashSet<>(ids).size());
        return ids.stream().map(id -> dossier.findings().stream().filter(finding -> finding.id().equals(id)).findFirst().orElseThrow()).toList();
    }

    static void require(boolean condition) { if (!condition) throw new IllegalArgumentException("Invalid evidence pipeline output"); }
}
