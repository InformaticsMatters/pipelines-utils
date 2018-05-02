#!groovy​

pipeline {

    // As we may need different flavours of agent,
    // the agent definition is deferred to each stage.
    agent none

    stages {

        // --------------------------------------------------------------------
        // Python Tests
        // --------------------------------------------------------------------

        stage('Unit Test') {

            parallel {

                stage('Python 2.7') {

                    agent {
                        label 'python-slave'
                    }

                    steps {

                        sh 'pip install -r package-requirements.txt'

                        dir('src/python') {
                            sh 'coverage run setup.py test'
                            sh 'coverage report'
                            sh 'pyroma .'
                        }

                    }
                }

                stage('Python 3.6') {

                    agent {
                        label 'python3-slave'
                    }

                    steps {

                        sh 'pip install -r package-requirements.txt'

                        dir('src/python') {
                            sh 'coverage run setup.py test'
                            sh 'coverage report'
                            sh 'pyroma .'
                        }

                    }

                }

            }

        }

    }

    // End-of-pipeline post-processing actions...
    post {

        failure {
            mail to: 'achristie@informaticsmatters.com tdudgeon@informaticsmatters.com',
            subject: 'Failed PipelineUtils Job',
            body: "Something is wrong with the Squonk CI/CD PIPELINES-UTILS build ${env.BUILD_URL}"
        }

    }

}
