#!/bin/sh
set -e
set -x

CLONE_DIR=$(mktemp -d)

rm -rf $CLONE_DIR/*

echo "Building"
mvn -B clean package -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

echo "Generating new data"
java -jar target/szczepimy-1.0-SNAPSHOT.jar -p $EREJ_PID_PLUTA -s $EREJ_SID -c $EREJ_CSRF -t $CLONE_DIR -v OPOLSKIE


echo "Cloning destination git repository"
git clone --single-branch --branch main "https://szczepienia:$PAT@github.com/szczepienia/szczepienia.github.io.git" "$CLONE_DIR"
cd "$CLONE_DIR"

echo "Adding git commit"
touch test.html
git config user.email "82411728+szczepienia@users.noreply.github.com"
git config user.name "szczepienia"
git add *.html
if git status | grep -q "Changes to be committed"
then
    git commit --message "remote update"
    echo "Pushing git commit"
    git push -u origin HEAD:main
else
    echo "No changes detected"
fi
