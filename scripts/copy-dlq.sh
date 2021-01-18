#!/usr/bin/env bash

###
#
# A script to copy the contents of the case-notes-to-probation DLQ into a local directory so the failure reason can be investigated.
#
# The following command line utilities are required:
#  * aws cli
#  * kubectl
#  * jq
#
# Access to the Kubernetes namespace being interrogated is also required.
#
###

ENV=${1:-prod}
DATA_DIR=${2:-dlq-messages}

getSecret() {
  kubectl get secret cnpp-sqs-dl-instance-output -o json | jq -r ".data.$1" | base64 --decode
}

# Gather secrets
AWS_DEFAULT_REGION=eu-west-2
export AWS_DEFAULT_REGION
kubectl config set-context live-1.cloud-platform.service.justice.gov.uk --namespace "case-notes-to-probation-$ENV"
AWS_ACCESS_KEY_ID=$(getSecret 'access_key_id')
AWS_SECRET_ACCESS_KEY=$(getSecret 'secret_access_key')
URL=$(getSecret 'sqs_cnpp_url')

# clear previous run's data
rm -rf "$DATA_DIR" 2> /dev/null
mkdir "$DATA_DIR"

# initialise summary csv
echo "messageNumber,offenderIdDisplay,caseNoteId,eventType,agencyLocationId,timestamp" > "$DATA_DIR"/summary.csv

extractField() {
  echo "$1" | jq -r '.Messages[0].Body' | jq -r '.Message' | jq ".$2"
}

# copy all DLQ messages (NOTE - we do not acknowledge the received messages so they will be returned to the DLQ)
MSG_COUNT=$(AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY aws sqs get-queue-attributes "--queue-url=$URL" --attribute-names ApproximateNumberOfMessages | jq ".Attributes.ApproximateNumberOfMessages" | tr -d '"')
echo "Found $MSG_COUNT dlq messages"
# shellcheck disable=SC2086
for i in $(seq $MSG_COUNT)
do
  contents=$(AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY aws sqs receive-message "--queue-url=$URL" --max-number-of-message 1)
  echo "$contents" > "$DATA_DIR/DLQ-$i.json"
  echo "$contents" | jq -r '.Messages[0].Body' | jq -r '.Message' > "$DATA_DIR/message-$i.json"
  # add to summary csv
  offenderIdDisplay=$(extractField "$contents" 'offenderIdDisplay')
  caseNoteId=$(extractField "$contents" 'caseNoteId')
  eventType=$(extractField "$contents" 'eventType')
  agencyLocationId=$(extractField "$contents" 'agencyLocationId')
  timestamp=$(echo "$contents" | jq -r '.Messages[0].Body' | jq -r '.Timestamp')
  echo "$i,$offenderIdDisplay,$caseNoteId,$eventType,$agencyLocationId,$timestamp" >> "$DATA_DIR"/summary.csv
  echo "Processed dlq message for $(echo $offenderIdDisplay | tr -d '"')/$(echo $caseNoteId | tr -d '"')"
done
