def OLD_BUILD_NUMBER = (BUILD_NUMBER as int) - 1

def which_dockerfile(){
    if (params.DOCKERFILE == 'Default_user') {
        dockerfile = 'Dockerfile'
    } else if (params.DOCKERFILE == 'New_user') {
        dockerfile = 'Dockerfile_user'
    } 
}

properties([
    parameters([
        string(
            name: 'BRANCH',
            defaultValue: 'django_app',
            description: 'What Branch do you choose?'
        ),
        [$class: 'ChoiceParameter', choiceType: 'PT_RADIO', description: 'Choose Registry',
        filterLength: 1, filterable: false, name: 'REGISTRY',
        randomName: 'choice-parameter-1683871426502398',
        script: [$class: 'GroovyScript', fallbackScript: [classpath: [], sandbox: true, 
            script: 'return "Error"'],
            script: [classpath: [], sandbox: true,
            script: '''return[
                    \'Local\',\'IBA\']''']]],
        choice(
            name: 'SERVER',
            choices: [
                'Test',
                'Prod'
            ]
        ),
        choice(
            name: 'DOCKERFILE',
            choices: [
                'Default_user',
                'New_user'
            ]
        )
    ])
])


pipeline {
    agent any

environment{
    image_for_IBA = "docker.icdc.io/dev-team/django:0.0.${BUILD_NUMBER}"
    image_for_Local = "10.221.22.193:5000/django:0.0.${BUILD_NUMBER}"

    registry = "http://docker.icdc.io"

    old_image_for_IBA = "docker.icdc.io/dev-team/django:0.0.${OLD_BUILD_NUMBER}"
    old_image_for_Local = "10.221.22.193:5000/django:0.0.${OLD_BUILD_NUMBER}"

    imageIBA_for_find = "docker.icdc.io/dev-team/django"
    imageLocal_for_find = "10.221.22.193:5000/django"

    arg1 = '{print $1}'
    arg3 = '{print $3}'

    IMAGE = ''
    OLD_IMAGE = ''
    FIND_IMAGE = ''
    PORT = ''
    CRED = ''
    dockerfile = ''
}

options {
    ansiColor('xterm')
  }

    stages {
        stage('Clone branch') {
            steps {
                echo "========= BRANCH IS \033[31m${params.BRANCH}\033[0m ========="
                script{
                    if (params.REGISTRY == 'Local'){
                        CRED = 'git-access-token'
                    } else if (params.REGISTRY == 'IBA') {
                        CRED = 'git_IBA'
                    } 
                }
                echo "Clonning django_app"
                git branch: 'django_app',
                    credentialsId: CRED ,
                    url: 'https://git.icdc.io/Paulovich/road-to-devops.git'
                
            }
        }

        stage('Docker Build&Push') {
            steps {
                echo "========= PUSHING ON \033[35m${params.REGISTRY}\033[0m REGISTRY ========="
                which_dockerfile()
                script {
                    try {  
                            //more options
                            // withDockerRegistry(credentialsId: 'docker_cred', url: 'http://docker.icdc.io') {<more docker commands>} 
                            //or:
                            // dockerImageDjango = docker.build image_for_IBA
                            // docker.withRegistry(registry, 'docker_cred') {
                            //     djangoImage = docker.image(image_for_IBA)
                            //     djangoImage.push()
                            //     }
                        
                        if (params.REGISTRY == 'Local') {
                            sh "docker build -t ${image_for_Local} -f ${dockerfile} ."
                            sh "docker push ${image_for_Local} "

                            IMAGE = image_for_Local
                            OLD_IMAGE = old_image_for_Local
                            FIND_IMAGE = imageLocal_for_find

                        } else if (params.REGISTRY == 'IBA') {
                            sh "docker build -t ${image_for_IBA} -f ${dockerfile} ."
                            sh "docker push ${image_for_IBA} "
                            
                            IMAGE = image_for_IBA
                            OLD_IMAGE = old_image_for_IBA
                            FIND_IMAGE = imageIBA_for_find
                        }
                            // for delete image in jenkins server
                            // sh '''docker images -a | grep docker.icdc.io/dev-team/django | awk '{print $3}' | xargs docker rmi'''      
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
        
        stage('Docker Login&Run') {
            steps {
                echo "========= SERVER IS \033[31m${params.SERVER}\033[0m =========" 
                script {
                    try {    
                        if (params.SERVER == 'Test') {
                            PORT = 8000
                        } else if (params.SERVER == 'Prod') {
                            PORT = 7777
                                }
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
                                            transfers: [sshTransfer(  execCommand: """
                                                    docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD} docker.icdc.io 
                                                    docker stop django_app_on_${PORT}
                                                    docker ps -a | grep django_app_on_${PORT} | awk '${arg1}' | xargs docker rm
                                                    docker images -a | grep ${FIND_IMAGE} | grep -v **0.0.${OLD_BUILD_NUMBER} | awk '${arg3}' | xargs docker rmi -f
                                                    docker run --name django_app_on_${PORT} -d -p ${PORT}:8000 ${IMAGE} ;
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

    // COLORS
//     options {
//     ansiColor('xterm')
//   }
                            // echo '\033[34mHello\033[0m \033[33mcolorful\033[0m \033[35mworld!\033[0m'
                            // script{
                            //     sh "echo \033[31mHI!!!\033[0m \033[31mcolorful\033[0m \033[31mworld!\033[0m "} <- red color



                            //export a=`docker `!!!!!!!!!!!!!!!! 
                            //docker images -a | grep docker.icdc.io/dev-team/django | grep -v **0.0.${OLD_BUILD_NUMBER} | awk '{print $3}' | xargs docker rmi ;





// docker login -u ''' + DOCKER_USER + ''' -p ''' + DOCKER_PASSWORD + ''' docker.icdc.io
// docker stop django_app_on_''' + PORT + '''
// docker ps -a | grep django_app_on_''' + PORT + ''' | awk '{print $1}' | xargs docker rm
// docker images -a | grep ''' + FIND_IMAGE + ''' | grep -v **0.0.''' + OLD_BUILD_NUMBER + ''' | awk '{print $3}' | xargs docker rmi -f
// docker run --name django_app_on_''' + PORT + ''' -d -p ''' + PORT + ''':8000 ''' + IMAGE + ''' ;


