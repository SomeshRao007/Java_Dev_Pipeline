This document describes continuous integration and continuous delivery (CI/CD) for a Java application. After submitting the code to your GitHub repository, Jenkins will start the pipeline. It organizes static code analysis using SonarQube, builds the application using Maven, saves the artifacts to Nexus and finally deploys them to the Tomcat server for execution. The following sections dive into the basic setup steps: installing the tool, configuring (SonarQube on Maven, Maven on Nexus, and finally Tomcat), creating a pipeline, and troubleshooting potential bugs. **At the end there is an ester egg** 🥚.

## Flow Diagram 

To have a better understanding on what we are doing its important you under the flow. This implementation is quite normal keep updated i will post the pipeline with docker and with pipeline script. 

![java pipeline](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/4304899f-f2a2-4ceb-979a-8880aed96eb7)


The workflow starts with a code push to your GitHub repository. Jenkins, the CI/CD orchestrator, is triggered and initiates the pipeline. SonarQube performs static code analysis for quality checks. Maven handles the build process. The built artifacts are stored in Nexus, the remote repository. Finally, the artifacts are deployed to the Tomcat server for running the Java application.



Before we start you should understand there are 4 steps: 

1) Tools Installation.
2) Tools configuration. (sonar to maven then maven to nexus then tomcat )
3) Pipeline Setup.
4) Understanding the errors. (yes! errors are evident !!)


## Tools Installation  

We are using 5 tools mainly and for easy of this tutorial i will showing you how to install each tool in a seprate VM. Now if you are a beginner then try this approch before trying via docker, remember baby steps!. the reason why i am install each application in a seperate VM is due to: 

+ Java version conflict. A few applications use a specific java version for example if you are using sonarqube 10+ version it will work on java 11+ (go with java 17).
+ Port conflict, some application use same port either (for example 8080).
+ Conflict with interdependencies. 

