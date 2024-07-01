FROM openjdk:17.0.2-oraclelinux8
ADD https://packages.baidu.com/app/openrasp/release/latest/rasp-java.tar.gz /tmp
RUN cd /tmp  && tar -xf rasp-java.tar.*  && mv rasp-*/rasp/ /rasp/  && rm -f rasp-java.tar.gz

ADD ROOT.jar /ROOT.jar

EXPOSE 80

ENV JAVA_OPTS=""

RUN bash -c 'touch /ROOT.jar'

#ENTRYPOINT java -XX:+UseZGC -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war
ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS -XX:+UseZGC -Duser.timezone=Asia/Shanghai --add-opens java.base/jdk.internal.loader=ALL-UNNAMED -javaagent:/rasp/rasp.jar -Djava.security.egd=file:/dev/./urandom -jar /ROOT.jar"]