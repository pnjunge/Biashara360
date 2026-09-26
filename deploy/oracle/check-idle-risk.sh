#!/usr/bin/env bash
set -Eeuo pipefail

MIN_MEMORY_PERCENT="${MIN_MEMORY_PERCENT:-25}"
TOTAL_KB="$(awk '/MemTotal:/ {print $2}' /proc/meminfo)"
AVAILABLE_KB="$(awk '/MemAvailable:/ {print $2}' /proc/meminfo)"
USED_PERCENT="$(( (TOTAL_KB - AVAILABLE_KB) * 100 / TOTAL_KB ))"

if (( USED_PERCENT < MIN_MEMORY_PERCENT )); then
  logger -p user.warning -t biashara360-idle-risk \
    "Memory utilization is ${USED_PERCENT}%, below ${MIN_MEMORY_PERCENT}%; review OCI 7-day idle metrics."
  echo "WARNING: memory utilization ${USED_PERCENT}% is below ${MIN_MEMORY_PERCENT}%."
  exit 1
fi

echo "OK: memory utilization ${USED_PERCENT}% is at or above ${MIN_MEMORY_PERCENT}%."
