#!/bin/bash
# Links everything into a self-contained executable using jlink.

set -e

# Needed if you have a java version other than 11 as default
JAVA_HOME=/usr/lib/jvm/java

# Compile sources
#mvn compile

# Patch gson
if [ ! -e modules/gson.jar ]; then
  ./scripts/patch_gson.sh
fi

# Build using jlink
rm -rf dist/linux
$JAVA_HOME/bin/jlink \
  --module-path modules/gson.jar:target/classes:target/dependency \
  --add-modules gson,javacs,wla_server,org.glassfish.java.json \
  --launcher launcher=wla_server/net.saga.snes.dev.wlalanguageserver.Main \
  --output dist/linux \
  --vm=server \
  --compress 2 
#  --strip-debug \

# strip -p --strip-unneeded dist/linux/lib/server/libjvm.so 
