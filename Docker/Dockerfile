FROM eclipse-temurin:17-jre-alpine

# 使用阿里云镜像源（可选，加速 apk）
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

RUN apk add ffmpeg

# 正确方式创建文件（不需要 bash）
RUN touch /ROOT.jar

# 添加你的 JAR 文件（这句应该在前面，不是创建空文件）
ADD ROOT.jar /ROOT.jar

EXPOSE 80

ENV JAVA_OPTS=""

#ENTRYPOINT java -XX:+UseZGC -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war
ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS -XX:+UseZGC -Duser.timezone=Asia/Shanghai --add-opens java.base/jdk.internal.loader=ALL-UNNAMED -javaagent:/rasp/rasp.jar -Djava.security.egd=file:/dev/./urandom -jar /ROOT.jar"]