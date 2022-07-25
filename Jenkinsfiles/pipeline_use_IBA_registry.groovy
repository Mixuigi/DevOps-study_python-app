def image = "docker.icdc.io/dev-team/django:0.0.${BUILD_NUMBER}"
def registry = "http://docker.icdc.io"
def OLD_BUILD_NUMBER = (BUILD_NUMBER as int) - 1

def old_image_for_IBA = "docker.icdc.io/dev-team/django:0.0.${OLD_BUILD_NUMBER}"

def imageIBA_for_find = "docker.icdc.io/dev-team/django"

pipeline {
    agent any

    environment{
        arg1 = '{print $1}'
        arg3 = '{print $3}'
        }

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
                            // sh '''docker images -a | grep docker.icdc.io/dev-team/django | awk '{print $3}' | xargs docker rmi'''
                            dockerImageDjango = docker.build image
                    }   catch (error) {
                                echo 'Job is failed!'
                                deleteDir()
                                continuePipeline = false
                                currentBuild.result = 'ABORTED'
                                error("Aborting the build.")
                        }   
                }
            }
        }



        stage('Docker push') {
            steps {
                script {
                    try {
                         docker.withRegistry(registry, 'docker_cred') {
                                djangoImage = docker.image(image)
                                djangoImage.push()
                                }
                                //вместо image.push пишем sh команду.
                            // withDockerRegistry(credentialsId: 'docker_cred', url: 'http://docker.icdc.io') {image.push()}
                    }   catch (error) {
                                echo 'Job is failed!'
                                deleteDir()
                                continuePipeline = false
                                currentBuild.result = 'ABORTED'
                                error("Aborting the build.")
                        }   
                }
            }
        }
        
        stage('Docker login&Run') {
            steps {
                script {
                    try {
                        withCredentials([
                                        usernamePassword(credentialsId: 'admin_vm2', passwordVariable: 'PASSWORD', usernameVariable: 'USR'),
                                        usernamePassword(credentialsId: 'docker_cred', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USER') ]) {
                                sshPublisher(
                                            continueOnError: false, failOnError: true,
                                            publishers: [
                                    sshPublisherDesc(
                                            verbose: true,
                                            configName: 'lol',
                                            sshCredentials: [encryptedPassphrase: "$PASSWORD", username: "$USR"],
                                            transfers: [sshTransfer(  execCommand:""" 
                                                                docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD} docker.icdc.io
                                                                docker stop django_app
                                                                docker ps -a | grep django_app | awk '${arg1}' | xargs docker rm
                                                                docker images -a | grep ${imageIBA_for_find} | grep -v **0.0.${OLD_BUILD_NUMBER} | awk '${arg3}' | xargs docker rmi -f
                                                                docker run --name django_app -d -p 8888:8000 ${image} ;
                                                            """
                                                        )]
                                    )
                                ])                                     
                        }
                        deleteDir()
                    }   catch (error) {
                                echo 'Job is failed!'
                                deleteDir()
                                continuePipeline = false
                                currentBuild.result = 'ABORTED'
                                error("Aborting the build.")
                        }   
                }
            }
        }


    }
}


        // buildStage = 'Docker Build'
        // stage ('Docker Build') { 
        // withDockerRegistry(credentialsId: 'ibaArtifactory', url: 'https://docker.icdc.io') {
        //     sh '''

        //                 cd /home/whr-dir-v2/$repo_name \
        //                 &&  unset PREDICT_VERSION \
        //                 && export PREDICT_VERSION=cat setup.py | grep version= | awk -F'=' -F'"' '{print $2}' \
        //                 && unset IMAGE_NAME \
        //                 && export IMAGE_NAME=cat Dockerfile | grep '###' | awk  -F'#'  '{print $4}' \
        //                 && docker build -t ${IMAGE_NAME}:${PREDICT_VERSION} .
        //         '''        
        //     }
        // }