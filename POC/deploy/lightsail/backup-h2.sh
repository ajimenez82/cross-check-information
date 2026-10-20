#!/usr/bin/env bash
set -euo pipefail
if [[ "$EUID" -ne 0 ]]; then echo "Run with sudo." >&2; exit 1; fi
cd /opt/crosscheck
unset DOCKER_HOST CONTAINER_HOST CONTAINER_CONNECTION
umask 077
install -d -m 700 /var/backups/crosscheck
api_id=$(podman ps -a --filter label=com.docker.compose.project=crosscheck-beta --filter label=com.docker.compose.service=api --format '{{.ID}}')
[[ "$api_id" =~ ^[0-9a-f]+$ ]] || { echo "API container not found." >&2; exit 1; }
[[ $(podman inspect --format '{{.State.Running}}' "$api_id") == true ]] || { echo "API must be running before this backup." >&2; exit 1; }
archive="/var/backups/crosscheck/h2-$(date -u +%Y%m%dT%H%M%SZ).tar"
# H2 must be closed while the file-backed volume is copied.
podman stop --time 45 "$api_id"
trap 'podman start "$api_id" >/dev/null' EXIT
podman volume export crosscheck-beta-analysis-data --output "$archive"
chmod 600 "$archive"
tar -tf "$archive" >/dev/null
printf 'Backup: %s\n' "$archive"
