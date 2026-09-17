terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# ── 1. CLAVE SSH ─────────────────────────────────────────────────
resource "tls_private_key" "clave_ssh" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "key_pair" {
  key_name   = "clave-smartlogix"
  public_key = tls_private_key.clave_ssh.public_key_openssh
}

resource "local_file" "guardar_clave" {
  content         = tls_private_key.clave_ssh.private_key_pem
  filename        = "${path.module}/clave-smartlogix.pem"
  file_permission = "0400"
}

# ── 2. RED ───────────────────────────────────────────────────────
resource "aws_vpc" "vpc_smartlogix" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  tags = { Name = "vpc-smartlogix" }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.vpc_smartlogix.id
  tags   = { Name = "igw-smartlogix" }
}

resource "aws_subnet" "subred_publica" {
  vpc_id                  = aws_vpc.vpc_smartlogix.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "us-east-1a"
  map_public_ip_on_launch = true
  tags = { Name = "subred-publica-smartlogix" }
}

resource "aws_route_table" "rt_publica" {
  vpc_id = aws_vpc.vpc_smartlogix.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
  tags = { Name = "rt-publica-smartlogix" }
}

resource "aws_route_table_association" "assoc_publica" {
  subnet_id      = aws_subnet.subred_publica.id
  route_table_id = aws_route_table.rt_publica.id
}

# ── 3. SECURITY GROUP ────────────────────────────────────────────
resource "aws_security_group" "sg_smartlogix" {
  name        = "smartlogix-sg"
  description = "SmartLogix - puertos necesarios"
  vpc_id      = aws_vpc.vpc_smartlogix.id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 5173
    to_port     = 5173
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8085
    to_port     = 8085
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8761
    to_port     = 8761
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8091
    to_port     = 8094
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "sg-smartlogix" }
}

# ── 4. INSTANCIA EC2 ─────────────────────────────────────────────
resource "aws_instance" "ec2_smartlogix" {
  ami                    = "ami-00e801948462f718a"
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.subred_publica.id
  vpc_security_group_ids = [aws_security_group.sg_smartlogix.id]
  key_name               = aws_key_pair.key_pair.key_name

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = <<-USERDATA
    #!/bin/bash
    yum update -y
    yum install docker git -y
    systemctl enable docker
    systemctl start docker
    usermod -aG docker ec2-user

    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -SL https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-x86_64 \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

    cd /home/ec2-user
    git clone https://github.com/Rodrigo-ACR/SmartLogix_Cloud.git SmartLogix
    chown -R ec2-user:ec2-user SmartLogix
    cd SmartLogix
    docker compose up -d --build
  USERDATA

  tags = { Name = "ec2-smartlogix" }
}

# ── 5. IP ELÁSTICA ───────────────────────────────────────────────
resource "aws_eip" "eip_smartlogix" {
  instance = aws_instance.ec2_smartlogix.id
  domain   = "vpc"
  tags     = { Name = "eip-smartlogix" }
}

# ── 6. OUTPUTS ───────────────────────────────────────────────────
output "ip_publica" {
  value = aws_eip.eip_smartlogix.public_ip
}

output "frontend_url" {
  value = "http://${aws_eip.eip_smartlogix.public_ip}:5173"
}

output "bff_url" {
  value = "http://${aws_eip.eip_smartlogix.public_ip}:8085"
}

output "eureka_url" {
  value = "http://${aws_eip.eip_smartlogix.public_ip}:8761"
}

output "ssh_comando" {
  value = "ssh -i clave-smartlogix.pem ec2-user@${aws_eip.eip_smartlogix.public_ip}"
}