[Maven](https://github.com/SomeshRao007/Java_Dev_Pipeline/blob/main/README.md#1-maven-installation)

[Nexus](https://github.com/SomeshRao007/Java_Dev_Pipeline/blob/main/README.md#2-nexus-installation)

[SonarQube](https://github.com/SomeshRao007/Java_Dev_Pipeline/blob/main/README.md#3-sonarqube-installation)

[Tomcat](https://github.com/SomeshRao007/Java_Dev_Pipeline/blob/main/README.md#4-tomcat-installation)

[Jenkins](https://github.com/SomeshRao007/Java_Dev_Pipeline/blob/main/README.md#5-jenkins-installation)

So, I am using multiple VM's to manage each application, one for Maven and nexus repository, one for sonar, one for tomcat and lastly one for jenkinns. 

Inititate EC2 you find that in my ollama repo, for all this i am using **t3a.medium** instance you can use instance of your choice. 
yes, using docker surely sovles all these problmes but get confidence and command over basics before moving on to containersization platform.


### 1. Maven Installation 

You should be clear, what is Maven and why do we use. 

 What is Maven ?
> Maven is a build automation tool used primarily for Java projects. It manages project dependencies, compiles code, runs tests, and packages the application, ensuring a consistent build process across all environments.

 Why do we use it ?
> + Dependency management: Maven automatically downloads and manages all the libraries (JAR files) your project needs. No more hunting around for downloads!
> + Project structure: Maven enforces a consistent project structure, making it easy for new developers to jump in and understand the codebase.
> + Build automation: With Maven, you can define common build tasks (compiling, testing, packaging) in one place and run them easily. This saves time and reduces errors.

Enough gibber jabber ! lets install it !!

**Step 1:**

First, let's install java 17.

~~~
sudo yum install java-17

java -version
~~~

**Step 2:**

[Visit Offical web site](https://maven.apache.org/download.cgi), find out more about system requirments and copy **Binary tar.gz archive** download link 

~~~
sudo wget https://dlcdn.apache.org/maven/maven-3/3.9.7/binaries/apache-maven-3.9.7-bin.tar.gz
~~~

**Step 3:**

Untar the mvn package to the /opt folder.

~~~
sudo tar xvf apache-maven-3.9.7-bin.tar.gz -C /opt
~~~

**Step 4:**

Create a symbolic link to the maven folder. 

> Why?
> > This way, when you have a new version of maven, you just have to update the symbolic link and the path variables remain the same.

~~~
sudo ln -s /opt/apache-maven-3.9.7 /opt/maven
~~~

**Step 5:**

Add Maven Folder To System PATH

> Now Why this ???
>> To access the mvn command systemwide, you need to either set the M2_HOME environment variable or add /opt/maven to the system PATH.
>> We will do both by adding them to the profile.d folder. So that every time the shell starts, it gets sourced and the mvn command will be available system-wide.

Create a script file named maven.sh in the **profile.d** folder.

~~~
sudo vi /etc/profile.d/maven.sh
~~~

**Step 6:** 

Add the following to the script and save the file.

~~~
export M2_HOME=/opt/maven
export PATH=${M2_HOME}/bin:${PATH}
~~~

**Step 7:** 

Add execute permission to the **maven.sh** script.

~~~
sudo chmod +x /etc/profile.d/maven.sh
~~~

**Step 8:** 

_Source the script_ for changes to take immediate effect.

> In shell scripting, "source" or "." is a command used to **read and execute** the contents of a script within your current shell session. So, basically we are applying modifications made in a script to your current working environment.

~~~
source /etc/profile.d/maven.sh
~~~

**Verify** maven installation

~~~
mvn -version
~~~

if you see something like this:

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/3af57f15-7406-4fd6-bb83-20b4b9850bd4)


then, 🥇 one down 4 more to go!

now if want to try out some basic command (such as clean install deploy ) using maven you can clone this repo and use this:

~~~
mvn clean install
~~~

to study more about what the command does refer to my short-hand repo [Maven guide](https://github.com/SomeshRao007/Maven-Guide).

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/514e9743-0b5f-49cd-8e65-4bf8d3c325e4)


yeah! dont just run it blindly go to the repo and run where you have POM.xml file or else you see face these red flags.

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/9ac020a8-e1a9-4834-91d3-da8dfe247184)

once you see **Build Success** green flag you can move on. 


#### Things u need to be aware off !!

+ **M2_HOME** environment variable: The location is the corresponding maven installation directory in the system.
+ **.m2** local repository:	**$HOME/.m2**	When you run maven commands, all the remote repositories get cached locally in the .m2 folder.
+ **settings.xml** (Global):	**$M2_HOME/conf/settings.xm**l	This file contains all the maven configurations that are common for all the projects.
+ **settings.xml** (User Specific): you will find that at	**$HOME/.m2/settings.xml**
 

### 2. Nexus Installation

Nexus is a repository manager that stores and manages software artifacts. It acts as a central hub for versioned binary artifacts, enabling developers to share and reuse them across projects. Nexus supports various repository formats, including Maven, npm, and Docker, facilitating efficient dependency management and continuous integration workflows.

> Why do we use nexus in current market? 

>>We use Nexus in the current market primarily for:
>> + Artifact Repository Management
>> + Build Promotion
>> + Vulnerability Scanning
>> + Controlling Access and Usage
>>
>> It serves as a central repository for managing and distributing software artifacts (libraries, packages, etc.) required for building applications, enabling better control and governance over the development lifecycle.


For installation:

**Step 1:** 

Install OpenJDK 1.8

~~~
sudo yum install java-1.8.0
~~~

~~~
sudo yum update && sudo yum upgrade 
~~~

**Step 2:** 

Create a directory named app and cd into the directory.

~~~
sudo mkdir /app && cd /app
~~~

**Step 3:**

Download the latest nexus.

~~~
sudo wget -O nexus.tar.gz https://download.sonatype.com/nexus/3/latest-unix.tar.gz
~~~

**Step 4:**

Untar the downloaded file and Rename the untared file to nexus.

~~~
sudo tar -xvf nexus.tar.gz && sudo mv nexus-3* nexus
~~~

**Step 5:** 

create a new user named nexus to run the nexus service. (for security reasons, it is not advised to run nexus service with root privileges)

~~~
sudo adduser nexus
~~~

**Step 6:**

Change the ownership of nexus files and nexus data directory to nexus user.

~~~
sudo chown -R nexus:nexus /app/nexus
sudo chown -R nexus:nexus /app/sonatype-work
~~~

**Step 7:** 

Open /app/nexus/bin/nexus.rc file

~~~
sudo vi  /app/nexus/bin/nexus.rc
~~~

Uncomment run_as_user parameter and set it as:  **run_as_user="nexus"**

**Step 8:** 

Running Nexus as a System Service.
>It is better to have systemd entry to manage nexus using systemctl, to start and stop server with ease.

Create a nexus systemd unit file.

~~~
sudo vi /etc/systemd/system/nexus.service
~~~

Add the following contents to the unit file.

~~~
[Unit]
Description=nexus service
After=network.target

[Service]
Type=forking
LimitNOFILE=65536
User=nexus
Group=nexus
ExecStart=/app/nexus/bin/nexus start
ExecStop=/app/nexus/bin/nexus stop
User=nexus
Restart=on-abort

[Install]
WantedBy=multi-user.target
~~~


Now we have all the configurations in place to run nexus. Execute the following command to add nexus service to boot.

~~~
sudo chkconfig nexus on
~~~

To start the Nexus service.

~~~
sudo systemctl start nexus
~~~

The above command will start the nexus service on port 8081. To access the nexus dashboard, visit http:// <public IP>:8081. You will be able to see the nexus homepage

![Screenshot 2024-05-31 111639](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/760b8a3e-3b9d-447e-bad9-47b4d1728b8a)

To verify installation:

~~~
curl --include --silent http://<public IP>:8081/ | grep Server
~~~
 
![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/53b37f96-4472-4ba9-9f40-bcdf37da82eb)


if click on sign in it will show you the path where you can get the default password. You simply have to do:

~~~
sudo cat <path> 
~~~

then in the next prompt it will ask to change to password now change it to your desired password. 

![Screenshot 2024-05-31 111707](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/43d1046c-278d-41ad-a365-716651d9506b)



Okay now 🥈2 down 3 more to go 


### 3. SonarQube Installation 

SonarQube is a tool for continuous inspection of code quality. It performs static code analysis to detect bugs, vulnerabilities, and code smells, providing detailed reports and metrics. SonarQube supports multiple programming languages and integrates with CI/CD pipelines to ensure code quality throughout the development lifecycle.

>Why exactly we use it ?
>> + **Improved Code Quality**: SonarQube analyzes code for bugs, vulnerabilities, code smells (bad coding practices), and duplication. This helps developers write cleaner, more maintainable code.
>> + **Early Bug Detection:** By identifying issues early in the development process, SonarQube helps prevent bugs from finding their way into production, saving time and money.
>> + **Security Focus:** SonarQube can detect potential security vulnerabilities in code, helping to prevent security breaches.
>> + **Consistent Coding Standards:** SonarQube enforces consistent coding standards across your codebase, making it easier for developers to collaborate and understand each other's code.

For installation: 

You need to install a database to store any reports and some data along with meta data. now sonarqube will work even without any database since it has embedded database for initial testing, we will configure PSQL database for this you can use database of your choice and follow the these steps:

~~~
sudo yum update -y

sudo yum install java-17
~~~

**Step 1:** 

Install PostgreSQL 15 and start the Postgress database

~~~
sudo dnf install postgresql15.x86_64 postgresql15-server
~~~

Initialize the PostgreSQL Database

~~~
sudo postgresql-setup --initdb
~~~

Add the PostgreSQL service to the system startup.

~~~
sudo systemctl start postgresql
~~~

~~~
sudo systemctl enable postgresql
~~~

Check the status of PostgreSQL using the following command.

~~~
sudo systemctl status postgresql
~~~


**Step 2:**

Create Sonar User and Database in PSQL, we need to have a sonar user and database for the sonar application.

Change the default password of the Postgres user. All Postgres commands have to be executed by this user.

~~~
sudo passwd postgres
~~~

**Step 3:** 

Login as postgres user with the new password.

~~~
su - postgres
~~~

Log in to the PostgreSQL CLI.

```psql```

**Step 4:**

Enable Remote Connection For PostgreSQL on Amazon Linux. By default the remote PostgreSQL connection is disabled. You need to add the following configuration to enable remote connectivity.

Open the postgresql.conf file

```
sudo vi /var/lib/pgsql/data/postgresql.conf
```

Find the following lines at the bottom of the file and change: 


**peer** ---to---> **trust** and **idnet** ---to---> **md5**

~~~
# TYPE  DATABASE        USER            ADDRESS                 METHOD

# "local" is for Unix domain socket connections only
local   all             all                                     peer
# IPv4 local connections:
host    all             all             127.0.0.1/32            ident
# IPv6 local connections:
host    all             all             ::1/128                 ident
~~~

To apply all the changes, restart the PostgreSQL service using the following command.

```
sudo systemctl restart postgresql
```

**Step 5:** 

Create a sonarqubedb database.

``` create database sonarqubedb; ```

Create the sonarqube DB user with a strongly encrypted password. Replace your-strong-password with a strong password.

~~~
create user sonarqube with encrypted password '<your password>';
~~~

Next, grant all privileges to sonrqube user on sonarqubedb.

~~~
grant all privileges on database sonarqubedb to sonarqube
~~~

Exit the psql prompt and return to normal user.

`\q`

`exit`

**Step 6:**

Download and install sonarqube application

```
cd /opt 
sudo wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.5.1.90531.zip
```

**Step 7:**

Unzip sonarqube source files and rename the folder.

~~~
sudo unzip sonarqube-10.5.1.90531.zip
sudo mv sonarqube-10.5.1.90531 sonarqube
~~~

**Step 8:**

Updaing sonarqube database, adding PSQL password in properties file.

Open /opt/sonarqube/conf/sonar.properties file.

~~~
sudo vi /opt/sonarqube/conf/sonar.properties
~~~

Uncomment and edit the parameters as shown below. Change the password accordingly. You will find the JDBC parameter under the PostgreSQL section.

~~~
sonar.jdbc.username=<PSQL_sonar_Username>                                                                                                                     
sonar.jdbc.password=<PSQL_sonar_user_password>
sonar.jdbc.url=jdbc:postgresql://<Public IP>/sonarqubedb
~~~

By default, sonar will run on 9000. In case you want to change port number, Scroll down and find these lines.

~~~
sonar.web.host=0.0.0.0
sonar.web.port=<Desired port no.>
~~~

we need to give permission for sonar user to run to able to run all type of files (optional)

open sudoers file and add sonar line :

~~~
sudo vi /etc/sudoers
root    ALL=(ALL:ALL) ALL
sonar    ALL=(ALL:ALL) ALL
~~~

**Step 9:**

Add Sonar User and Privileges, Create a user named sonar and make it the owner of the /opt/sonarqube directory.

~~~
sudo useradd sonar
sudo chown -R sonar:sonar /opt/sonarqube
~~~

add sonar user to 

sudo vi /etc/sudoers

**Step 10:**

Start Sonarqube Service

To start sonar service, you need to use the script in sonarqube bin directory.

Login as sonar user

~~~
sudo su - sonar
~~~

Navigate to the start script directory.

~~~
cd /opt/sonarqube/bin/linux-x86-64
~~~

if you try to list all the files using `ls` you will see a script file sonar.sh to start sonar you need to run that script.

`./sonar.sh start` or `bash sonar.sh start`

`sudo ./sonar.sh status`

you should be able to access sonarqube on the browser on port 9000

in case sonar didnt start and it shows like this:

~~~
/bin/java
Removed stale pid file: ./SonarQube.pid
SonarQube is not running.
~~~

![Screenshot 2024-05-08 124333](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/06250861-d1e4-484f-b35e-24638c6f6331)

first thought should be checking the logs :

~~~
cd sonarqube/logs/
~~~

if you notice there **aren't** any `es.log` or `sonar.log` files and **only** nohup.log file exits then most likely its LinkageError error. Its a type of error where system default java version is not configured properly, this might happen either due to: 

A) Multiple java versions are avaliable in the system.

B) Improper installation of java. 


for me it was A) because i started with java 11 which only supported 55 packages out of 65 packages in some compilation action of sonarqube application. then followed:

~~~
sudo yum install java-17
sudo update-alternatives --config javac
sudo update-alternatives --config java
~~~

in last 2 command it will ask for your input to select the required version. 


then i tried starting and check its status, only to find out that sonar user was not able to perform some actions. so i had to give sonar user permission for the sonar application folder:

~~~
sudo chown -R sonar:sonar /opt/sonarqube
~~~

and then it worked !!

The default credientals are **USERNAME=admin and PASSWORD=admin**. 

Good 3 🥉 down 2 more to go!


### 4. Tomcat Installation 

Apache Tomcat is an open-source web server and servlet container. It implements the Java Servlet, JavaServer Pages (JSP), and Java Expression Language (EL) specifications, enabling developers to run Java-based web applications. 

>why we use it in current market ?
>> We use Tomcat in the current market because it provides a robust, lightweight, and highly scalable environment for deploying and running Java servlets, JavaServer Pages (JSP), and web services. Its cross-platform compatibility, efficient thread management, and extensive community support make it a widely adopted choice for running Java web applications in production environments.


For Installation: 

**Step 1:**

first install java 

~~~
yum install java-1.8* 
~~~

**Step 2:**

Change to /opt dir

~~~
sudo su -

cd /

cd /opt
~~~

**Step 3:**

Download and unzip source code
~~~
wget https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.24/src/apache-tomcat-10.1.24-src.tar.gz
~~~


~~~
tar -zvxf apache-tomcat-10.1.24.tar.gz
~~~

**Step 4:**

Give execuatble permissions to the startup and shutdown scripts.

~~~
cd apache-tomcat-10.1.24

cd bin
~~~

~~~
chmod +x startup.sh
chmod +x shutdown.sh
~~~

**Step 5:**

Create symbolic links files for tomcat server up and down

> what are links in linux??
> > In Linux, creating a link refers to creating a special file that acts as a shortcut to another file or directory. There are two main types of links:
> > 1. **Hard Link:** This creates a new file that points directly to the original file's data. Any changes made to the link or the original file affect both. Hard links can only be created for files, not directories.
> > 2. **Symbolic Link (Symlink):** This is a more common type of link. It's like a pointer that tells the system where the actual file or directory resides. Modifying the link itself doesn't affect the original file. Symbolic links can be used for both files and directories. 



~~~
ln -s /opt/apache-tomcat-10.1.24/bin/startup.sh /usr/local/bin/tomcatup
ln -s /opt/apache-tomcat-10.1.24/bin/shutdown.sh /usr/local/bin/tomcatdown
~~~

this will help running scripts from anywhere

~~~
tomcatup
~~~

**Step 6:**

Change Settings to Manage Tomcat

~~~
cd apache-tomcat-10.1.24
~~~

find -name **context.xml**

Comment value tag sections in below all files:

open these context.xml file: 

![Screenshot 2024-06-03 191740](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/9dc9e042-e3d8-4056-98e4-7f9d32386d30)

~~~
vi ./webapps/examples/META-INF/context.xml
vi ./webapps/host-manager/META-INF/context.xml
vi ./webapps/manager/META-INF/context.xml
~~~


and find this tag in all the file and comment it out we are doing this to avoid any potential conflicts by commenting it we are essentially disabling the funcationality :

 ![Screenshot 2024-06-03 192353](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/b3a2b310-cc22-474a-826b-fdf81bf77d2a)


**Step 7:**

Update tje user info in **tomcat-user.xml** file (we can create new user here and give any user permissions here in this file.)

~~~
cd ..
cd /opt/apache-tomcat-10.1.24
cd conf
vi tomcat-users.xml
~~~

Add below lines between <tomcat-users> tag

~~~

<role rolename="manager-gui"/>
<role rolename="manager-script"/>
<role rolename="manager-jmx"/>
<role rolename="manager-status"/>   
<user username="admin" password="admin" roles="manager-gui,manager-script,manager-jmx,manager-status"/>
<user username="deployer" password="deployer" roles="manager-script"/>
<user username="tomcat" password="s3cret" roles="manager-gui"/>

~~~

the server can be accessed on port 8080. http://<Public IP>:8080/ 

![Screenshot 2024-05-22 170651](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/06c079b1-5dcd-4fa5-b51b-1f353c5f1149)


To access any service or any example app you need to login you can use credientials as you configured. 



### 5. Jenkins Installation 

Last one to start with configuration.


Jenkins is an open-source automation server that enables continuous integration and continuous delivery/deployment (CI/CD) of software projects. It helps automate the non-human part of the software development process, including building, testing, and deploying applications. 

> Why do we use jenkins?
>> 1. **Continuous Integration and Continuous Delivery** (CI/CD): Jenkins is widely adopted for implementing CI/CD pipelines, which enable teams to build, test, and deploy software efficiently and frequently.
>> 2. **Extensibility and Plugins**: Jenkins has a vast ecosystem of plugins that integrate with various tools and technologies used in the software development process.
>> 3. **Cross-platform and Language Support**: Jenkins supports multiple platforms (Windows, Linux, macOS) and a wide range of programming languages.
>> 4. **Scalability and Distributed Builds**: Jenkins can be scaled horizontally by adding more nodes (agents) to handle larger workloads or distributed builds, making it suitable for large-scale projects and enterprises.
>> 5. **Open-Source and Community**: Being open-source, Jenkins benefits from a large and active community contributing plugins, documentation, and support, which helps organizations avoid vendor lock-in and reduce costs.


For installation:

lets update packages :  `sudo yum update –y`

**Step 1:**

Add the Jenkins repo:

~~~
sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
~~~

**Step 2:**

Import a key file from Jenkins-CI to enable installation from the package:

~~~
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key
sudo yum upgrade
~~~

**Step 3:**

Install Java (Amazon Linux 2023):

~~~
sudo dnf install java-17-amazon-corretto -y
~~~

**Step 4:** 

Install Jenkins:

~~~
sudo yum install jenkins -y
~~~

**Step 5:**

Enable the Jenkins service to start at boot and Start Jenkins as a service:

~~~
sudo systemctl enable jenkins
sudo systemctl start jenkins
~~~


You can check the status of the Jenkins service:

~~~
sudo systemctl status jenkins
~~~


you can access jenkins via http://<public_ip>:8080 from your browser. 

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/37974267-74be-4d4d-8538-d9c86833c13e)


