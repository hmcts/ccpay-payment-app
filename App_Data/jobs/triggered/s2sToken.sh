#!/bin/sh
export PATH=$JAVA_HOME/bin:$PATH
function s2sToken() {
    otp="$(java -cp $WEBROOT_PATH/payment-otp-all.jar uk.gov.hmcts.payment.otp.OtpBootstrap $WEBJOB_S2S_CLIENT_SECRET)"
    s2s_token=$(curl -X POST $AUTH_PROVIDER_SERVICE_CLIENT_BASEURL/lease -H "Content-Type:application/json" -d "{\"microservice\":\"$WEBJOB_S2S_CLIENT_ID\",\"oneTimePassword\":\"$otp\"}")
    echo $s2s_token
}
