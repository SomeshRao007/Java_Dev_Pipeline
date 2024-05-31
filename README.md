Well seeing the image below you might be wondering, how he end up in this position!!

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

**Step 5:**

Download and install sonarqube application

```
cd /opt 
sudo wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.5.1.90531.zip
```

**Step 6:**

Unzip sonarqube source files and rename the folder.

~~~
sudo unzip sonarqube-10.5.1.90531.zip
sudo mv sonarqube-10.5.1.90531 sonarqube
~~~

**Step 7:**

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

**Step 8:**

Add Sonar User and Privileges, Create a user named sonar and make it the owner of the /opt/sonarqube directory.

~~~
sudo useradd sonar
sudo chown -R sonar:sonar /opt/sonarqube
~~~

add sonar user to 

sudo vi /etc/sudoers

**Step 9:**

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












This implementation is quite normal keep updated i will post the pipeline with docker 





