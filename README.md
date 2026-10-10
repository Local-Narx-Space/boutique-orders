# boutique-orders

Runtime: Java 21 / Maven 3.9. HTTP port: 8083.

## Test

```bash
mvn -B verify
```

`docker build -t boutique-orders:local .` runs service unit tests in the build and creates a non-root runtime image. Runtime bases are pinned by digest. Review and update those digests regularly; pinning does not replace patching.

## Configuration

See `../boutique-platform/local/compose.yaml` for required environment names. Credentials are provided through environment bindings, never committed. `/health/live` checks process health; `/health/ready` checks essential dependencies. Metrics are available at /actuator/prometheus.

The root `Jenkinsfile` calls the version-pinned, sandboxed shared library in `boutique-ci`. Jenkins discovers protected main only; this pipeline tests, scans and publishes the image and chart. Configure branch protection and scoped credentials before enabling publication. PR/fork execution requires isolated untrusted workers before it can be enabled.

## Helm deployment

`helm/` owns this service’s Kubernetes chart. Jenkins lints/renders it, packages it with the full source commit, publishes it to GHCR OCI and archives its checksum alongside the tested image digest. The GitOps environment chart vendors that exact package for promotion and rollback. See [Helm delivery](https://github.com/subhankar12-spec/boutique-gitops/blob/main/docs/helm-delivery.md) and [the Debian/kind deployment guide](https://github.com/subhankar12-spec/boutique-platform/blob/main/docs/deploy-cicd-kind.md).
