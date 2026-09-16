One-time bootstrap: creates the S3 bucket + DynamoDB lock table that `environments/*` store their
remote state in. Uses local state itself (there's no remote backend to bootstrap a remote backend
with) — run once per AWS account, by hand, before initialising any environment. See
`docs/deployment.md`.
