terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

resource "aws_db_subnet_group" "ledger" {
  name       = "${var.name}-db"
  subnet_ids = var.private_subnet_ids
}

resource "aws_db_instance" "ledger" {
  identifier              = "${var.name}-postgres"
  engine                  = "postgres"
  engine_version          = "16.3"
  instance_class          = var.db_instance_class
  allocated_storage       = 20
  storage_encrypted       = true
  db_name                 = "ledger"
  username                = var.db_username
  password                = var.db_password
  db_subnet_group_name    = aws_db_subnet_group.ledger.name
  vpc_security_group_ids  = [aws_security_group.db.id]
  backup_retention_period = 7
  skip_final_snapshot     = var.environment != "prod"

  tags = local.tags
}

resource "aws_security_group" "db" {
  name        = "${var.name}-db"
  description = "Postgres access from the service tier only"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.service.id]
  }

  tags = local.tags
}

resource "aws_security_group" "service" {
  name        = "${var.name}-service"
  description = "Ledger API and settlement tasks"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_msk_cluster" "ledger" {
  cluster_name           = "${var.name}-kafka"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = var.kafka_brokers

  broker_node_group_info {
    instance_type   = var.kafka_instance_type
    client_subnets  = var.private_subnet_ids
    security_groups = [aws_security_group.service.id]

    storage_info {
      ebs_storage_info { volume_size = 50 }
    }
  }

  encryption_info {
    encryption_in_transit { client_broker = "TLS" }
  }

  tags = local.tags
}

resource "aws_ecr_repository" "api" {
  name                 = "${var.name}-api"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration { scan_on_push = true }
  tags = local.tags
}

resource "aws_secretsmanager_secret" "db" {
  name = "${var.name}/db"
  tags = local.tags
}

locals {
  tags = {
    Project     = var.name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
