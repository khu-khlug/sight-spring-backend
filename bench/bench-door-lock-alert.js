import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'internal-api-key';

export const options = {
  scenarios: {
    door_lock_alert: {
      executor: 'ramping-arrival-rate',
      startRate: 1,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 300,
      stages: [
        { target: 2, duration: '20s' },
        { target: 5, duration: '20s' },
        { target: 10, duration: '20s' },
        { target: 20, duration: '20s' },
        { target: 40, duration: '20s' },
        { target: 80, duration: '20s' },
        { target: 120, duration: '20s' },
      ],
    },
  },
};

export default function () {
  const res = http.post(
    `${BASE_URL}/internal/door-lock/alert-die`,
    JSON.stringify({ roomNumber: 405 }),
    { headers: { 'Content-Type': 'application/json', 'x-api-key': API_KEY } },
  );
  check(res, { 'status is 204': (r) => r.status === 204 });
}
