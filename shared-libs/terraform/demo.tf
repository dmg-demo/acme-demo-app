# Demo infrastructure — intentionally misconfigured for Frogbot IaC scanning demo.
# DO NOT deploy to a real environment.

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

# IaC DEMO: S3 bucket with public-read ACL
# Checkov: CKV_AWS_20 — S3 bucket ACL allows public READ access
resource "aws_s3_bucket" "demo_data" {
  bucket = "acme-demo-public-data"
  acl    = "public-read"
}

# IaC DEMO: S3 bucket with no versioning enabled
# Checkov: CKV_AWS_21 — versioning not enabled
resource "aws_s3_bucket" "demo_logs" {
  bucket = "acme-demo-logs"
}

# IaC DEMO: Security group with unrestricted SSH and database ingress
# Checkov: CKV_AWS_25 — port 22 open to 0.0.0.0/0
# Checkov: CKV_AWS_24 — port 3306 open to 0.0.0.0/0
resource "aws_security_group" "demo_sg" {
  name        = "demo-wide-open"
  description = "Demo security group — not for production"

  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "MySQL from anywhere"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