enter the password found in /var/lib/jenkins/secrets/initialAdminPassword

~~~ 
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
~~~

The Jenkins installation script directs you to the Customize Jenkins page. Click **Install suggested plugins**.

Create **First Admin User** will open. Enter your information, and then select Save and Continue.

For more info [check here](https://www.jenkins.io/doc/tutorials/tutorial-for-installing-jenkins-on-AWS/)


## Tools configuration

we are now done with installation part and lets move to configuring these apps. 

### Maven for .WAR file 

Initially maven executes build process and packs in `.JAR` but since we are using Tomcat application it uses only `.war` files. so we need make some changes to make Maven builes in .war package. 

#### In POM.xml

Add this plugin under **Plugins tag**. 
~~~
        <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-war-plugin</artifactId>
	            <version>3.3.2</version> <!-- Update to the latest version if need> -->
	            <configuration>
		            <warSourceDirectory>src/main/webapp</warSourceDirectory>
		            <webXml>src/main/webapp/WEB-INF/web.xml</webXml>
	            </configuration>
        </plugin>

~~~

#### In main project DIRectory

now as we mentioned above in configuration tab we need to create folder and a web.xml file. 

+ From your main project directory create a folder in this path src>main>**webbapp>WEB-INF** (highlighted ones are the new folder names).
+ Under WEB-INF folder create web.xml folder, and paste this content.

~~~
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <display-name>Your Web Application</display-name>
</web-app>
~~~

And change package name to war as shown below.

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/d78283d6-9986-4280-a74c-4898c885b121)

that's it!!

### Maven to Nexus

Maven requires the specification of repository details to deploy the artifacts generated during the package phase of the build process. This configuration is defined through the distributionManagement element, which allows Maven to locate the target repository where the packaged artifacts should be deployed.

#### Changes in the pom.xml

add this <distrubutionmanagment> tag in <projects> tag
~~~
<distributionManagement>
	  <repository>
		  <id>nexus-releases</id>
		  <name>Nexus Release Repository</name>
		  <url>http://13.234.20.254:8081/repository/maven-releases/</url>
		  </repository>


   	<snapshotRepository>
     		 <id>nexus-snapshots</id>
		 <url>http://13.234.20.254:8081/repository/maven-snapshots/</url>
   	</snapshotRepository>
</distributionManagement>
~~~


Nexus makes it easy to determine the URLs of its hosted repositories – each repository displays the exact entry to be added in the <distributionManagement> of the project pom, under the Summary tab.


#### Changes in Plugins tag

Maven handles the deployment mechanism via the `maven-deploy-plugin` – this mapped to the deployment phase of the default Maven lifecycle.

~~~
<plugin>
         <artifactId>maven-deploy-plugin</artifactId>
         <version>2.8.1</version>
         <executions>
            <execution>
              <id>default-deploy</id>
              <phase>deploy</phase>
              <goals>
                <goal>deploy</goal>
              </goals>
            </execution>
          </executions>
</plugin>
~~~

While the `maven-deploy-plugin` can handle the task of deploying project artifacts to Nexus, it does not fully leverage the advanced features offered by Nexus. To take advantage of Nexus's capabilities, such as staging functionality, Sonatype developed the `nexus-staging-maven-plugin`. This custom Nexus plugin is specifically designed to interact seamlessly with Nexus and harness its advanced functionalities.


Despite the staging functionality not being a requirement for a simple deployment process, we will proceed with the `nexus-staging-maven-plugin`. This decision is driven by the plugin's purpose-built nature, ensuring optimal communication and integration with Nexus.

add this <plugin> tag under <**plugins**> tag in pom.xml 

~~~
<plugin>
          <groupId>org.sonatype.plugins</groupId>
          <artifactId>nexus-staging-maven-plugin</artifactId>
          <version>1.5.1</version>
          <executions>
            <execution>
              <id>default-deploy</id>
              <phase>deploy</phase>
              <goals>
                <goal>deploy</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <serverId>nexus</serverId>
            <nexusUrl>http://13.234.20.254:8081/nexus/</nexusUrl>
            <skipStaging>true</skipStaging>
          </configuration>
</plugin>
~~~

The deploy goal of the plugin is mapped to the deploy phase of the Maven build. Also notice that, as discussed, we do not need staging functionality in a simple deployment of -SNAPSHOT artifacts to Nexus, so that is fully disabled via the <skipStaging> element.


#### Settings.xml 

Deploying to Nexus is a secured operation, and a dedicated deployment user is provided by default on any Nexus instance for this purpose.

Maven needs to be configured with this deployment user's credentials to interact properly with Nexus. However, these credentials cannot be placed in the project's `pom.xml` file because its syntax does not support this, and the `pom.xml` may be a public artifact, making it unsuitable for storing sensitive information.

Instead, the server credentials must be defined in Maven's global `settings.xml` file.


~~~
 <servers>
    <!-- server
     | Specifies the authentication information to use when connecting to a particular server, identified by
     | a unique name within the system (referred to by the 'id' attribute below).
     |
     | NOTE: You should either specify username/password OR privateKey/passphrase, since these pairings are
     |       used together.
    | -->
    <server>
      <id>nexus-releases</id>
      <username>admin</username>
      <password>qwerty1234</password>
    </server>

    <server>
      <id>nexus-snapshots</id>
      <username>admin</username>
      <password>qwerty1234</password>
    </server>

    <!-- Another sample, using keys to authenticate.
    <server>
      <id>siteServer</id>
      <privateKey>/path/to/private/key</privateKey>
      <passphrase>optional; leave empty if not used.</passphrase>
    </server>
    -->
  </servers>
~~~

Add your username and password as shown above. Make these changes in `settings.xlm` located in your installation folder. 

now if you want to test it run this command where your pom.xlm is located. 

~~~
mvn clean deploy -Dmaven.test.skip=true
~~~



### Sonarqube configuration

Sonarqube is used for static code analysis but where dooes that code comes from ? well if you are using Gitlab cicd then sonarqube fetchs data from maven/nexus repo. for that we need to make some changes in `pom.xml`

add this under the existing <**plugins**> tag:

~~~
        <plugin>
          <groupId>org.sonarsource.scanner.maven</groupId>
          <artifactId>sonar-maven-plugin</artifactId>
          <version>3.9.1.2184</version>
        </plugin>
~~~

and then in your `.gitlab-ci.yml` add this: (it contains for all the applications if u want to try enjoy!)

~~~
variables:
   MAVEN_OPTS: -Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository
   MAVEN_CLI_OPTS: "-s ./settings.xml --batch-mode"
   # MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  
  

image: maven:latest
  
# maven:latest

stages:
    - build
    - deploy
    - sonarqube

cache:
  paths:
    - .m2/repository
    - target

# sonarqube-check:
#   stage: sonarqube  
#   variables:
#     SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
#     GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
#   cache:
#     key: "${CI_JOB_NAME}"
#     paths:
#       - .sonar/cache
#   script: 
#     - mvn verify sonar:sonar -Dsonar.projectKey=Sonar-maven
#   allow_failure: true
#   only:
#     - main

