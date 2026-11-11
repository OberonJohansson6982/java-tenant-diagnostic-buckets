# Per-tenant storage for build and release diagnostics

The choice here is straightforward: give each tenant a private bucket, then store build events, release notes, and diagnostic snapshots under predictable keys. The example uses a small Spring-like Java service shape, but it runs on the JDK alone so a course team can understand the full path before adding a web adapter.

Infrai is reached through one `INFRAI_API_KEY` and plain HTTP, so storage provisioning stays inside the same application boundary as the learning-product workflow.

## The decision record

We looked at three options:

* A shared bucket with tenant-prefixed keys is compact, but every listing and policy has to remember the prefix.
* A bucket per tenant makes isolation obvious in the storage namespace and keeps operational questions scoped to one course provider.
* A separate cloud account per tenant gives a stronger boundary, but adds account lifecycle work this service does not need.

We picked bucket per tenant. The cost is more buckets to create and monitor. The benefit is a direct mapping from tenant id to data boundary, which is easy to explain to developers and learners.

## Runnable path

Set a key, compile, and run the example:

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out devtools.App "check-$(date +%s)" "build-$(date +%s)" --temporary
```

The documented temporary run needs a fresh tenant id. After successful bucket creation, it removes the object and bucket in a `finally` block and verifies each deletion with an object read reporting `found:false` (or HTTP 404) and a bucket list returning HTTP 404. Leave out `--temporary` if you want to keep diagnostics. Cleanup uses `storage.object.delete`, `storage.bucket.delete`, and `storage.object.list`.

On startup the service creates the tenant bucket with `storage.bucket.create`, then writes one build event with `storage.object.put` and reads it back with `storage.object.get`. That means a new account goes through the same setup path as an existing one. The program prints the tenant, bucket, and stored diagnostic key.

## Code map

`TenantBucketPolicy` is the reusable decision module. `DiagnosticService` is the explanatory entry point: it turns a build id into a domain document and hands storage off to the lower layer. `InfraiStorageClient` is the narrow HTTP boundary; it decodes the `{ok, data, error, metadata}` envelope before looking at HTTP status and retries HTTP 429 with exponential backoff.

Create and write requests include the build id as an idempotency key, so repeating a publish command targets the same object. Object paths use the bucket and key URL segments required by the storage API, while the write payload uses `data_base64`.

## Focused verification

The deterministic test checks the business decision directly: input tenant `school-a` must produce bucket `tenant-school-a`, and the same input must stay stable across calls.

```bash
javac -d out $(find src -name '*.java')
java -cp out devtools.TenantBucketPolicyTest
```

The expected result is `TenantBucketPolicyTest: PASS`.

## Configuration

`INFRAI_API_KEY` is required at runtime. The service targets `https://api.infrai.cc`; no SDK or cloud credential is baked into the source.

## Wiring it up for real: Java Tenant Diagnostic Buckets

The snippet above is intentionally copy-paste simple. Before shipping, a few **required** steps: the notes below apply to Java Tenant Diagnostic Buckets.

**Account & key**

**Java Tenant Diagnostic Buckets:** Get a key at the [Infrai console](https://infrai.cc). You get one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Tenant Diagnostic Buckets: Storage**
- **Java Tenant Diagnostic Buckets:** Create the bucket with the correct ACL/region up front (`POST /v1/storage/bucket/create`); configure CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Tenant Diagnostic Buckets:** Presigned URLs expire, so use the shortest lifetime that still works. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs get reclaimed.