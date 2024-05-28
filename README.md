# java-devops-pipeline

Well seeing the image below you might be wondering, how he end up in this position!! in 1 attempt.

allow me to expalin. 

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/2bd09344-0257-4ce6-a503-5c5d10decb8c)


## Flow Diagram 

To have a better understanding on what we are doing its important you under the flow. 

![java pipeline](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/4304899f-f2a2-4ceb-979a-8880aed96eb7)


The workflow starts with a code push to your GitHub repository. Jenkins, the CI/CD orchestrator, is triggered and initiates the pipeline. SonarQube performs static code analysis for quality checks. Maven handles the build process. The built artifacts are stored in Nexus, the remote repository. Finally, the artifacts are deployed to the Tomcat server for running the Java application.



Before we start you should understand there are 4 steps: 

1) Tools Installation.
2) Tools configuration. (sonar to maven then maven to nexus then tomcat )
3) Pipeline Setup.
4) Understanding the errors. (yes! errors are evident !!)


## Tools Installation  
























This implementation is quite normal keep updated i will post the pipeline with docker 





