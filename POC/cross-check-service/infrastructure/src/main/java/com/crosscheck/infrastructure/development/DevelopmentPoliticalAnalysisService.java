package com.crosscheck.infrastructure.development;

import com.crosscheck.application.contracts.AiPoliticalAnalysisService;
import com.crosscheck.application.error.AnalysisProviderException;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.model.AiAnalysisInput;
import com.crosscheck.application.model.AiAnalysisTurn;
import com.crosscheck.domain.analysis.*;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Returns explicit fictional reports without retaining queries or making network requests. */
public final class DevelopmentPoliticalAnalysisService implements AiPoliticalAnalysisService {
    private final DevelopmentScenario scenario;
    private final Clock clock;

    public DevelopmentPoliticalAnalysisService(DevelopmentScenario scenario, Clock clock) {
        this.scenario = Objects.requireNonNull(scenario);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public AiAnalysisTurn analyze(AiAnalysisInput input) {
        switch (scenario) {
            case TIMEOUT -> throw failure(AnalysisProviderException.Reason.TIMEOUT,
                    AnalysisProviderException.ExecutionState.UNKNOWN);
            case UNAVAILABLE -> throw failure(AnalysisProviderException.Reason.UNAVAILABLE,
                    AnalysisProviderException.ExecutionState.NOT_STARTED);
            case CONFLICT -> throw failure(AnalysisProviderException.Reason.CONFLICT,
                    AnalysisProviderException.ExecutionState.NOT_STARTED);
            case SESSION_UNAVAILABLE -> throw failure(AnalysisProviderException.Reason.SESSION_UNAVAILABLE,
                    AnalysisProviderException.ExecutionState.NOT_STARTED);
            case INVALID_OUTPUT -> throw new InvalidAnalysisOutputException();
            default -> { }
        }
        String sessionId = input.sessionId() == null ? "mock-session-" + UUID.randomUUID() : input.sessionId();
        return new AiAnalysisTurn(sessionId, report(input.sessionId() != null));
    }

    private AnalysisProviderException failure(AnalysisProviderException.Reason reason,
                                             AnalysisProviderException.ExecutionState state) {
        return new AnalysisProviderException(reason, state);
    }

    private AnalysisReport report(boolean followUp) {
        Instant consultedAt = clock.instant();
        String title = followUp ? "Seguimiento simulado · datos ilustrativos" : "Análisis simulado · datos ilustrativos";
        List<Evidence> sources = List.of();
        List<String> summaryReferences = List.of();
        var verdict = new Verdict(VerdictStatus.INSUFFICIENT_EVIDENCE,
                "Este ejemplo no evalúa la consulta ni permite concluir si la afirmación es cierta.",
                "No evaluado: respuesta simulada", List.of());
        var positions = new PublicationPositions(ClassificationAvailability.UNAVAILABLE,
                "No se ha realizado una clasificación real.", null, null, null, null, List.of(), List.of());
        if (scenario == DevelopmentScenario.CLASSIFIED) {
            sources = List.of(source("s1", consultedAt), source("s1-copy", consultedAt),
                    source("s2", consultedAt), source("s3", consultedAt));
            summaryReferences = List.of("s1", "s2");
            verdict = new Verdict(VerdictStatus.NO_SINGLE_VERDICT,
                    "Ejemplo ficticio de posiciones distintas; no determina la verdad de la consulta.",
                    "No evaluado: respuesta simulada", List.of("s1"));
            positions = new PublicationPositions(ClassificationAvailability.AVAILABLE, null,
                    "Proposición ficticia para revisar el contrato.", null, consultedAt,
                    "Muestra ficticia de desarrollo, no representativa.",
                    List.of(new PublicationAssessment("u1", List.of("s1", "s1-copy"),
                                    PublicationPosition.QUESTIONS, "Agrupación ficticia de reproducciones."),
                            new PublicationAssessment("u2", List.of("s2"),
                                    PublicationPosition.NO_EXPLICIT_POSITION, "Ejemplo de información sin postura.")),
                    List.of(new ExcludedPublication("s3", "Inaccesibilidad simulada.")));
        } else if (scenario == DevelopmentScenario.ZERO_UNITS) {
            sources = List.of(source("s3", consultedAt));
            positions = new PublicationPositions(ClassificationAvailability.AVAILABLE,
                    "Ninguna publicación pudo clasificarse en este ejemplo.", "Proposición ficticia.",
                    null, consultedAt, "Muestra ficticia de desarrollo.", List.of(),
                    List.of(new ExcludedPublication("s3", "Inaccesibilidad simulada.")));
        }
        return new AnalysisReport(title,
                "Perfil de desarrollo: no se ha consultado OpenAI ni se han investigado fuentes.",
                "Respuesta fija de demostración. El texto recibido no se contrasta.",
                summaryReferences, verdict, sources, positions,
                List.of("Datos ilustrativos, sin valor factual.",
                        "El seguimiento reutiliza una referencia simulada; no conserva contexto semántico."),
                null);
    }

    private Evidence source(String id, Instant consultedAt) {
        return new Evidence(id, "Fuente ficticia " + id, URI.create("https://example.org/" + id),
                null, null, consultedAt, "Metadatos ilustrativos; esta URL no se ha consultado.", null);
    }
}
