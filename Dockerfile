FROM java:11

MAINTAINER tutinformatics

# copy files to workdir
ADD . /ofbiz
WORKDIR /ofbiz

# Setup backend connection
# RUN cat deploy/entity/entityengine.xml > framework/entity/config/entityengine.xml

EXPOSE 8443
EXPOSE 8080
EXPOSE 4567
EXPOSE 1099

# Run ofbiz
ENTRYPOINT ./gradlew cleanAll loadAll ofbiz --no-daemon

