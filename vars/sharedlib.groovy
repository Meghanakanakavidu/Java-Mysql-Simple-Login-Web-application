def call(Map params) { 
node('master') {
    stage('pull') {
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/Meghanakanakavidu/Java-Mysql-Simple-Login-Web-application.git']]])
    }
    stage('artifact details') {
        tool name: 'java', type: 'jdk'
	    tool name: 'maven', type: 'maven'
	    def mvnHome = tool 'maven'
	    env.PATH = "${mvnHome}/bin:${env.PATH}"
        rtMavenDeployer (
          id: 'deployer-unique-id',
          serverId: 'JFrog',
          releaseRepo: 'release/${BUILD_NUMBER}',
          snapshotRepo: "snapshot/${BUILD_NUMBER}"
        )
        
    }
    stage('maven build'){
        
          rtMavenRun (
          tool: 'maven',
	      type: 'maven',
          pom: 'pom.xml',
          goals: 'clean install',
          opts: '-Xms1024m -Xmx4096m',
          //resolverId: 'resolver-unique-id',
          deployerId: 'deployer-unique-id',
          )
        
    }
    stage('publish the artifact'){
        
          rtPublishBuildInfo (
          serverId: "JFrog"
          )
         
    }
    stage('sonar'){
        // def scannerHome = tool 'Sonar';
        withSonarQubeEnv('sonar') {
         sh 'mvn clean install sonar:sonar'
       }  
    }
     stage("Quality Gate"){
      timeout(time: 60, unit: 'SECONDS') { // Just in case something goes wrong, pipeline will be killed after a timeout
      def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
      if (qg.status != 'OK') {
        error "Pipeline aborted due to quality gate failure: ${qg.status}"
    }
  }
  
 }
    stage('Download artifact'){
        rtDownload (
           serverId: "JFrog",
           spec:
             """{
           "files": [
            {
              "pattern": "snapshot/${BUILD_NUMBER}/com/javawebtutor/LoginWebApp/1.0-SNAPSHOT/LoginWebApp-*.war",
              "target": "/var/lib/jenkins/workspace/${JOB_NAME}/"
            }
         ]
         }"""
        )
     }
    stage('Copy'){
        sh 'mv /var/lib/jenkins/workspace/${JOB_NAME}/${BUILD_NUMBER}/com/javawebtutor/LoginWebApp/1.0-SNAPSHOT/*.war  /var/lib/jenkins/workspace/${JOB_NAME}/${BUILD_NUMBER}/com/javawebtutor/LoginWebApp/1.0-SNAPSHOT/loginwebapp.war'
         
        sh 'scp -v -o StrictHostKeyChecking=no  /var/lib/jenkins/workspace/${JOB_NAME}/${BUILD_NUMBER}/com/javawebtutor/LoginWebApp/1.0-SNAPSHOT/*.war root@52.176.41.66:tomcat/webapps'    

        
    }
}    
}


