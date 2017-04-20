# case-notes-poll-push

Self-contained fat-jar micro-service to poll a source API for case notes and push to a target API.

### Building and running

Prerequisites:
- sbt (Scala Build Tool) http://www.scala-sbt.org/release/docs

Build commands:

- Build and run tests `sbt test`
- Run locally `sbt run`
- Build deployable pollPush.jar  `sbt assembly`

Running deployable fat jar:
- `java -jar pollPush.jar`

Configuration parameters can be supplied via environment variables, e.g.:
- `POLL_SECONDS=60 sbt run`
- `POLL_SECONDS=60 java -jar pollPush.jar`

### Development notes

The pull/push functionality is unit tested via dependency-injected mock APIs. The source and target REST APIs are also directly tested via WireMock HTTP Servers that mock the HTTP endpoints, and an integration test also connects to an embedded MongoDB server.

The implementation will be updated as reference source and target API environments become available.

### Deployment notes

Requires access to a MongoDB database instance to store and retrieve the last processed time in case of a process or server restart, configurable via environment parameters:

- `MONGO_DB_URL` (defaults to `mongodb://localhost:27017` i.e. a locally running MongoDB instance)
- `MONGO_DB_NAME` (defaults to `pollpush`)

See `Configuration.scala` for a full list of configuration parameters.

### Building and running with Docker

- Build Docker Image `./buildDocker.sh`
- Run Docker Container `docker run -d pollpush -e POLL_SECONDS=30 -e MONGO_DB_URL=mongodb://localhost:27017` (replace parameter values as appropriate)

To run a Docker based integration test against mocked APIs and a real MongoDB instance:

- Build Docker Mock API Image `./buildDockerMockApi.sh`
- Run the integration test `./dockerIntegrationTest.sh`
- (To clean up afterwards run `./dockerCleanupTest.sh`)

### Running against a mocked Delius instance
Run wiremock standalone and configure for Delius mock endpoint

- Download wiremock standalone: `http://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-standalone/2.6.0/wiremock-standalone-2.6.0.jar`

- Override the Delius endpoint `PUSH_BASE_URL=http://localhost:8085/delius java -jar pollPush.jar`

- Start wiremock `java -jar wiremock-standalone-2.6.0.jar --port 8085 &` 

- Configure wiremock endpoint `curl -X POST -d @./src/test/resources/mappings/putCaseNote.json http://localhost:8085/__admin/mappings`

### Generating suitable keys

- openssl ecparam -name prime256v1 -genkey -noout -out client.key
- openssl ec -in client.key -pubout -out client.pub
- openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in client.key -out client.pkcs8.key

Use the whole output, as is, including header and line breaks if you like
