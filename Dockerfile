FROM openjdk:17
# 这个地方我填写的是后端jar包所在的目录位置
VOLUME target/CPT202Music-0.0.1-SNAPSHOT.jar
# 这个地方我填写的是后端jar包名称，例如你的jar包名字叫aa.jar  这个地方就填aa.jar aa.jar(填两遍)
ADD CPT202Music-0.0.1-SNAPSHOT.jar CPT202Music-0.0.1-SNAPSHOT.jar
# 后端项目的端口号
EXPOSE 80
# 前面都一样，只需要把后面的换成你的jar名称
ENTRYPOINT ["java", "-jar", "/CPT202Music-0.0.1-SNAPSHOT.jar"]