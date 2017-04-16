#!/usr/bin/env bash

docker kill mongo nomis delius pollpush
docker rm mongo nomis delius pollpush
docker network rm pollpush
