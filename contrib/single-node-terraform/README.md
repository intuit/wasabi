

Install terraform on mac

brew doctor
brew update
brew upgrade
brew install terraform

AWS configuration

1. You need aws account credentials (access key and secret key)
2. Private key(.pem)

Using terraform for setting up single node infra in the aws

create ssh key pair and upload to the AWS account
e.g. mkdir ssh && ssh-keygen -t rsa -C "test-key" -P '' -f ssh/test-key

aws.tf will create these for you
- create VPC
- create internet gateway
- create subnet
- create routing table
- create security groups

How to execute this

You may want to export your AWS credential

export AWS_ACCESS_KEY_ID=<Your AWS Access key>

export AWS_SECRET_ACCESS_KEY=<Your AWS secret key>

Or You can use the file "terraform.vars" as well to store your AWS keys, but then for secuity reasons don't push that file to git or remote

Or

You can store keys to your local filesystem and reference them into terrafrom

How to run

terraform plan single-instance-setup 
terraform apply single-instance-setup





