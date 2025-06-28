FROM openjdk:21
COPY app/build/libs/app-all.jar /app/app.jar

WORKDIR /app

# The ENTRYPOINT is the main command to run.
ENTRYPOINT ["java", "-jar", "app.jar"]

# The CMD provides the default argument to the ENTRYPOINT.
# This will be overridden by the argument in the `docker run` command from makeDocker.sh.
CMD ["/app/bot2.json"]
