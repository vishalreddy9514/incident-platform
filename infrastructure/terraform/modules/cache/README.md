# Terraform module: cache

A single-node ElastiCache Redis cluster (`cache.t4g.micro`) plus its subnet group — not a
replication group, since Redis here is a cache/rate-limit backend, not a source of truth, and a
replica's extra cost isn't justified for a portfolio dev environment.
