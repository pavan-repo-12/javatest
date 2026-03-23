pipeline {
        agent { label 'jenkinslocal' }
 

    environment {
        IMAGE_NAME = "java"                         // Base Docker image name
        K8S_DEPLOYMENT_FILE = "k8s/deployment.yaml"
        K8S_SERVICE_FILE = "k8s/service.yaml"
        GIT_CREDENTIALS_ID = "gitclassictoken"  // Jenkins GitHub credentials ID
        GIT_BRANCH = "feature/javawinlinvm"
        SSH_CREDENTIALS_ID = "linuxvmkey"
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

        stage('Build Docker Image') {
            steps {
                script {
                    def imageTag = env.BUILD_NUMBER
                    def fullImageName = "${env.IMAGE_NAME}:${imageTag}"
                    sh """
                        eval \$(minikube docker-env)
                        docker build -t ${fullImageName} .
                    """
                    env.IMAGE_NAME_FULL = fullImageName
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

        // stage('Commit & Push YAML') {
        //     steps {
        //         script {
        //             sh """
        //                 git config user.name "jenkins"
        //                 git config user.email "jenkins@ci"
        //                 git add ${env.K8S_DEPLOYMENT_FILE}
        //                 git commit -m "Update deployment image to ${env.IMAGE_NAME_FULL} [ci skip]" || echo "No changes to commit"
        //                 git push origin ${env.GIT_BRANCH}
        //             """
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

                        git push https://pavan-repo-12:${GIT_TOKEN}@github.com/pavan-repo-12/javatest.git feature/javawinlinvm
                    '''
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
        stage('Deploy to Linux') {
            steps {
                withCredentials([sshUserPrivateKey(
                    credentialsId: "${SSH_CREDENTIALS_ID}",
                    keyFileVariable: 'SSH_KEY'
                )]) {
                    sh '''
                        cp $SSH_KEY id_rsa
                        chmod 600 id_rsa

                        export ANSIBLE_HOST_KEY_CHECKING=False
                        export LANG=en_US.UTF-8
                        export LC_ALL=en_US.UTF-8

                        ansible-playbook -i ansible_linux/inventory.ini \
                        ansible_linux/deploy.yml \
                        --private-key $SSH_KEY
                    '''
                }
            }
        }

        // stage('Deploy to Windows') {
        //     steps {
        //         withCredentials([
        //             string(credentialsId: "${WIN_PASSWORD_ID}", variable: 'WIN_PASSWORD'),
        //             file(credentialsId: "${WINRM_CERT_ID}", variable: 'WIN_CERT')
        //         ]) {
        //             sh '''
        //                 export ANSIBLE_HOST_KEY_CHECKING=False
        //                 export LANG=en_US.UTF-8
        //                 export LC_ALL=en_US.UTF-8

        //                 # Copy cert locally if needed
        //                 cp $WIN_CERT win11.crt

        //                 ansible-playbook -i ansible_win/inventory.ini \
        //                 ansible_win/deploy.yml \
        //                 --extra-vars "ansible_password=$WIN_PASSWORD"
        //             '''
        //         }
        //     }
        // }


    }
    


    post {
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed."
        }
    }
}