maven-build:
  stage: build
  script:
    - 'mvn compile'
    - 'mvn $MAVEN_OPTS package -Dmaven.test.skip=true'
    - echo "Packaging the code"
    # - 'mvn deploy'

  artifacts: 
   paths: 
      - target/*.war 


nexus-deploy:
  stage: deploy
  script:
      - mvn $MAVEN_CLI_OPTS deploy -Dmaven.test.skip=true 
      - echo "installing the package in local repository" 
   #  - 'mvn deploy -DskipTests -DaltDeploymentRepository=nexus::default::$NEXUS_URL'
    # - 'mvn $MAVEN_OPTS deploy -Dmaven.test.skip=true'



  # environment:
  #   name: nexus-deployment
  # variables:
  #   # MAVEN_SETTINGS_XML: "${CI_PROJECT_DIR}/settings.xml"
  #   NEXUS_USER: "$NEXUS_USER"
  #   NEXUS_PASS: "$NEXUS_PASS"
  # dependencies:
  #   - maven-build

# tomcat-deploy:
#   stage: deploy
#   script:
#     - 'curl --upload-file target/*.war "$TOMCAT_URL/manager/text/deploy?path=/app&update=true" -u $TOMCAT_USER:$TOMCAT_PASS'
#   dependencies:
#     - maven-build

~~~

> Now go to sonarqube application and click on new project and get your Access token then in the next step it will give a command to run where your maven and nexus is configured. 

### Access Tokens for Github and Gitlab

we need access tokens from git server so that our application(jenkins) can communicate on our behalf and automate things.

#### For GitLab


1. Login to you git lab and go to edit profile. 

![Screenshot 2024-06-04 174114](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/f7f8c1ff-78c5-4cc4-8fe7-8ae36dd2638f)


2. Click on access token and add new token.

![Screenshot 2024-06-04 174305](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/d046c5bd-1241-4278-94d7-bd2c262e1d15)


3. Give a name and give read scope along with read/write. if you wish to do something else then include scope for that too.

![Screenshot 2024-06-04 174607](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/fec474e5-d615-4887-92e3-15e9d7acb08b)

that's it done!!

#### FOr Github

1. Login then on right menu go for settings.

![Screenshot 2024-06-04 175340](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/5c4e20aa-1f97-44ef-b6e5-3e5f6da536de)

2. Scroll down and go fr developer settings.

![Screenshot 2024-06-04 175539](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/ff8fd142-e564-47ac-b31b-e8700a6f8042)

3. you will see a bunch of option here, select classic token.

> Personal access tokens (classic) function like ordinary OAuth access tokens. They can be used instead of a password for Git over HTTPS, or can be used to authenticate to the API over Basic Authentication.

![Screenshot 2024-06-04 175720](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/cd2d39d0-167c-4d93-aaab-2bda0236a4b2)

fill up name and scope and that's it!!


### Configuration of Jenkines 

Now we have to integrate everything we have donw with Jenkins.

#### Download required plugins 

Before we go start with building pipeline we need to install a few plugins. I will provide the list here check out that. 

1. deploy to container
2. Maven Integration plugin
3. Nexus Artifact Uploader
4. SonarQube Scanner for Jenkins
5. Git plugin
6. GitHub API Plugin
7. GitLab API Plugin
8. GitHub plugin
9. GitLab Authentication plugin
10. GitLab Plugin

So, here i mentioned both gitlab and github plugins you can install the one which u will be using for this project. 

#### Setup Credentials  

In your Jenkins, open Manage Jenkins > System > Global credentials domain


![Screenshot 2024-06-04 180206](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/eb59ef55-9336-4376-9894-581da60fe06f)

Don't go for add new domain. 

![Screenshot 2024-06-04 180617](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/dc51eee3-0b0f-4590-9b4e-246b13589405)

Click on ` + add credentails`

![Screenshot 2024-06-04 180844](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/b067ba25-caa5-4d86-89f2-5802b4cec7d5)


Add name to it and then add your access token. 

Do the same thing for username and password / access tokens for all the applications we configured. this step is important as this will allow jenkins to access those application and take required action. 


![Screenshot 2024-06-04 173508](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/e089a576-42ff-459d-88d3-7127a79a6dea)


and just like that you have added all your credentials to jenkins!!


#### Jenkins Tools Configuration 

The plan of integrate jenkines with other applications translates here by configuring tools here, which inturn install them in your VM to run all the commands such as `mvn clean deploy` . 

Go to dashboard > Manage Jenkins > Tools 

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/1904c598-b083-44bf-9903-4199d20624ee)

Here we Maven Configuration, select default/Global configuration for maven. if we scroll along we will see JDBC setting here you will enter Postgress url if port configuration is unaltered then its  http:// <public_IP>:5432 (include context path if there is any), and select JDBC credentials from drop down menu. 


if we scroll down we will see JDK and GIT installation select as shown in the figure: 

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/5e198919-d72d-4449-a6f0-1f2776ff6f59)

In a similar fashion select sonarqube:  


![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/1214b764-b22a-4914-b1ed-a6ae059eb298)


and maven:

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/6cfc2d61-cf23-4ad7-82c2-9bbf53f49182)


if we click on add installer we will notice other ways to install our application, if you wish to proceed with any specific version you want, you can do that here.



## Pipeline Setup

Fom jenkins dashboard click on `+ build item` then you see this screen :


![Screenshot 2024-06-04 173231](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/ca6fc0dc-d555-492d-98b4-4bb37c8e80ac)


now select maven project and then click ok. you will see this screen:

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/e629282b-76e6-40c4-85d0-40b46bef96cc)


on the left hand side you will see these sections:

1. **General**: Configures basic project details and general settings.
2. **Source Code Management**: Defines where and how to retrieve the source code.
3. **Build Triggers**: Specifies conditions that initiate the build process.
4. **Build Environment**: Sets up the environment before the build starts.
5. **Pre Steps**: Executes tasks before the main build steps.
6. **Build**: Contains the primary steps to compile, test, and package the code.
7. **Post Steps**: Executes tasks immediately after the build steps.
8. **Build Settings**: Provides additional configurations for the build process.
9. **Post-build Actions**: Defines actions to perform after the build is completed, like notifications or deployments.


if scroll down in general section you will notice `GitLab Connection` (i was using gitlab) select drop down versin and selct your gitlab/github account name (it will be the same name which u have given for your access token).


If you want jenkins to pick changes from Github/lab you need to fill up Source code management section and fill up the details as shown in the below image.  

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/73f40d09-bba3-42a4-ac12-cacf4494fc71)

These are some Build triggers i have set pretty much self explonatory. 

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/b9114f7e-9810-4f0a-9019-55d09441dbc4)


now in pre-build section we will these option: 
![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/80c24cc9-03fb-4ddf-8e33-9741213458f6)

select execute sonarqube: 

Select the JDK version as configured previously.

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/b0792cf8-b15d-4f30-90a0-b0b18c48a6b9)

Coming to analysis proerties, which allows to pass some configuration parameters to SonarQube.

> Parameters defined here take precedence over the ones potentially defined in the specified sonar-project.properties file. It is even possible to specify all SonarQube parameters here and leave the "Path to project properties" input field empty.

so i have defined: 

~~~
sonar.projectKey=sonar_jenkins
sonar.projectName=sonar_jenkins
sonar.projectVersion=1.0
sonar.sources=src/main/java
sonar.java.binaries=target/classes
~~~

i will explain how i got path for `sonar.java.binaries=target/classes` (which might not be the same for everyone) in debugging section. 

+ i have selected additional parameters as `-X` which means debugging more it helps to solve error if you happen to find any.
+ I am not showing nexus part its up to you configure in such a way that, when you puch code it uploads a file to nexus repo.

IN build setting you can configure your email. 

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/5bfd98f9-59d9-4879-b3b0-739921d70a74)


In post build actions :

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/17215124-6d1c-4c08-b22e-4a1d1f0df17d)

**execute pipeline and check:**

> http://<TOMCAT_SERVER_IP>:8080/yourContextPath(mywebauto)

 ![Screenshot 2024-05-28 224907](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/f60a85ed-58fa-4f97-826c-c6f7dfb7d767)


you will see your app running,

if you wish to check more about code (static analysis) 

visit: http://<SONARQUBE_SERVER_IP>:9000

![Screenshot 2024-05-30 125904](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/b62e76a7-1722-4539-8aef-2ded33df13a2)

similarly check for nexus repo:

![Screenshot 2024-05-31 111750](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/d15c089a-ada1-4cc4-8a66-b588a34fb2f4)

## Understanding the Errors

Initially, I got alot of errors in setting up each application server. A few of them are:

1.**Nexus Authentication**

I was getting Authentication error while pushing my build to nexus repo. 
> Error 401 build unsuccessful
> > **401 Unauthorized Error is an HTTP status code error that represents the request sent by the client to the server that lacks valid authentication credentials.**

~~~
[INFO] --- maven-deploy-plugin:2.8.1:deploy (default-deploy) @ my-app ---
Downloading from nexus-snapshots: http://3.110.180.206:8081/repository/maven-snapshots/com/mycompany/app/my-app/1.0-SNAPSHOT/maven-metadata.xml
Uploading to nexus-snapshots: http://3.110.180.206:8081/repository/maven-snapshots/com/mycompany/app/my-app/1.0-SNAPSHOT/my-app-1.0-20240515.111829-1.war
Uploading to nexus-snapshots: http://3.110.180.206:8081/repository/maven-snapshots/com/mycompany/app/my-app/1.0-SNAPSHOT/my-app-1.0-20240515.111829-1.pom
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.606 s
[INFO] Finished at: 2024-05-15T11:18:29Z
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-deploy-plugin:2.8.1:deploy (default-deploy) on project my-app: Failed to deploy artifacts: Could not transfer artifact com.mycompany.app:my-app:war:1.0-20240515.111829-1 from/to nexus-snapshots (http://3.110.180.206:8081/repository/maven-snapshots/): Transfer failed for http://3.110.180.206:8081/repository/maven-snapshots/com/mycompany/app/my-app/1.0-SNAPSHOT/my-app-1.0-20240515.111829-1.war 401 Unauthorized -> [Help 1]
[ERROR]
~~~

After some researching i found there are **2 Settings.xml** files, one for global setting and one for local user. when we are making request like this, Nexus credentials where we menntion in `settings.xml` must be written in the **gloabl file**. 

+ Location for global files can be found in installation folder under `bin` or in `conf`.
+ Location to local files can be found in .M2 repository ( `cd ~` then `cd .m2`).
+ And wherever name you write under <id> tag the value in this tag should be the **same** in all the places used in this regards.

if you have network issues and unable to open webpage then you can access vis this command.

~~~
curl -u your-username:your-password http://3.110.180.206:8081/repository/maven-snapshots/
~~~

if you want to get prompted in debugging mode then use `-X`: 

~~~
mvn deploy -X
~~~

2. **Sonar Errors**
![f375d287-037d-4a7f-b885-f1e2da8fc605](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/58b75d12-5e15-47f4-936b-7d6083b924a0)

i got this while setting up sonarqube server i was using brand new version which only supports **Java version above 11** it does not support even java 11. i installed java 17 and problem was solved instantly. 

+ You need to understand why an error occured, you have t question your self wwhere you went wrong and answer for that lies in **logs.**

 ANother Error example is this:

 ~~~
[ELASTICSEARCH\] from \[/opt/sonarqube/elasticsearch\]: /usr/lib/jvm/java-17-amazon-corretto.x86\_64/bin/java -Xms4m -Xmx64m -XX:+UseSerialGC -Dcli.name=server -Dcli.script=./bin/elasticsearch -Dcli.libs=lib/tools/server-cli -Des.path.home=/opt/sonarqube/elasticsearch -Des.path.conf=/opt/sonarqube/temp/conf/es -Des.distribution.type=tar -cp /opt/sonarqube/elasticsearch/lib/\*:/opt/sonarqube/elasticsearch/lib/cli-launcher/\* org.elasticsearch.launcher.CliToolLauncher

2024.05.29 09:19:39 INFO app\[\]\[o.s.a.SchedulerImpl\] Waiting for Elasticsearch to be up and running

2024.05.29 09:19:45 WARN app\[\]\[o.s.a.p.AbstractManagedProcess\] Process exited with exit value \[ElasticSearch\]: 1

2024.05.29 09:19:45 INFO app\[\]\[o.s.a.SchedulerImpl\] Process\[ElasticSearch\] is stopped

2024.05.29 09:19:45 INFO app\[\]\[o.s.a.SchedulerImpl\] SonarQube is stopped
~~~

- The log indicates that the embedded **Elasticsearch process failed to start** correctly, which is causing SonarQube to fail to start as well.
- Check Elasticsearch logs: The SonarQube log doesn't provide details on why Elasticsearch failed to start. Look for the Elasticsearch log files, which should be located in the `/opt/sonarqube/logs/` directory open `es.log` file foe elasticsearch related issues. The log files may provide more information about the failure.
- After reading logs i got to know, i havent given **read/write permissions** on the `/opt/sonarqube` directory and its subdirectories. so running `sudo chown -R sonarqube:sonarqube /opt/sonarqube` solved this error.


3. **Jenkins Errors**

Ironically i was getting the same sonarqube error when i was running jenkins pipeline for the first time. I realised this is because I kept incorrect path for _Analysis properties_ --> `sonar.source` parameter. 

in my finding i found out: 

+ The sonar.sources property is used to specify the paths to the source code directories that you want SonarQube to analyze.
+  source code is in the root directory of your project then `sonar.sources=.`.
+  If your source code is in a subdirectory (src/main/java) then: `sonar.sources=src/main/java`.
+  If your source code is in multiple subdirectories (src/main/java, src/main/resources) then: `sonar.sources=src/main/java,src/main/resources`
+  Multiple dir but same Parent dir then `sonar.sources=src`

Ironic part was idk my path, i was confused at this point then, i wrote a script and executed as pre-build in jenkins to print out my path, later i edited my `sonar.source` parameter with correct path. 

![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/01887b5b-c6b3-4e54-ab97-4119783027ec)


script :

~~~
find . -type d -name 'java' -printf '%P\n'
~~~

This command will list all directories named 'java' (replace 'java' with your source code directory name if different) relative to the current directory. You can then use this output to construct the sonar.sources value.

>>Note that if your project uses multiple programming languages like Java and JavaScript, you may need to specify separate **sonar.sources** values for each language using the appropriate language-specific property: **sonar.java.sources** and **sonar.javascript.sources**.

Immediately after this i got this output: 

~~~
11:48:22 Started by user somesh rao
11:48:22 Running as SYSTEM
11:48:22 Building in workspace /var/lib/jenkins/workspace/sonarnexus
11:48:22 The recommended git tool is: NONE
11:48:22 using credential 6886c502-3e09-4920-8c53-c07d01f3377d
11:48:22  > git rev-parse --resolve-git-dir /var/lib/jenkins/workspace/sonarnexus/.git # timeout=10
11:48:22 Fetching changes from the remote Git repository
11:48:22  > git config remote.origin.url https://gitlab.com/devopsprojects4800914/maven-java-ci-cd.git # timeout=10
11:48:22 Fetching upstream changes from https://gitlab.com/devopsprojects4800914/maven-java-ci-cd.git
11:48:22  > git --version # timeout=10
11:48:22  > git --version # 'git version 2.40.1'
11:48:22 using GIT_ASKPASS to set credentials GITLAB SOMESH
11:48:22  > git fetch --tags --force --progress -- https://gitlab.com/devopsprojects4800914/maven-java-ci-cd.git +refs/heads/*:refs/remotes/origin/* # timeout=10
11:48:23  > git rev-parse refs/remotes/origin/main^{commit} # timeout=10
11:48:23 Checking out Revision 058ccda6e01d42649f502b5bebf5c275ed81cfbf (refs/remotes/origin/main)
11:48:23  > git config core.sparsecheckout # timeout=10
11:48:23  > git checkout -f 058ccda6e01d42649f502b5bebf5c275ed81cfbf # timeout=10
11:48:23 Commit message: "Update pom.xml"
11:48:23  > git rev-list --no-walk 058ccda6e01d42649f502b5bebf5c275ed81cfbf # timeout=10
11:48:23 [sonarnexus] $ /bin/sh -xe /tmp/jenkins10354394323322641499.sh
11:48:23 + find . -type d -name java -printf '%P\n'
11:48:23 src/main/java
11:48:23 src/test/java
11:48:23 [sonarnexus] $ /var/lib/jenkins/tools/hudson.plugins.sonar.SonarRunnerInstallation/qubeforanalysis/bin/sonar-scanner -X -Dsonar.host.url=http://3.110.50.2:9000/ ******** -Dsonar.projectKey=sonar_jenkins -Dsonar.projectName=sonar_jenkins -Dsonar.projectVersion=1.0 -Dsonar.sources=. -Dsonar.projectBaseDir=/var/lib/jenkins/workspace/sonarnexus
11:48:23 06:18:23.848 INFO: Scanner configuration file: /var/lib/jenkins/tools/hudson.plugins.sonar.SonarRunnerInstallation/qubeforanalysis/conf/sonar-scanner.properties
11:48:23 06:18:23.889 INFO: Project root configuration file: NONE
11:48:23 06:18:23.943 INFO: SonarScanner 5.0.1.3006
11:48:23 06:18:23.944 INFO: Java 17.0.11 Amazon.com Inc. (64-bit)
11:48:23 06:18:23.944 INFO: Linux 6.1.90-99.173.amzn2023.x86_64 amd64
11:48:24 06:18:24.305 DEBUG: keyStore is : 
11:48:24 06:18:24.306 DEBUG: keyStore type is : pkcs12
11:48:24 06:18:24.307 DEBUG: keyStore provider is : 
11:48:24 06:18:24.307 DEBUG: init keystore
11:48:24 06:18:24.308 DEBUG: init keymanager of type SunX509
11:48:24 06:18:24.541 DEBUG: Create: /var/lib/jenkins/.sonar/cache
11:48:24 06:18:24.542 INFO: User cache: /var/lib/jenkins/.sonar/cache
11:48:24 06:18:24.543 DEBUG: Create: /var/lib/jenkins/.sonar/cache/_tmp
11:48:24 06:18:24.546 DEBUG: Extract sonar-scanner-api-batch in temp...
11:48:24 06:18:24.550 DEBUG: Get bootstrap index...
11:48:24 06:18:24.551 DEBUG: Download: http://3.110.50.2:9000/batch/index
11:48:24 06:18:24.653 DEBUG: Get bootstrap completed
11:48:24 06:18:24.665 DEBUG: Create isolated classloader...
11:48:24 06:18:24.694 DEBUG: Start temp cleaning...
11:48:24 06:18:24.703 DEBUG: Temp cleaning done
11:48:24 06:18:24.703 DEBUG: Execution getVersion
11:48:24 06:18:24.732 INFO: Analyzing on SonarQube server 10.5.1.90531
11:48:24 06:18:24.732 INFO: Default locale: "en", source code encoding: "UTF-8" (analysis is platform dependent)
11:48:24 06:18:24.733 DEBUG: Work directory: /var/lib/jenkins/workspace/sonarnexus/.scannerwork
11:48:24 06:18:24.735 DEBUG: Execution execute
11:48:25 06:18:25.463 DEBUG: Community 10.5.1.90531
11:48:25 06:18:25.907 INFO: Load global settings
11:48:26 06:18:26.003 DEBUG: GET 200 http://3.110.50.2:9000/api/settings/values.protobuf | time=94ms
11:48:26 06:18:26.072 INFO: Load global settings (done) | time=165ms
11:48:26 06:18:26.122 INFO: Server id: 147B411E-AY_HwAqcsSyVoJJeB1fg
11:48:26 06:18:26.136 INFO: User cache: /var/lib/jenkins/.sonar/cache
11:48:26 06:18:26.149 INFO: Loading required plugins
11:48:26 06:18:26.150 INFO: Load plugins index
11:48:26 06:18:26.167 DEBUG: GET 200 http://3.110.50.2:9000/api/plugins/installed | time=17ms
11:48:26 06:18:26.214 INFO: Load plugins index (done) | time=64ms
11:48:26 06:18:26.215 INFO: Load/download plugins
11:48:26 06:18:26.314 INFO: Load/download plugins (done) | time=99ms
11:48:26 06:18:26.314 DEBUG: Plugins not loaded because they are optional: [csharp, flex, go, web, java, javascript, kotlin, php, ruby, sonarscala, vbnet]
11:48:26 06:18:26.357 DEBUG: Plugins loaded:
11:48:26 06:18:26.361 DEBUG:   * Python Code Quality and Security 4.17.0.14845 (python)
11:48:26 06:18:26.361 DEBUG:   * Clean as You Code 2.3.0.1782 (cayc)
11:48:26 06:18:26.361 DEBUG:   * XML Code Quality and Security 2.10.0.4108 (xml)
11:48:26 06:18:26.361 DEBUG:   * JaCoCo 1.3.0.1538 (jacoco)
11:48:26 06:18:26.362 DEBUG:   * IaC Code Quality and Security 1.27.0.9518 (iac)
11:48:26 06:18:26.362 DEBUG:   * Text Code Quality and Security 2.10.0.2188 (text)
11:48:26 06:18:26.642 DEBUG: register org.eclipse.jgit.util.FS$FileStoreAttributes$$Lambda$300/0x00007f689027c200@75b21c3b with shutdown hook
11:48:26 06:18:26.903 INFO: Process project properties
11:48:26 06:18:26.916 INFO: Process project properties (done) | time=12ms
11:48:26 06:18:26.932 INFO: Project key: sonar_jenkins
11:48:26 06:18:26.933 INFO: Base dir: /var/lib/jenkins/workspace/sonarnexus
11:48:26 06:18:26.933 INFO: Working dir: /var/lib/jenkins/workspace/sonarnexus/.scannerwork
11:48:26 06:18:26.933 DEBUG: Project global encoding: UTF-8, default locale: en
11:48:26 06:18:26.949 INFO: Load project settings for component key: 'sonar_jenkins'
11:48:26 06:18:26.990 DEBUG: GET 200 http://3.110.50.2:9000/api/settings/values.protobuf?component=sonar_jenkins | time=40ms
11:48:26 06:18:26.998 INFO: Load project settings for component key: 'sonar_jenkins' (done) | time=49ms
11:48:27 06:18:27.044 DEBUG: Creating module hierarchy
11:48:27 06:18:27.045 DEBUG:   Init module 'sonar_jenkins'
11:48:27 06:18:27.046 DEBUG:     Base dir: /var/lib/jenkins/workspace/sonarnexus
11:48:27 06:18:27.046 DEBUG:     Working dir: /var/lib/jenkins/workspace/sonarnexus/.scannerwork
11:48:27 06:18:27.046 DEBUG:     Module global encoding: UTF-8, default locale: en
11:48:27 06:18:27.065 INFO: Load quality profiles
11:48:27 06:18:27.218 DEBUG: GET 200 http://3.110.50.2:9000/api/qualityprofiles/search.protobuf?project=sonar_jenkins | time=152ms
11:48:27 06:18:27.256 INFO: Load quality profiles (done) | time=191ms
11:48:27 06:18:27.279 INFO: Auto-configuring with CI 'Jenkins'
11:48:27 06:18:27.323 INFO: Load active rules
11:48:27 06:18:27.498 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=6e28ff22-d416-4438-9f7d-f74e8a1b2f38&ps=500&p=1 | time=174ms
11:48:27 06:18:27.831 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=7e356748-cad6-46c8-95ca-cd87956e55bf&ps=500&p=1 | time=175ms
11:48:28 06:18:28.046 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=cc2d38ed-cdff-4a72-a425-5d389e37e174&ps=500&p=1 | time=201ms
11:48:28 06:18:28.069 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=6e41c646-24f6-4101-bb7b-6c703964c5f6&ps=500&p=1 | time=13ms
11:48:29 06:18:29.837 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=48163586-12df-4b41-abfa-2db4fd3faa25&ps=500&p=1 | time=1766ms
11:48:31 06:18:31.194 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=8ecf26c1-3064-4dab-9621-0944148c0e45&ps=500&p=1 | time=1249ms
11:48:31 06:18:31.537 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=86296b7d-d160-4a73-96c3-6ecb8de5f10e&ps=500&p=1 | time=250ms
11:48:34 06:18:34.011 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=e5919e0e-9958-431a-b145-c058770797f0&ps=500&p=1 | time=2467ms
11:48:34 06:18:34.307 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=e5919e0e-9958-431a-b145-c058770797f0&ps=500&p=2 | time=131ms
11:48:34 06:18:34.782 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=b6a54bd0-3a5d-46b7-8e7b-65a245e36a53&ps=500&p=1 | time=467ms
11:48:34 06:18:34.939 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=df347da7-9402-43a7-9fc8-9e192c733e59&ps=500&p=1 | time=153ms
11:48:35 06:18:35.102 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=af805cf3-2a10-4cd5-9cc6-fee0ff4e5994&ps=500&p=1 | time=158ms
11:48:35 06:18:35.123 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=d2c84546-8d79-48cc-bf38-ddc2ffbe946b&ps=500&p=1 | time=17ms
11:48:35 06:18:35.147 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=afd7eb4d-b9a6-439a-93d4-f3d6755b04da&ps=500&p=1 | time=22ms
11:48:35 06:18:35.858 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=9956ba37-ea7b-4953-9ba2-d416c57a6a8a&ps=500&p=1 | time=708ms
11:48:36 06:18:36.103 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=d8e18aec-1995-4fde-90b3-ab0c2ef37c8f&ps=500&p=1 | time=233ms
11:48:36 06:18:36.120 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=1c17cf42-f52e-4bc0-8ce9-ef126732888d&ps=500&p=1 | time=14ms
11:48:36 06:18:36.198 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=1e4a1993-e64f-4f8e-bbe6-aa4d9941c837&ps=500&p=1 | time=77ms
11:48:36 06:18:36.990 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=efdc2227-8807-4c89-aa36-3712bbf9e013&ps=500&p=1 | time=790ms
11:48:37 06:18:37.635 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=ab2da13f-d6bb-49f8-ac7f-33c59c347096&ps=500&p=1 | time=635ms
11:48:37 06:18:37.779 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=26704e00-d3ae-4263-9598-c8db3a45418d&ps=500&p=1 | time=139ms
11:48:39 06:18:39.195 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=b7002a79-b186-44c3-b5a9-f1cf0e9c4f10&ps=500&p=1 | time=1413ms
11:48:39 06:18:39.871 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=aa47980b-55de-4460-ad70-233ce0c676c8&ps=500&p=1 | time=652ms
11:48:40 06:18:40.332 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=e7810812-c702-40da-b278-6efc66a8b38d&ps=500&p=1 | time=454ms
11:48:40 06:18:40.461 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=ccd84e8f-76eb-414c-b3d7-8ffad3db8d85&ps=500&p=1 | time=126ms
11:48:42 06:18:42.152 DEBUG: GET 200 http://3.110.50.2:9000/api/rules/list.protobuf?qprofile=0c437d47-0061-449a-9339-3395ff61d661&ps=500&p=1 | time=1688ms
11:48:42 06:18:42.243 INFO: Load active rules (done) | time=14920ms
11:48:42 06:18:42.268 INFO: Load analysis cache
11:48:42 06:18:42.285 DEBUG: GET 404 http://3.110.50.2:9000/api/analysis_cache/get?project=sonar_jenkins | time=16ms
11:48:42 06:18:42.288 INFO: Load analysis cache (404) | time=20ms
11:48:42 06:18:42.349 DEBUG: GET 200 http://3.110.50.2:9000/api/languages/list | time=12ms
11:48:42 06:18:42.452 DEBUG: Declared patterns of language Kubernetes were converted to sonar.lang.patterns.kubernetes : 
11:48:42 06:18:42.460 DEBUG: Declared patterns of language CSS were converted to sonar.lang.patterns.css : **/*.css,**/*.less,**/*.scss,**/*.sass
11:48:42 06:18:42.461 DEBUG: Declared patterns of language Scala were converted to sonar.lang.patterns.scala : **/*.scala
11:48:42 06:18:42.462 DEBUG: Declared patterns of language JSP were converted to sonar.lang.patterns.jsp : **/*.jsp,**/*.jspf,**/*.jspx
11:48:42 06:18:42.464 DEBUG: Declared patterns of language JavaScript were converted to sonar.lang.patterns.js : **/*.js,**/*.jsx,**/*.cjs,**/*.mjs,**/*.vue
11:48:42 06:18:42.464 DEBUG: Declared patterns of language Python were converted to sonar.lang.patterns.py : **/*.py
11:48:42 06:18:42.465 DEBUG: Declared patterns of language Docker were converted to sonar.lang.patterns.docker : **/Dockerfile,**/*.dockerfile
11:48:42 06:18:42.466 DEBUG: Declared patterns of language Java were converted to sonar.lang.patterns.java : **/*.java,**/*.jav
11:48:42 06:18:42.468 DEBUG: Declared patterns of language HTML were converted to sonar.lang.patterns.web : **/*.html,**/*.xhtml,**/*.cshtml,**/*.vbhtml,**/*.aspx,**/*.ascx,**/*.rhtml,**/*.erb,**/*.shtm,**/*.shtml,**/*.cmp,**/*.twig
11:48:42 06:18:42.469 DEBUG: Declared patterns of language Flex were converted to sonar.lang.patterns.flex : **/*.as
11:48:42 06:18:42.470 DEBUG: Declared patterns of language XML were converted to sonar.lang.patterns.xml : **/*.xml,**/*.xsd,**/*.xsl,**/*.config
11:48:42 06:18:42.471 DEBUG: Declared patterns of language JSON were converted to sonar.lang.patterns.json : **/*.json
11:48:42 06:18:42.472 DEBUG: Declared patterns of language Text were converted to sonar.lang.patterns.text : 
11:48:42 06:18:42.473 DEBUG: Declared patterns of language VB.NET were converted to sonar.lang.patterns.vbnet : **/*.vb
11:48:42 06:18:42.473 DEBUG: Declared patterns of language CloudFormation were converted to sonar.lang.patterns.cloudformation : 
11:48:42 06:18:42.474 DEBUG: Declared patterns of language YAML were converted to sonar.lang.patterns.yaml : **/*.yaml,**/*.yml
11:48:42 06:18:42.475 DEBUG: Declared patterns of language Go were converted to sonar.lang.patterns.go : **/*.go
11:48:42 06:18:42.477 DEBUG: Declared patterns of language Kotlin were converted to sonar.lang.patterns.kotlin : **/*.kt,**/*.kts
11:48:42 06:18:42.477 DEBUG: Declared patterns of language Secrets were converted to sonar.lang.patterns.secrets : 
11:48:42 06:18:42.478 DEBUG: Declared patterns of language Ruby were converted to sonar.lang.patterns.ruby : **/*.rb
11:48:42 06:18:42.479 DEBUG: Declared patterns of language C# were converted to sonar.lang.patterns.cs : **/*.cs,**/*.razor
11:48:42 06:18:42.481 DEBUG: Declared patterns of language PHP were converted to sonar.lang.patterns.php : **/*.php,**/*.php3,**/*.php4,**/*.php5,**/*.phtml,**/*.inc
11:48:42 06:18:42.482 DEBUG: Declared patterns of language Terraform were converted to sonar.lang.patterns.terraform : **/*.tf
11:48:42 06:18:42.483 DEBUG: Declared patterns of language AzureResourceManager were converted to sonar.lang.patterns.azureresourcemanager : **/*.bicep
11:48:42 06:18:42.488 DEBUG: Declared patterns of language TypeScript were converted to sonar.lang.patterns.ts : **/*.ts,**/*.tsx,**/*.cts,**/*.mts
11:48:42 06:18:42.526 INFO: Preprocessing files...
11:48:42 06:18:42.549 DEBUG: loading config FileBasedConfig[/var/lib/jenkins/.config/jgit/config]
11:48:42 06:18:42.550 DEBUG: readpipe [/bin/git, --version],/bin
11:48:42 06:18:42.572 DEBUG: readpipe may return 'git version 2.40.1'
11:48:42 06:18:42.572 DEBUG: remaining output:
11:48:42 
11:48:42 06:18:42.573 DEBUG: readpipe [/bin/git, config, --system, --show-origin, --list, -z],/bin
11:48:42 06:18:42.577 DEBUG: readpipe may return 'null'
11:48:42 06:18:42.578 DEBUG: remaining output:
11:48:42 
11:48:42 06:18:42.606 DEBUG: readpipe rc=128
11:48:42 06:18:42.607 DEBUG: Exception caught during execution of command '[/bin/git, config, --system, --show-origin, --list, -z]' in '/bin', return code '128', error message 'fatal: unable to read config file '/etc/gitconfig': No such file or directory
11:48:42 '
11:48:42 06:18:42.608 DEBUG: loading config FileBasedConfig[/var/lib/jenkins/.config/git/config]
11:48:42 06:18:42.609 DEBUG: loading config UserConfigFile[/var/lib/jenkins/.gitconfig]
11:48:42 06:18:42.697 DEBUG: 21 non excluded files in this Git repository
11:48:42 06:18:42.766 INFO: 3 languages detected in 18 preprocessed files
11:48:42 06:18:42.767 INFO: 0 files ignored because of scm ignore settings
11:48:42 06:18:42.770 INFO: Loading plugins for detected languages
11:48:42 06:18:42.770 DEBUG: Detected languages: [java, web, xml]
11:48:42 06:18:42.771 INFO: Load/download plugins
11:48:42 06:18:42.816 INFO: Load/download plugins (done) | time=45ms
11:48:42 06:18:42.817 DEBUG: Optional language-specific plugins not loaded: [csharp, flex, go, kotlin, php, ruby, sonarscala, vbnet]
11:48:42 06:18:42.855 DEBUG: Plugins loaded:
11:48:42 06:18:42.855 DEBUG:   * Java Code Quality and Security 7.33.0.35775 (java)
11:48:42 06:18:42.855 DEBUG:   * HTML Code Quality and Security 3.15.0.5107 (web)
11:48:42 06:18:42.855 DEBUG:   * JavaScript/TypeScript/CSS Code Quality and Security 10.13.2.25981 (javascript)
11:48:43 06:18:43.287 INFO: Inconsistent constructor declaration on bean with name 'org.sonarsource.scanner.api.internal.IsolatedClassloader@36b4cef0-org.sonar.scanner.issue.IssueFilters': single autowire-marked constructor flagged as optional - this constructor is effectively required since there is no default constructor to fall back to: public org.sonar.scanner.issue.IssueFilters(org.sonar.api.batch.fs.internal.DefaultInputProject)
11:48:43 06:18:43.314 INFO: Load project repositories
11:48:43 06:18:43.331 DEBUG: GET 200 http://3.110.50.2:9000/batch/project.protobuf?key=sonar_jenkins | time=17ms
11:48:43 06:18:43.340 INFO: Load project repositories (done) | time=26ms
11:48:43 06:18:43.369 DEBUG: Available languages:
11:48:43 06:18:43.369 DEBUG:   * Java => "java"
11:48:43 06:18:43.370 DEBUG:   * HTML => "web"
11:48:43 06:18:43.370 DEBUG:   * JSP => "jsp"
11:48:43 06:18:43.370 DEBUG:   * JavaScript => "js"
11:48:43 06:18:43.370 DEBUG:   * TypeScript => "ts"
11:48:43 06:18:43.370 DEBUG:   * CSS => "css"
11:48:43 06:18:43.370 DEBUG:   * Python => "py"
11:48:43 06:18:43.370 DEBUG:   * XML => "xml"
11:48:43 06:18:43.370 DEBUG:   * Terraform => "terraform"
11:48:43 06:18:43.370 DEBUG:   * CloudFormation => "cloudformation"
11:48:43 06:18:43.371 DEBUG:   * Kubernetes => "kubernetes"
11:48:43 06:18:43.371 DEBUG:   * Docker => "docker"
11:48:43 06:18:43.371 DEBUG:   * AzureResourceManager => "azureresourcemanager"
11:48:43 06:18:43.371 DEBUG:   * YAML => "yaml"
11:48:43 06:18:43.371 DEBUG:   * JSON => "json"
11:48:43 06:18:43.371 DEBUG:   * Text => "text"
11:48:43 06:18:43.371 DEBUG:   * Secrets => "secrets"
11:48:43 06:18:43.373 INFO: Indexing files...
11:48:43 06:18:43.373 INFO: Project configuration:
11:48:43 06:18:43.382 DEBUG: 'pom.xml' indexed with language 'xml'
11:48:43 06:18:43.388 DEBUG: 'settings.xml' indexed with language 'xml'
11:48:43 06:18:43.389 DEBUG: 'src/main/java/com/mycompany/app/App.java' indexed with language 'java'
11:48:43 06:18:43.390 DEBUG: 'src/main/webapp/WEB-INF/web.xml' indexed with language 'xml'
11:48:43 06:18:43.390 DEBUG: 'src/main/webapp/index.html' indexed with language 'web'
11:48:43 06:18:43.391 DEBUG: 'src/test/java/com/mycompany/app/AppTest.java' indexed with language 'java'
11:48:43 06:18:43.393 DEBUG: 'target/classes/com/mycompany/app/App.class' indexed with no language
11:48:43 06:18:43.394 DEBUG: 'target/maven-archiver/pom.properties' indexed with no language
11:48:43 06:18:43.398 DEBUG: 'target/maven-status/maven-compiler-plugin/compile/default-compile/createdFiles.lst' indexed with no language
11:48:43 06:18:43.401 DEBUG: 'target/maven-status/maven-compiler-plugin/compile/default-compile/inputFiles.lst' indexed with no language
11:48:43 06:18:43.403 DEBUG: 'target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/createdFiles.lst' indexed with no language
11:48:43 06:18:43.404 DEBUG: 'target/maven-status/maven-compiler-plugin/testCompile/default-testCompile/inputFiles.lst' indexed with no language
11:48:43 06:18:43.405 DEBUG: 'target/my-app-1.0-SNAPSHOT.war' indexed with no language
11:48:43 06:18:43.406 DEBUG: 'target/my-app-1.0-SNAPSHOT/WEB-INF/classes/com/mycompany/app/App.class' indexed with no language
11:48:43 06:18:43.407 DEBUG: 'target/my-app-1.0-SNAPSHOT/WEB-INF/web.xml' indexed with language 'xml'
11:48:43 06:18:43.407 DEBUG: 'target/surefire-reports/TEST-com.mycompany.app.AppTest.xml' indexed with language 'xml'
11:48:43 06:18:43.408 DEBUG: 'target/surefire-reports/com.mycompany.app.AppTest.txt' indexed with no language
11:48:43 06:18:43.409 DEBUG: 'target/test-classes/com/mycompany/app/AppTest.class' indexed with no language
11:48:43 06:18:43.410 INFO: 18 files indexed
11:48:43 06:18:43.412 INFO: Quality profile for java: Sonar way
11:48:43 06:18:43.412 INFO: Quality profile for web: Sonar way
11:48:43 06:18:43.413 INFO: Quality profile for xml: Sonar way
11:48:43 06:18:43.413 INFO: ------------- Run sensors on module sonar_jenkins
11:48:43 06:18:43.549 INFO: Load metrics repository
11:48:43 06:18:43.567 DEBUG: GET 200 http://3.110.50.2:9000/api/metrics/search?ps=500&p=1 | time=17ms
11:48:43 06:18:43.588 INFO: Load metrics repository (done) | time=39ms
11:48:45 06:18:45.415 DEBUG: Added 308 checks for language='ts', repository='typescript'
11:48:45 06:18:45.422 DEBUG: Added 307 checks for language='js', repository='javascript'
11:48:45 06:18:45.480 DEBUG: 'Import external issues report' skipped because one of the required properties is missing
11:48:45 06:18:45.480 DEBUG: 'Python Sensor' skipped because there is no related file in current project
11:48:45 06:18:45.480 DEBUG: 'Cobertura Sensor for Python coverage' skipped because there is no related file in current project
11:48:45 06:18:45.481 DEBUG: 'PythonXUnitSensor' skipped because there is no related file in current project
11:48:45 06:18:45.482 DEBUG: 'Import of Pylint issues' skipped because there is no related file in current project
11:48:45 06:18:45.482 DEBUG: 'Import of Bandit issues' skipped because there is no related file in current project
11:48:45 06:18:45.482 DEBUG: 'Import of Flake8 issues' skipped because there is no related file in current project
11:48:45 06:18:45.482 DEBUG: 'Import of Mypy issues' skipped because there is no related file in current project
11:48:45 06:18:45.482 DEBUG: 'Import of Ruff issues' skipped because there is no related file in current project
11:48:45 06:18:45.483 DEBUG: 'Import of Checkstyle issues' skipped because one of the required properties is missing
11:48:45 06:18:45.483 DEBUG: 'Import of PMD issues' skipped because one of the required properties is missing
11:48:45 06:18:45.484 DEBUG: 'Import of SpotBugs issues' skipped because one of the required properties is missing
11:48:45 06:18:45.484 DEBUG: 'Removed properties sensor' skipped because one of the required properties is missing
11:48:45 06:18:45.486 DEBUG: 'IaC Terraform Sensor' skipped because there is no related file in current project
11:48:45 06:18:45.486 DEBUG: 'IaC CloudFormation Sensor' skipped because there is no related file in current project
11:48:45 06:18:45.486 DEBUG: 'IaC Kubernetes Sensor' skipped because there is no related file in current project
11:48:45 06:18:45.487 DEBUG: 'IaC AzureResourceManager Sensor' skipped because there is no related file in current project
11:48:45 06:18:45.487 DEBUG: 'JavaScript/TypeScript analysis' skipped because there is no related file in current project
11:48:45 06:18:45.487 DEBUG: 'JavaScript inside YAML analysis' skipped because there is no related file in current project
11:48:45 06:18:45.488 DEBUG: 'JavaScript/TypeScript Coverage' skipped because there is no related file in current project
11:48:45 06:18:45.489 DEBUG: 'Import of ESLint issues' skipped because one of the required properties is missing
11:48:45 06:18:45.489 DEBUG: 'Import of TSLint issues' skipped because one of the required properties is missing
11:48:45 06:18:45.489 DEBUG: 'CSS Metrics' skipped because there is no related file in current project
11:48:45 06:18:45.490 DEBUG: 'Import of stylelint issues' skipped because one of the required properties is missing
11:48:45 06:18:45.506 DEBUG: 'Generic Test Executions Report' skipped because one of the required properties is missing
11:48:45 06:18:45.507 DEBUG: Sensors : JavaSensor -> SurefireSensor -> HTML -> XML Sensor -> JaCoCo XML Report Importer -> JavaScript inside HTML analysis -> CSS Rules -> IaC Docker Sensor -> TextAndSecretsSensor
11:48:45 06:18:45.508 INFO: Sensor JavaSensor [java]
11:48:45 06:18:45.525 DEBUG: Property 'sonar.java.jdkHome' resolved with:
11:48:45 []
11:48:45 06:18:45.525 DEBUG: Property 'sonar.java.libraries' resolved with:
11:48:45 []
11:48:45 06:18:45.539 INFO: ------------------------------------------------------------------------
11:48:45 06:18:45.539 INFO: EXECUTION FAILURE
11:48:45 06:18:45.540 INFO: ------------------------------------------------------------------------
11:48:45 06:18:45.540 INFO: Total time: 21.709s
11:48:45 06:18:45.633 INFO: Final Memory: 14M/50M
11:48:45 06:18:45.633 INFO: ------------------------------------------------------------------------
11:48:45 06:18:45.633 ERROR: Error during SonarScanner execution
11:48:45 org.sonar.java.AnalysisException: Your project contains .java files, please provide compiled classes with sonar.java.binaries property, or exclude them from the analysis with sonar.exclusions property.
11:48:45 	at org.sonar.java.classpath.ClasspathForMain.init(ClasspathForMain.java:73)
11:48:45 	at org.sonar.java.classpath.AbstractClasspath.getElements(AbstractClasspath.java:319)
11:48:45 	at org.sonar.java.SonarComponents.getJavaClasspath(SonarComponents.java:206)
11:48:45 	at org.sonar.java.JavaFrontend.<init>(JavaFrontend.java:95)
11:48:45 	at org.sonar.plugins.java.JavaSensor.execute(JavaSensor.java:112)
11:48:45 	at org.sonar.scanner.sensor.AbstractSensorWrapper.analyse(AbstractSensorWrapper.java:64)
11:48:45 	at org.sonar.scanner.sensor.ModuleSensorsExecutor.execute(ModuleSensorsExecutor.java:88)
11:48:45 	at org.sonar.scanner.sensor.ModuleSensorsExecutor.lambda$execute$1(ModuleSensorsExecutor.java:61)
11:48:45 	at org.sonar.scanner.sensor.ModuleSensorsExecutor.withModuleStrategy(ModuleSensorsExecutor.java:79)
11:48:45 	at org.sonar.scanner.sensor.ModuleSensorsExecutor.execute(ModuleSensorsExecutor.java:61)
11:48:45 	at org.sonar.scanner.scan.SpringModuleScanContainer.doAfterStart(SpringModuleScanContainer.java:82)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.startComponents(SpringComponentContainer.java:226)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.execute(SpringComponentContainer.java:205)
11:48:45 	at org.sonar.scanner.scan.SpringProjectScanContainer.scan(SpringProjectScanContainer.java:204)
11:48:45 	at org.sonar.scanner.scan.SpringProjectScanContainer.scanRecursively(SpringProjectScanContainer.java:200)
11:48:45 	at org.sonar.scanner.scan.SpringProjectScanContainer.doAfterStart(SpringProjectScanContainer.java:173)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.startComponents(SpringComponentContainer.java:226)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.execute(SpringComponentContainer.java:205)
11:48:45 	at org.sonar.scanner.bootstrap.SpringScannerContainer.doAfterStart(SpringScannerContainer.java:351)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.startComponents(SpringComponentContainer.java:226)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.execute(SpringComponentContainer.java:205)
11:48:45 	at org.sonar.scanner.bootstrap.SpringGlobalContainer.doAfterStart(SpringGlobalContainer.java:138)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.startComponents(SpringComponentContainer.java:226)
11:48:45 	at org.sonar.core.platform.SpringComponentContainer.execute(SpringComponentContainer.java:205)
11:48:45 	at org.sonar.batch.bootstrapper.Batch.doExecute(Batch.java:71)
11:48:45 	at org.sonar.batch.bootstrapper.Batch.execute(Batch.java:65)
11:48:45 	at org.sonarsource.scanner.api.internal.batch.BatchIsolatedLauncher.execute(BatchIsolatedLauncher.java:46)
11:48:45 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
11:48:45 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
11:48:45 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
11:48:45 	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
11:48:45 	at org.sonarsource.scanner.api.internal.IsolatedLauncherProxy.invoke(IsolatedLauncherProxy.java:60)
11:48:45 	at jdk.proxy1/jdk.proxy1.$Proxy0.execute(Unknown Source)
11:48:45 	at org.sonarsource.scanner.api.EmbeddedScanner.doExecute(EmbeddedScanner.java:189)
11:48:45 	at org.sonarsource.scanner.api.EmbeddedScanner.execute(EmbeddedScanner.java:138)
11:48:45 	at org.sonarsource.scanner.cli.Main.execute(Main.java:126)
11:48:45 	at org.sonarsource.scanner.cli.Main.execute(Main.java:81)
11:48:45 	at org.sonarsource.scanner.cli.Main.main(Main.java:62)
11:48:45 06:18:45.666 DEBUG: Cleanup org.eclipse.jgit.util.FS$FileStoreAttributes$$Lambda$300/0x00007f689027c200@75b21c3b during JVM shutdown
11:48:46 WARN: Unable to locate 'report-task.txt' in the workspace. Did the SonarScanner succeed?
11:48:46 ERROR: SonarQube scanner exited with non-zero code: 1
11:48:46 [DeployPublisher][INFO] Build failed, project not deployed
11:48:46 Finished: FAILURE
~~~

