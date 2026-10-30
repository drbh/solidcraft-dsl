// RC car firmware (examples/27_rc_car.sc): ESP32-C3 SuperMini, two MG995 steering servos (front
// and rear axle) and a brushless outrunner through a bidirectional (car) ESC, on a 2S LiPo.
//
// Libraries (Arduino Library Manager): ESP32Servo. WiFi and WebServer come with the core.
// Board: "ESP32C3 Dev Module", USB CDC On Boot: Enabled.
//
// Wiring: the ESC's BEC (5-6 V) feeds the servos and the board's 5 V pin; all grounds common.
// Put a 470 uF capacitor across 5 V and GND near the servos (two MG995s stalling draw amps).
//   GPIO 3  front steering servo signal      GPIO 5  ESC signal
//   GPIO 4  rear steering servo signal
//   The ESC arms at neutral (1500 us) for ARM_MS after power-up; keep the wheels off the ground.
//   If forward runs backwards swap any two motor wires. A unidirectional (plane) ESC: set
//   DRIVE_STOP_US to 1000 and DRIVE_RANGE_US to 1000, there is no reverse then.
//   THROTTLE_LIMIT scales the stick: 1000KV on 2S through 5.7:1 is ~7 m/s flat out, start at 0.4.
//
// The car is its own Wi-Fi access point ("rc-car", password below). Join it with a phone and
// open http://192.168.4.1 : a steering slider, a throttle slider and three steering modes:
//   FRONT  only the front wheels steer (a normal car)
//   4WS    the rear wheels steer the other way (half the turning circle)
//   CRAB   the rear wheels steer the same way (the car moves sideways)
// The page sends the sticks ten times a second; the ESC goes to neutral half a second after the
// last message (phone asleep, out of range). The serial port (115200) echoes what it receives.
//
// Calibrate first: with the horns off, power up, the servos centre; fit each horn so its pin is
// in the middle of the tie rod's slot, then trim the *_CENTRE values if the car does not run
// straight. A 20 deg wheel angle is about 25 deg at the servo (22 mm horn, 28 mm knuckle arm);
// STEER_TRAVEL keeps the knuckles clear of the yokes, do not raise it much.

#include <ESP32Servo.h>
#include <WiFi.h>
#include <WebServer.h>

const char *AP_NAME = "rc-car", *AP_PASSWORD = "drivefast";

const int PIN_SERVO_FRONT = 3, PIN_SERVO_REAR = 4, PIN_DRIVE = 5;

// Servo centre and the travel each way for full lock (degrees at the servo)
const int FRONT_CENTRE = 90, REAR_CENTRE = 90, STEER_TRAVEL = 25;
// Both servos lie the same way up with their horns pointing up; the front tie rod sits behind
// its axle and the rear one in front of its axle, so the same wheel angle needs opposite servo
// directions. Flip either sign if a horn was fitted the other way round.
const int FRONT_SIGN = 1, REAR_SIGN = -1;
// ESC: 1500 us neutral, 1000/2000 us full reverse/forward; THROTTLE_LIMIT caps the range used
const int DRIVE_STOP_US = 1500, DRIVE_RANGE_US = 500, DRIVE_SIGN = 1;
const float THROTTLE_LIMIT = 0.4f;
const unsigned long ARM_MS = 3000;
const unsigned long FAILSAFE_MS = 500;

enum Mode { FRONT = 0, FOUR_WHEEL = 1, CRAB = 2 };

Servo front, rear, drive;
WebServer server(80);
float steer = 0, throttle = 0;   // -1..1
Mode mode = FRONT;
unsigned long lastCommand = 0;

const char PAGE[] PROGMEM = R"HTML(<!doctype html><html><head><meta name=viewport content="width=device-width,initial-scale=1,user-scalable=no">
<title>rc-car</title><style>
body{margin:0;font-family:sans-serif;background:#111;color:#eee;user-select:none;-webkit-user-select:none}
.row{display:flex;justify-content:space-around;align-items:center;height:60vh}
input[type=range]{width:70vw;height:40px}
#t{writing-mode:vertical-lr;direction:rtl;height:50vh;width:40px}
.modes{display:flex;justify-content:center;gap:8px;padding:12px}
button{font-size:18px;padding:12px 18px;border:0;border-radius:8px;background:#333;color:#eee}
button.on{background:#2a8}
#s{margin:20px auto;display:block}
</style></head><body>
<div class=modes><button id=m0 class=on onclick="setMode(0)">FRONT</button><button id=m1 onclick="setMode(1)">4WS</button><button id=m2 onclick="setMode(2)">CRAB</button></div>
<div class=row><input id=t type=range min=-100 max=100 value=0></div>
<input id=s type=range min=-100 max=100 value=0>
<script>
let mode=0;const s=document.getElementById('s'),t=document.getElementById('t');
function setMode(m){mode=m;for(let i=0;i<3;i++)document.getElementById('m'+i).className=i==m?'on':'';}
function spring(el){const up=()=>{el.value=0};el.addEventListener('pointerup',up);el.addEventListener('pointercancel',up);el.addEventListener('touchend',up);}
spring(t);
setInterval(()=>{fetch('/ctl?s='+s.value+'&t='+t.value+'&m='+mode).catch(()=>{})},100);
</script></body></html>)HTML";

void writeOutputs() {
  // Rear steering per mode; the throttle as the ESC's pulse width about its stop point
  float rearSteer = mode == FOUR_WHEEL ? -steer : (mode == CRAB ? steer : 0);
  front.write(FRONT_CENTRE + FRONT_SIGN * (int)(steer * STEER_TRAVEL));
  rear.write(REAR_CENTRE + REAR_SIGN * (int)(rearSteer * STEER_TRAVEL));
  drive.writeMicroseconds(DRIVE_STOP_US + DRIVE_SIGN * (int)(throttle * THROTTLE_LIMIT * DRIVE_RANGE_US));
}

void handleControl() {
  steer = constrain(server.arg("s").toInt(), -100, 100) / 100.0f;
  throttle = constrain(server.arg("t").toInt(), -100, 100) / 100.0f;
  int m = server.arg("m").toInt();
  mode = m == 1 ? FOUR_WHEEL : (m == 2 ? CRAB : FRONT);
  lastCommand = millis();
  writeOutputs();
  server.send(200, "text/plain", "ok");
  Serial.printf("steer %.2f throttle %.2f mode %d\n", steer, throttle, (int)mode);
}

void setup() {
  Serial.begin(115200);
  ESP32PWM::allocateTimer(0);
  ESP32PWM::allocateTimer(1);
  for (Servo *s : {&front, &rear, &drive}) s->setPeriodHertz(50);
  front.attach(PIN_SERVO_FRONT, 500, 2400);
  rear.attach(PIN_SERVO_REAR, 500, 2400);
  drive.attach(PIN_DRIVE, 1000, 2000);
  writeOutputs();   // centred, ESC at neutral
  delay(ARM_MS);    // let the ESC arm before Wi-Fi comes up
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_NAME, AP_PASSWORD);
  server.on("/", []() { server.send_P(200, "text/html", PAGE); });
  server.on("/ctl", handleControl);
  server.begin();
  Serial.printf("rc-car: join %s and open http://%s\n", AP_NAME, WiFi.softAPIP().toString().c_str());
}

void loop() {
  server.handleClient();
  if (throttle != 0 && millis() - lastCommand > FAILSAFE_MS) {
    throttle = 0;   // no phone: stop; the steering stays where it was
    writeOutputs();
    Serial.println("failsafe: drive stopped");
  }
}
