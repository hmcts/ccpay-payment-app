const { execSync } = require('child_process');
// stderr is sent to stdout of parent process
// you can set options.stdio if you want it to go elsewhere
let stdout = execSync('npm install');

const csv = require('csv')
const request = require('request')
const moment = require('moment')

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
  memoLine: 'Memo line',
  nac: 'NAC',
  volume: 'Fee volume'
}

function exportCSV (data, filename) {
  csv.stringify(data, function (err, data) {
    if (!filename) {
      process.stdout.write(data)
    }
  })
}

function preparePaymentsForCSV (payments) {
  let result = []
  result.push(header)

  payments.forEach(payment => {
    payment.fees.forEach(feeInPayment => {
      // TODO: reformat dates
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

// let host = process.env.WEBSITE_HOSTNAME
let host = 'http://localhost:8080'
let paymentMethod = 'card'

request(`${host}/payments?start_date=${yesterday}&end_date=${today}&payment_method=${paymentMethod}`, (error, response, body) => {
  try {
    exportCSV(preparePaymentsForCSV(JSON.parse(body).payments))
  } catch (e) {
    console.log(body, e)
  }
})

paymentMethod = 'pba'

request(`${host}/payments?start_date=${yesterday}&end_date=${today}&payment_method=${paymentMethod}`, (error, response, body) => {
  try {
    exportCSV(preparePaymentsForCSV(JSON.parse(body).payments))
  } catch (e) {
    console.log(body, e)
  }
})
