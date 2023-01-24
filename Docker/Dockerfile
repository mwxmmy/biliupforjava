FROM openjdk:17.0.2-oraclelinux8

ADD ROOT.war /ROOT.war

EXPOSE 80
EXPOSE 5005

RUN bash -c 'touch /ROOT.war'
ENTRYPOINT java -XX:+UseZGC -XX:MaxRAMPercentage=75.0 -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war
