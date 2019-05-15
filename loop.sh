#!/usr/bin/env bash

number="$1"

while (( ${number} > 0))
do
    ./restart.sh &&
	java -jar test/target/test.jar &&
	number=$((number - 1))
done