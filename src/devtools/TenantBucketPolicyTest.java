package devtools;

public final class TenantBucketPolicyTest {
  public static void main(String[] args) {
    String first = TenantBucketPolicy.bucketFor("school-a");
    String second = TenantBucketPolicy.bucketFor("school-a");
    if (!first.equals("tenant-school-a") || !first.equals(second)) throw new AssertionError(first);
    System.out.println("TenantBucketPolicyTest: PASS");
  }
}
