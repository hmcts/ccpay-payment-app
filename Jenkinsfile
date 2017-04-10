#!groovy

@Library('Reform') _

properties(
    [[$class: 'GithubProjectProperty', displayName: 'Payment API', projectUrlStr: 'http://git.reform/common-components/payment-app/'],
    pipelineTriggers([[$class: 'GitHubPushTrigger']])]
)

node {
    def server = Artifactory.server 'artifactory.reform'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo = Artifactory.newBuildInfo()

    try {
        stage('Checkout') {
            checkout scm
        }

        stage('Build (JAR)') {
            rtMaven.tool = 'apache-maven-3.3.9'
            rtMaven.opts = mavenOpts(env)
            rtMaven.deployer releaseRepo:'libs-release', snapshotRepo:'libs-snapshot', server: server
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

        stage('Publish (JAR)') {
            server.publishBuildInfo buildInfo
        }

        stage('Build (Docker)') {
            dockerImage imageName: 'common-components/payments-api'
            dockerImage imageName: 'common-components/payments-database', context: 'docker/database'
        }
    } catch (err) {
         slackSend(
             channel: '#cc_tech',
             color: 'danger',
             message: "${env.JOB_NAME}: <${env.BUILD_URL}console|Build ${env.BUILD_DISPLAY_NAME}> has FAILED")
         throw err
     }
}

private void mavenOpts(env) {
    return "${env.MAVEN_OPTS != null ? env.MAVEN_OPTS : ''} ${proxySystemProperties(env)}"
}

private proxySystemProperties(env) {
    def systemProperties = []
    if (env.http_proxy != null) {
        def proxyURL = new URL(env.http_proxy)
        systemProperties.add("-Dhttp.proxyHost=${proxyURL.getHost()}")
        systemProperties.add("-Dhttp.proxyPort=${proxyURL.getPort()}")
    }
    if (env.https_proxy != null) {
        def proxyURL = new URL(env.https_proxy)
        systemProperties.add("-Dhttps.proxyHost=${proxyURL.getHost()}")
        systemProperties.add("-Dhttps.proxyPort=${proxyURL.getPort()}")
    }
    if (env.no_proxy != null) {
        systemProperties.add("-Dhttp.nonProxyHosts=${env.no_proxy}")
    }
    return systemProperties.join(' ')
}
