pipeline {
        agent { label 'jenkinslocal' }
 

    environment {
        IMAGE_NAME = "java"                         // Base Docker image name
        K8S_DEPLOYMENT_FILE = "k8s/deployment.yaml"
        K8S_SERVICE_FILE = "k8s/service.yaml"
        GIT_CREDENTIALS_ID = "gitclassictoken"  // Jenkins GitHub credentials ID
        GIT_BRANCH = "feature/javatest"
    }

    stages {

        stage('Checkout Code') {
            steps {
                git branch: "${env.GIT_BRANCH}",
                    url: "https://github.com/pavan-repo-12/javatest.git",
                    credentialsId: "${env.GIT_CREDENTIALS_ID}"
            }
        }

        // stage('Install Maven') {
        //     steps {
        //         sh '''
        //         sudo apt-get update
        //         sudo apt-get install -y maven
        //         '''
        //     }

        // }    
    
        stage('Build with Maven') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Archive JAR') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // stage('Build Docker Image') {
        //     steps {
        //         script {
        //             def imageTag = env.BUILD_NUMBER
        //             def fullImageName = "${env.IMAGE_NAME}:${imageTag}"
        //             sh "docker build -t ${fullImageName} ."
        //             env.IMAGE_NAME_FULL = fullImageName
        //         }
        //     }
        // }
        stage('Commit & Push YAML') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'gitclassictoken',
                    usernameVariable: 'GIT_USERNAME',
                    passwordVariable: 'GIT_TOKEN'
                )]) {
                    sh '''
                        git config user.name "jenkins"
                        git config user.email "jenkins@ci"

                        git add k8s/deployment.yaml
                        git commit -m "Update deployment image to ${IMAGE_NAME_FULL} [ci skip]" || echo "No changes to commit"

                        git push https://${GIT_USERNAME}:${GIT_TOKEN}@github.com/pavan-repo-12/javatest.git feature/javatest
                    '''
                }
            }
        }

        stage('Update Deployment YAML') {
            steps {
                sh """
                    sed -i "s|image:.*|image: ${env.IMAGE_NAME_FULL}|g" ${env.K8S_DEPLOYMENT_FILE}
                """
            }
        }

        stage('Commit & Push YAML') {
            steps {
                script {
                    sh """
                        git config user.name "jenkins"
                        git config user.email "jenkins@ci"
                        git add ${env.K8S_DEPLOYMENT_FILE}
                        git commit -m "Update deployment image to ${env.IMAGE_NAME_FULL} [ci skip]" || echo "No changes to commit"
                        git push origin ${env.GIT_BRANCH}
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh """
                    kubectl apply -f ${env.K8S_DEPLOYMENT_FILE}
                    kubectl apply -f ${env.K8S_SERVICE_FILE}
                """
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline completed successfully!"
        }
        failure {
            echo "❌ Pipeline failed."
        }
    }
}