/* setup data source for user data upload */
# https://www.terraform.io/docs/configuration/interpolation.html
data "template_file" "user_data" {
    template = "${file("${path.module}/user-data.sh")}"
    vars {
    port = "${var.port}"
  }
}

/* setup provider, keys & region */
/* provider defines the configuration for AWS */

provider "aws" {
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
  region = "${var.aws_region}"
}

/* resources being created in AWS */

resource "aws_vpc" "wasabi-setup" {
  cidr_block = "${var.aws_vpc_cidr}"
  enable_dns_hostnames = true
  tags { Name = "wasabi-setup"}
}

resource "aws_internet_gateway" "wasabi-setup" {
  vpc_id = "${aws_vpc.wasabi-setup.id}"
  tags { Name = "wasabi-setup" }
}

resource "aws_subnet" "wasabi-setup" {
  vpc_id  = "${aws_vpc.wasabi-setup.id}"
  cidr_block = "${var.aws_vpc_cidr}"
  tags    = { Name = "wasabi-setup"}
  map_public_ip_on_launch = true
}

resource "aws_route_table" "wasabi-setup" {
  vpc_id  = "${aws_vpc.wasabi-setup.id}"

  route {
    cidr_block  = "${var.aws_gw_cidr}"
    gateway_id  = "${aws_internet_gateway.wasabi-setup.id}"
  }
  tags  = { Name  = "wasabi-setup"}
}

resource "aws_route_table_association" "wasabi-setup" {
  subnet_id = "${aws_subnet.wasabi-setup.id}"
  route_table_id  = "${aws_route_table.wasabi-setup.id}"
}

# security group setup
resource "aws_security_group" "wasabi-setup" {
  name  = "wasabi-setup-web"
  vpc_id  = "${aws_vpc.wasabi-setup.id}"
# ingress access for ssh from anywhere
  ingress {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22
    cidr_blocks  = ["0.0.0.0/0"]
  }
  
  ingress {
    protocol  = "tcp"
    from_port = 8080
    to_port   = 8080
    cidr_blocks  = ["0.0.0.0/0"]
  }

  ingress {
    protocol  = "tcp"
    from_port = 80
    to_port   = 80
    cidr_blocks  = ["0.0.0.0/0"]
  }
# outbound internet access
  egress {
    protocol  = -1
    from_port = 0
    to_port   = 0
    cidr_blocks  = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "websrv" {
  count = 1
  ami = "${lookup(var.aws_amis, var.aws_region)}"
  instance_type = "${var.instance_type}"
  subnet_id = "${aws_subnet.wasabi-setup.id}"
  user_data = "${data.template_file.user_data.rendered}"
  key_name = "${var.key_pair_name}"
  vpc_security_group_ids = ["${aws_security_group.wasabi-setup.id}"]
  tags = { Name = "websrv-${count.index}" }

connection {
user = "ec2-user"
private_key = "${file("~/.ssh/wasabi-key")}"
  }

  provisioner "file" {
    source = "${var.artifact_path}"
    destination = "/tmp"
  }

  provisioner "file" {
    source      = "${path.module}/index.html"
    destination = "/tmp/index.html"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo mv /tmp/index.html /var/www/html/index.html",
      "sudo chmod 655 /var/www/html/index.html",
      "sleep 1"
    ]
  }
}

