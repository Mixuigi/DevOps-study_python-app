pipeline {
    agent any

    stages {
        stage('Clone branch') {
            steps {
                echo "Clonning django_app"
                git branch: 'django_app',
                    credentialsId: 'host_id',
                    url: 'git@git.icdc.io:Paulovich/road-to-devops.git'
            }
        }

        stage('Docker build&push') {
            steps {
                script {
                        try {
                            sh 'pwd'
                            sh 'docker image prune -f -a --filter "until=1h" '
                            sh 'docker build -t django:0.0.${BUILD_NUMBER} .'
                            sh 'docker tag django:0.0.${BUILD_NUMBER} 10.221.22.193:5000/django:0.0.${BUILD_NUMBER}'
                            sh 'docker push 10.221.22.193:5000/django:0.0.${BUILD_NUMBER}'
                        }   catch (error) {
                                echo 'Job is failed!'
                                deleteDir()
                                continuePipeline = false
                                currentBuild.result = 'FAILURE'
                            }   
                }
            }
        }
        
        stage ('Run server') {
            steps([$class: 'BapSshPromotionPublisherPlugin']) {
                script {
                    try {
                    last_started = env.STAGE_NAME
                    withCredentials([usernamePassword(credentialsId: 'admin_vm2', passwordVariable: 'PASSWORD', usernameVariable: 'USR')]) {
                    sshPublisher(
                        continueOnError: false, failOnError: true,
                        publishers: [
                    sshPublisherDesc(
                        verbose: true,
                        configName: 'lol',
                        sshCredentials: [encryptedPassphrase: "$PASSWORD", username: "$USR"],
                        transfers: [sshTransfer(
                                           execCommand: """
                                            docker pull 10.221.22.193:5000/django:0.0.${BUILD_NUMBER}
                                            docker stop django_app
                                            docker container prune -f 
                                            docker image prune -f -a --filter "until=1h"
                                            docker run --name django_app -d -p 7777:8000 10.221.22.193:5000/django:0.0.${BUILD_NUMBER} ;
                                           """)]
                            )
                       ])
                    }
                    deleteDir()
                    }   catch (error) {
                                echo 'Job is failed!'
                                deleteDir()
                                continuePipeline = false
                                currentBuild.result = 'FAILURE'
                            }
                }
                
            }
        }
    }
}
