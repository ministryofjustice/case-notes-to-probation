#!/usr/bin/env bash

cd /root

POLL_PUSH_RUNNING=$(ps -ef | grep -v grep | grep -c pollPush.jar) # Start servers if not running

if [ "$POLL_PUSH_RUNNING" -eq 0 ]; then
    POLL_SECONDS=60 nohup java -server -jar pollPush.jar > caseNotes.log 2>&1 &
fi

# crontab -e
# * * * * * /root/watchdog.sh
# Runs every minute
