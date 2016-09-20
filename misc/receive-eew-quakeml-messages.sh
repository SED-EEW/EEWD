#!/bin/bash

# 
# A script to save every received EEW QuakeML message to a different
# file with a name containing the timestamp of the receiving time.
#
# It calls the python script stompy_client.py 
#
# Author: Enrico Ballarin Dolfin, SED ETH Zurich, (C) 2016
#

echo "strompy client started"
echo "Press [CTRL+C] to stop.."

while true
do

   python stompy_client.py receiver -u admin -p admin -H 165.98.224.45 -P 61618 -t eewd -f message.xml 
 
   if [[ $(find message.xml -type f -size +160c 2>/dev/null) ]]; then
      TIMESTAMP=`date +"%Y-%m-%dT%H:%M:%S.%6N%Z"`
      mv message.xml message-$TIMESTAMP.xml
      cat message-$TIMESTAMP.xml
   fi

   sleep 0.25
done

