#!/bin/sh
# Unix shell script for starting threadlogic. If you have big log files
# you might need to adjust Xmx setting.
cd $(dirname $0)/..
java -Xmx512m -jar ./threadlogic.jar 
