FROM ubuntu

RUN apt update
RUN apt-get install -y vim
RUN apt-get install -y curl
RUN apt-get install -y gpg

# get GPG key for cassandra
RUN curl https://downloads.apache.org/cassandra/KEYS | apt-key add -

 #Install OpenJDK-8
RUN apt-get update -y && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

#Fix certificate issues
RUN apt-get update -y && \
    apt-get install -y ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;


RUN echo "deb http://www.apache.org/dist/cassandra/debian 40x main" | tee /etc/apt/sources.list.d/cassandra.list
RUN apt-get update -y && apt-get upgrade -y
RUN apt-get install -y cassandra 

RUN apt-get install -y openjdk-11-jdk

COPY database/build/database*.jar .

RUN echo "#!/bin/sh" >> entry.sh && \
	echo "trap \"service cassandra stop\" INT" >> entry.sh && \
	echo "service cassandra start" >> entry.sh && \
	echo "java -jar database.jar \"$1\"" >> entry.sh
#RUN ./gradlew sourcesJar
RUN mv database*.jar database.jar 
RUN java -version
ENTRYPOINT ["sh", "entry.sh"]
