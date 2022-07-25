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

        stage('Docker build') {
            steps {
                script {
                        try {
                            sh 'pwd'
                            sh 'docker build -t django:0.0.${BUILD_NUMBER} .'
                            sh 'docker image save --output=django-app_${BUILD_NUMBER}.tar django:0.0.${BUILD_NUMBER} '
                        }   catch (error) {
                                echo 'Job is failed!'
                                deleteDir()
                                continuePipeline = false
                                currentBuild.result = 'FAILURE'
                            }   
                }
            }
        }

        stage('Docker image load') {
            steps {
                script {
                    try {
                    withCredentials([usernamePassword(credentialsId: 'admin_vm2', passwordVariable: 'PASSWORD', usernameVariable: 'USR')]){
                        sshPublisher(
                        continueOnError: false, failOnError: true,
                        publishers: [
                    sshPublisherDesc(
                        verbose: true,
                        configName: 'lol',
                        sshCredentials: [encryptedPassphrase: "$PASSWORD", username: "$USR"],
                        transfers: [sshTransfer(sourceFiles: '*.tar',
                                            removePrefix: '',
                                            remoteDirectory: "/home/admin/prod/dj_docker_app",
                                            execCommand: """
                                            pwd
                                            cd /home/admin/prod/dj_docker_app
                                            pwd
                                            docker image load < django-app_${BUILD_NUMBER}.tar ;
                                            """)]
                        )
                       ])
                    }
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
                                            docker stop django_app
                                            docker container prune -f 
                                            docker image prune -f -a --filter "until=48h"
                                            docker run --name django_app -d -p 7777:8000 django:0.0.${BUILD_NUMBER} ;
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
         stage ('Archivate and del old files') {
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
                                            cd /home/admin/prod/dj_docker_app
                                            gzip django-app_${BUILD_NUMBER}.tar
                                            find . -mtime +1 -delete;
                                           """)]
                            )
                       ])
                    }
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