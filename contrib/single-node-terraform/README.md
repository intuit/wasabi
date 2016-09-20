# Wasabi single node deployment on AWS using Terraform

## Setup

Install terraform and awscli (mac osx)

brew doctor

brew update

brew upgrade

brew install terraform

AWS configuration

1. You need aws account credentials (access key and secret key).Go to `IAM > Users >`, click on your username,

   choose `Security Credentials` and `Create Access Key`. Download it and set your awscli up:

      aws configure --profile $YOUR_ACCOUNT_PROFILE

2. Create an ssh key pair with the name `wasabi-key` and upload to the AWS account:
    
    ssh-keygen -t rsa -C "wasabi-key" -P '' -f $HOME/.ssh/wasabi-key

To upload it, go `EC2` and choose Key Pairs under `Network & Security`.

You can copy your public key from `$HOME/.ssh/wasabi-key.pub` and upload there.


## Running terraform

To make it work you will need to provide your AWS access keys. There are three ways:


1. Exporting:

    export AWS_ACCESS_KEY_ID=<Your AWS Access key>

    export AWS_SECRET_ACCESS_KEY=<Your AWS secret key>

2. Var file: [https://www.terraform.io/intro/getting-started/variables.html](https://www.terraform.io/intro/getting-started/variables.html)

3. You can enter the keys when terraform prompts.

Once your keys are set, plan the cluster configuration and apply your plans:

    terraform plan

    terraform apply

This will

- create a VPC

- create an internet gateway

- create a subnet

- create a routing table

- create security groups


It will also print information how to reach the instance.

SSH into your instance and unpack the rpm.

    ssh -i $HOME/.ssh/wasabi-key ec2-user@$INSTANCE_IP_ADDRESS

Check the logs if installation is complete:

    tail -f /var/log/user-data.log

    cd /tmp/</path/to/your/*.rpm>
    rpm -ivh </Path/to/your/rpm>

You can now access Wasabi on your single node!

## How to run wasabi

    cd /usr/local/<wasabi-main-xxxx>
    ./bin/run &


Test if wasabi is running

    curl <hostname/ip address>:8080/api/v1/ping

You can now access Wasabi on your single node!



To destroy the instance and clean up, just run

    terraform destroy


## You have no rpms?
Run `./bin/wasabi.sh package` from the wasabi root directory!
