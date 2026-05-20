# Acme Storefront — nested Release Bundle demo

A minimal demo showing how three independent teams + one shared-libs team
can ship a single application via **nested JFrog Release Bundles v2**,
eliminating the "rebuild the world for any change" pattern.

## The story

```
Acme-Storefront-RB                  (parent / aggregated RBv2)
├── shared-libs-RB        1.2.0     Maven jar — used by api + payments
├── service-api-RB        3.4.0     Spring Boot + Docker image
├── service-payments-RB   1.8.2     Spring Boot + Docker image
└── service-frontend-RB   2.1.0     nginx + static html
```

Each child team ships on its own cadence. The parent is **assembled, not
compiled** — pure metadata, takes seconds.

## Layout

```
shared-libs/         Maven library (Java 17)
service-api/         Spring Boot, depends on shared-libs
service-payments/    Spring Boot, depends on shared-libs
service-frontend/    static html + nginx Dockerfile
.github/workflows/
  shared-libs.yml          child build & RB
  api.yml                  child build & RB
  payments.yml             child build & RB
  frontend.yml             child build & RB
  storefront-release.yml   parent assembly + optional promotion
```

## JFrog instance prerequisites (you wire this up)

**Project:** `acme-storefront`

**Repos (all under the project):**
| Repo | Type |
|---|---|
| `shared-libs-mvn-rel-local` | Maven local |
| `api-mvn-rel-local` | Maven local |
| `payments-mvn-rel-local` | Maven local |
| `api-docker-rel-local` | Docker local |
| `payments-docker-rel-local` | Docker local |
| `frontend-docker-rel-local` | Docker local |
| `mvn-virtual` | Maven virtual — includes Maven Central remote + the three `*-mvn-rel-local` repos so each service can resolve shared-libs |

**Lifecycle stages:** `DEV → QA → PROD` (Platform Admin → Lifecycle)

**Signing:** create a GPG key in Platform Admin → Keys, e.g. `acme-prod`

**OIDC:** add an OIDC integration in Platform Admin → General Management →
OIDC named `github-acme`, scoped to this GitHub repo.

## GitHub Actions configuration

Repo-level **Variables** (Settings → Secrets and variables → Actions → Variables):

| Variable | Example |
|---|---|
| `JF_URL` | `https://dylanmo.jfrog.io` |
| `JF_HOST` | `dylanmo.jfrog.io` |
| `RB_SIGNING_KEY` | `acme-prod` |

No secrets needed — OIDC handles auth.

## Demo flow (~10 min)

1. **Frame the pain (30s).** "Today this customer rebuilds 48 hours for any change."

2. **Show a code change.** Edit `service-payments/src/main/java/com/acme/payments/PaymentsApplication.java`, bump pom.xml to `1.8.3`. Push.

3. **Watch payments pipeline.** Builds in ~90s, publishes Build-Info, creates `service-payments-RB:1.8.3`, attaches release-notes evidence.

4. **Trigger storefront assembly.** Actions → `storefront parent release bundle` → Run workflow with:
   - storefront_version: `1.0.1`
   - shared_libs_version: `1.2.0`
   - api_version: `3.4.0`
   - payments_version: `1.8.3`   ← the new one
   - frontend_version: `2.1.0`

5. **Open the parent RB in the UI.** Show:
   - Content graph: nested children visible
   - Each child's artifacts and Build-Info
   - Evidence tab: per-child release notes + the parent rollup

6. **Promote DEV → QA.** Run the same workflow with `promote_to: QA` (or use the UI). Xray gate evaluates once at the parent level.

7. **Promote QA → PROD.** Same again.

## What this demo proves

- Independent build cadence per team
- Shared-libs versioning without forced rebuilds
- Immutable, signed, SBOM-validated bundles at every level
- One-shot parent assembly replaces the 48-hour monolith build
- Evidence (release notes today, attestations tomorrow) flows with the bundle

## Open placeholders

- `dylanmo.jfrog.io` — soleng tenant
- OIDC provider name `github-acme` — match whatever you create in Platform Admin
- `acme-prod` signing key — match the GPG key you create
