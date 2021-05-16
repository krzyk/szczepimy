#!/bin/bash
set -e
set -x

ALL_VOIS="WIELKOPOLSKIE DOLNOŚLĄSKIE WARMIŃSKO_MAZURSKIE KUJAWSKO_POMORSKIE LUBELSKIE LUBUSKIE MAŁOPOLSKIE PODKARPACKIE OPOLSKIE ŚLĄSKIE PODLASKIE MAZOWIECKIE ŁÓDZKIE ŚWIĘTOKRZYSKIE POMORSKIE ZACHODNIOPOMORSKIE"

USER_ID=$1
USER_COUNT=$2
CLONE_DIR=$(mktemp -d)
OUTPUT=$(pwd)/output

echo "USER_ID=$USER_ID, USER_COUNT=$USER_COUNT"

VOIS=$(echo $ALL_VOIS | awk '{for (i = '$USER_ID'; i <= NF; i+='$USER_COUNT') {print $i}}')
#VOIS=$ALL_VOIS

echo "VOIS = $VOIS";

source ~/.erejEnv

P=$EREJ_PID_STASZEK
S=$EREJ_SID
C=$EREJ_CSRF

for VOI in $VOIS; do
    rm -rf $OUTPUT
    mkdir -p $OUTPUT
    java -jar /home/kkrason/dev/moje/szczepimy/target/szczepimy-1.0-SNAPSHOT.jar -p $P -s $S -c $C -t $OUTPUT -v $VOI --wait 900

    git pull --rebase --prune --tags

    rsync -av $OUTPUT/ .
    git add -v -A
    git add _includes/stats

    if git status | grep -q "Changes to be committed"
    then
        git commit --message "remote update"
        echo "Pushing git commit"
        git push -u origin HEAD:main || git pull --rebase --prune --tags &&  git push -u origin HEAD:main || git pull --rebase --prune --tags &&  git push -u origin HEAD:main
    else
        echo "No changes detected"
    fi

done

