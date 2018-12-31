#!/bin/bash
# Links everything into a self-contained executable using jlink.

set -e

# Needed if you have a java version other than 11 as default
JAVA_HOME=/etc/alternatives/java_sdk

# Compile sources
mvn compile

# Patch gson
if [ ! -e modules/gson.jar ]; then
  ./scripts/patch_gson.sh
fi

# Download windows jdk
if [ ! -e jdks/windows/jdk-11.0.1 ]; then
  mkdir -p jdks/windows
  cd jdks/windows
  curl https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_windows-x64_bin.zip > windows.zip
  unzip windows.zip
  rm windows.zip
  cd ../..
fi

# Set env variables to build with mac toolchain but windows target
JAVA_HOME="./jdks/windows/jdk-11.0.1"
REAL_JAVA_HOME=$(/usr/libexec/java_home -v 11)

# Build in dist/windows
rm -rf dist/windows
$REAL_JAVA_HOME/bin/jlink \
  --module-path $JAVA_HOME/jmods:target/classes \
  --add-modules gson,javacs,wla_server \
  --launcher launcher=wla_server/net.saga.snes.dev.wlalanguageserver.Main \
  --output dist/windows \
  --compress 2 