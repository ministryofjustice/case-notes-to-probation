#!/bin/bash
openssl ecparam -name prime256v1 -genkey -noout -out client.key
openssl ec -in client.key -pubout -out client.pub
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in client.key -out client.pkcs8.key