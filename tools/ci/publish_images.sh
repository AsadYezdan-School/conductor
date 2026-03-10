#!/usr/bin/env bash
set -euo pipefail

cd "${BUILD_WORKSPACE_DIRECTORY}"

bazel run //scheduler:publish_image
bazel run //submitter:publish_image
bazel run //worker:publish_image