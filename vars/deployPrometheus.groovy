def call(Map config = [:]) {
    def SLACK_CHANNEL       = config.SLACK_CHANNEL_NAME ?: 'jenkins-neuroninja'
    def ENVIRONMENT         = config.ENVIRONMENT ?: 'dev'
    def ACTION_MESSAGE      = config.ACTION_MESSAGE ?: "Deploying Prometheus to ${ENVIRONMENT}"
    def ANSIBLE_REPO_PATH   = config.ANSIBLE_REPO_PATH ?: 'Ansible-Prometheus-Install'
    def TERRAFORM_REPO_PATH = config.TERRAFORM_REPO_PATH ?: 'terraform-prometheus-infra'
    def KEEP_APPROVAL_STAGE = (config.KEEP_APPROVAL_STAGE ?: 'true').toBoolean()

    // 1️⃣ Clone Terraform repo
    stage('Clone Terraform Repo') {
        dir(TERRAFORM_REPO_PATH) {
            git branch: 'main', url: 'https://github.com/liya0077/terraform-prometheus-infra.git'
        }
    }

    // 2️⃣ Build infra with Terraform
    stage('Terraform Apply') {
        dir(TERRAFORM_REPO_PATH) {
            sh """
                terraform init -reconfigure
                terraform plan -out=tfplan
                terraform apply -auto-approve tfplan
            """
        }
    }

    stage('Waiting for instace to get ready') {
            sh '''
                echo "Waiting for instances to be ready..."
                sleep 60
            '''
    }
    

    // 3️⃣ Clone Ansible repo
    stage('Clone Ansible Repo') {
        dir(ANSIBLE_REPO_PATH) {
            git branch: 'main', url: 'https://github.com/liya0077/Ansible-Prometheus-Install.git'
        }
    }

    // 4️⃣ Run Ansible
    stage('Run Ansible Playbook') {
        dir(ANSIBLE_REPO_PATH) {
            sh '''#!/bin/bash
                set -e
                echo "📂 Files inside the repo:"
                ls -la

                if [ ! -d "venv" ]; then
                    python3 -m venv venv
                fi

                source venv/bin/activate
                pip install --upgrade pip
                pip install ansible boto boto3

                ansible-playbook -i inventory.aws_ec2.yml site.yml --extra-vars "env=${ENVIRONMENT}"
            '''
        }
    }
}
