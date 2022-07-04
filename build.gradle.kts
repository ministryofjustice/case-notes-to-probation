plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.2.3"
  kotlin("plugin.spring") version "1.6.21"
  id("com.google.cloud.tools.jib") version "3.2.1"
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

  implementation("org.springdoc:springdoc-openapi-ui:1.6.9")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.9")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.9")

  implementation("com.google.code.gson:gson")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.1.6")

  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.35.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.testcontainers:localstack:1.17.2")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {

  val copyAgentJar by registering(Copy::class) {
    from("${project.buildDir}/libs")
    include("applicationinsights-agent*.jar")
    into("${project.buildDir}/agent")
    rename("applicationinsights-agent(.+).jar", "agent.jar")
    dependsOn("assemble")
  }

  val jib by getting {
    dependsOn += copyAgentJar
  }

  val jibBuildTar by getting {
    dependsOn += copyAgentJar
  }

  val jibDockerBuild by getting {
    dependsOn += copyAgentJar
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}

jib {
  container {
    creationTime = "USE_CURRENT_TIMESTAMP"
    jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
    mainClass = "uk.gov.justice.digital.hmpps.pollpush.PollPushApplicationKt"
    user = "2000:2000"
  }
  from {
    image = "eclipse-temurin:17-jre-alpine"
  }
  extraDirectories {
    paths {
      path {
        setFrom("${project.buildDir}")
        includes.add("agent/agent.jar")
      }
      path {
        setFrom("${project.rootDir}")
        includes.add("applicationinsights*.json")
        setInto("/agent")
      }
    }
  }
}
