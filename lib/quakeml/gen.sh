#!/bin/bash

SRC_DIR=src

function cleanup()
{
  if [ -d ${SRC_DIR} ]; then
    rm -rf ${SRC_DIR}
  fi
  mkdir ${SRC_DIR}
}

echo "building quakeml lib"
cleanup
scomp -out quakeml.jar -src ${SRC_DIR} *.xsd
jar cf quakeml_src.jar ${SRC_DIR}/
