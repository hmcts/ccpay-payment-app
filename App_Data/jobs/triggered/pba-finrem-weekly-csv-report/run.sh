#!/bin/sh
java -jar ./ccpay-scheduled-jobs-1.1.8.jar $WEBJOB_S2S_CLIENT_SECRET $AUTH_PROVIDER_SERVICE_CLIENT_BASEURL $WEBJOB_S2S_CLIENT_ID $PAYMENT_SERVER_URL pba-finrem-weekly-csv-report
