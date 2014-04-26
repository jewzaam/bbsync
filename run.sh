#!/bin/sh

if [ ! -e target/bbsync-0.1.0-SNAPSHOT-static.jar ]; then
    mvn clean install
fi

java -jar target/bbsync-0.1.0-SNAPSHOT-static.jar
