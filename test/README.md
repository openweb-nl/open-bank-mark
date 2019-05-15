## Frontend test

### use

#### Running test

Can be used without arguments as fat jar run fro the main folder to run a performance test. On the top of the core.clj file are some properties that can be easily changed. With the help of lispyclouds/clj-docker-client it's alos possible to do some 'chaos-monkey' actions. Since the cluster will need to restore you will need to set the timeout higher, and probably don't need to check evere 50 ms if the expected value is there.

#### Generating diagrams

Can be used with one argument, pointing to some mapping file, to determine which file(s) need to be grouped together. When the name ends with a '*' all the files starting with that will be combined.

