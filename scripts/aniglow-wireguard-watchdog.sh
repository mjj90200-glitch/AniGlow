#!/bin/sh

set -u

export PATH="/opt/homebrew/bin:/opt/homebrew/sbin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

WG="/opt/homebrew/bin/wg"
WG_QUICK="/opt/homebrew/bin/wg-quick"
CONF="/opt/homebrew/etc/wireguard/wg0.conf"
MAX_HANDSHAKE_AGE=300

log() {
  printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

check_tunnel() {
  interfaces="$($WG show interfaces 2>/dev/null || true)"
  if [ -z "$interfaces" ]; then
    log "WireGuard interface missing; starting wg0"
    "$WG_QUICK" up "$CONF"
    status=$?
    log "wg0 start finished with status $status"
    return
  fi

  latest="$($WG show all latest-handshakes 2>/dev/null | awk 'NR == 1 { print $3 }')"
  now="$(date +%s)"

  if [ -z "$latest" ] || [ "$latest" -eq 0 ] || [ $((now - latest)) -gt "$MAX_HANDSHAKE_AGE" ]; then
    log "WireGuard handshake is stale; restarting wg0"
    "$WG_QUICK" down "$CONF" >/dev/null 2>&1 || true
    "$WG_QUICK" up "$CONF"
    status=$?
    log "wg0 restart finished with status $status"
  fi
}

log "AniGlow WireGuard watchdog started"
while true; do
  check_tunnel
  sleep 60
done
