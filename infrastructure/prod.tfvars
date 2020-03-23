vault_section = "prod"
capacity = "2"
idam_api_url = "https://idam-api.platform.hmcts.net"

card_payments_report_scheduler_enabled = "true"
bar_payments_report_scheduler_enabled = "true"
pba_cmc_payments_report_scheduler_enabled = "true"
pba_probate_payments_report_scheduler_enabled = "true"
pba_finrem_payments_report_scheduler_enabled = "true"
pba_divorce_payments_report_scheduler_enabled = "true"
pba_fpl_payments_report_scheduler_enabled = "true"

spring_mail_host = "mta.reform.hmcts.net"
spring_mail_port = "25"
spring_mail_properties_mail_smtp_starttls_enable = "true"
spring_mail_properties_email_smtp_ssl_trust = "*"

card_payments_email_from = "no-reply@reform.hmcts.net"
card_payments_email_subject = "CNP: Card Payments Reconciliation Report"
card_payments_email_message = "Hi <br/><br/>Please find attached today''s reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

bar_payments_email_from = "no-reply@reform.hmcts.net"
bar_payments_email_subject = "CNP: Bar Payments Reconciliation Report"
bar_payments_email_message = "Hi <br/><br/>Please find attached today''s reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

pba_cmc_payments_email_from = "no-reply@reform.hmcts.net"
pba_cmc_payments_email_subject = "CNP: PBA Reconciliation Report for CMC"
pba_cmc_payments_email_message = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

pba_probate_payments_email_from = "no-reply@reform.hmcts.net"
pba_probate_payments_email_subject = "CNP: PBA Reconciliation Report for PROBATE"
pba_probate_payments_email_message = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

pba_finrem_payments_email_from = "no-reply@reform.hmcts.net"
pba_finrem_payments_email_subject = "CNP: PBA Reconciliation Report for FINREM"
pba_finrem_payments_email_message = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

pba_divorce_payments_email_from = "no-reply@reform.hmcts.net"
pba_divorce_payments_email_subject = "CNP: PBA Divorce Reconciliation Report for Divorce"
pba_divorce_payments_email_message = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

pba_fpl_payments_email_from = "no-reply@reform.hmcts.net"
pba_fpl_payments_email_subject = "CNP:PBA Reconciliation Report for FPL"
pba_fpl_payments_email_message = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

feature_payments_search = true

external_host_name ="payment.platform.hmcts.net"

pci_pal_callback_url = "https://core-api-mgmt-prod.azure-api.net/telephony-api/telephony/callback"
paybubble_home_url = "https://paybubble.platform.hmcts.net"

telephony_api_gateway_certificate_thumbprints = ["68EDF481C5394D65962E9810913455D3EC635FA5", "C46826BF1E82DF37664F7A3678E6498D056DA4A9"]
pci_pal_callback_url = "https://core-api-mgmt-prod.azure-api.net/telephony-api/telephony/callback"


callback_payments_cutoff_time_in_minutes = 2

core_case_data_api_url = "http://ccd-data-store-api-prod.service.core-compute-prod.internal"

#feature_duplicate_payment_check = false

