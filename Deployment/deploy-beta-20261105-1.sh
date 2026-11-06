#!/usr/bin/env bash
set -euo pipefail
cd /home/ubuntu/crosscheck-releases/beta-20261105-1
sha256sum -c SHA256SUMS
for file in compose.yaml Caddyfile nginx.conf start-web.sh sites/contrasta.caddy; do
  sudo cmp "$file" "/opt/crosscheck/$file"
done
sudo podman load -i api.tar
sudo podman load -i web.tar
sudo systemctl start crosscheck-backup.service
test "$(sudo systemctl show crosscheck-backup.service -p Result --value)" = success
sudo install -m 600 /opt/crosscheck/.env /opt/crosscheck/.env.before-beta-20261105-1
rollback() {
  echo 'Deployment failed; restoring previous release.' >&2
  sudo cp /opt/crosscheck/.env.before-beta-20261105-1 /opt/crosscheck/.env
  sudo systemctl restart crosscheck-beta.service
}
trap rollback ERR
sudo sed -i 's/^RELEASE_TAG=.*/RELEASE_TAG=beta-20261105-1/' /opt/crosscheck/.env
sudo systemctl restart crosscheck-beta.service
for attempt in $(seq 1 36); do
  api=$(sudo podman inspect --format '{{.State.Health.Status}}' crosscheck-beta_api_1)
  web=$(sudo podman inspect --format '{{.State.Health.Status}}' crosscheck-beta_web_1)
  if [[ "$api" = healthy && "$web" = healthy ]] && curl --fail --silent --max-time 10 https://www.contrastaloquetecuentan.com/healthz; then
    sudo podman ps --format '{{.Names}} {{.Image}} {{.Status}}'
    trap - ERR
    echo 'Deployment successful: beta-20261105-1'
    exit 0
  fi
  sleep 5
done
echo 'Health check timed out.' >&2
false
