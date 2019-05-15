#!/usr/bin/env bash

for i in 1 2 3 4 5 6 7 8 9 10
do
   docker-compose -f docker-prep.yml up
   failures=`docker inspect -f '{{ .State.ExitCode }}' synchronizer`
   if [[ "$failures" == "0" ]]; then
    echo -e "Successful prepped cluster in loop ${i}"
    exit 0
   fi
   sleep 3
done
exit 1