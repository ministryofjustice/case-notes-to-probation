# case-notes-to-probation

[![CircleCI](https://circleci.com/gh/ministryofjustice/case-notes-to-probation/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/case-notes-to-probation)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/case-notes-to-probation/status)](https://quay.io/repository/hmpps/case-notes-to-probation)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://case-notes-to-probation-dev.prison.service.justice.gov.uk/swagger-ui.html)

A Spring Boot app to listen on an AWS queue and send case notes to probation.

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
For offenders that don't yet exist in Delius this will create a 404 which will then be ignored.

### Running against localstack

Localstack has been introduced for some integration tests and it is also possible to run the application against localstack.

* In the root of the localstack project, run command
```
sudo rm -rf /tmp/localstack && docker-compose down && docker-compose up
```
to clear down and then bring up localstack
* Start the Spring Boot app with profile='localstack'
* You can now use the aws CLI to send messages to the queue
* The queue's health status should appear at the local healthcheck: http://localhost:8082/health
* Note that you will also need local copies of Oauth server, Case notes API and Delius API running to do anything useful

### Running the tests

With localstack now up and running (see previous section), run
```bash
./gradlew test
```

### Investigating Dead Letter Queue (DLQ) messages

When we fail to process a case note due to an unexpected error an exception will be thrown and the case note will be moved to the DLQ.

If the failure was due to a recoverable error - e.g. network issues - then the DLQ message can and should be retried.

However, if the error is not recoverable - e.g. some new error scenario we weren't expecting - then we need to investigate the error and either:
* fix the bug that is causing the error OR
* handle and log the error so that the exception is no longer thrown and the message does not end up on the DLQ

#### Steps for investigating DLQ messages
* Import the swagger collection into Postman - link to API docs at the top of this README.
* Obtain an access token with `ROLE_CASE_NOTE_QUEUE_ADMIN` role - #dps_tech_team will be able to help with that
* Call the `/queue-admin/transfer-dlq` endpoint to transfer all DLQ entries back onto the main queue - this should get rid of any messages with recoverable errors
* Check that the messages have gone from the dlq by going to https://case-notes-to-probation.prison.service.justice.gov.uk/health

For messages that don't then disappear from the dlq:
* cd into the `scripts` directory and run the `copy-dlq.sh` script which copies the contents of the DLQ locally and summarises in `summary.csv` 
* run an AppInsights Logs query looking for exceptions shortly after the timestamp found in the csv
* if there was an error calling a DPS service, check the logs for that service and possibly check the data in DPS
* if there was an error calling a Delius service, check the Delius AWS logs and possibly check the data in Delius
* identify mitigation for the error - fix bug or ignore error
* once this code change is in production transfer the DLQ messages onto the main queue again and all should now be handled without exceptions
