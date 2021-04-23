#!/bin/sh
set -e
set -x

CLONE_DIR=$(mktemp -d)

echo "Cloning destination git repository"
git config --global user.email "82411728+szczepienia@users.noreply.github.com"
git config --global user.name "szczepienia"
git clone --single-branch --branch main "https://x-access-token:$PAT@github.com/szczepienia/szczepienia.github.io.git" "$CLONE_DIR"

echo "Building"
mvn -B clean package -DskipTests

echo "Generating new data"
java -jar target/szczepimy-1.0-SNAPSHOT.jar -p $EREJ_PID_PLUTA -s $EREJ_SID -c $EREJ_CSRF -t $CLONE_DIR -v OPOLSKIE

cd "$CLONE_DIR"

echo "Adding git commit"
git add *.html
if git status | grep -q "Changes to be committed"
then
    git commit --message "remote update"
    echo "Pushing git commit"
    git push -u origin HEAD:main
else
    echo "No changes detected"
fi
