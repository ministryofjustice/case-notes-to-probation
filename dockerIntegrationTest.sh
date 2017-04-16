#!/usr/bin/env bash

docker network create pollpush  # Create shared network that all 4 separate docker instances below can communicate over

docker run -d --net=pollpush --name mongo mongo     # Real instance of MongoDB available at mongodb://mongo on pollpush network
docker run -d --net=pollpush --name nomis mockapi   # Instance of MockAPI available at http://nomis on pollpush network
docker run -d --net=pollpush --name delius mockapi  # Instance of MockAPI available at http://delius on pollpush network

# Runs a Docker based instance of the pollPush micro-service that connects to a Docker hosted MongoDB and two separate instances of MockAPI

docker run -it --net=pollpush --name pollpush -e POLL_SECONDS=1 -e MONGO_DB_URL=mongodb://mongo -e PULL_BASE_URL=http://nomis/nomisapi/offenders/events/case_notes -e PUSH_BASE_URL=http://delius/delius pollpush
