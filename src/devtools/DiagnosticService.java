package devtools;

public final class DiagnosticService {
  private final InfraiStorageClient storage;
  public DiagnosticService(InfraiStorageClient storage) { this.storage = storage; }

  public String recordBuild(String tenantId, String buildId) {
    return recordBuild(tenantId, buildId, false);
  }

  public String recordBuild(String tenantId, String buildId, boolean temporary) {
    String bucket = TenantBucketPolicy.bucketFor(tenantId);
    storage.createBucket(bucket);
    String key = "builds/" + buildId + ".json";
    try {
      String document = "{\"tenant\":\"" + tenantId + "\",\"build_id\":\"" + buildId + "\",\"status\":\"passed\"}";
      storage.put(bucket, key, document, buildId);
      storage.get(bucket, key);
      return bucket + "/" + key;
    } finally {
      if (temporary) storage.cleanup(bucket, key);
    }
  }
}
