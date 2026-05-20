#!/usr/bin/env bash
# Demo helper: bump service-payments to a new patch version,
# add a release-notes bullet, optionally commit & push.
#
# Usage:
#   scripts/demo-change-payments.sh                 # bumps patch, no commit
#   scripts/demo-change-payments.sh --commit        # bumps + commits + pushes
#   scripts/demo-change-payments.sh --version 1.8.5 # explicit version
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SVC_DIR="$ROOT/service-payments"
POM="$SVC_DIR/pom.xml"
NOTES="$SVC_DIR/release-notes.md"
APP="$SVC_DIR/src/main/java/com/acme/payments/PaymentsApplication.java"

COMMIT=false
NEW_VERSION=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --commit) COMMIT=true; shift ;;
    --version) NEW_VERSION="$2"; shift 2 ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

# Find the project's own <version> — the one that follows
# <artifactId>service-payments</artifactId>. Not the <parent> version above it.
CUR_VERSION="$(awk '
  /<artifactId>service-payments<\/artifactId>/ { found=1; next }
  found && /<version>/ {
    sub(/.*<version>/, ""); sub(/<\/version>.*/, ""); print; exit
  }
' "$POM")"

if [[ -z "$CUR_VERSION" ]]; then
  echo "ERROR: could not find <version> for artifactId 'service-payments' in $POM" >&2
  exit 1
fi

if [[ -z "$NEW_VERSION" ]]; then
  IFS='.' read -r MA MI PA <<<"$CUR_VERSION"
  NEW_VERSION="${MA}.${MI}.$((PA+1))"
fi

echo "service-payments: ${CUR_VERSION} -> ${NEW_VERSION}"

# Bump only the project's own <version>, leaving <parent><version> alone.
awk -v new="$NEW_VERSION" '
  /<artifactId>service-payments<\/artifactId>/ { found=1; print; next }
  found && !done && /<version>/ {
    sub(/<version>[^<]+<\/version>/, "<version>" new "</version>"); done=1
  }
  { print }
' "$POM" > "$POM.tmp" && mv "$POM.tmp" "$POM"

# 2. Append release-notes bullet
DATE="$(date +%Y-%m-%d)"
cat >> "$NOTES" <<EOF

## ${NEW_VERSION} (${DATE})
- Internal change for demo (no behavior change)
EOF

# 3. Touch the source so build-info reflects a real change
TS="$(date +%s)"
sed -i.bak -E "s|// demo-touch: [0-9]+|// demo-touch: ${TS}|" "$APP" || true
if ! grep -q 'demo-touch' "$APP"; then
  awk -v ts="$TS" '
    /^public class PaymentsApplication/ { print "// demo-touch: " ts; print; next }
    { print }
  ' "$APP" > "$APP.tmp" && mv "$APP.tmp" "$APP"
fi
rm -f "$APP.bak"

echo "Updated:"
echo "  - $POM"
echo "  - $NOTES"
echo "  - $APP"

if $COMMIT; then
  git -C "$ROOT" add service-payments
  git -C "$ROOT" commit -m "demo: bump service-payments to ${NEW_VERSION}"
  git -C "$ROOT" push
  echo
  echo "Pushed. Watch the payments workflow:"
  echo "  https://github.com/<org>/<repo>/actions/workflows/payments.yml"
fi

echo
echo "When the workflow finishes, assemble the parent with:"
echo "  Actions -> 'storefront parent release bundle' -> Run workflow"
echo "    payments_version: ${NEW_VERSION}"
