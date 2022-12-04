FROM anapsix/alpine-java:8_server-jre

ADD ROOT.war /ROOT.war

EXPOSE 80

RUN bash -c 'touch /ROOT.war'

CMD ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx512m","-Xms512m","-Xmn256m", "-Duser.timezone=Asia/Shanghai","-jar","/ROOT.war"]
