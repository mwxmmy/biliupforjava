FROM khipu/openjdk17-alpine

ADD ROOT.war /ROOT.war

EXPOSE 80

RUN bash -c 'touch /ROOT.war'

CMD ["java","-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Shanghai","-jar","/ROOT.war"]
