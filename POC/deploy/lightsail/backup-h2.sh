#!/usr/bin/env bash
set -euo pipefail
if [[ "$EUID" -ne 0 ]]; then echo "Run with sudo." >&2; exit 1; fi
cd /opt/crosscheck
export PODMAN_COMPOSE_PROVIDER=/usr/bin/podman-compose
unset DOCKER_HOST CONTAINER_HOST CONTAINER_CONNECTION
umask 077
install -d -m 700 /var/backups/crosscheck
api_id=$(podman compose --env-file .env -f compose.yaml ps -q api)
[[ -n "$api_id" ]] || { echo "API container not found." >&2; exit 1; }
[[ $(podman inspect --format '{{.State.Running}}' "$api_id") == true ]] || { echo "API must be running before this backup." >&2; exit 1; }
archive="/var/backups/crosscheck/h2-$(date -u +%Y%m%dT%H%M%SZ).tar"
# H2 must be closed while the file-backed volume is copied.
podman stop --time 45 "$api_id"
trap 'podman start "$api_id" >/dev/null' EXIT
podman volume export crosscheck-beta-analysis-data --output "$archive"
tar -tf "$archive" >/dev/null
printf 'Backup: %s\n' "$archive"
