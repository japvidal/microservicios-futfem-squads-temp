pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    parameters {
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Ejecuta tests Maven')
        booleanParam(name: 'BUILD_DOCKER', defaultValue: false, description: 'Construye imagen Docker si existe Dockerfile')
    }

    environment {
        APP_NAME = ''
        DOCKERFILE_PATH = ''
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Project') {
            steps {
                script {
                    env.APP_NAME = sh(script: 'basename "$PWD"', returnStdout: true).trim()
                }
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw -B clean install -DskipTests'
            }
        }

        stage('Test') {
            when {
                expression { return params.RUN_TESTS }
            }
            steps {
                sh './mvnw -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Detect Dockerfile') {
            steps {
                script {
                    if (fileExists('Dockerfile')) {
                        env.DOCKERFILE_PATH = 'Dockerfile'
                    } else {
                        env.DOCKERFILE_PATH = sh(
                            script: 'ls *.Dockerfile 2>/dev/null | head -n 1 || true',
                            returnStdout: true
                        ).trim()
                    }
                }
            }
        }

        stage('Build Docker Image') {
            when {
                expression { return params.BUILD_DOCKER && env.DOCKERFILE_PATH?.trim() }
            }
            steps {
                sh "docker build -f ${env.DOCKERFILE_PATH} -t ${env.APP_NAME}:${env.BUILD_NUMBER} ."
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true, fingerprint: true
        }
    }
}
