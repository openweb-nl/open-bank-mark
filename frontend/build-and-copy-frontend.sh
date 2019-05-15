#!/usr/bin/env bash

lein clean &&
lein sass once &&
lein cljsbuild once min &&
cp -rf resources/public .