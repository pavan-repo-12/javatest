pipeline {
    agent {
        kubernetes {
            label 'jenkins-agent1'
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    jenkins-agent1: "true"
spec:
  serviceAccountName: jenkins
  volumes:
    - name: workspace-volume
      emptyDir: {}
    - name: docker-sock
      hostPath:
        path: /var/run/docker.sock
  containers:
    - name: jnlp
      image: jenkins/inbound-agent:latest
      workingDir: /home/jenkins/agent
      volumeMounts:
        - name: workspace-volume
          mountPath: /home/jenkins/agent
    - name: maven
      image: maven:3.9.3-eclipse-temurin-17
      command: ['cat']
      tty: true
      volumeMounts:
        - name: workspace-volume
          mountPath: /home/jenkins/agent
    - name: helm
      image: alpine/helm:3.12.0
      command: ['cat']
      tty: true
      volumeMounts:
        - name: workspace-volume
          mountPath: /home/jenkins/agent
    - name: docker
      image: docker:25
      command: ['cat']
      tty: true
      volumeMounts:
        - name: workspace-volume
          mountPath: /home/jenkins/agent
        - name: docker-sock
          mountPath: /var/run/docker.sock
  restartPolicy: Never
"""
        }
    }

    environment {
        IMAGE_NAME = "pavan0411199/java-test"
        K8S_DEPLOYMENT_FILE = "k8s/deployment.yaml"
        HELMVALUES= "values-javadev2.yaml"
        K8S_SERVICE_FILE = "k8s/service.yaml"
        GIT_CREDENTIALS_ID = "gitclasic2"
        GIT_BRANCH = "feature/javajenkins2"
        CHARTNAME = "javadev2"
        HELM_NAMESPACE = "javadev2"

        SONAR_TOKEN = credentials('sonar_access_token')   // Jenkins credential for Sonar token
    }

    stages {

        stage('Checkout Code') {
            steps {
                container('jnlp') {
                    git branch: "${env.GIT_BRANCH}",
                        url: "https://github.com/pavan-repo-12/javajenkins.git",
                        credentialsId: "${env.GIT_CREDENTIALS_ID}"
                }
            }
        }

        stage('Build with Maven') {
            steps {
                container('maven') {https://github.com/pavan-repo-12/javatest.git
                    sh 'mvn clean package'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                container('maven') {
                    withSonarQubeEnv('SonarQubeServer') {
                        sh '''
                        mvn sonar:sonar \
                          -Dsonar.projectKey=javatesting \
                          -Dsonar.projectName=javatesting \
                          -Dsonar.host.url=http://192.168.1.103:9000 \
                          -Dsonar.token=$SONAR_TOKEN \
                          -Dsonar.qualitygate.wait=false
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    echo "Waiting for SonarQube Quality Gate result..."
                    timeout(time: 1, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        echo "Quality Gate Status: ${qg.status}"
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to Quality Gate failure"
                        }
                    }
                }
            }
        }

        stage('Archive JAR') {
            steps {
                container('maven') {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

       
        stage('Build & push  Docker Image') {
            steps {
                container('docker') {
                    script {
                        def imageTag = env.BUILD_NUMBER
                        def fullImageName = "${env.IMAGE_NAME}:${imageTag}"
                        withCredentials([usernamePassword(
                            credentialsId: 'dockerhubpass',
                            usernameVariable: 'DOCKER_HUB_USR',
                            passwordVariable: 'DOCKER_HUB_PSW'
                        )]) {
                            sh """
                                echo \$DOCKER_HUB_PSW | docker login -u \$DOCKER_HUB_USR --password-stdin
                                docker build -t ${fullImageName} .
                                docker push ${fullImageName}
                            """
                        }
                        env.IMAGE_NAME_FULL = fullImageName
                    }
                }
            }
        }

        stage('Update Deployment YAML') {
            steps {
                container('jnlp') {
                    sh """
                          ls -l 
                          ls -l javatest-chart/
                          sed -i "s|tag:.*|tag:  ${env.BUILD_NUMBER} |g" javatest-chart/${env.HELMVALUES}
                    
                    """
                }
            }
        }

        stage('Commit & Push YAML') {
            steps {
                container('jnlp') {
                    withCredentials([usernamePassword(
                        credentialsId: 'gitclasic2',
                        usernameVariable: 'GIT_USERNAME',
                        passwordVariable: 'GIT_TOKEN'
                    )]) {
                        sh """
                            git config user.name "jenkins"
                            git config user.email "jenkins@ci"

                            git add javatest-chart/${env.HELMVALUES}
                            git commit -m "Update deployment image to ${IMAGE_NAME_FULL} [ci skip]" || echo "No changes to commit"

                            git push https://pavan-repo-12:${GIT_TOKEN}@github.com/pavan-repo-12/javajenkins.git feature/javajenkins2
                        """
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                container('helm') {
                    sh """
                         helm upgrade --install ${env.CHARTNAME} ./javatest-chart   -f javatest-chart/${env.HELMVALUES}    --namespace ${env.HELM_NAMESPACE}  --create-namespace
                    """
                }
            }
        }

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