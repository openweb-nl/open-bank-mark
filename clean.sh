#!/usr/bin/env bash

docker-compose -f docker-bank.yml -f docker-cluster.yml -f docker-prep.yml down