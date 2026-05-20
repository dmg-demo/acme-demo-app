# JFrog instance setup checklist

Setup for the **acme-storefront** demo on your soleng tenant. Roughly 20–30
minutes end to end. Do these once, then the demo is reusable.

Replace `<TENANT>` with your tenant host (e.g. `soleng.jfrog.io`).

---

## 1. Create the Project

Platform → **Administration → Projects → New Project**

- [ ] Project Key: `acme-storefront`
- [ ] Display Name: `Acme Storefront Demo`
- [ ] Storage Quota: 5 GB (demo-sized)
- [ ] Assign yourself as Project Admin

---

## 2. Create the local repos

Administration → **Repositories → Repositories → New Local Repository**, all
assigned to project `acme-storefront`:

- [ ] `shared-libs-mvn-rel-local` — Maven
- [ ] `api-mvn-rel-local` — Maven
- [ ] `payments-mvn-rel-local` — Maven
- [ ] `api-docker-rel-local` — Docker
- [ ] `payments-docker-rel-local` — Docker
- [ ] `frontend-docker-rel-local` — Docker

Tag each repo with **Environment = DEV** initially (this gates lifecycle
promotion in step 5).

---

## 3. Create the remote + virtual for Maven

Without these, the services can't resolve `shared-libs` or Spring Boot.

- [ ] **Remote:** `maven-central-remote` → URL `https://repo1.maven.org/maven2/`
- [ ] **Virtual:** `mvn-virtual` → includes (in order):
  1. `shared-libs-mvn-rel-local`
  2. `api-mvn-rel-local`
  3. `payments-mvn-rel-local`
  4. `maven-central-remote`

Default deployment repo for the virtual: **none** (deploys are explicit per
service workflow).

> Optional Docker remote `docker-hub-remote` if your runners need to pull
> `eclipse-temurin` or `nginx` through JFrog. Not required for the demo flow.

---

## 4. Create the signing key

Administration → **Platform Configuration → Keys → Signing Keys → Add Key**

- [ ] Key Alias: `acme-prod`
- [ ] Type: GPG
- [ ] Let the platform generate it (export public key for prospects later if asked)
- [ ] Save — this name goes into the GitHub `RB_SIGNING_KEY` variable.

---

## 5. Define lifecycle stages

Administration → **Lifecycle Management → Stages**

- [ ] `DEV` — already exists as default
- [ ] `QA` — Add Stage
- [ ] `PROD` — Add Stage

Then **Promotions**: allow promotion from `DEV → QA → PROD` for the project.

Map each repo's environment property:

- [ ] All `*-rel-local` repos initially: Environment = `DEV`
- (Promotion of a Release Bundle moves its contained artifacts up the lifecycle,
  you don't have to re-tag repos manually.)

---

## 6. Xray policy (optional but worth showing)

Xray → **Watches → New Watch**

- [ ] Name: `acme-storefront-promotion-gate`
- [ ] Resource: Project `acme-storefront`
- [ ] Policy: block on **High** or **Critical** CVEs
- [ ] Trigger: on Release Bundle promotion to `QA`

This is what makes the "single gate at the parent" story tangible during demo.

---

## 7. OIDC integration for GitHub Actions

Administration → **General Management → Identity & Access → Integrations → OIDC**

- [ ] Provider Name: `github-acme`  *(must match `oidc-provider-name` in workflows)*
- [ ] Provider URL: `https://token.actions.githubusercontent.com`
- [ ] Audience: `<TENANT>.jfrog.io`
- [ ] Add **Identity Mapping**:
  - Claims JSON: `{"repository": "<github-org>/<repo>", "ref": "refs/heads/main"}`
  - Token scope: project `acme-storefront`, role `Project Admin` (demo simplicity)

Loosen the `ref` claim if you want feature branches to publish too.

---

## 8. GitHub repository configuration

Push `~/claude-workspace/acme-demo/` to a GitHub repo, then in
**Settings → Secrets and variables → Actions → Variables**:

- [ ] `JF_URL` = `https://<TENANT>.jfrog.io`
- [ ] `JF_HOST` = `<TENANT>.jfrog.io`
- [ ] `RB_SIGNING_KEY` = `acme-prod`

No secrets needed — OIDC handles auth.

---

## 9. Seed the initial bundles (one-time)

To have something to assemble the first time:

- [ ] Trigger each child workflow manually (`workflow_dispatch`) once:
  - `shared-libs.yml`
  - `api.yml`
  - `payments.yml`
  - `frontend.yml`
- [ ] Verify in **Application → Release Bundles v2** you see four child bundles:
  - `shared-libs-RB:1.2.0`
  - `service-api-RB:3.4.0`
  - `service-payments-RB:1.8.2`
  - `service-frontend-RB:2.1.0`

---

## 10. First parent assembly

- [ ] Actions → **storefront parent release bundle** → Run workflow:
  - `storefront_version`: `1.0.0`
  - `shared_libs_version`: `1.2.0`
  - `api_version`: `3.4.0`
  - `payments_version`: `1.8.2`
  - `frontend_version`: `2.1.0`
  - `promote_to`: *(leave blank)*

- [ ] Open `Acme-Storefront-RB:1.0.0` in the UI → content graph shows nested
      children → Evidence tab shows rollup release notes.

You now have a working demo. From here, the day-of flow is just:
`scripts/demo-change-payments.sh --commit` → wait ~90s → reassemble parent.

---

## Troubleshooting cheat sheet

| Symptom | Likely cause |
|---|---|
| `403` on `jf rt bp` | OIDC mapping not scoped to the right repo or project |
| RB create says "build not found" | Build-Info wasn't published — check `jf rt bce` ran |
| Parent create says "release bundle not found" | Child RB versions don't match exactly (case sensitive) |
| Docker push 401 | Forgot `jf docker-login` step, or token lacks Docker permission |
| Xray gate didn't fire | Watch resource is repo-scoped, not project-scoped — switch to project |
