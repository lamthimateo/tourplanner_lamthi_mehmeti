#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

command -v docker >/dev/null 2>&1 || { echo "docker is required"; exit 1; }

free_port() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    local pids
    pids="$(lsof -ti "tcp:${port}" 2>/dev/null || true)"
    if [[ -n "$pids" ]]; then
      echo "Port ${port} is in use. Stopping process(es): ${pids}"
      # Terminate gracefully first, then force-kill if needed.
      kill $pids 2>/dev/null || true
      sleep 0.5
      local still
      still="$(lsof -ti "tcp:${port}" 2>/dev/null || true)"
      if [[ -n "$still" ]]; then
        kill -9 $still 2>/dev/null || true
      fi
    fi
  fi
}

cleanup() {
  echo
  echo "Stopping dev processes..."
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
  if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "$FRONTEND_PID" >/dev/null 2>&1; then
    kill "$FRONTEND_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

free_port 8081
free_port 4200

echo "Starting PostgreSQL (docker compose up -d)..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up -d

echo "Starting backend..."
(
  cd "$ROOT_DIR/backend"
  # Loads backend/.env if present
  "$ROOT_DIR/run_backend.sh"
) &
BACKEND_PID=$!

echo "Starting frontend..."
(
  cd "$ROOT_DIR/frontend"
  "$ROOT_DIR/run_frontend.sh"
) &
FRONTEND_PID=$!

echo
echo "Backend:  http://localhost:8081"
echo "Frontend: http://localhost:4200"
echo
echo "Press Ctrl+C to stop."

wait "$BACKEND_PID" "$FRONTEND_PID"
