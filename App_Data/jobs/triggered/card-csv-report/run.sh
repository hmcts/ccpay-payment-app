#!/bin/sh
source $WEBROOT_PATH/App_Data/jobs/triggered/s2sToken.sh

if [ "$SLOT" == "PRODUCTION" ]
then
    AUTH_TOKEN=$(s2sToken)
    URL=$PAYMENT_SERVER_URL/jobs/email-pay-reports?payment_method=CARD
    printf "Invoking email-pay-reports on :%s \n" $URL

    curl -X POST $URL -H "ServiceAuthorization: Bearer $AUTH_TOKEN" -d {}

    printf "\nFinished emailing CARD csv report"
else
    printf "Unsupported app slot:%s to run this job. \n" $SLOT
fi


