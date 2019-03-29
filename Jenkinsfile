@Library('SonarSource@2.1.1') _
pipeline {
  agent {
    label 'linux'
  }
  parameters {
    string(name: 'GIT_SHA1', description: 'Git SHA1 (provided by travisci hook job)')
    string(name: 'CI_BUILD_NAME', defaultValue: 'sonar-security', description: 'Build Name (provided by travisci hook job)')
    string(name: 'CI_BUILD_NUMBER', description: 'Build Number (provided by travisci hook job)')
    string(name: 'GITHUB_BRANCH', defaultValue: 'master', description: 'Git branch (provided by travisci hook job)')
    string(name: 'GITHUB_REPOSITORY_OWNER', defaultValue: 'SonarSource', description: 'Github repository owner(provided by travisci hook job)')
  }
  environment {
    SONARSOURCE_QA = 'true'
    MAVEN_TOOL = 'Maven 3.3.x'
    JDK_VERSION = 'Java 8'
  }
  stages {
    stage('Notify') {
      steps {
        sendAllNotificationQaStarted()
      }
    }
    stage('QA') {
      parallel {
        stage('plugin/DOGFOOD/linux') {
          agent {
            label 'linux'
          }
          steps {
            runITs("plugin","DOGFOOD",JDK_VERSION)
          }
        }     
        stage('plugin/LATEST_RELEASE[6.7]/linux') {
          agent {
            label 'linux'
          }
          steps {
            runITs("plugin","LATEST_RELEASE[6.7]",JDK_VERSION)
          }
        } 
        stage('ruling/LATEST_RELEASE[6.7]/linux') {
          when { expression { return params.GITHUB_BRANCH.equals('master') } } 
          agent {
            label 'linux'
          }
          steps {
            //fetch submodule containing sources of ruling projects
            sh "git submodule update --init --recursive"   
            runITs("ruling","LATEST_RELEASE[6.7]",JDK_VERSION)            
          }
        }   
        stage('ruling/LATEST_RELEASE[6.7]/windows') {
          when { expression { return params.GITHUB_BRANCH.contains('PULLREQUEST-') } }           
          agent {
            label 'windows'
          }
          steps {
            //fetch submodule containing sources of ruling projects
            sh "git submodule update --init --recursive"   
            runITs("ruling","LATEST_RELEASE[6.7]",JDK_VERSION)            
          }
        }           
        stage('semantic/LATEST_RELEASE[6.7]/linux') {
          agent {
            label 'linux'
          }
          steps {
            //fetch submodule containing sources of projects used for semantic ITs
            sh "git submodule update --init --recursive"
            runITs("semantic","LATEST_RELEASE[6.7]",JDK_VERSION)            
          }          
        } 
        stage('QA-OS/windows') {
          when { expression { return params.GITHUB_BRANCH.equals('master') } } 
          agent {
            label 'windows'
          }
          steps {
            runQAOS(JDK_VERSION)      
          }
        }
        stage('QA-OS/macOS') {
          when { expression { return params.GITHUB_BRANCH.equals('master') } } 
          agent {
            label 'macosx'
          }
          steps {
            runQAOS(JDK_VERSION)      
          }
        } 
      }         
      post {
        always {
          sendAllNotificationQaResult()
        }
      }

    }
    stage('Promote') {
      steps {
        repoxPromoteBuild()
      }
      post {
        always {
          sendAllNotificationPromote()
        }
      }
    }
  }
}

def withQAEnv(def body) {
    withCredentials([string(credentialsId: 'ARTIFACTORY_PRIVATE_API_KEY', variable: 'ARTIFACTORY_API_KEY')]) {
        body.call()
    }
}

def withJava(jdk, def body) {
  def javaHome = tool name: jdk, type: 'hudson.model.JDK'
  withEnv(["JAVA_HOME=${javaHome}"]) {
    body.call()
  }
}

def runITs(TEST,SQ_VERSION,JDK) {  
  withQAEnv {
    withJava(JDK) {
      withMaven(maven: MAVEN_TOOL) {
        mavenSetBuildVersion()
        def mvnCommand = isUnix() ? 'mvn' : 'mvn.cmd'
        dir("its/$TEST") {    
          sh "${mvnCommand} package -Pit-$TEST -Dsonar.runtimeVersion=$SQ_VERSION -Dmaven.test.redirectTestOutputToFile=false -B -e -V"
        }
      }
    }
  }
}

def runQAOS(JDK) {
  withQAEnv {
    withJava(JDK) {
      withMaven(maven: MAVEN_TOOL) {
        mavenSetBuildVersion()
        def mvnCommand = isUnix() ? 'mvn' : 'mvn.cmd'
        sh "${mvnCommand} clean verify -B -e -V"        
      }
    }
  }
}
