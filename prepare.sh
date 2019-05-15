#!/usr/bin/env bash

git submodule update --init
cd frontend
./build-and-copy-frontend.sh
cd ../topology
lein install
cd ..
lein modules uberjar
java -jar test/target/test.jar mapping