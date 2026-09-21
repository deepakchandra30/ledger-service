variable "name" {
  description = "Resource name prefix"
  type        = string
  default     = "ledger"
}

variable "environment" {
  description = "dev, staging or prod"
  type        = string
  default     = "dev"
}

variable "region" {
  type    = string
  default = "eu-west-1"
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "db_instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "db_username" {
  type      = string
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "kafka_brokers" {
  type    = number
  default = 2
}

variable "kafka_instance_type" {
  type    = string
  default = "kafka.t3.small"
}
