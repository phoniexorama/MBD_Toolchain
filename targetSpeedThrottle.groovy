pipeline {
    agent {
        label 'WinLocalagent' //Label for Windows agent
    }

    environment {
        LOGS_PATH = "./Code"
        ARTIFACTS_DOWNLOAD_PATH = "C:/Users/${env.GITLAB_USER_LOGIN}/Downloads"
    }

    stages {
        stage('verify') {
            steps {
                script {
                    // This job executes the Model Advisor Checks file for TargetSpeedThrottle model
                    matlabScript("TargetSpeedThrottleModelAdvisor;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["./Design/TargetSpeedThrottle/pipeline/analyze/**/*", "$LOGS_PATH/logs/"])
                    }
                }
            }
        }

        stage('build') {
            steps {
                script {
                    // This job performs code generation on the model
                    matlabScript("TargetSpeedThrottleBuild;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["./Code/codegen/TargetSpeedThrottle_ert_rtw", "./Design/TargetSpeedThrottle/pipeline/analyze/**/*", "$LOGS_PATH/logs/"])
                    }
                }
            }
        }

        stage('testing') {
            steps {
                script {
                    // This job executes the unit tests defined in the collection
                    matlabScript("TargetSpeedThrottleTest;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["./Design/TargetSpeedThrottle/pipeline/analyze/**/*", "$LOGS_PATH/logs/", "./Code/codegen/TargetSpeedThrottle_ert_rtw"])
                        junit './Design/TargetSpeedThrottle/pipeline/analyze/testing/TargetSpeedThrottleJUnitFormatTestResults.xml'
                    }
                }
            }
        }

        stage('package') {
            steps {
                script {
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    echo "The model TargetSpeedThrottle has been checked"
                    echo "There is a Summary report generated targetSpeedThrottleReport.html"
                    matlabScript("generateXMLFromLogs('TargetSpeedThrottle'); generateHTMLReport('TargetSpeedThrottle'); deleteLogs;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["./Design/TargetSpeedThrottle/pipeline/analyze/**/*", "./Code/codegen/TargetSpeedThrottle_ert_rtw"])
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "Any deployments of code can be made here"
                echo "All artifacts of previous stage can be found here"
                script {
                    // Curl command to download artifacts
                    bat "curl.exe --location --output \"$ARTIFACTS_DOWNLOAD_PATH/TargetSpeedThrottleArtifacts.zip\" --header \"PRIVATE-TOKEN: %CIPROJECTTOKEN%\" \"%CI_SERVER_URL%/api/v4/projects/%CI_PROJECT_ID%/jobs/artifacts/%CI_COMMIT_BRANCH%/download?job=TargetSpeedThrottlePackage\""
                }
            }
            post {
                always {
                    archiveArtifacts(artifacts: ["./Design/TargetSpeedThrottle/pipeline/analyze/**/*", "./Code/codegen/TargetSpeedThrottle_ert_rtw"])
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlExample.prj'); ${script}\""
}
