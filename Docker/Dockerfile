FROM eclipse-temurin:17-jre-jammy

# 设置时区（可选）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 使用阿里云镜像源加速 apt
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/security.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list

# 更新并安装 ffmpeg（或其他你需要的工具）
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 正确方式创建文件（不需要 bash）
RUN touch /ROOT.jar

# 添加你的 JAR 文件（这句应该在前面，不是创建空文件）
ADD ROOT.jar /ROOT.jar

EXPOSE 80

ENV JAVA_OPTS=""

#ENTRYPOINT java -XX:+UseZGC -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom -jar /ROOT.war
ENTRYPOINT ["/bin/sh", "-c", "java $JAVA_OPTS -XX:+UseZGC -Duser.timezone=Asia/Shanghai --add-opens java.base/jdk.internal.loader=ALL-UNNAMED -Djava.security.egd=file:/dev/./urandom -jar /ROOT.jar"]