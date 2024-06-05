![image](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/9b9d6f5a-7eff-4e31-b61f-7a41c1b9b696)Well seeing the image below you might be wondering, how he end up in this position!!

allow me to expalin. 

![Screenshot 2024-05-30 130915](https://github.com/SomeshRao007/java-devops-pipeline/assets/111784343/8a85122c-2f7e-4f7d-9bca-0db277c24da9)


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

We are using 5 tools mainly and for easy of this tutorial i will showing you how to install each tool in a seprate VM. Now if you are a beginner then try this approch before trying via docker, remember baby steps!. the reason why i am install each application in a seperate VM is due to: 

+ Java version conflict. A few applications use a specific java version for example if you are using sonarqube 10+ version it will work on java 11+ (go with java 17).
+ Port conflict, some application use same port either (for example 8080).
+ Conflict with interdependencies. 

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

ACCESS TOKENS GITLAB AND HUB








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


This implementation is quite normal keep updated i will post the pipeline with docker 
