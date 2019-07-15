FROM azul/zulu-openjdk-debian:11

RUN mkdir /templates
COPY ./src/test/resources/sample.docx /templates/sample.docx
COPY ./src/test/resources/sample2.docx /templates/sample2.docx

RUN apt-get update && apt-get -y install \        
        apt-transport-https locales-all libpng16-16 libxinerama1 libgl1-mesa-glx libfontconfig1 libfreetype6 libxrender1 \
        libxcb-shm0 libxcb-render0 adduser cpio findutils \        
        procps \

        && apt-get -y install libreoffice --no-install-recommends \
        && rm -rf /var/lib/apt/lists/*

VOLUME /templates
ARG JAR_FILE
ADD target/${JAR_FILE} pleodox-community.jar


EXPOSE 8080
ENV SPRING_APPLICATION_JSON {\"jodconverter\":{\"local\":{\"office-home\":\"\"}}, \"pleodox\":{\"storage\":{\"templatesDir\":\"/templates\"}}}
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/pleodox-community.jar"]