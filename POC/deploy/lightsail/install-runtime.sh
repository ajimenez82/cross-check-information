#!/usr/bin/env bash
set -euo pipefail
if [[ "$EUID" -ne 0 ]]; then echo "Run with sudo." >&2; exit 1; fi
source /etc/os-release
if [[ "$ID" != ubuntu || "$VERSION_ID" != 24.04 ]]; then
  echo "This installer targets Ubuntu 24.04 LTS." >&2; exit 1
fi
apt-get update
apt-get install -y --no-install-recommends podman podman-compose curl ca-certificates openssl
install -d -m 700 /opt/crosscheck /var/backups/crosscheck
install -m 644 "$(dirname "$0")/crosscheck-beta.service" /etc/systemd/system/crosscheck-beta.service
install -m 644 "$(dirname "$0")/crosscheck-backup.service" /etc/systemd/system/crosscheck-backup.service
install -m 644 "$(dirname "$0")/crosscheck-backup.timer" /etc/systemd/system/crosscheck-backup.timer
systemctl daemon-reload
# Restart containers after unexpected failures/reboots; the compose unit creates the stack on boot.
systemctl enable podman-restart.service
printf 'Runtime ready. Install release files and .env before enabling crosscheck-beta.service.\n'
