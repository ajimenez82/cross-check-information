package com.crosscheck.domain.analysis;

import java.util.List;

/** Declared justification, not independent verification of the source content. */
public record PublicationTrace(Owner stanceOwner, String stanceHolder, Scope scope,
                               TemporalRelation temporalRelation, List<Argument> arguments) {
    public PublicationTrace {
        stanceOwner = ReportChecks.required(stanceOwner, "trace.stanceOwner");
        stanceHolder = ReportChecks.optionalText(stanceHolder, "trace.stanceHolder");
        scope = ReportChecks.required(scope, "trace.scope");
        temporalRelation = ReportChecks.required(temporalRelation, "trace.temporalRelation");
        arguments = ReportChecks.list(arguments, "trace.arguments");
        ReportChecks.require(!arguments.isEmpty(), "Trace needs arguments");
    }
    public enum Owner { AUTHOR, THIRD_PARTY, NONE, UNDETERMINED }
    public enum Match { MATCH, PARTIAL, OUT_OF_SCOPE, UNCERTAIN }
    public enum TemporalRelation { REQUESTED_PERIOD, RETROSPECTIVE, BACKGROUND, UNKNOWN }
    public enum Attribution { AUTHOR, THIRD_PARTY, DESCRIPTIVE }
    public enum Relation { SUPPORTS, QUESTIONS, CONTEXT }
    public record Scope(String outcome, String geography, String studyPeriod, String policy,
                        Match match, String explanation) {
        public Scope {
            outcome = ReportChecks.text(outcome, "scope.outcome");
            geography = ReportChecks.text(geography, "scope.geography");
            studyPeriod = ReportChecks.optionalText(studyPeriod, "scope.studyPeriod");
            policy = ReportChecks.optionalText(policy, "scope.policy");
            match = ReportChecks.required(match, "scope.match");
            explanation = ReportChecks.text(explanation, "scope.explanation");
        }
    }
    public record Argument(String sourceId, String locator, String paraphrase,
                           Attribution attribution, Relation relation) {
        public Argument {
            sourceId = ReportChecks.text(sourceId, "argument.sourceId");
            locator = ReportChecks.optionalText(locator, "argument.locator");
            paraphrase = ReportChecks.text(paraphrase, "argument.paraphrase");
            attribution = ReportChecks.required(attribution, "argument.attribution");
            relation = ReportChecks.required(relation, "argument.relation");
        }
    }
}
