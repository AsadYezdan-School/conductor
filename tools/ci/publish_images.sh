#!/usr/bin/env bash
set -euo pipefail

cd "${BUILD_WORKSPACE_DIRECTORY}"

SHORT_SHA="${COMMIT_SHA:0:7}"

if [[ "${BRANCH_NAME}" == release/* ]]; then
    TAG="${RELEASE_TAG}"
elif [[ "${BRANCH_NAME}" == main ]]; then
    TAG="main-${SHORT_SHA}"
elif [[ "${BRANCH_NAME}" == dev-* ]]; then
    TAG="${BRANCH_NAME}-${SHORT_SHA}"
else
    echo "No publish rule for branch '${BRANCH_NAME}', skipping."
    echo "Branch name must start with 'dev-'"
    exit 1
fi

echo "Publishing images with tag: ${TAG}"

# Export tag for subsequent CI steps (no-op outside GitHub Actions)
if [[ -n "${GITHUB_ENV:-}" ]]; then
  echo "TAG=${TAG}" >> "$GITHUB_ENV"
fi

bazel run //scheduler:publish_image -- --tag "$TAG"
bazel run //submitter:publish_image -- --tag "$TAG"
bazel run //worker:publish_image -- --tag "$TAG"