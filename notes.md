## What Needs Doing

1) ~~Setup bazel to build containers~~
2) Setup up CI pipeline to build code (might need to some special stuff to leverage bazel build cache for build in the pipeline), run tests, then build an publish containers.
3) Create and deploy to AWS - Use AWS CDK in typescript, and add a new stage to github actions ci to deploy changes.
3) Decide/setup integration for developing on AWS - how to do it, how to do it in a way that allows debugging etc.
4) Setup some infra for schema migrations of my RDS instance - probably something like flyway or liquibase, but need to do some research on it.
5) Setup some monitoring - look to integrate Otel
6) 25, 