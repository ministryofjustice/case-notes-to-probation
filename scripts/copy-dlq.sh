#!/usr/bin/env bash

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

ENV=${1:-prod}
DATA_DIR=${2:-dlq-messages}

# Gather secrets
AWS_DEFAULT_REGION=eu-west-2
kubectl config set-context live-1.cloud-platform.service.justice.gov.uk --namespace "case-notes-to-probation-$ENV"
AWS_ACCESS_KEY_ID=$(kubectl get secret cnpp-sqs-dl-instance-output -o json | jq -r ".data.access_key_id" | base64 --decode)
AWS_SECRET_ACCESS_KEY=$(kubectl get secret cnpp-sqs-dl-instance-output -o json | jq -r ".data.secret_access_key" | base64 --decode)
URL=$(kubectl get secret cnpp-sqs-dl-instance-output -o json | jq -r ".data.sqs_cnpp_url" | base64 --decode)

# export secrets
export AWS_DEFAULT_REGION
export AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY

# clear previous run's data
rm "$DATA_DIR"/DLQ*
rm "$DATA_DIR"/message*

# copy all DLQ messages (NOTE - we do not acknowledge the received messages so they will be returned to the DLQ)
MSG_COUNT=$(aws sqs get-queue-attributes "--queue-url=$URL" --attribute-names ApproximateNumberOfMessages | jq ".Attributes.ApproximateNumberOfMessages" | tr -d '"')
# shellcheck disable=SC2086
for i in $(seq $MSG_COUNT)
do
  contents=$(aws sqs receive-message "--queue-url=$URL" --max-number-of-message 1)
  echo "$contents" > "$DATA_DIR/DLQ-$i.json"
  echo "$contents" | jq -r '.Messages[0].Body' | jq -r '.Message' > "$DATA_DIR/message-$i.json"
done
