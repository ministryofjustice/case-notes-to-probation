
### Example deploy command
```
helm --namespace case-notes-to-probation-dev  --tiller-namespace case-notes-to-probation-dev upgrade case-notes-to-probation ./case-notes-to-probation/ --install --values=values-dev.yaml --values=example-secrets.yaml
```

### Rolling back a release
Find the revision number for the deployment you want to roll back:
```
helm --tiller-namespace case-notes-to-probation-dev history case-notes-to-probation -o yaml
```
(note, each revision has a description which has the app version and circleci build URL)

Rollback
```
helm --tiller-namespace case-notes-to-probation-dev rollback case-notes-to-probation [INSERT REVISION NUMBER HERE] --wait
```

### Helm init

```
helm init --tiller-namespace case-notes-to-probation-dev --service-account tiller --history-max 200
helm init --tiller-namespace case-notes-to-probation-preprod --service-account tiller --history-max 200
helm init --tiller-namespace case-notes-to-probation-prod --service-account tiller --history-max 200
```

### Setup Lets Encrypt cert

Ensure the certificate definition exists in the cloud-platform-environments repo under the relevant namespaces folder

e.g.
```
cloud-platform-environments/namespaces/live-1.cloud-platform.service.justice.gov.uk/[INSERT NAMESPACE NAME]/05-certificate.yaml
```
