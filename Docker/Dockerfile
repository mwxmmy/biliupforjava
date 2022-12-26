FROM khipu/openjdk17-alpine

ADD ROOT.war /ROOT.war

EXPOSE 80

RUN bash -c 'touch /ROOT.war'
ENV JAVA_OPTS = ""
ENTRYPOINT java ${JAVA_OPTS} -XX:+UseZGC -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war
