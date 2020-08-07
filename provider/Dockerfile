FROM openjdk:11

COPY ./install.sh .
RUN ./install.sh
COPY ./entrypoint.sh .
COPY ./build/libs .

ENV WEBSCRAPER_BOOTSTRAPS="localhost:9092"
RUN mv PluginProvider*.jar provider.jar
ENTRYPOINT ["./entrypoint.sh"]
