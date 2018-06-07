#!groovy
@Library("Reform")
import uk.gov.hmcts.Ansible
import uk.gov.hmcts.Packager
import uk.gov.hmcts.RPMTagger

def packager = new Packager(this, 'cc')
def ansible = new Ansible(this, 'ccpay')
RPMTagger rpmTagger = new RPMTagger(this, 'payment-api', packager.rpmName('payment-api', params.rpmVersion), 'cc-local')

def server = Artifactory.server 'artifactory.reform'
def buildInfo = Artifactory.newBuildInfo()

properties(
    [[$class: 'GithubProjectProperty', displayName: 'Payment API', projectUrlStr: 'https://github.com/hmcts/ccpay-payment-app'],
     pipelineTriggers([[$class: 'GitHubPushTrigger']])]
)

milestone()
lock(resource: "payment-app-${env.BRANCH_NAME}", inversePrecedence: true) {
    node {
        try {
            stage('Checkout') {
                deleteDir()
                checkout scm
                dir('ansible-management') {
                  git url: "https://github.com/hmcts/ansible-management", branch: "master", credentialsId: "jenkins-public-github-api-token"
              }
            }

            stage('Build') {
                def rtGradle = Artifactory.newGradleBuild()
                rtGradle.tool = 'gradle-4.2'
                rtGradle.deployer repo: 'libs-release', server: server
                rtGradle.deployer.deployArtifacts = (env.BRANCH_NAME == 'master')
                rtGradle.run buildFile: 'build.gradle', tasks: 'clean build dependencyCheckAnalyze artifactoryPublish sonarqube', buildInfo: buildInfo
            }

            def paymentsApiDockerVersion
            def paymentsDatabaseDockerVersion

            stage('Build docker') {
                paymentsApiDockerVersion = dockerImage imageName: 'common-components/payments-api'
                paymentsDatabaseDockerVersion = dockerImage imageName: 'common-components/payments-database', context: 'docker/database'
            }

//            stage("Trigger acceptance tests") {
//                build job: '/common-components/payment-app-acceptance-tests/master', parameters: [
//                    [$class: 'StringParameterValue', name: 'paymentsApiDockerVersion', value: paymentsApiDockerVersion],
//                    [$class: 'StringParameterValue', name: 'paymentsDatabaseDockerVersion', value: paymentsDatabaseDockerVersion]
//                ]
//            }

            onMaster {
                stage('Publish JAR') {
                    server.publishBuildInfo buildInfo
                }

                def rpmVersion

                stage("Publish RPM") {
                    rpmVersion = packager.javaRPM('master', 'payment-api', '$(ls build/libs/payment-app.jar)',
                        'springboot', 'api/src/main/resources/application.properties')
                    packager.publishJavaRPM('payment-api')
                }

                stage('Deploy to Dev') {
                    ansible.runDeployPlaybook("{payment_api_version: ${rpmVersion}}", 'dev')
                    rpmTagger.tagDeploymentSuccessfulOn('dev')
                }

//                stage("Trigger smoke tests in Dev") {
//                    build job: '/common-components/payment-app-smoke-tests/master', parameters: [
//                        [$class: 'StringParameterValue', name: 'environment', value: 'dev']
//                    ]
//                    rpmTagger.tagTestingPassedOn('dev')
//                }

                stage('Deploy to Test') {
                    ansible.runDeployPlaybook("{payment_api_version: ${rpmVersion}}", 'test')
                    rpmTagger.tagDeploymentSuccessfulOn('test')
                }

//                stage("Trigger smoke tests in Test") {
//                    build job: '/common-components/payment-app-smoke-tests/master', parameters: [
//                        [$class: 'StringParameterValue', name: 'environment', value: 'test']
//                    ]
//                    rpmTagger.tagTestingPassedOn('test')
//                }
            }

            milestone()
        } catch (err) {
            notifyBuildFailure channel: '#cc-payments-tech'
            throw err
        }
    }
}
