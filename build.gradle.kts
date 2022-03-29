plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.1"
  kotlin("plugin.spring") version "1.6.0"
}

configurations {
  implementation { exclude(module = "tomcat-jdbc") }
  implementation { exclude(module = "spring-boot-starter-validation") }
  implementation { exclude(module = "spring-boot-graceful-shutdown") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

ext["jackson.version.databind"] = "2.13.2.2" // Overriding Jackson databind version to fix CVE-2020-36518

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.1")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.1")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.1")

  implementation("javax.transaction:javax.transaction-api:1.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:3.0.2")
  implementation("com.sun.xml.bind:jaxb-core:3.0.2")

  implementation("com.google.code.gson:gson:2.8.9")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.0.3")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.testcontainers:localstack:1.16.2")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
