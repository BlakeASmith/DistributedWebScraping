#!/bin/sh

name=$(sudo docker run -d --network host spotify/kafka)
trap "sudo docker kill $name" SIGINT
sudo docker ps
[ "$1" == "attach" ] && sudo docker attach $name
sleep inf


