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

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ENDPOINT_URL }}"

  - name: CASENOTES_ENDPOINT_URL
    value: "{{ .Values.env.CASENOTES_ENDPOINT_URL }}"

  - name: DELIUS_ENDPOINT_URL
    value: "{{ .Values.env.DELIUS_ENDPOINT_URL }}"

  - name: APPLICATION_INSIGHTS_IKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

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

{{- end -}}
