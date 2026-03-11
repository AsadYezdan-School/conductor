#!/usr/bin/env bash
set -euo pipefail

cd "${BUILD_WORKSPACE_DIRECTORY}"

# Use COMMIT_SHA env var if set, otherwise fall back to git
TAG="${COMMIT_SHA:-$(git rev-parse --short HEAD)}"

bazel run //scheduler:publish_image -- --tag "$TAG"
bazel run //submitter:publish_image -- --tag "$TAG"
bazel run //worker:publish_image -- --tag "$TAG"