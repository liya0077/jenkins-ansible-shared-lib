def call(Map config = [:]) {
    def ACTION              = config.ACTION ?: 'apply'
    def SLACK_CHANNEL       = config.SLACK_CHANNEL_NAME ?: 'jenkins-neuroninja'
    def ENVIRONMENT         = config.ENVIRONMENT ?: 'dev'
    def ANSIBLE_REPO_PATH   = config.ANSIBLE_REPO_PATH ?: 'Ansible-Prometheus-Install'
    def TERRAFORM_REPO_PATH = config.TERRAFORM_REPO_PATH ?: 'terraform-prometheus-infra'

    if (ACTION == 'apply') {
        stage('Clone Terraform Repo') {
            dir(TERRAFORM_REPO_PATH) {
                git branch: 'main', url: 'https://github.com/liya0077/terraform-prometheus-infra.git'
            }
        }

        stage('Terraform Apply') {
            dir(TERRAFORM_REPO_PATH) {
                sh """
                    terraform init
                    terraform plan -out=tfplan
                    terraform apply -auto-approve tfplan
                """
            }
        }

        stage('Waiting for instance to be ready') {
            sh '''
                echo "Waiting for instances to be ready..."
                sleep 60
            '''
        }

        stage('Clone Ansible Repo') {
            dir(ANSIBLE_REPO_PATH) {
                git branch: 'main', url: 'https://github.com/liya0077/Ansible-Prometheus-Install.git'
            }
        }

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

    } else if (ACTION == 'destroy') {
        stage('Clone Terraform Repo') {
            dir(TERRAFORM_REPO_PATH) {
                git branch: 'main', url: 'https://github.com/liya0077/terraform-prometheus-infra.git'
            }
        }

        stage('Terraform Destroy') {
            dir(TERRAFORM_REPO_PATH) {
                sh """
                    terraform init
                    terraform destroy -auto-approve
                """
            }
        }
    }
}
