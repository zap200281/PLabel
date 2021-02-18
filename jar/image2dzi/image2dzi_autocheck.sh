#!/bin/bash
url=http://0.0.0.0:8080/checkAlive
NAME="image2dzi.py"
FILEPATH="/home/image2dzi/image2dzi.py"
check_http(){
    status_code=$(curl -m 5 -s -o /dev/null -w %{http_code} $url)
}
 
check_http
date=$(date +%Y%m%d-%H:%M:%S) 
if [ $status_code -ne 200 ];then
	echo "$date server error, auto restarting" >> /home/image2dzi/sh_log.log
	ID=$(ps -ef|grep $NAME|awk '{printf $2}')
	echo $ID
		for id in $ID
		do
		echo $id
		kill -9 $id
		echo 'killed $id'
	done
	python3 $FILEPATH &
fi
