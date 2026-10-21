#!/bin/sh
set -eu
resolver=$(awk '/^nameserver / { print $2; exit }' /etc/resolv.conf)
[ -n "$resolver" ] || { echo "Container DNS resolver missing" >&2; exit 1; }
case "$resolver" in *:*) resolver="[$resolver]" ;; esac
nginx_config=$(mktemp /tmp/nginx.XXXXXX)
sed "s/__CONTAINER_DNS__/$resolver/g" /etc/nginx/nginx.conf > "$nginx_config"
if [ "${1:-}" = "--check" ]; then exec nginx -t -c "$nginx_config"; fi
exec nginx -c "$nginx_config" -g 'daemon off;'
