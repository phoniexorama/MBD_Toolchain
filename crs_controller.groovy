pipeline {
    agent none
    environment {
        LOGS_PATH = "./Code"
        ARTIFACTS_DOWNLOAD_PATH = "C:/Users/${env.GITLAB_USER_LOGIN}/Downloads"
    }

    stages {
        stage('verify') {
            agent {
                label 'LocalMatlabServer'
            }
            steps {
                script {
                    // This job executes the Model Advisor Checks for the model
                    matlabScript("crs_controllerModelAdvisor;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["$LOGS_PATH/logs/", "./Design/crs_controller/pipeline/analyze/**/*"])
                    }
                }
            }
        }

        stage('build') {
            agent {
                label 'LocalMatlabServer'
            }
            steps {
                script {
                    // This job performs code generation on the model
                    matlabScript("crs_controllerBuild;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["./Code/codegen/crs_controller_ert_rtw", "./Design/crs_controller/pipeline/analyze/**/*", "$LOGS_PATH/logs/"])
                    }
                }
            }
        }

        stage('testing') {
            agent {
                label 'LocalMatlabServer'
            }
            steps {
                script {
                    // This job executes the functional tests defined in the collection
                    matlabScript("crs_controllerTestFile;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["./Design/crs_controller/pipeline/analyze/**/*", "$LOGS_PATH/logs/", "./Code/codegen/crs_controller_ert_rtw"])
                        junit './Design/crs_controller/pipeline/analyze/testing/crs_controllerJUnitFormatTestResults.xml'
                    }
                }
            }
        }

        stage('package') {
            agent {
                label 'LocalMatlabServer'
            }
            steps {
                script {
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    echo "The model crs_controller has been checked"
                    echo "There is a Summary report generated crs_controllerReport.html"
                    matlabScript("generateXMLFromLogs('crs_controller'); generateHTMLReport('crs_controller'); deleteLogs;")
                }
                post {
                    always {
                        archiveArtifacts(artifacts: ["./Design/crs_controller/pipeline/analyze/**/*", "./Code/codegen/crs_controller_ert_rtw"])
                    }
                }
            }
        }

        stage('Deploy') {
            agent {
                label 'LocalMatlabServer'
            }
            steps {
                echo "Any deployments of code can be made here"
                echo "All artifacts of previous stage can be found here"
                script {
                    // Curl command to download artifacts
                    bat "curl.exe --location --output \"$ARTIFACTS_DOWNLOAD_PATH/Crs_ControllerArtifacts.zip\" --header \"PRIVATE-TOKEN: %CIPROJECTTOKEN%\" \"%CI_SERVER_URL%/api/v4/projects/%CI_PROJECT_ID%/jobs/artifacts/%CI_COMMIT_BRANCH%/download?job=Crs_ControllerPackage\""
                }
            }
            post {
                always {
                    archiveArtifacts(artifacts: ["./Design/crs_controller/pipeline/analyze/**/*", "./Code/codegen/crs_controller_ert_rtw"])
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlExample.prj'); ${script}\""
}
