#!/bin/sh

printf "Invoke email-pay-reports endpoint on :%s\n" $PAYMENT_SERVER_URL

if [ "$SLOT" == "PRODUCTION" ]
then
    curl -X POST $PAYMENT_SERVER_URL/payments/email-pay-reports -d {}
    printf "\nFinished emailing csv reports"
else
    printf "Unsupported app slot:%s to run this job. \n" $SLOT
fi


