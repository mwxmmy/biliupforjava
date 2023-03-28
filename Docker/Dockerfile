FROM openjdk:17.0.2-oraclelinux8

ADD ROOT.war /ROOT.war

EXPOSE 80

ENV JAVA_OPTS=""

RUN bash -c 'touch /ROOT.war'

#ENTRYPOINT java -XX:+UseZGC -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war
ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS -XX:+UseZGC -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war"]