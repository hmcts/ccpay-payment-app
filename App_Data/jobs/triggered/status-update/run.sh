#!/bin/sh
source $WEBROOT_PATH/App_Data/jobs/triggered/s2sToken.sh

if [ "$SLOT" == "PRODUCTION" ]
then
    AUTH_TOKEN=$(s2sToken)
    printf "Invoke status-update endpoint on :%s\n" $PAYMENT_SERVER_URL

    curl -X PATCH $PAYMENT_SERVER_URL/jobs/card-payments-status-update -H "ServiceAuthorization: Bearer $AUTH_TOKEN" -d {}

    printf "\nFinished updating status"
else
    printf "Unsupported app slot:%s to run this job. \n" $SLOT
fi
