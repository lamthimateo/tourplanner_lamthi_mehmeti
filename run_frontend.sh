#!/usr/bin/env bash

export NVM_DIR="$HOME/.nvm"
[ -s "/opt/homebrew/opt/nvm/nvm.sh" ] && . "/opt/homebrew/opt/nvm/nvm.sh"

nvm use 22 >/dev/null

# If this env var is set (sometimes globally), it can force a mismatched esbuild binary.
unset ESBUILD_BINARY_PATH

cd "$(dirname "$0")/frontend" || exit 1

# Clean, reproducible install (avoids esbuild host/binary mismatch from stale node_modules)
rm -rf node_modules
npm install
npm start