#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
scan_dir="$(mktemp -d)"
trap 'rm -rf "$scan_dir"' EXIT
# Export only source-controlled or unignored worktree files: private local credentials never leave their directory.
python3 - "$scan_dir" <<'PYTHON'
import pathlib, shutil, subprocess, sys
root=pathlib.Path.cwd(); target=pathlib.Path(sys.argv[1])
files=subprocess.check_output(['git','ls-files','-z','--cached','--others','--exclude-standard']).split(b'\0')
for raw in files:
    if not raw: continue
    relative=pathlib.Path(raw.decode()); source=root/relative
    if not source.is_file() or source.is_symlink(): continue
    destination=target/relative; destination.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(source,destination)
PYTHON
mkdir -p target/security
chmod 700 target/security
docker run --rm -v "$scan_dir:/source:ro" -v "$PWD/target/security:/reports" ghcr.io/gitleaks/gitleaks:v8.30.1 dir /source --redact --no-banner --report-format json --report-path /reports/gitleaks.json --exit-code 1
# Resolve the aggregate Java graph once using Maven's lock/version management and local cache.
./mvnw -B org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom -DskipTests
# A full SBOM avoids independently fetching every Maven POM from a rate-limited public repository.
docker run --rm -v "$PWD/target:/reports" -v eduze_trivy_cache:/root/.cache/trivy aquasec/trivy:0.74.0 sbom /reports/bom.json --severity HIGH,CRITICAL --exit-code 1 --format json --output /reports/security/java-vulnerabilities.json --quiet
# Client lockfiles cover resolved production and build dependencies; do not scan node_modules/private files.
docker run --rm -v "$PWD/web/pnpm-lock.yaml:/scan/web/pnpm-lock.yaml:ro" -v "$PWD/packages/contracts/pnpm-lock.yaml:/scan/contracts/pnpm-lock.yaml:ro" -v "$PWD/target/security:/reports" -v eduze_trivy_cache:/root/.cache/trivy aquasec/trivy:0.74.0 fs /scan --scanners vuln --severity HIGH,CRITICAL --exit-code 1 --format json --output /reports/client-vulnerabilities.json --quiet
