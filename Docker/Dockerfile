FROM khipu/openjdk17-alpine

ADD ROOT.war /ROOT.war

EXPOSE 80

RUN bash -c 'touch /ROOT.war'
ENV JAVA_OPTS = "-XX:+PrintFlagsFinal"
ENTRYPOINT java ${JAVA_OPTS} -XX:+UseZGC -XX:MinRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war
