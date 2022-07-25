pipeline {
    agent any

    stages {
        stage('Clone_branch') {
            steps {
                echo "Clonning django_app"
                git branch: 'django_app',
                    credentialsId: 'host_id',
                    url: 'git@git.icdc.io:Paulovich/road-to-devops.git'
            }
        }

        stage ('Copy files...') {
            steps([$class: 'BapSshPromotionPublisherPlugin']) {
                script {
                    last_started = env.STAGE_NAME
                    withCredentials([usernamePassword(credentialsId: 'admin_vm2', passwordVariable: 'PASSWORD', usernameVariable: 'USR')]) {
                    sshPublisher(
                        continueOnError: false, failOnError: true,
                        publishers: [
                    sshPublisherDesc(
                        verbose: true,
                        configName: 'lol',
                        sshCredentials: [encryptedPassphrase: "$PASSWORD", username: "$USR"],
                        transfers: [sshTransfer(sourceFiles: 'IBA-empty-project/**',
                                           removePrefix: 'IBA-empty-project/',
                                           remoteDirectory: "/home/admin/prod/prod_project",
                                           execCommand: """
                                             pwd && ls -ahl ;
                                           """)]
                            )
                       ])
                    }
                }
            }
        }

        stage ('install requirements') {
            steps([$class: 'BapSshPromotionPublisherPlugin']) {
                script {
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
                                             source /home/admin/prod/env/bin/activate 
                                             cd /home/admin/prod/prod_project
                                             pip3 install -r requirements.txt 
                                             /home/admin/prod/env/bin/python3 /home/admin/prod/prod_project/manage.py makemigrations 
                                             python3 /home/admin/prod/prod_project/manage.py migrate ;
                                           """)]
                            )
                       ])
                    }
                }
            }
        }

        stage ('run server') {
            steps([$class: 'BapSshPromotionPublisherPlugin']) {
                script {
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
                                            sudo systemctl stop django.service
                                            sudo systemctl start django.service 
                                            echo "server running on 10.221.23.214:7777" ;
                                           """)]
                            )
                       ])
                    }
                }
            }
        }
    }
}