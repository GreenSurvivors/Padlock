pipeline {
    agent any
    stages {
        stage('Build') {
            tools {
                jdk "jdk25"
            }
            steps {
                sh './gradlew publish'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            cleanWs()
        }
    }
}
