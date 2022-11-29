FROM anapsix/alpine-java:8_server-jre

ADD ROOT.war /ROOT.war

EXPOSE 80

RUN bash -c 'touch /ROOT.war'

CMD ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/ROOT.war"]