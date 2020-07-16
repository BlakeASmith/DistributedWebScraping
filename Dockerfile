FROM ubuntu
RUN apt update
RUN apt-get install -y vim
RUN apt-get install -y curl
RUN apt-get install -y gpg

# get GPG key for cassandra
RUN curl https://downloads.apache.org/cassandra/KEYS | apt-key add -

# Install OpenJDK-8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME

RUN echo "deb http://www.apache.org/dist/cassandra/debian 40x main" | tee /etc/apt/sources.list.d/cassandra.list
RUN apt-get update && apt-get upgrade
RUN apt-get install -y cassandra 

CMD ["cassandra", "-R"]
ENTRYPOINT ["cat"]

