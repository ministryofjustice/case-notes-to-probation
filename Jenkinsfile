def get_casenotes_version() {
    sh '''
    #!/bin/bash +x
    grep "version := " build.sbt | awk '{print $3}' | sed 's/\"//g' > ./casenotes.version
    '''
    return readFile("./casenotes.version")
}

pipeline {
    agent { label "jenkins_slave" }

    environment {
        docker_image = "hmpps/casenotes"
        aws_region = 'eu-west-2'
        ecr_repo = ''
        CASENOTES_VERSION = get_casenotes_version()
    }

    stages {
        // stage ('Notify build started') {
        //     steps {
        //         slackSend(message: "Build Started - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL.replace('http://', 'https://').replace(':8080', '')}|Open>)")
        //     }
        // }

        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "CASENOTES_VERSION = ${CASENOTES_VERSION}"
                '''
            }
        }

       stage('Verify Prerequisites') {
           steps {
               sh '''
                    echo "Testing AWS Connectivity and Credentials"
                    aws sts get-caller-identity
                    echo "Testing sbt is installed"
                    sbt -h
               '''
           }
       }

       stage('SBT Assembly') {
           steps {
                sh '''
                    sbt assembly
                '''
# TODO will need to stash the resultant artefact here and pass to docker build stage                
           }
       }

        stage('Get ECR Login') {
            steps {
                sh '''
                    #!/bin/bash +x
                    make ecr-login
                '''
                // Stash the ecr repo to save a repeat aws api call
                stash includes: './ecr.repo', name: 'ecr.repo'
            }
        }
        stage('Build Docker image') {
           steps {
# this path will need changing
                dir('./docker'){
                    unstash 'artefacts'
                }
                unstash 'ecr.repo'
                sh '''
                    #!/bin/bash +x
                    make build casenotes_version=${CASENOTES_VERSION}
                '''
            }
        }
        stage('Image Tests') {
            steps {
                // Run dgoss tests
                sh '''
                    #!/bin/bash +x
                    make test
                '''
            }
        }
        stage('Push image') {
            steps{
                unstash 'ecr.repo'
                sh '''
                    #!/bin/bash +x
                    make push casenotes_version=${CASENOTES_VERSION}
                '''
                
            }            
        }
        stage ('Remove untagged ECR images') {
            steps{
                unstash 'ecr.repo'
                sh '''
                    #!/bin/bash +x
                    make clean-remote
                '''
            }
        }
        stage('Remove Unused docker image') {
            steps{
                unstash 'ecr.repo'
                sh '''
                    #!/bin/bash +x
                    make clean-local casenotes_version=${CASENOTES_VERSION}
                '''
            }
        }
    }
    post {
        always {
            deleteDir()
        }
        // success {
        //     slackSend(message: "Build successful -${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL.replace('http://', 'https://').replace(':8080', '')}|Open>)", color: 'good')
        // }
        // failure {
        //     slackSend(message: "Build failed - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL.replace('http://', 'https://').replace(':8080', '')}|Open>)", color: 'danger')
        // }
    }
}
