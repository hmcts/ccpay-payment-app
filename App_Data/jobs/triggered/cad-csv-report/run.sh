#!/bin/sh

if [ "$SLOT" == "PRODUCTION" ]
then
    var URL = $PAYMENT_SERVER_URL/jobs/email-pay-reports?payment_method=CARD
    printf "Invoking email-pay-reports on :%s \n" $URL
    curl -X POST $URL -d {}
    printf "\nFinished emailing CARD csv report"
else
    printf "Unsupported app slot:%s to run this job. \n" $SLOT
fi


