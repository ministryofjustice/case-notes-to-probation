    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: JWT_PUBLIC_KEY
    value: "{{ .Values.env.JWT_PUBLIC_KEY }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "logstash"

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ENDPOINT_URL }}"

  - name: CASENOTES_ENDPOINT_URL
    value: "{{ .Values.env.CASENOTES_ENDPOINT_URL }}"

  - name: DELIUS_ENDPOINT_URL
    value: "{{ .Values.env.DELIUS_ENDPOINT_URL }}"

  - name: DELIUS_ENABLED
    value: "{{ .Values.env.DELIUS_ENABLED }}"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY
  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=$(APPINSIGHTS_INSTRUMENTATIONKEY)"

  - name: OAUTH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: CASENOTES_CLIENT_ID

  - name: OAUTH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: CASENOTES_CLIENT_SECRET

  - name: SQS_AWS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: cnpp-sqs-instance-output
        key: access_key_id

  - name: SQS_AWS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: cnpp-sqs-instance-output
        key: secret_access_key

  - name: SQS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: cnpp-sqs-instance-output
        key: sqs_cnpp_name

  - name: SQS_AWS_DLQ_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: cnpp-sqs-dl-instance-output
        key: access_key_id

  - name: SQS_AWS_DLQ_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: cnpp-sqs-dl-instance-output
        key: secret_access_key

  - name: SQS_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: cnpp-sqs-dl-instance-output
        key: sqs_cnpp_name

{{- end -}}
