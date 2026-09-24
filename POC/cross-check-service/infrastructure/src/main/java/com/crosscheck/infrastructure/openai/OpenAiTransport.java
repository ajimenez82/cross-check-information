package com.crosscheck.infrastructure.openai;

import com.crosscheck.application.error.AnalysisProviderException;
import com.crosscheck.application.error.InvalidAnalysisOutputException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import tools.jackson.databind.JsonNode;
import static com.crosscheck.application.error.AnalysisProviderException.Reason.*;
import static com.crosscheck.application.error.AnalysisProviderException.ExecutionState.*;

/** One HTTP attempt per operation; response bodies and credentials never enter exceptions or logs. */
final class OpenAiTransport {
    private final HttpClient client;
    private final URI baseUri;
    private final String apiKey;

    OpenAiTransport(HttpClient client, URI baseUri, String apiKey) {
        this.client = client;
        this.baseUri = baseUri;
        this.apiKey = apiKey;
    }

    JsonNode request(String path, Object body, long deadline, boolean sessionResource, boolean submitted) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw new AnalysisProviderException(TIMEOUT, submitted ? UNKNOWN : NOT_STARTED);
        var builder = HttpRequest.newBuilder(baseUri.resolve(path)).timeout(Duration.ofNanos(remaining))
                .header("Authorization", "Bearer " + apiKey).header("OpenAI-Beta", "agents=v1")
                .header("Accept", "application/json");
        if (body != null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OpenAiJson.MAPPER.writeValueAsString(body)));
        } else {
            builder.GET();
        }
        var pending = client.sendAsync(builder.build(), ignored -> new LimitedBody());
        try {
            var response = pending.get(remaining, TimeUnit.NANOSECONDS);
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                var state = submitted || status >= 500 || status == 408 ? UNKNOWN : NOT_STARTED;
                var reason = switch (status) {
                    case 404, 410 -> sessionResource ? SESSION_UNAVAILABLE : UNAVAILABLE;
                    case 409 -> CONFLICT;
                    case 408, 504 -> TIMEOUT;
                    default -> UNAVAILABLE;
                };
                throw new AnalysisProviderException(reason, state);
            }
            if (response.body().length == 0 && body != null) return OpenAiJson.MAPPER.createObjectNode();
            try {
                var json = OpenAiJson.MAPPER.readTree(response.body());
                if (json == null || !json.isObject()) throw new InvalidAnalysisOutputException();
                return json;
            } catch (RuntimeException invalid) {
                throw new InvalidAnalysisOutputException();
            }
        } catch (TimeoutException timeout) {
            throw new AnalysisProviderException(TIMEOUT, submitted || body != null ? UNKNOWN : NOT_STARTED);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AnalysisProviderException(UNAVAILABLE, submitted || body != null ? UNKNOWN : NOT_STARTED);
        } catch (ExecutionException failed) {
            for (Throwable cause = failed.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof InvalidAnalysisOutputException) throw new InvalidAnalysisOutputException();
            }
            var reason = failed.getCause() instanceof java.net.http.HttpTimeoutException ? TIMEOUT : UNAVAILABLE;
            throw new AnalysisProviderException(reason, submitted || body != null ? UNKNOWN : NOT_STARTED);
        } finally {
            if (!pending.isDone()) pending.cancel(true);
        }
    }

    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        @Override public CompletionStage<byte[]> getBody() { return result; }
        @Override public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }
        @Override public void onNext(List<ByteBuffer> buffers) {
            for (var buffer : buffers) {
                if ((long) bytes.size() + buffer.remaining() > 2_000_000) {
                    result.completeExceptionally(new InvalidAnalysisOutputException());
                    subscription.cancel();
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable error) { result.completeExceptionally(error); }
        @Override public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
