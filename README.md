~~~ bash
#!/bin/bash

export MSYS_NO_PATHCONV=1

JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport -javaagent:/usr/local/tomcat/performance-agent.jar -Dcmdev.profiler.filters.path=/usr/local/tomcat/filters.properties"
mvn -q clean install
rootDir=$PWD
pushd test-app
mvn -q clean install
docker build -t test-app-tomcat -f Dockerfile-Tomcat9jdk17 .
docker run -p 8080:8080 -p 8090:8090 -p 8787:8787 \
    -e JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS}" \
    -v $rootDir/test-app/filters.properties:/usr/local/tomcat/filters.properties \
    -v $rootDir/target/performance-agent.jar:/usr/local/tomcat/performance-agent.jar \
    -v $rootDir/test-app/traces:/tmp/traces \
    test-app-tomcat
popd
~~~