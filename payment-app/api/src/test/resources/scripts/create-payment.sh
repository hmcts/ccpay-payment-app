curl -s -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' --header 'service_id: divorce' -d '{
  "amount": 100,
  "application_reference": "application_reference",
  "payment_reference": "payment_reference",
  "description": "description",
  "return_url": "https://localhost",
  "email": "payments-test-email@binkmail.com"
}' 'http://localhost:8181/payments' | jq '.'
