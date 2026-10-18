# boutique-orders

Runtime: Java 21 / Spring Boot 4.0.8; build with Maven 3.9. HTTP port: 8083.

## Test

```bash
mvn -B verify
```

`docker build -t boutique-orders:local .` builds a non-root runtime image; tests run separately in Jenkins’s **Test** stage. Runtime bases are pinned by digest. Review and update those digests regularly; pinning does not replace patching.

To run the same isolated tests locally:

```bash
mkdir -p test-reports
docker build --target test -t boutique-orders:test .
test_user="$(id -u):$(id -g)"
# Rootless container UID 0 maps to the unprivileged daemon owner.
case "$(docker info --format '{{json .SecurityOptions}}')" in
  *'"name=rootless"'*) test_user=0:0 ;;
esac
docker run --rm --user "$test_user" --env HOME=/tmp \
  --cap-drop ALL --security-opt no-new-privileges:true \
  --mount "type=bind,source=$(pwd)/test-reports,target=/reports" boutique-orders:test
```

`ci/test.sh` runs the test suite and writes JUnit XML. Jenkins records these
results even on failure, stops before image building/publication if tests fail,
and archives the reports. The test environment image caches dependencies;
`docker run` executes the tests afresh. Building the runtime image alone does
not run tests.

## Configuration

See `../boutique-platform/local/compose.yaml` for required environment names. Credentials are provided through environment bindings, never committed. `/health/live` checks process health; `/health/ready` checks essential dependencies. Metrics are available at /actuator/prometheus.

The root `Jenkinsfile` calls the version-pinned, sandboxed shared library in `boutique-ci`. Jenkins discovers protected main only; this pipeline tests, scans and publishes the image and chart. Configure branch protection and scoped credentials before enabling publication. PR/fork execution requires isolated untrusted workers before it can be enabled.

## Helm deployment

`helm/` owns this service’s Kubernetes chart. Jenkins lints/renders it, packages it with the full source commit, publishes it to GHCR OCI and archives its checksum alongside the tested image digest. The GitOps environment chart vendors that exact package for promotion and rollback. See [Helm delivery](https://github.com/Local-Narx-Space/boutique-gitops/blob/main/docs/helm-delivery.md) and [the Debian/kind deployment guide](https://github.com/Local-Narx-Space/boutique-platform/blob/main/docs/deploy-cicd-kind.md).

## Security patch verification (2026-10-16)

Spring Boot 4.0.8 manages Spring Framework 7.0.9, fixing the reported critical
CVE-2026-47890 in spring-webmvc 6.2.19. The application uses Jackson 3 imports
and the Boot 4 Flyway starter. Compatible security patch overrides select
Jackson 3.1.7, legacy Jackson 2.21.7 and Tomcat 11.0.25; both Jackson generations
are present through managed dependencies and were checked in the packaged JAR.

Two unit tests passed in the isolated test container with JUnit reports. A real
runtime check against disposable PostgreSQL verified Flyway migration,
readiness, JSON item/pricing responses, checkout/retry idempotency, retrieval,
owner isolation, invalid-input rejection and the Prometheus endpoint. The final
non-root runtime passed Trivy 0.75.0 with the existing HIGH/CRITICAL,
fixed-vulnerability/secret policy and zero blocking findings; the exception list
remains empty. Scan databases were freshly downloaded from the official GHCR
repositories with normal TLS verification.

This cloud runner required a private Maven proxy configuration and a pre-fetched
Maven cache because Docker build DNS cannot reach Maven Central here. The local
validation Dockerfile used that cache offline; these runner settings were kept
outside Git. The checked-in Dockerfile retains its normal dependency download
and separate test/runtime stages. Full Jenkins publication and GitOps promotion
still need to run for the new commit.
