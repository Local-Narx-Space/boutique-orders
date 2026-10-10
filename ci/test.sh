#!/bin/sh
set -eu
mkdir -p /reports
# Maven needs a writable source/output directory and dependency cache for the
# host UID used by Jenkins. Keep the prepared image itself unchanged.
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
cp pom.xml "$work/"
cp -R src "$work/"
cp -R /opt/maven-cache "$work/.m2"
cd "$work"
result=0
mvn -B -Dmaven.repo.local="$work/.m2" verify || result=$?
# Preserve the test exit status while making reports available even on failure.
for report in target/surefire-reports/TEST-*.xml target/failsafe-reports/TEST-*.xml; do
    [ ! -f "$report" ] || cp "$report" /reports/
done
exit "$result"
