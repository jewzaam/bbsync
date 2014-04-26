#!/bin/sh

# very very rough and basic script to start the viewer.. major cleanup required!
mvn clean install; java -classpath target/bbsync-0.1.0-SNAPSHOT.jar
