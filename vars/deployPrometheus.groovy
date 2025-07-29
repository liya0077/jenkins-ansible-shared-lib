def call(Map config) {
    pipeline {
        agent any

        environment {
            SLACK_CHANNEL        = config.SLACK_CHANNEL_NAME ?: 'jenkins-neuroninja'
            ENVIRONMENT          = config.ENVIRONMENT ?: 'dev'
            ACTION_MESSAGE       = config.ACTION_MESSAGE ?: "Deploying Prometheus to ${ENVIRONMENT}"
            CODE_BASE_PATH       = config.CODE_BASE_PATH ?: 'Ansible-Prometheus-Install/Ansible-Prometheus-install'
            KEEP_APPROVAL_STAGE  = config.KEEP_APPROVAL_STAGE ?: 'false'
        }

        stages {
            stage('Clone Repo') {
                steps {
                    git 'https://github.com/liya0077/Ansible-Prometheus-Install.git'
                }
            }

            stage('User Approval') {
                when {
                    expression { return KEEP_APPROVAL_STAGE.toBoolean() }
                }
                steps {
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: "Do you want to proceed with Prometheus deployment to ${ENVIRONMENT}?"
                    }
                }
            }

            stage('Run Ansible Playbook') {
                steps {
                    dir('Ansible-Prometheus-Install/Ansible-Prometheus-install') {
                        script {
                            sh '''
                            # Create venv if not exists
                            if [ ! -d "venv" ]; then
                              echo "✅ Creating virtual environment..."
                              python3 -m venv venv
                            fi

                            echo "✅ Activating virtual environment..."
                            source venv/bin/activate

                            echo "✅ Installing dependencies..."
                            pip install --upgrade pip
                            pip install ansible boto boto3

                            echo "✅ Running Ansible Playbook..."
                            ansible-playbook -i inventory.aws_ec2.yml site.yml --extra-vars "env=${ENVIRONMENT}"
                            '''
                        }
                    }
                }
            }

            stage('Notify Slack') {
                steps {
                    slackSend(channel: "${SLACK_CHANNEL}", message: "${ACTION_MESSAGE} ✅ completed on ${ENVIRONMENT}")
                }
            }
        }
    }
}
