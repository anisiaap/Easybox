import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// Use environment variables or defaults for your server URLs.
const CENTRAL_URL = __ENV.CENTRAL_URL || 'http://localhost:8080';
const DEVICE_URL  = __ENV.DEVICE_URL  || 'http://localhost:8081';

// Custom metrics
let registrationTrend = new Trend('registration_time');
let getEasyboxesTrend = new Trend('get_easyboxes_time');
let deviceStatusTrend = new Trend('device_status_time');

// A helper function to safely parse JSON.
function safeJson(r) {
    try {
        return r.json();
    } catch (e) {
        return null;
    }
}

export let options = {
    stages: [
        { duration: '30s', target: 20 }, // ramp up to 20 VUs
        { duration: '1m', target: 20 },  // hold at 20 VUs for 1 minute
        { duration: '30s', target: 0 }   // ramp down to 0 VUs
    ]
};

export default function () {
    // Group 1: Central Backend - Device Registration
    group('Central Device Registration', function () {
        const url = `${CENTRAL_URL}/api/device/register`;
        let payload = JSON.stringify({
            address: "Strada Coriolan Brediceanu, Timisoara",
            deviceUrl: DEVICE_URL,
            status: "active"
        });
        let params = { headers: { 'Content-Type': 'application/json' } };

        let res = http.post(url, payload, params);
        registrationTrend.add(res.timings.duration);

        // If registration is successful (200), check that "id" is present.
        // Otherwise, for error responses (like 409 Conflict or 422 from Geocoding),
        // check that the JSON error response contains a "message" field.
        if (res.status === 200) {
            check(res, {
                'Registration status 200': (r) => r.status === 200,
                'Registration returns Easybox id': (r) => r.json('id') !== undefined,
            });
        } else {
            let json = safeJson(res);
            check(res, {
                'Registration error returns JSON with message': (r) =>
                    json !== null && json.message !== undefined,
            });
        }
        sleep(1);
    });

    // Group 2: Central Backend - Get All Easyboxes (Admin Endpoint)
    group('Central Get Easyboxes', function () {
        const url = `${CENTRAL_URL}/api/admin/easyboxes`;
        let res = http.get(url);
        getEasyboxesTrend.add(res.timings.duration);
        check(res, {
            'Get Easyboxes status 200': (r) => r.status === 200,
            'Response is array': (r) => Array.isArray(r.json()),
        });
        sleep(1);
    });

    // Group 3: Device Backend - Get Status
    group('Device - Get Status', function () {
        const url = `${DEVICE_URL}/api/ondevice/status`;
        let res = http.get(url);
        deviceStatusTrend.add(res.timings.duration);
        check(res, {
            'Device status 200': (r) => r.status === 200,
            'Status message contains "online"': (r) => r.body && r.body.includes("online"),
        });
        sleep(1);
    });

    // Group 4: Device Backend - Get Compartments
    group('Device - Get Compartments', function () {
        const url = `${DEVICE_URL}/api/ondevice/compartments`;
        let res = http.get(url);
        check(res, {
            'Device compartments status 200': (r) => r.status === 200,
            'Response is array': (r) => Array.isArray(r.json()),
        });
        sleep(1);
    });

    // Group 5: Device Backend - Get Config
    group('Device - Get Config', function () {
        const url = `${DEVICE_URL}/api/ondevice/config`;
        let res = http.get(url);
        check(res, {
            'Device config status 200': (r) => r.status === 200,
            'Response contains expected keys': (r) => r.json('compartments') !== undefined,
        });
        sleep(1);
    });

    // Group 6: Device Backend - Reserve a Compartment
    group('Device - Reserve Compartment', function () {
        // Testing compartment id 1; adjust if needed.
        const url = `${DEVICE_URL}/api/ondevice/compartments/1/reserve`;
        let res = http.post(url, null, { headers: { 'Content-Type': 'application/json' } });
        check(res, {
            'Reserve status is 200 or 409': (r) => r.status === 200 || r.status === 409,
            'Error response has message if not 200': (r) => {
                if (r.status !== 200) {
                    let json = safeJson(r);
                    return json !== null && json.message !== undefined;
                }
                return true;
            }
        });
        sleep(1);
    });

    // Group 7: Device Backend - Clean a Compartment
    group('Device - Clean Compartment', function () {
        // Testing compartment id 1; adjust if needed.
        const url = `${DEVICE_URL}/api/ondevice/compartments/1/clean`;
        let res = http.post(url, null, { headers: { 'Content-Type': 'application/json' } });
        check(res, {
            'Clean status is 200 or 404': (r) => r.status === 200 || r.status === 404,
            'Error response has message if not 200': (r) => {
                if (r.status !== 200) {
                    let json = safeJson(r);
                    return json !== null && json.message !== undefined;
                }
                return true;
            }
        });
        sleep(1);
    });
}
