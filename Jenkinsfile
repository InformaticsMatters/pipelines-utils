#!groovy​

pipeline {

    // As we may need different flavours of agent,
    // the agent definition is deferred to each stage.
    agent none

    stages {

        // --------------------------------------------------------------------
        // Python Test
        // --------------------------------------------------------------------

        stage ('Test') {

            agent {
                label 'python-slave'
            }

            steps {

                // Crete a Python2 environment and move into it.
                sh 'python3.6 -m venv python3'
                sh '. python3/bin/activate'

                sh 'pip install -r package-requirements.txt'

                dir('src/python') {
                    sh 'coverage run setup.py test'
                    sh 'coverage report'
                    sh 'pyroma .'
                    sh 'python setup.py bdist_wheel'
                }

            }

        }

    }

    // End-of-pipeline post-processing actions...
    post {

        failure {
            mail to: 'achristie@informaticsmatters.com',
            subject: 'Failed PipelineUtils Job',
            body: "Something is wrong with the Squonk CI/CD PIPELINES-UTILS build ${env.BUILD_URL}"
        }

    }

}
