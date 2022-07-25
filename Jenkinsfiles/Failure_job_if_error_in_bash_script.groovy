pipeline {
    agent any
environment {
    SCRIPT = '/admin/proj/text.sh'
    PARAM = 'uuuuu'
    NOT_PASS = 'fedcsdfcsjeirf'
    comm_status = '$?'
}

options {
    ansiColor('xterm')
  }

    stages {   
                stage('message1') {
            steps {
                script{
                    sh "echo 'i am work!'"
                }
                
            }
        }

        stage('Docker Login') {
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
                                                            transfers: [sshTransfer(  execCommand: """
                                                            docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD} docker.icdc.io
                                                            if [[ ${comm_status} -ne 0 ]]; then exit error; fi
                                                            ${SCRIPT} ${PARAM} ;
                                                                    """
                                                            )]
                                                    )
                                                ])                                     
                                            }
                        if (currentBuild.result == 'FAILURE') {
                            error('Script Failure')
                        }
                        deleteDir()
                    } catch (Exception e) {
                                echo 'Job is failed!'
                                deleteDir()
                                continuePipeline = false
                                currentBuild.result = 'FAILURE'
                                error("Aborting the build.")
                        }   
                }
            }
        }

        stage('message2') {
            steps {
                script{
                    sh "echo 'i am work!'"
                }
                
            }
        }
    }
}
