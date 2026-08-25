// Jenkins Declarative Pipeline for com-API-Hybrid-Framework
//
// Requirements:
//   1. Jenkins with JDK 17+ and Maven installed (use tools{} names matching
//      your Jenkins "Global Tool Configuration").
//   2. A "Secret text" credential holding your gorest.co.in bearer token.
//      Create it under: Jenkins > Manage Jenkins > Credentials (Global).
//      Update the `credentialsId` below to match what you create.
//   3. The "Pipeline" plugin (default) and either the GitHub plugin/webhook
//      or Poll SCM to trigger builds.
pipeline {
    agent any

    tools {
        // Must match the JDK + Maven names configured in
        // Jenkins -> Manage Jenkins -> Tools
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        // Injected as an env var, so ConfigManager picks it up via
        // GOREST_BEARER_TOKEN and the framework never stores it in the repo.
        GOREST_BEARER_TOKEN = credentials('gorest-bearer-token')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean verify'
            }
        }
    }

    post {
        always {
            // Publish TestNG / surefire reports so they show in the job page
            junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            publishHTML(target: [
                reportDir: 'test-output',
                reportFiles: 'index.html',
                reportName: 'TestNG Report'
            ])
        }
    }
}