pipeline {
    agent any

    stages {
        stage('build') {
            steps {
                echo 'build app'
            }
        }
        }
    post
    {
        always
        {
            emailext body: 'summary', subject: 'pipeline status', to: 'adithya8112002@gmail.com'
        }
    }
}
