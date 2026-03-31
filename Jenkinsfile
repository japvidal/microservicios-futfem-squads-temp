pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    parameters {
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Ejecuta tests Maven')
        booleanParam(name: 'BUILD_DOCKER', defaultValue: true, description: 'Construye imagen Docker si existe Dockerfile')
        booleanParam(name: 'PUSH_DOCKER', defaultValue: true, description: 'Publica la imagen Docker al registry configurado')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'ghcr.io/japvidal', description: 'Registry Docker opcional, por ejemplo ghcr.io/japvidal')
        string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: 'ghcr-japvidal', description: 'Credencial Jenkins para docker login')
    }

    environment {
        APP_NAME = ''
        DOCKERFILE_PATH = ''
        IMAGE_TAG = ''
        IMAGE_REF = ''
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
                    env.IMAGE_TAG = (env.BRANCH_NAME ?: "build-${env.BUILD_NUMBER}").replaceAll('[^A-Za-z0-9_.-]', '-')
                    env.IMAGE_REF = params.DOCKER_REGISTRY?.trim()
                        ? "${params.DOCKER_REGISTRY}/tikitakas/${env.APP_NAME}:${env.IMAGE_TAG}"
                        : "tikitakas/${env.APP_NAME}:${env.IMAGE_TAG}"
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
                sh "docker build -f ${env.DOCKERFILE_PATH} -t ${env.IMAGE_REF} ."
            }
        }

        stage('Push Docker Image') {
            when {
                expression { return params.BUILD_DOCKER && params.PUSH_DOCKER && env.DOCKERFILE_PATH?.trim() }
            }
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: params.DOCKER_CREDENTIALS_ID,
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) {
                        sh '''
                            set +x
                            if [ -n "${DOCKER_REGISTRY}" ]; then
                              echo "${DOCKER_PASSWORD}" | docker login "${DOCKER_REGISTRY}" -u "${DOCKER_USERNAME}" --password-stdin
                            fi
                            docker push "${IMAGE_REF}"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true, fingerprint: true
        }
    }
}
