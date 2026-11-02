# Variables del proyecto SmartLogix
variable "region" {
  description = "Región AWS"
  default     = "us-east-1"
}

variable "instance_type" {
  description = "Tipo de instancia EC2"
  default     = "t3.large"
}

variable "root_volume_size" {
  description = "Tamano del disco raiz en GB (imagenes Docker de ~24 contenedores + logs de Kafka)"
  type        = number
  default     = 40
}
