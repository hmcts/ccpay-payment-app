#!/bin/sh
source $WEBROOT_PATH/App_Data/jobs/triggered/s2sToken.sh

AUTH_TOKEN=$(s2sToken)
printf "Invoke notify-callbacks endpoint on :%s\n" $PAYMENT_SERVER_URL

curl -X PATCH $PAYMENT_SERVER_URL/jobs/notify-callbacks -H "ServiceAuthorization: Bearer $AUTH_TOKEN" -d {}

printf "\nFinished notify-callbacks"
