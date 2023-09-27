FROM maven:3-openjdk-18-slim as builder

WORKDIR /home/sirius/src

ADD --chown=sirius:sirius . .

RUN mvn package

FROM scireum/sirius-runtime-jdk18

RUN mkdir /home/sirius/data && \
    mkdir /home/sirius/multipart && \
    mkdir /home/sirius/logs

COPY --from=builder --chown=sirius:sirius /home/sirius/src/target/release-dir /home/sirius/

VOLUME /home/sirius/data
VOLUME /home/sirius/logs
EXPOSE 9000
