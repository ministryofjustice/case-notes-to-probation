# case-notes-to-probation

[![CircleCI](https://circleci.com/gh/ministryofjustice/case-notes-to-probation/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/case-notes-to-probation)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/case-notes-to-probation/status)](https://quay.io/repository/hmpps/case-notes-to-probation)

A Spring Boot app to listen on AWS queue and send case notes to probation.

### To build:

```bash
./gradlew build
```

### Health

- `/health/ping`: will respond `{"status":"UP"}` to all requests.  This should be used by dependent systems to check connectivity to keyworker,
rather than calling the `/health` endpoint.
- `/health`: provides information about the application health and its dependencies.  This should only be used
by keyworker health monitoring (e.g. pager duty) and not other systems who wish to find out the state of keyworker.
- `/info`: provides information about the version of deployed application.

### Pre Release Testing

Case notes to probation is best tested by the DPS front end.  To manually smoke test / regression test:

1. Navigate to [DPS](https://digital-preprod.prison.service.justice.gov.uk/) and search for a prisoner
1. Add an OMIC case note to the prisoner
1. Add a keyworker case note to the prisoner
1. Wait 5 minutes or so and then check app insights to see that the case note has been sent to probation:
```
requests
| where cloud_RoleName == "community-api"
| where name == "PUT CaseNoteController/upsertCaseNotesToDelius"
```
For offenders that don't yet exist in Delius this will create a 404 and then be retried twice.

### Running against localstack
Localstack has been introduced for some integration tests and it is also possible to run the application against localstack.

* Checkout localstack from [GitHub](https://github.com/localstack/localstack)
* Make sure you have docker, docker-compose and aws CLI installed
* In the root of the localstack project, run command `docker-compose up` to bring up localstack
* Start the Spring Boot app with profile='localstack', env var SQS_PROVIDER=localstack and SQS_EMBEDDED=false 
* You can now use the aws CLI to send messages to the queue
* The queue's health status should appear at the local healthcheck: http://localhost:8082/health
* Note that you will also need local copies of Oauth server, Case notes API and Delius API running to do anything useful
