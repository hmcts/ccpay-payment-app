const {execSync} = require('child_process')
let stdout = execSync('npm install')

const csv = require('csv')
const request = require('request')
const moment = require('moment')
const nodemailer = require('nodemailer')
const fs = require('fs')

const header = {
  service_name: 'Service',
  payment_group_reference: 'Payment Group reference',
  payment_reference: 'Payment reference',
  ccd_case_number: 'CCD reference',
  case_reference: 'Case reference',
  date_created: 'Payment created date',
  date_updated: 'Payment status updated date',
  payment_status: 'Payment status',
  payment_channel: 'Payment channel',
  payment_method: 'Payment method',
  amount: 'Payment amount',
  site_id: 'Site id',
  code: 'Fee code',
  version: 'Version',
  calculated_amount: 'Calculated amount',
  memo_line: 'Memo line',
  natural_account_code: 'NAC',
  volume: 'Fee volume'
}

let sendCSVemail = (csv) => {
  nodemailer.createTestAccount((err, account) => {
    let transporter = nodemailer.createTransport({
      host: 'mta.reform.hmcts.net',
      port: 25,
      secure: false,
      tls: {
        // do not fail on invalid certs
        rejectUnauthorized: false
      }
    })

    // setup email data with unicode symbols
    // TODO: setup env vars properly and make sure subject of the message is correct
    let mailOptions = {
      from: process.env.CARD_PAYMENTS_EMAIL_FROM, // sender address
      to: process.env.CARD_PAYMENTS_EMAIL_TO, // list of receivers
      subject: `${csv.title}`, // TODO: make sure this is correct
      text: csv.body, // plain text body
      html: csv.body, // html body
      attachments: [
        {   // filename and content type is derived from path
          path: csv.filename
        }
      ]
    }

    // send mail with defined transport object
    transporter.sendMail(mailOptions, (error, info) => {
      if (error) {
        return console.log(error)
      }
      console.log('Message sent: %s', info.messageId)
      console.log('Preview URL: %s', nodemailer.getTestMessageUrl(info))
    })
  })
}

function exportCSV (data, date, paymentMethod) {
  let filename = `${paymentMethod}-${date}.csv`
  let title = `${paymentMethod} CSV for ${date}`
  csv.stringify(data, function (err, data) {
    fs.writeFile(filename, data, function (err) {
      if (err) {
        return console.log(err)
      }
      console.log('The file was saved!')
      sendCSVemail({
        body: data,
        filename: filename,
        date: date,
        paymentMethod: paymentMethod,
        title: title
      })
      console.log(data)
    })

  })
}

function preparePaymentsForCSV (payments) {
  let result = []
  result.push(header)

  payments.forEach(payment => {
    payment.fees.forEach(feeInPayment => {
      // TODO: reformat dates, use moment.js
      result.push({
        ...payment,
        ...feeInPayment
      })
    })
  })

  return result
}

let yesterday = moment().subtract(1, 'days').format('YYYY-MM-DD')
let today = moment().format('YYYY-MM-DD')

let host = process.env.WEBSITE_HOSTNAME
let paymentMethod = 'card'

request(`${host}/payments?start_date=${yesterday}&end_date=${today}&payment_method=${paymentMethod}`, (error, response, body) => {
  try {
    exportCSV(preparePaymentsForCSV(JSON.parse(body).payments), yesterday, paymentMethod)
  } catch (e) {
    console.log(body, e)
  }
})

paymentMethod = 'pba'

request(`${host}/payments?start_date=${yesterday}&end_date=${today}&payment_method=${paymentMethod}`, (error, response, body) => {
  try {
    exportCSV(preparePaymentsForCSV(JSON.parse(body).payments), yesterday, paymentMethod)
  } catch (e) {
    console.log(body, e)
  }
})
