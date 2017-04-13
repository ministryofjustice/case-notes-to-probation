#!/usr/bin/env bash

sbt assembly
docker build -t pollpush .

# To run within Docker:
# docker run -d pollpush -e POLL_SECONDS=30 -e MONGO_DB_URL=mongodb://localhost:27017
