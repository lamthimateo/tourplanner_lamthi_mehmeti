#!/usr/bin/env bash

export NVM_DIR="$HOME/.nvm"
[ -s "/opt/homebrew/opt/nvm/nvm.sh" ] && . "/opt/homebrew/opt/nvm/nvm.sh"

nvm use 22 >/dev/null

# If this env var is set (sometimes globally), it can force a mismatched esbuild binary.
unset ESBUILD_BINARY_PATH

cd "$(dirname "$0")/frontend" || exit 1

# Install dependencies only when they are missing — a full reinstall on every
# start wastes minutes. If you ever hit an esbuild host/binary mismatch after
# switching Node versions, delete node_modules once and rerun this script.
if [ ! -d node_modules ]; then
  npm install
fi
npm start