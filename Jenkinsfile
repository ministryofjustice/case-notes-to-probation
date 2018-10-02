def prepare_env() {
    sh '''
    #!/usr/env/bin bash
    docker pull mojdigitalstudio/hmpps-oraclejdk-builder:latest
    '''
}

pipeline {

    agent { label "jenkins_slave" }

    options {
        ansiColor('xterm')
    }

    stages {

        stage('Setup') {
            steps {
                prepare_env()
            }
        }

        stage('Build') {
            steps {
                sh "docker run --rm -v `pwd`:/home/tools/data mojdigitalstudio/hmpps-oraclejdk-builder:latest bash -c 'sbt test:compile && sbt clean test && sbt assembly'"
            }
        }

        stage ('Package') {
            steps {
                sh 'docker build -t 895523100917.dkr.ecr.eu-west-2.amazonaws.com/hmpps/case-notes-poll-push:latest --file ./Dockerfile .'
                sh 'aws ecr get-login --no-include-email --region eu-west-2 | source /dev/stdin'
                sh 'docker push 895523100917.dkr.ecr.eu-west-2.amazonaws.com/hmpps/case-notes-poll-push:latest'
            }
        }

    }

    post {
        always {
            deleteDir()
        }
    }

}
