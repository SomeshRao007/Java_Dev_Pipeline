node {
    stage('clean'){
        echo "cleaning the code"
        sh 'mvn clean'
    }
    stage('compile'){
        echo "copiling the code"
        sh 'mvn compile'
    }
    stage('package'){
        echo "creating the package"
        sh 'mvn package'
    }
    stage('install'){
        echo "installing the package"
        sh 'mvn install'
        pwd()
    }
}