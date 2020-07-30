FROM openjdk:11

COPY ./install.sh .
RUN ./install.sh

COPY ./build/libs .
COPY ./entrypoint.sh .
COPY ./config.json .

ENV WEBSCRAPER_CONFIG="config.json"

ENTRYPOINT ["./entrypoint.sh"]
