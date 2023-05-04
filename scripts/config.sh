# Broad Configuration

#S2S AAT
export S2S_URL=https://rpe-service-auth-provider-aat.service.core-compute-aat.internal/lease

# Tactical IDAM Dev
# export IDAM_BASE_URL=http://betaDevAccidamAppLB.reform.hmcts.net

# Tactical IDAM Preprod
# BACKEND (Requires SSH Tunnel enabled)
export IDAM_BASE_URL=https://preprod-idamapi.reform.hmcts.net:3511
# FRONTEND (Requires IDAM Oauth2 Token on the frontend, currently broken on TIDAM Preprod)
export USE_IDAM_FRONTEND_PROXY=false
# export IDAM_BASE_URL=https://idam.preprod.ccidam.reform.hmcts.net

export HTTP_PROXY=http://proxyout.reform.hmcts.net:8080

# PER SERVICE CONFIGURATION: Follow the example template to add the required environment variables for each service you require to use

export CMC_S2S_SERVICE=probate_frontend
export CMC_S2S_SECRET=xx
export CMC_IDAM_USER=xx
export CMC_IDAM_PASSWORD=xx
export CMC_IDAM_CLIENT_ID=fees_admin_frontend
export CMC_IDAM_CLIENT_SECRET=xx
export CMC_IDAM_REDIRECT_URI=https://fees-register-frontend-aat-staging.aat.platform.hmcts.net/oauth2/callback


export DIVORCE_S2S_SERVICE=probate_frontend
export DIVORCE_S2S_SECRET=xx
export DIVORCE_IDAM_USER=xx
export DIVORCE_IDAM_PASSWORD=xx
export DIVORCE_IDAM_CLIENT_ID=fees_admin_frontend
export DIVORCE_IDAM_CLIENT_SECRET=xx
export DIVORCE_IDAM_REDIRECT_URI=https://fees-register-frontend-aat-staging.aat.platform.hmcts.net/oauth2/callback

export PROBATE_S2S_SERVICE=probate_frontend
export PROBATE_S2S_SECRET=xx
export PROBATE_IDAM_USER=xx
export PROBATE_IDAM_PASSWORD=xx
export PROBATE_IDAM_CLIENT_ID=fees_admin_frontend
export PROBATE_IDAM_CLIENT_SECRET=xx
export PROBATE_IDAM_REDIRECT_URI=https://fees-register-frontend-aat-staging.aat.platform.hmcts.net/oauth2/callback

export CCD_S2S_SERVICE=probate_frontend
export CCD_S2S_SECRET=xx
export CCD_IDAM_USER=xx
export CCD_IDAM_PASSWORD=xx
export CCD_IDAM_CLIENT_ID=fees_admin_frontend
export CCD_IDAM_CLIENT_SECRET=xx
export CCD_IDAM_REDIRECT_URI=https://fees-register-frontend-aat-staging.aat.platform.hmcts.net/oauth2/callback

export BAR_S2S_SERVICE=probate_frontend
export BAR_S2S_SECRET=xx
export BAR_IDAM_USER=xx
export BAR_IDAM_PASSWORD=xx
export BAR_IDAM_CLIENT_ID=fees_admin_frontend
export BAR_IDAM_CLIENT_SECRET=xx
export BAR_IDAM_REDIRECT_URI=https://fees-register-frontend-aat-staging.aat.platform.hmcts.net/oauth2/callback

export BAR_S2S_SERVICE=probate_frontend
export BAR_S2S_SECRET=xx
export BAR_IDAM_USER=xx
export BAR_IDAM_PASSWORD=xx
export BAR_IDAM_CLIENT_ID=fees_admin_frontend
export BAR_IDAM_CLIENT_SECRET=xx
export BAR_IDAM_REDIRECT_URI=https://fees-register-frontend-aat-staging.aat.platform.hmcts.net/oauth2/callback

export FEES_S2S_SERVICE=probate_frontend
export FEES_S2S_SECRET=ASK
export FEES_IDAM_USER=ASK
export FEES_IDAM_PASSWORD=ASK
export FEES_IDAM_CLIENT_ID=fees_admin_frontend
export FEES_IDAM_CLIENT_SECRET=ASK
export FEES_IDAM_REDIRECT_URI=https://fees-register-frontend-aat-staging.aat.platform.hmcts.net/oauth2/callback
