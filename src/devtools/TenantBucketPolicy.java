package devtools;

public final class TenantBucketPolicy {
  private TenantBucketPolicy() {}

  public static String bucketFor(String tenantId) {
    if (tenantId == null || !tenantId.matches("[a-z0-9-]{2,40}"))
      throw new IllegalArgumentException("tenant id must use lowercase letters, digits, and hyphens");
    return "tenant-" + tenantId;
  }
}
