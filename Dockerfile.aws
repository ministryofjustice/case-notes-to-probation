FROM java

MAINTAINER Nick Talbot <nick.talbot@digital.justice.gov.uk>

COPY target/scala-2.12/pollPush-*.jar /root/pollPush.jar

ENTRYPOINT ["/usr/bin/java", "-jar", "/root/pollPush.jar"]
