def call(Map config = [:]) {
    // Set defaults using Groovy
    def SLACK_CHANNEL       = config.SLACK_CHANNEL_NAME ?: 'jenkins-neuroninja'
    def ENVIRONMENT         = config.ENVIRONMENT ?: 'dev'
    def ACTION_MESSAGE      = config.ACTION_MESSAGE ?: "Deploying Prometheus to ${ENVIRONMENT}"
    def CODE_BASE_PATH      = config.CODE_BASE_PATH ?: 'Ansible-Prometheus-Install/Ansible-Prometheus-install'
    def KEEP_APPROVAL_STAGE = (config.KEEP_APPROVAL_STAGE ?: 'false').toBoolean()

    pipeline {
        agent any

        stages {
            stage('Clone Repo') {
                steps {
                    git 'https://github.com/liya0077/Ansible-Prometheus-Install.git'
                }
            }

            stage('User Approval') {
                when {
                    expression { KEEP_APPROVAL_STAGE }
                }
                steps {
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: "Do you want to proceed with Prometheus deployment to ${ENVIRONMENT}?"
                    }
                }
            }

            stage('Run Ansible Playbook') {
                steps {
                    dir(CODE_BASE_PATH) {
                        script {
                            sh """
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
                            """
                        }
                    }
                }
            }

            stage('Notify Slack') {
                steps {
                    script {
                        slackSend(channel: SLACK_CHANNEL, message: "${ACTION_MESSAGE} ✅ completed on ${ENVIRONMENT}")
                    }
                }
            }
        }
    }
}
