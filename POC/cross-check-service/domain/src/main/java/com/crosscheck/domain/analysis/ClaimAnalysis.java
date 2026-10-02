package com.crosscheck.domain.analysis;

import java.util.*;
import java.util.stream.Collectors;

/** Documentary conclusions per input claim; the global verdict is derived, never supplied. */
public record ClaimAnalysis(String analysisTarget, Decomposition decomposition, List<Claim> claims) {
    public enum Kind { SINGLE, MULTIPLE }
    public enum Basis { SINGLE_PROPOSITION, EXPLICIT_CONJUNCTION, EXPLICIT_DIMENSIONS }
    public record Decomposition(Kind kind, Basis basis, String explanation) {
        public Decomposition {
            kind = ReportChecks.required(kind, "decomposition.kind");
            basis = ReportChecks.required(basis, "decomposition.basis");
            explanation = ReportChecks.text(explanation, "decomposition.explanation");
        }
    }
    public record Claim(String id, String inputExcerpt, String proposition, Verdict verdict) {
        public Claim {
            id = ReportChecks.text(id, "claim.id");
            inputExcerpt = ReportChecks.text(inputExcerpt, "claim.inputExcerpt");
            proposition = ReportChecks.text(proposition, "claim.proposition");
            verdict = ReportChecks.required(verdict, "claim.verdict");
            ReportChecks.require(EnumSet.of(VerdictStatus.SUPPORTED, VerdictStatus.REFUTED,
                    VerdictStatus.MISLEADING, VerdictStatus.INSUFFICIENT_EVIDENCE, VerdictStatus.OPINION)
                    .contains(verdict.status()), "Invalid individual verdict");
        }
    }
    public ClaimAnalysis {
        analysisTarget = ReportChecks.text(analysisTarget, "analysisTarget");
        decomposition = ReportChecks.required(decomposition, "decomposition");
        claims = ReportChecks.list(claims, "claims");
        ReportChecks.require(!claims.isEmpty() && claims.size() <= 8, "Invalid claim count");
        boolean single = decomposition.kind() == Kind.SINGLE;
        ReportChecks.require(single ? claims.size() == 1 && decomposition.basis() == Basis.SINGLE_PROPOSITION
                : claims.size() >= 2 && decomposition.basis() != Basis.SINGLE_PROPOSITION, "Invalid decomposition");
        var ids = new HashSet<String>();
        var propositions = new HashSet<String>();
        for (var claim : claims) {
            ReportChecks.require(ids.add(claim.id()), "Duplicate claim identifier");
            ReportChecks.require(propositions.add(claim.proposition().strip().replaceAll("\\s+", " ")
                    .toLowerCase(Locale.ROOT)), "Duplicate proposition");
        }
    }
    public void validateInput(String input) {
        validateInputs(List.of(input));
    }
    public void validateInputs(List<String> inputs) {
        inputs.forEach(input -> ReportChecks.text(input, "input"));
        var spans = new ArrayList<int[]>();
        var excerpts = new HashSet<String>();
        for (var claim : claims) {
            ReportChecks.require(excerpts.add(claim.inputExcerpt()), "Duplicate input anchor");
            boolean found = false;
            for (int source = 0; source < inputs.size(); source++) {
                String input = inputs.get(source);
                int start = input.indexOf(claim.inputExcerpt());
                if (start < 0 || input.indexOf(claim.inputExcerpt(), start + 1) >= 0) continue;
                int end = start + claim.inputExcerpt().length();
                for (var span : spans) ReportChecks.require(source != span[2] || start >= span[1] || span[0] >= end,
                        "Overlapping input anchors");
                spans.add(new int[]{start, end, source});
                found = true;
                break;
            }
            ReportChecks.require(found, "Missing or ambiguous input anchor");
        }
    }
    public Verdict aggregate() {
        if (claims.size() == 1) return claims.getFirst().verdict();
        var statuses = claims.stream().map(claim -> claim.verdict().status()).collect(Collectors.toSet());
        var status = statuses.size() == 1 ? statuses.iterator().next() : VerdictStatus.NO_SINGLE_VERDICT;
        String explanation = claims.stream().map(claim -> claim.proposition() + ": " + claim.verdict().explanation())
                .collect(Collectors.joining("\n"));
        String support = claims.stream().map(claim -> claim.proposition() + ": " + claim.verdict().documentarySupport())
                .collect(Collectors.joining("\n"));
        var references = claims.stream().flatMap(claim -> claim.verdict().sourceIds().stream()).distinct().toList();
        return new Verdict(status, explanation, support, references);
    }
}
