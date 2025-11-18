#!/bin/bash

docker rm -f config 2>/dev/null || true

docker run --name config -d \
-p8070:8070 \
--memory=1g \
--network oracle-network \
configd