
FROM openjdk:11

COPY ./install.sh .
RUN ./install.sh

COPY ./build/libs .
COPY ./entrypoint.sh .

ENV WEBSCRAPER_BOOTSTRAPS="localhost:9092"

ENTRYPOINT ["./entrypoint.sh"]

