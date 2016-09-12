variable "aws_access_key" {
  description = "aws access key"
  //default = ""
}

variable "aws_secret_key" {
  description = "aws secret key"
  //default = ""
}

variable "aws_region" {
  description = "aws region for your network"
  default = "us-west-2"
}

variable "aws_vpc_cidr" {
  default = "10.0.0.0/16"
}

variable "aws_subnet_cidr" {
  default = "10.10.10.0/24"
}

variable "aws_gw_cidr" {
  default = "0.0.0.0/0"
}

variable "aws_amis" {
  default = {
  us-west-2 = "ami-6f69a40f"
  }
}

variable "artifact_path" {
  description = "provide local wasabi artifact absolute source path"
  //default = "~/WORK/wasabi/target"
}

variable "instance_type" {
  default = "m4.4xlarge"
}
variable "key_pair_name" {
  description = "aws key pair name"
  aws_key_pair = "wasabi-setup"
  default = "wasabi-key"
}

variable "port" {
  description = "port on wasabi that listen for http request"
  port = 8080
  default = "8080"
}
