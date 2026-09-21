import http from 'k6/http'
import { check } from 'k6'
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'

// Load profile: ramp to 50 virtual users posting transfers between two fixed
// accounts, which is the contended path. Set FROM, TO and TOKEN first.
export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.30'],       // 409s under contention are expected
    http_req_duration: ['p(99)<500'],
  },
}

const FROM = __ENV.FROM
const TO = __ENV.TO
const TOKEN = __ENV.TOKEN

export default function () {
  const response = http.post(
    'http://localhost:8080/api/transfers',
    JSON.stringify({ from: FROM, to: TO, amount: '1.0000' }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': uuidv4(),
        Authorization: `Bearer ${TOKEN}`,
      },
    },
  )
  check(response, {
    'posted or cleanly rejected': (r) => r.status === 201 || r.status === 409,
  })
}
