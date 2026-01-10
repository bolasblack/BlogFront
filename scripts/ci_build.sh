#!/usr/bin/env bash

set -euo pipefail

export MISE_OVERRIDE_TOOL_VERSIONS_FILENAMES=none
export MISE_OVERRIDE_CONFIG_FILENAMES=.mise.toml
export PATH="$HOME/.local/share/mise/shims:$PATH"

curl https://mise.run | sh
mise install
pnpm run build