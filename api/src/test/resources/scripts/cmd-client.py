#!/usr/bin/env python
import json
import urllib2
import time

opener = urllib2.build_opener()


def prepareRequest(path, data):
    payload = None if data is None else json.dumps(data)
    return urllib2.Request('http://localhost:8181/payments/' + path, payload,
                           {'Content-Type': 'application/json', 'service_id': 'divorce'})


def printFormattedResponse(response):
    print(json.dumps(json.load(response), indent=4, sort_keys=True))


def printMenu():
    print "1: Get payment"
    print "2: Create payment"
    print "3: Cancel payment"
    print "4: Refund payment"


def getPaymentId():
    return raw_input("Payment id:")


while True:
    printMenu()
    choice = raw_input("Choice: ")

    if choice == "1":
        printFormattedResponse(opener.open(prepareRequest(getPaymentId(), None)))

    if choice == "2":
        printFormattedResponse(opener.open(prepareRequest("", {
            "amount": 100,
            "reference": "reference_" + `int(time.time())`,
            "description": "description",
            "return_url": "https://localhost",
            "email": "payments-test-email@binkmail.com"
        })))

    if choice == "3":
        paymentId = getPaymentId()
        opener.open(prepareRequest(paymentId + "/cancel", {}))
        printFormattedResponse(opener.open(prepareRequest(paymentId, None)))

    if choice == "4":
        paymentId = getPaymentId()
        opener.open(prepareRequest(paymentId + "/refunds", {
            "amount": 100,
            "refund_amount_available": 100
        }))
