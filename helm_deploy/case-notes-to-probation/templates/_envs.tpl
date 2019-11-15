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

  - name: ELITE2_API_BASE_URL
    value: "{{ .Values.env.ELITE2_API_BASE_URL }}"

  - name: OAUTH_API_BASE_URL
    value: "{{ .Values.env.OAUTH_API_BASE_URL }}"

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
