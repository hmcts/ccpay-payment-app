#!groovy
@Library("Reform")
import uk.gov.hmcts.Ansible
import uk.gov.hmcts.Packager

def packager = new Packager(steps, 'cc');
def ansible = new Ansible(steps, 'ccpay');

def server = Artifactory.server 'artifactory.reform'
def rtMaven = Artifactory.newMavenBuild()
def buildInfo = Artifactory.newBuildInfo()

properties(
    [[$class: 'GithubProjectProperty', displayName: 'Payment API', projectUrlStr: 'https://git.reform.hmcts.net/common-components/payment-app/'],
     pipelineTriggers([[$class: 'GitHubPushTrigger']])]
)

stageWithNotification('Checkout') {
    deleteDir()
    checkout scm
}

stageWithNotification('Build') {
    rtMaven.tool = 'apache-maven-3.3.9'
    rtMaven.deployer releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: server
    rtMaven.run pom: 'pom.xml', goals: 'clean install site site:stage', buildInfo: buildInfo
    archiveArtifacts 'api/target/*.jar'

    publishHTML([
        allowMissing         : false,
        alwaysLinkToLastBuild: true,
        keepAll              : false,
        reportDir            : 'target/staging',
        reportFiles          : 'index.html',
        reportName           : 'Maven Site'
    ])
}

ifMaster {
    def rpmVersion

    stageWithNotification('Publish JAR') {
        server.publishBuildInfo buildInfo
    }

    stageWithNotification("Publish RPM") {
        rpmVersion = packager.javaRPM('master', 'payment-api', '$(ls api/target/payment-api-*.jar)', 'springboot', 'api/src/main/resources/application.properties')
        packager.publishJavaRPM('payment-api')
    }

    stageWithNotification('Publish Docker') {
        dockerImage imageName: 'common-components/payments-api'
        dockerImage imageName: 'common-components/payments-database', context: 'docker/database'
    }

    stageWithNotification('Deploy') {
        def version = "{payment_api_version: ${rpmVersion}}"
        ansible.runDeployPlaybook(version, 'dev')
    }
}

private ifMaster(Closure body) {
    if ("master" == "${env.BRANCH_NAME}") {
        body()
    }
}

private stageWithNotification(String name, Closure body) {
    stage(name) {
        node {
            try {
                body()
            } catch (err) {
                notifyBuildFailure channel: '#cc_tech'
                throw err
            }
        }
    }
}
