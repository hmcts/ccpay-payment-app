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
            }

            def artifactVersion = readFile('version.txt').trim()
            def versionAlreadyPublished = checkJavaVersionPublished group: 'payment', artifact: 'payment-api', version: artifactVersion

            onPR {
                if (versionAlreadyPublished) {
                    print "Artifact version already exists. Please bump it."
                    error "Artifact version already exists. Please bump it."
                }
            }

            stage('Build') {
                def descriptor = Artifactory.mavenDescriptor()
                descriptor.version = artifactVersion
                descriptor.transform()

                def rtMaven = Artifactory.newMavenBuild()
                rtMaven.tool = 'apache-maven-3.3.9'
                rtMaven.deployer releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: server
                rtMaven.deployer.deployArtifacts = (env.BRANCH_NAME == 'master') && !versionAlreadyPublished
                rtMaven.run pom: 'pom.xml', goals: 'clean install sonar:sonar', buildInfo: buildInfo
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
                    rpmVersion = packager.javaRPM('master', 'payment-api', '$(ls api/target/payment-api-*.jar)', 'springboot', 'api/src/main/resources/application.properties')
                    packager.publishJavaRPM('payment-api')
                }

                stage('Deploy to Dev') {
                    ansible.runDeployPlaybook("{payment_api_version: ${rpmVersion}}", 'dev')
                    rpmTagger.tagDeploymentSuccessfulOn('dev')
                }

                stage("Trigger smoke tests in Dev") {
                    build job: '/common-components/payment-app-smoke-tests/master', parameters: [
                        [$class: 'StringParameterValue', name: 'environment', value: 'dev']
                    ]
                    rpmTagger.tagTestingPassedOn('dev')
                }

                stage('Deploy to Test') {
                    ansible.runDeployPlaybook("{payment_api_version: ${rpmVersion}}", 'test')
                    rpmTagger.tagDeploymentSuccessfulOn('test')
                }

                stage("Trigger smoke tests in Test") {
                    build job: '/common-components/payment-app-smoke-tests/master', parameters: [
                        [$class: 'StringParameterValue', name: 'environment', value: 'test']
                    ]
                    rpmTagger.tagTestingPassedOn('test')
                }
            }

            milestone()
        } catch (err) {
            notifyBuildFailure channel: '#cc-payments-tech'
            throw err
        }
    }
}
