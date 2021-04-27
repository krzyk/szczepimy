#!/bin/sh
set -e
set -x

CLONE_DIR=$(mktemp -d)


echo HOST=$HOST

rm -rf $CLONE_DIR/*

echo "Building"
mvn -B clean package -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
gpg -c --batch --yes -z 0 --cipher-algo AES256 -o enc --passphrase "$KEY" target/szczepimy-1.0-SNAPSHOT.jar

JAR_DIR=$(pwd)

echo "Cloning destination git repository"
git clone --single-branch --branch master "https://krzyk:$PAT@github.com/krzyk/srunner.git" "$CLONE_DIR"
cd "$CLONE_DIR"

echo "Adding git commit"
git config user.email "Krzysztof.Krason@gmail.com"
git config user.name "krzyk"

cp $JAR_DIR/enc .
git add enc
if git status | grep -q "Changes to be committed"
then
    git commit --message "update"
    echo "Pushing git commit"
    git push -u origin HEAD:master
else
    echo "No changes detected"
fi
