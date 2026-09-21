# Infrastructure

Postgres (RDS), Kafka (MSK), an ECR repository for the API image and a
Secrets Manager entry for database credentials. Networking is passed in
(`vpc_id`, `private_subnet_ids`) rather than created here, so the same module
runs against an existing VPC.

```bash
terraform init
terraform plan  -var-file=dev.tfvars
terraform apply -var-file=dev.tfvars
```

Credentials never live in the repo: `db_username` and `db_password` are marked
sensitive and supplied from the environment (`TF_VAR_db_password`) or from
Secrets Manager in CI.
