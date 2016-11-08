output "aws_region" {
  value = "${var.aws_region}"
}

output "aws_access_key" {
  value = "${var.aws_access_key}"
}

output "port" {
  value = "${var.port}"
}

output "artifact_path" {
  value = "${var.artifact_path}"
}

output "ip" {
    value = "${join(",", aws_instance.websrv.*.public_ip)}"
}

output "key_pair_name" {
  value = "${var.key_pair_name}"
}

output "aws_subnet" {
  value = ["${aws_subnet.wasabi-setup.*.id}"]
}