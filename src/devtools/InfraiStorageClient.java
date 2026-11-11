package devtools;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;

public final class InfraiStorageClient {
  private final ServiceConfig config;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public InfraiStorageClient(ServiceConfig config) { this.config = config; }

  // Canonical capability: storage.bucket.create
  public void createBucket(String bucket) { call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + bucket + "\"}"); }

  public void put(String bucket, String key, String value, String idempotencyKey) {
    String encoded = Base64.getEncoder().encodeToString(value.getBytes());
    call("PUT", "/v1/storage/object/put/" + bucket + "/" + key,
        "{\"data_base64\":\"" + encoded + "\",\"content_type\":\"application/json\",\"idempotency_key\":\"" + idempotencyKey + "\"}");
  }

  public String get(String bucket, String key) { return call("GET", "/v1/storage/object/get/" + bucket + "/" + key, null); }

  public void cleanup(String bucket, String key) {
    try {
      call("DELETE", "/v1/storage/object/delete/" + bucket + "/" + key, null);
      call("GET", "/v1/storage/object/get/" + bucket + "/" + key, null, true);
    } finally {
      call("DELETE", "/v1/storage/bucket/delete/" + bucket, null);
      call("GET", "/v1/storage/object/list/" + bucket, null, true);
    }
  }

  private String call(String method, String path, String body) {
    return call(method, path, body, false);
  }

  private String call(String method, String path, String body, boolean expectMissing) {
    try {
      for (int attempt = 0; attempt < 3; attempt++) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
            .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + config.apiKey())
            .header("Content-Type", "application/json");
        HttpRequest req = b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        String env = res.body();
        if (expectMissing) {
          if (res.statusCode() == 404 || (res.statusCode() == 200 && env.contains("\"ok\":true") && env.contains("\"found\":false"))) return env;
          throw new IllegalStateException("Deletion verification expected a missing resource: " + res.statusCode() + " " + env);
        }
        if (!env.contains("\"ok\":true")) throw new IllegalStateException("Infrai request rejected: " + env);
        if (res.statusCode() == 429 && attempt < 2) { Thread.sleep((long) Math.pow(2, attempt) * 200); continue; }
        return env;
      }
      throw new IllegalStateException("request retry budget exhausted");
      } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
      catch (Exception e) { throw new IllegalStateException(e); }
  }
}
