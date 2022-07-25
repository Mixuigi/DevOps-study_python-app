#!/bin/bash

docker ps -a | grep docker.icdc.io/dev-team/django | awk '{print $1}' | xargs docker rm  

docker images -a | grep docker.icdc.io/dev-team/django | awk '{print $3}' | xargs docker rmi