To summerise this was the error: 
`org.sonar.java.AnalysisException: Your project contains .java files, please provide compiled classes with sonar.java.binaries property, or exclude them from the analysis with sonar.exclusions property.`

This error occurs when the SonarQube scanner is unable to find the compiled Java classes (.class files) for the Java source files in your project. The scanner needs access to the compiled classes to perform static code analysis.

to sovle this there are 2 ways: 

1. If you have already compiled your Java code and have the .class files available, you can specify the location of these files using the sonar.java.binaries property. `sonar.java.binaries=target/classes` (Replace target/classes with your path if you have configured in a diffterent dir)

or 

2. Ensure that your Jenkins pipeline is compiling the Java code successfully before running the SonarQube scanner. If the compilation step fails, the scanner won't be able to find the compiled classes.

I already had .class files compiled so 1. worked for me. 

4. **Tomcat Error**

Just when i thought everything is alright another error slammed on my face. 

+ I dont have log file for this one but error was no html page to view the project. Since my project is a pretty basic java hello-world project. i didnt have any html page to view that so i had to create one **but where??**
+ If you remember we created '/webapps' dir, which typically servers as the default deployment path for Tomcat installations. So our `index.html` should be in this folder.

Index.html:

~~~
<!DOCTYPE html>
<html>
<head>
    <title>Welcome dummy to my webpage </title>
    <style>
        body {
            font-family: Arial, sans-serif;
            text-align: center;
            padding-top: 100px;
        }
    </style>
