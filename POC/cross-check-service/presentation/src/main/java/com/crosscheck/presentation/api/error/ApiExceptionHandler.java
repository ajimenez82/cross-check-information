package com.crosscheck.presentation.api.error;

import com.crosscheck.application.error.AnalysisProviderException;
import com.crosscheck.application.error.ExpiredConversationReferenceException;
import com.crosscheck.application.error.InvalidAnalysisInputException;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import com.crosscheck.application.error.InvalidConversationReferenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(com.crosscheck.application.features.analysis.jobs.JobException.class)
    ResponseEntity<Object> jobFailure(com.crosscheck.application.features.analysis.jobs.JobException exception, WebRequest request) {
        String message = switch (exception.code()) {
            case "IDEMPOTENCY_CONFLICT" -> "La clave de envío ya se utilizó para otra consulta.";
            case "ANALYSIS_CONFLICT" -> "La conversación tiene otro análisis en curso o el contexto está desactualizado.";
            case "ANALYSIS_CAPACITY_EXCEEDED" -> "La cola de análisis está llena. Inténtalo más tarde.";
            case "ANALYSIS_JOB_EXPIRED" -> "El resultado del análisis ha caducado.";
            case "ANALYSIS_JOB_NOT_FOUND", "INVALID_JOB_CREDENTIAL" -> "No se puede acceder a este análisis.";
            case "ANALYSIS_STORAGE_UNAVAILABLE" -> "No se ha podido acceder al almacenamiento de análisis.";
            case "ASYNC_ANALYSIS_REQUIRED" -> "Este servicio requiere el flujo de análisis asíncrono.";
            default -> "La solicitud de análisis no es válida.";
        };
        var response=error(exception.status(),exception.code(),message,
                exception.status()==503 ? "UNKNOWN" : "NOT_STARTED",request);
        var headers=new HttpHeaders(); headers.putAll(response.getHeaders()); headers.setCacheControl("no-store");
        if(exception.status()==429) headers.set("Retry-After","3");
        return new ResponseEntity<>(response.getBody(),headers,response.getStatusCode());
    }

    @ExceptionHandler(InvalidAnalysisInputException.class)
    ResponseEntity<Object> invalidInput(WebRequest request) {
        return error(400, "INVALID_ANALYSIS_INPUT", "La consulta no es válida.", "NOT_STARTED", request);
    }

    @ExceptionHandler(InvalidConversationReferenceException.class)
    ResponseEntity<Object> invalidReference(WebRequest request) {
        return error(400, "INVALID_CONVERSATION_REFERENCE",
                "La referencia de conversación no es válida.", "NOT_STARTED", request);
    }

    @ExceptionHandler(ExpiredConversationReferenceException.class)
    ResponseEntity<Object> expiredReference(WebRequest request) {
        return error(410, "CONVERSATION_REFERENCE_EXPIRED",
                "La referencia de conversación ha caducado.", "NOT_STARTED", request);
    }

    @ExceptionHandler(InvalidAnalysisOutputException.class)
    ResponseEntity<Object> invalidOutput(WebRequest request) {
        return error(502, "INVALID_ANALYSIS_OUTPUT",
                "El proveedor devolvió un resultado no utilizable.", "UNKNOWN", request);
    }

    @ExceptionHandler(AnalysisProviderException.class)
    ResponseEntity<Object> providerFailure(AnalysisProviderException exception, WebRequest request) {
        String state = exception.executionState().name();
        return switch (exception.reason()) {
            case TIMEOUT -> error(504, "ANALYSIS_TIMEOUT",
                    "El análisis ha superado el tiempo de espera.", state, request);
            case UNAVAILABLE -> error(503, "ANALYSIS_PROVIDER_UNAVAILABLE",
                    "El proveedor de análisis no está disponible temporalmente.", state, request);
            case SESSION_UNAVAILABLE -> error(410, "CONVERSATION_UNAVAILABLE",
                    "La conversación ya no está disponible.", state, request);
            case CONFLICT -> error(409, "ANALYSIS_CONFLICT",
                    "La conversación tiene una operación en curso.", state, request);
        };
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception exception, WebRequest request) {
        // Exception messages and stack traces may contain provider data or credentials.
        log.error("Unexpected API failure: requestId={}, type={}",
                request.getAttribute(RequestIdFilter.ATTRIBUTE, WebRequest.SCOPE_REQUEST),
                exception.getClass().getSimpleName());
        return error(500, "INTERNAL_ERROR", "No se ha podido completar la solicitud.", "UNKNOWN", request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (exception instanceof MethodArgumentNotValidException) {
            return invalidInput(request);
        }
        String code = switch (status.value()) {
            case 400 -> "INVALID_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "HTTP_ERROR";
        };
        String message = switch (status.value()) {
            case 400 -> "El cuerpo JSON de la solicitud no es válido.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "El método HTTP no está permitido.";
            case 406 -> "El formato de respuesta solicitado no está disponible.";
            case 415 -> "La solicitud debe utilizar Content-Type application/json.";
            default -> "No se ha podido procesar la solicitud HTTP.";
        };
        return response(status.value(), code, message, status.is4xxClientError() ? "NOT_STARTED" : "UNKNOWN",
                request, headers);
    }

    private ResponseEntity<Object> error(int status, String code, String message,
                                         String state, WebRequest request) {
        return response(status, code, message, state, request, HttpHeaders.EMPTY);
    }

    private ResponseEntity<Object> response(int status, String code, String message,
                                            String state, WebRequest request, HttpHeaders headers) {
        String requestId = (String) request.getAttribute(RequestIdFilter.ATTRIBUTE, WebRequest.SCOPE_REQUEST);
        return ResponseEntity.status(status).headers(headers).contentType(MediaType.APPLICATION_JSON)
                .body(new ApiErrorResponse(code, message, requestId, state));
    }
}
