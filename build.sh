#!/bin/bash
./gradlew --refresh-dependencies clean gitChangelogTask eclipse build install -i
