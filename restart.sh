#!/usr/bin/env bash

./clean.sh &&
docker-compose -f docker-cluster.yml up -d &&
sleep 10 &&
./setup-db.sh db 25432 &&
./synchronize.sh &&
docker-compose -f docker-bank.yml up -d
