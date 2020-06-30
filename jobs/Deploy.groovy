properties([
    parameters(
        [
            stringParam(
                name: 'GIT_REPO',
                defaultValue: ''
            ),
            stringParam(
                name: 'VERSION',
                defaultValue: ''
            ),
            choiceParam(
                name: 'ENV',
                choices: ['test', 'staging', 'production']
            )
        ]
    )
])

pipeline {

    agent {
        kubernetes {
            label 'jenkins-agent'
            defaultContainer 'jnlp'
            yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    job: habr-demo-deploy
spec:
  containers:
  - name: git
    image: alpine/git
    command: ["cat"]
    tty: true
  - name: helm-cli
    image: ibmcom/k8s-helm:v2.6.0
    command: ["cat"]
    tty: true
"""
        }
    }

    stages {

        stage('Find deployment descriptor') {
            steps {
                container('git') {
                    script {
                        def revision = params.VERSION.substring(0, 7)
                        sh "git clone https://github.com/guanwei/${params.GIT_REPO}.git"
                        dir ("${params.GIT_REPO}") {
                            sh "git checkout ${revision}"
                        }
                    }
                }
            }
        }
        stage('Deploy to env') {
            steps {
                container('helm-cli') {
                    script {
                        dir ("${params.GIT_REPO}") {
                            sh "./helm/setRevision.sh ${params.VERSION}"
                            sh "/helm upgrade ${params.ENV}-${params.GIT_REPO.toLowerCase()} helm/ --install --namespace ${params.ENV}"
                        }
                    }
                }
            }
        }
    }
}
