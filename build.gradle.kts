plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.2.1"
  kotlin("plugin.spring") version "1.5.0"
}

configurations {
  implementation { exclude(module = "tomcat-jdbc") }
  implementation { exclude(module = "spring-boot-starter-validation") }
  implementation { exclude(module = "spring-boot-graceful-shutdown") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

  implementation("org.springdoc:springdoc-openapi-ui:1.5.8")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.5.8")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.8")

  implementation("javax.transaction:javax.transaction-api:1.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:3.0.1")
  implementation("com.sun.xml.bind:jaxb-core:3.0.1")

  implementation("com.google.code.gson:gson:2.8.6")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  implementation("org.springframework:spring-jms")
  implementation(platform("com.amazonaws:aws-java-sdk-bom:1.11.1018"))
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.25.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
