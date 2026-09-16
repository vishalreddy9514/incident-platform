# Terraform module: iam

One shared ECS execution role (pulls images, writes logs, reads the Secrets Manager entries
referenced in each task definition) plus one task role per service (`backend`, `ai-service`,
`frontend`). Task roles currently hold no permissions beyond assuming the role — none of the
three services call another AWS API at runtime yet — but stay separate per service so a future
service-specific need doesn't touch the others' identities.