</head>
<body>
    <h1>dummy is working !</h1>
    <p>This is a simple HTML page served by Tomcat.</p>
    <p>Current Date and Time: <span id="datetime"></span></p>

    <script>
        function updateDateTime() {
            const now = new Date();
            const dateTimeString = now.toLocaleString();
            document.getElementById("datetime").textContent = dateTimeString;
        }

        setInterval(updateDateTime, 1000);
    </script>
</body>
</html>
~~~

![Screenshot 2024-05-30 130915](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/8a85122c-2f7e-4f7d-9bca-0db277c24da9)

AND EVERYTHING WORKS !! 

## Bonus

You can do all this via shell commands too. just fun to try out!

Jenkins --> + build item --> free style --> execute shell

script 1:

~~~
echo "Cleaning workspace"
rm -rf *
ls -la
~~~

script 2:

~~~
echo "manual download"
curl -u admin:qwerty1234 -O http://13.234.20.254:8081/repository/maven-snapshots/com/mycompany/app/my-app/1.2-SNAPSHOT/my-app-1.2-20240528.144737-1.war
ls -la
~~~

Script 3:

~~~
echo "List all files after artifact download"
ls -la

echo "Deploying to Tomcat..."
curl -u admin:admin123 -T my-app-1.2-20240528.144737-1.war "http://3.111.56.155:8080/manager/text/deploy?path=/myapp&update=true"
~~~

and then you can configure tomcat which will work with selected version of the build. 





~~~
You made it this far you learned alot congrats for that!!!

This implementation is quite normal keep updated i will post the pipeline with docker 
~~~
