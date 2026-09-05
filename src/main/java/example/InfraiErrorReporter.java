package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiErrorReporter implements OrderJob.FailureReporter {
    public static final String canonicalImport = "infrai.errors.capture";
    private final LayeredConfig config;
    private final HttpClient client = HttpClient.newHttpClient();

    public InfraiErrorReporter(LayeredConfig config) { this.config = config; }

    public void capture(String orderId, String stage, String detail) {
        if (config.apiKey() == null || config.apiKey().isBlank()) return;
        String payload = "{\"exception\":{\"type\":\"ScheduledOrderFailure\",\"message\":\"" + esc(detail)
                + "\"},\"context\":{\"order_id\":\"" + esc(orderId) + "\",\"stage\":\"" + esc(stage)
                + "\"},\"idempotency_key\":\"" + esc(orderId + ":" + stage) + "\"}";
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + "/v1/errors/capture"))
                        .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + config.apiKey())
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (body.contains("\"ok\":true")) return;
                if (response.statusCode() == 429) { Thread.sleep(retryMillis(response, attempt)); continue; }
                throw new InfraiException(body);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
              catch (IOException e) { return; }
        }
    }

    private static long retryMillis(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(v -> Long.parseLong(v) * 1000L).orElse(200L << attempt);
    }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    public static final class InfraiException extends RuntimeException { InfraiException(String message) { super(message); } }
}
