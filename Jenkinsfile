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
        IMAGE_NAME = ''
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
                    def repoName = sh(script: 'basename "$PWD"', returnStdout: true).trim()
                    env.IMAGE_NAME = "tikitakas/${repoName.replaceFirst('^microservicios-futfem-', '')}"
                    env.IMAGE_TAG = (env.BRANCH_NAME ?: "build-${env.BUILD_NUMBER}").replaceAll('[^A-Za-z0-9_.-]', '-')
                    env.IMAGE_REF = params.DOCKER_REGISTRY?.trim()
                        ? "${params.DOCKER_REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                        : "${env.IMAGE_NAME}:${env.IMAGE_TAG}"
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

        stage('Build Docker Image') {
            when {
                expression { return params.BUILD_DOCKER }
            }
            steps {
                script {
                    def registry = params.DOCKER_REGISTRY?.trim()
                    if (!registry) {
                        registry = 'ghcr.io/japvidal'
                    }
                    def imageName = env.IMAGE_NAME?.trim()
                    if (!imageName) {
                        def repoName = sh(script: 'basename "$PWD"', returnStdout: true).trim()
                        imageName = "tikitakas/${repoName.replaceFirst('^microservicios-futfem-', '')}"
                    }
                    def imageTag = (env.BRANCH_NAME ?: "build-${env.BUILD_NUMBER}").replaceAll('[^A-Za-z0-9_.-]', '-')
                    env.IMAGE_TAG = imageTag
                    def imageRef = "${registry}/${imageName}:${imageTag}"
                    env.IMAGE_REF = imageRef
                    echo "IMAGE_REF=${imageRef}"
                    writeFile file: '.docker-image-ref', text: "${imageRef}\n"
                    sh "docker build -f Dockerfile -t ${imageRef} ."
                }
            }
        }

        stage('Push Docker Image') {
            when {
                expression { return params.BUILD_DOCKER && params.PUSH_DOCKER }
            }
            steps {
                script {
                    def imageRef = readFile('.docker-image-ref').trim()
                    env.IMAGE_REF = imageRef
                    echo "PUSH_IMAGE_REF=${imageRef}"
                    withCredentials([
                        usernamePassword(
                            credentialsId: params.DOCKER_CREDENTIALS_ID,
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) {
                        sh """
                            set +x
                            if [ -n "${params.DOCKER_REGISTRY?.trim()}" ]; then
                              echo "\${DOCKER_PASSWORD}" | docker login "${params.DOCKER_REGISTRY?.trim()}" -u "\${DOCKER_USERNAME}" --password-stdin
                            else
                              echo "\${DOCKER_PASSWORD}" | docker login ghcr.io/japvidal -u "\${DOCKER_USERNAME}" --password-stdin
                            fi
                            docker push "${imageRef}"
                        """
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
