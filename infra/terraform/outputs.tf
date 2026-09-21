output "db_endpoint" {
  value = aws_db_instance.ledger.endpoint
}

output "kafka_bootstrap_brokers" {
  value = aws_msk_cluster.ledger.bootstrap_brokers_tls
}

output "api_repository_url" {
  value = aws_ecr_repository.api.repository_url
}
