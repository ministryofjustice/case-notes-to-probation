FROM java

MAINTAINER Nick Talbot <nick.talbot@digital.justice.gov.uk>

COPY target/scala-2.11/pollPush.jar /root/

ENTRYPOINT ["/usr/bin/java", "-jar", "/root/pollPush.jar"]
