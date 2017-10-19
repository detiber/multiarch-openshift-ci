properties([
  parameters([
    choiceParam(
      name: 'ARCH',
      choices: "x86_64\nppc64le\naarch64",
      description: 'Architecture'
    ),
    string(
      name: 'OS_BUILD_ENV_IMAGE',
      description: 'openshift-release image',
      defaultValue: 'openshiftmultiarch/origin-release:golang-1.8'
    )
  ])
])

library identifier: "multiarch-openshift-ci@trigger-test",
        retriever: modernSCM([$class: 'GitSCMSource',
                              remote: "https://github.com/detiber/multiarch-openshift-ci"])

node("paas-sig-ci-slave01") {
  ansiColor('xterm') {
    timestamps {
      withEnv(["PROVISION_STAGE_NAME=provision", "DEPROVISION_STAGE_NAME=deprovision"]) {
        stage('pre') {
          checkout(
            changelog: true,
            poll: true,
            scm: [
              $class: 'GitSCM',
              branches: [
                [name:'origin/master']
              ],
              doGenerateSubmoduleConfigurations: false,
              browser: [
                $class: 'GithubWeb',
                repoUrl: 'https://github.com/openshift/origin'
              ],
              extensions: [
                [$class: 'CleanBeforeCheckout']
                [$class: 'RelativeTargetDirectory', relativeTargetDir:'origin'],
              ],
              gitTool: 'Default',
              submoduleCfg: [],
              userRemoteConfigs: [
                [
                   name: 'origin',
                   refspec: '+refs/heads/master:refs/remotes/origin/master',
                   url:'https://github.com/openshift/origin.git'
                ],
                [
                   name: 'detiber',
                   refspec: '+refs/heads/multiarch:refs/remotes/detiber/multiarch',
                   url:'https://github.com/detiber/origin.git'
                ]
              ]
            ]
          )
          sh('''#!/usr/bin/bash -xeu
                pushd origin
                git merge detiber/multiarch
                popd
             ''')
        }
        withCiHost {
          stage('prep') {
            remoteCommands([
              "yum install -y rsync docker git",
              "echo 'insecure_registries: [172.30.0.0/16]' >> /etc/containers/registries.conf",
              "systemctl enable docker",
              "systemctl start docker"
            ])
            remoteSync(['origin'])
          }
          def failed_stages = []
          try {
            stage('check') {
              try {
                remoteCommands([
                  "cd origin; OS_BUILD_ENV_IMAGE=${OS_BUILD_ENV_IMAGE} hack/env TEST_KUBE=true JUNIT_REPORT=true DETECT_RACES=false TIMEOUT=300s make check -j -k"
                ])
              }
              catch (exc) {
                throw exc
              }
              finally {
                fetchScriptOutput()
                archiveArtifacts 'origin/_output/scripts/**/*'
                junit 'origin/_output/scripts/**/*.xml'
              }
            }
          }
          catch (exc) {
            failed_stages += 'check'
          }
          try {
            stage('build release') {
              try {
                remoteCommands([
                  "cd origin; OS_BUILD_ENV_IMAGE=${OS_BUILD_ENV_IMAGE} hack/env hack/build-base-images.sh",
                  "cd origin; OS_BUILD_ENV_IMAGE=${OS_BUILD_ENV_IMAGE} hack/env JUNIT_REPORT=true make release"
                ])
              }
              catch (exc) {
                throw exc
              }
              finally {
                fetchScriptOutput()
                archiveArtifacts 'origin/_output/scripts/**/*'
                junit 'origin/_output/scripts/**/*.xml'
              }
            }
          }
          catch (exc) {
            failed_stages += 'build release'
          }
          try {
            stage('integration') {
              try {
                remoteCommands([
                  "cd origin; OS_BUILD_ENV_IMAGE=${OS_BUILD_ENV_IMAGE} hack/env JUNIT_REPORT=true TIMEOUT=300s make test-tools test-integration"
                ])
              }
              catch (exc) {
                throw exc
              }
              finally {
                fetchScriptOutput()
                archiveArtifacts 'origin/_output/scripts/**/*'
                junit 'origin/_output/scripts/**/*.xml'
              }
            }
          }
          catch (exc) {
            failed_stages += 'integration'
          }
          try {
            stage('e2e') {
              try {
                def go_arch
                switch(params.ARCH) {
                  case "x86_64":
                    go_arch="amd64"
                    break
                  case "aarch64":
                    go_arch="arm64"
                    break
                  default:
                    go_arch=params.ARCH
                    break
                }
                remoteCommands([
                  "cd origin; OS_BUILD_ENV_IMAGE=${OS_BUILD_ENV_IMAGE} hack/env JUNIT_REPORT=true OS_BUILD_ENV_PRESERVE=_output/local/bin/linux/${go_arch}/end-to-end.test hack/env make build-router-e2e-test",
                  "cd origin; OS_BUILD_ENV_IMAGE=${OS_BUILD_ENV_IMAGE} hack/env JUNIT_REPORT=true OS_BUILD_ENV_PRESERVE=_output/local/bin/linux/${go_arch}/etcdhelper hack/env make build WHAT=tools/etcdhelper",
                  "OPENSHIFT_SKIP_BUILD='true' JUNIT_REPORT='true' make test-end-to-end -o build"
                ])
              }
              catch (exc) {
                throw exc
              }
              finally {
                fetchScriptOutput()
                archiveArtifacts 'origin/_output/scripts/**/*'
                junit 'origin/_output/scripts/**/*.xml'
              }
            }
          }
          catch (exc) {
            failed_stages += 'e2e'
          }
        }
      }
    }
  }
}
