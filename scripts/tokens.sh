#!/usr/bin/env bash

source ./config.sh

if [ $# -eq 0 ]
  then
    echo "Usage: /.tokens.sh SERVICE"
    echo "Service configuration is defined on config.sh (Ex. EXAMPLE)"
    exit 1
fi

function jsonValue() {
    KEY=$1
    num=$2
    awk -F"[,:}]" '{for(i=1;i<=NF;i++){if($i~/'$KEY'\042/){print $(i+1)}}}' | tr -d '"' | sed -n ${num}p
}

SERVICE=$1

echo "Fetching tokens for $SERVICE..."

S2S_SECRET=${SERVICE}"_S2S_SECRET"
S2S_SECRET=`eval echo \$c"${!S2S_SECRET}"`

S2S_SERVICE=${SERVICE}"_S2S_SERVICE"
S2S_SERVICE=`eval echo \$c"${!S2S_SERVICE}"`

# Generate secret with Oathtool
otp=`oathtool --totp -b "$S2S_SECRET"`

# Curl S2S service

s2s_token=`curl -X POST -s -H "Content-Type:application/json" "$S2S_URL" --proxy "$HTTP_PROXY" -k -d "{\"microservice\":\"$S2S_SERVICE\",\"oneTimePassword\":\"$otp\"}"`

# Output S2S
echo ""
echo "S2S TOKEN:"
echo "$s2s_token"

# Login with IDAM

IDAM_USER=${SERVICE}"_IDAM_USER"
IDAM_USER=`eval echo \$c"${!IDAM_USER}"`

IDAM_PASSWORD=${SERVICE}"_IDAM_PASSWORD"
IDAM_PASSWORD=`eval echo \$c"${!IDAM_PASSWORD}"`

IDAM_CLIENT_ID=${SERVICE}"_IDAM_CLIENT_ID"
IDAM_CLIENT_ID=`eval echo \$c"${!IDAM_CLIENT_ID}"`

IDAM_CLIENT_SECRET=${SERVICE}"_IDAM_CLIENT_SECRET"
IDAM_CLIENT_SECRET=`eval echo \$c"${!IDAM_CLIENT_SECRET}"`

REDIRECT_URI=${SERVICE}"_IDAM_REDIRECT_URI"
REDIRECT_URI=`eval echo \$c"${!REDIRECT_URI}"`

if [ "$USE_IDAM_FRONTEND_PROXY" = "true" ]; then

    url="$IDAM_BASE_URL/login?state=xx&response_type=code&client_id=$IDAM_CLIENT_ID&redirect_uri=$REDIRECT_URI"

    # GET
    idam_get=`curl -s -k -c - "$url"`

    csrf=$(echo "$idam_get" | grep -Po '<input type="hidden" name="_csrf" value="\K.*')
    csrf=${csrf::-2}

    csrf_cookie=$(echo "$idam_get" | tail -1 | grep -Po '.*_csrf\K.*' | xargs)

    # POST

    form="state=xx&username=$IDAM_USER&password=$IDAM_PASSWORD&response_type=code&redirect_uri=$REDIRECT_URI&client_id=$IDAM_CLIENT_ID&_csrf=$csrf"
    idam_post_reply=`curl -X POST -s -k -H "Content-Type:application/x-www-form-urlencoded" --cookie "_csrf=$csrf_cookie" -d "$form" "$url"`

   if [[ ${idam_post_reply} != *"Redirecting to"* ]]; then
        echo "$idam_post_reply"
        echo "ERROR LOGIN WITH IDAM BY FRONTEND, MOST LIKELY CAUSE: WRONG USER OR PASSWORD"
        exit 1
    fi

    idam_code=$(echo "$idam_post_reply" | grep -Po '.*Redirecting.*&code=\K.*')

    echo "IDAM CODE IS $idam_code"

else
    idam_code=`curl -X POST -s -k --proxy "$HTTP_PROXY" --user "$IDAM_USER:$IDAM_PASSWORD" "$IDAM_BASE_URL/oauth2/authorize?response_type=code&client_id=$IDAM_CLIENT_ID&redirect_uri=$REDIRECT_URI"`
    idam_code=`echo "$idam_code" | jsonValue code`
fi

idam_token_output=`curl -X POST -s -k -H "Content-Type:application/x-www-form-urlencoded" "$IDAM_BASE_URL/oauth2/token?code=$idam_code&grant_type=authorization_code&redirect_uri=$REDIRECT_URI&client_id=$IDAM_CLIENT_ID&client_secret=$IDAM_CLIENT_SECRET"`

echo "$idam_token_output"

idam_token=$( echo "$idam_token_output" | jsonValue access_token)

# Output IDAM Token
echo ""
echo "IDAM TOKEN:"
echo "$idam_token"
echo ""
