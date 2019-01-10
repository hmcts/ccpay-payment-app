vault_section = "prod"
capacity = "2"
idam_api_url = "https://idam-api.platform.hmcts.net"

card_payments_report_scheduler_enabled = "true"
pba_cmc_payments_report_scheduler_enabled = "true"
pba_divorce_payments_report_scheduler_enabled = "true"

spring_mail_host = "mta.reform.hmcts.net"
spring_mail_port = "25"
spring_mail_properties_mail_smtp_starttls_enable = "true"
spring_mail_properties_email_smtp_ssl_trust = "*"

card_payments_email_from = "no-reply@reform.hmcts.net"
card_payments_email_subject = "CNP: Card Payments Reconciliation Report"
card_payments_email_message = "Hi <br/><br/>Please find attached today''s reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

pba_cmc_payments_email_from = "no-reply@reform.hmcts.net"
pba_cmc_payments_email_subject = "CNP: PBA Reconciliation Report for CMC"
pba_cmc_payments_email_message = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

pba_divorce_payments_email_from = "no-reply@reform.hmcts.net"
pba_divorce_payments_email_subject = "PBA Divorce Reconciliation Report for Divorce"
pba_divorce_payments_email_message = "Hi <br/><br/>Please find attached today''s Payment by Account reconciliation report. <br/><br/>Regards <br/><br/>Payments team<br/><br/>"

feature_payments_search = true

external_host_name ="payment.platform.hmcts.net"
