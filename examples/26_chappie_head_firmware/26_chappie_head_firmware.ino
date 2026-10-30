// Chappie head firmware (examples/26_chappie_head.sc): ESP32-C3 SuperMini, four micro servos
// (ears, brow loop, chin loops) and a WS2812B strip behind the visor.
//
// Libraries (Arduino Library Manager): ESP32Servo, Adafruit NeoPixel.
// Board: "ESP32C3 Dev Module", USB CDC On Boot: Enabled.
//
// Wiring (servos and strip powered from the 5 V pin, i.e. USB; use a 2 A supply and put a
// 470 uF capacitor across 5 V and GND):
//   GPIO 3  ear, right        GPIO 5  brow loop
//   GPIO 4  ear, left         GPIO 6  chin loops
//   GPIO 10 LED strip data (through a 330 ohm resistor)
//
// It idles on its own: blinks, looks about, twitches its ears. Send a letter over serial
// (115200) to show an expression: n neutral, h happy, s sad, c curious, a angry, t talk,
// or 0 to stop the servos (calibration: every servo goes to its centre).
//
// Calibrate first: with the horns off, send 0, fit each ear and loop at its centre pose, then
// set the *_CENTRE values below if a part sits a few degrees off.

#include <ESP32Servo.h>
#include <Adafruit_NeoPixel.h>

const int PIN_EAR_R = 3, PIN_EAR_L = 4, PIN_BROW = 5, PIN_CHIN = 6, PIN_LEDS = 10;
const int NUM_LEDS = 12;

// Centre angle and the travel each way that clears the head (degrees)
const int EAR_CENTRE = 90, EAR_TRAVEL = 40;
const int BROW_CENTRE = 90, BROW_TRAVEL = 15;
const int CHIN_CENTRE = 90, CHIN_TRAVEL = 15;

Servo earR, earL, brow, chin;
Adafruit_NeoPixel leds(NUM_LEDS, PIN_LEDS, NEO_GRB + NEO_KHZ800);

// A pose: each part from -1 to 1 of its travel. Ears: + is up/forward, - is drooped back.
// Brow: + is raised. Chin: + is open.
struct Pose { float earR, earL, brow, chin; };
// The eyes: colour, and how wide each eye is (in LEDs, 1-3)
struct Eyes { uint8_t r, g, b; int width; };

const Pose NEUTRAL = {0, 0, 0, 0}, HAPPY = {0.7, 0.7, 0.6, 0.3}, SAD = {-1, -1, -0.8, -0.2},
           CURIOUS = {0.8, -0.3, 1, 0}, ANGRY = {-0.5, -0.5, -1, -0.6};
const Eyes EYES_NEUTRAL = {120, 220, 255, 2}, EYES_HAPPY = {120, 255, 160, 3},
           EYES_SAD = {40, 80, 255, 1}, EYES_CURIOUS = {200, 230, 255, 3},
           EYES_ANGRY = {255, 60, 20, 2};

Pose current = NEUTRAL, target = NEUTRAL;
Eyes eyes = EYES_NEUTRAL;
int lookOffset = 0;              // eyes shifted left/right by this many LEDs
bool talking = false, servosOn = true;
unsigned long nextBlink = 0, nextIdle = 0, moodUntil = 0;

int angleFor(float v, int centre, int travel, bool reversed) {
  v = constrain(v, -1.0f, 1.0f);
  return centre + (reversed ? -1 : 1) * (int)(v * travel);
}

void writeServos() {
  if (!servosOn) return;
  // The left ear's servo faces the other way, so it turns the other way for the same pose
  earR.write(angleFor(current.earR, EAR_CENTRE, EAR_TRAVEL, false));
  earL.write(angleFor(current.earL, EAR_CENTRE, EAR_TRAVEL, true));
  brow.write(angleFor(current.brow, BROW_CENTRE, BROW_TRAVEL, false));
  chin.write(angleFor(current.chin, CHIN_CENTRE, CHIN_TRAVEL, true));
}

void drawEyes(float open) {
  // Two eyes, a third of the way in from each end of the visor; `open` 0..1 dims them for a blink
  leds.clear();
  int centres[2] = {NUM_LEDS / 4, NUM_LEDS - 1 - NUM_LEDS / 4};
  for (int e = 0; e < 2; e++) {
    for (int k = 0; k < eyes.width; k++) {
      int i = centres[e] + lookOffset + k - eyes.width / 2;
      if (i >= 0 && i < NUM_LEDS) leds.setPixelColor(i, leds.Color(eyes.r * open, eyes.g * open, eyes.b * open));
    }
  }
  leds.show();
}

void blink() {
  for (float o = 1; o >= 0; o -= 0.25) { drawEyes(o); delay(15); }
  for (float o = 0; o <= 1; o += 0.25) { drawEyes(o); delay(15); }
}

void setMood(const Pose &p, const Eyes &e, unsigned long ms) {
  target = p;
  eyes = e;
  moodUntil = millis() + ms;
}

void handleSerial() {
  while (Serial.available()) {
    char c = Serial.read();
    talking = false;
    switch (c) {
      case 'n': setMood(NEUTRAL, EYES_NEUTRAL, 0); break;
      case 'h': setMood(HAPPY, EYES_HAPPY, 4000); break;
      case 's': setMood(SAD, EYES_SAD, 4000); break;
      case 'c': setMood(CURIOUS, EYES_CURIOUS, 4000); break;
      case 'a': setMood(ANGRY, EYES_ANGRY, 4000); break;
      case 't': talking = true; setMood(NEUTRAL, EYES_NEUTRAL, 4000); break;
      case '0':
        current = target = NEUTRAL;
        servosOn = true; writeServos(); servosOn = false;
        Serial.println("servos centred and stopped; send n to start again");
        break;
      default: break;
    }
    if (c != '0' && c != '\n' && c != '\r') servosOn = true;
  }
}

void idle(unsigned long now) {
  // Back to neutral when a mood runs out; otherwise look about and twitch an ear now and then
  if (moodUntil && now > moodUntil) { setMood(NEUTRAL, EYES_NEUTRAL, 0); moodUntil = 0; talking = false; }
  if (now < nextIdle || moodUntil) return;
  nextIdle = now + random(1500, 4000);
  switch (random(4)) {
    case 0: lookOffset = random(-1, 2); break;
    case 1: target.earR = random(-20, 60) / 100.0; break;
    case 2: target.earL = random(-20, 60) / 100.0; break;
    case 3: target.brow = random(-20, 40) / 100.0; break;
  }
}

void setup() {
  Serial.begin(115200);
  ESP32PWM::allocateTimer(0);
  ESP32PWM::allocateTimer(1);
  for (Servo *s : {&earR, &earL, &brow, &chin}) s->setPeriodHertz(50);
  earR.attach(PIN_EAR_R, 500, 2400);
  earL.attach(PIN_EAR_L, 500, 2400);
  brow.attach(PIN_BROW, 500, 2400);
  chin.attach(PIN_CHIN, 500, 2400);
  leds.begin();
  leds.setBrightness(80);
  writeServos();
  drawEyes(1);
  randomSeed(esp_random());
  Serial.println("Chappie: n h s c a t, 0 to centre");
}

void loop() {
  unsigned long now = millis();
  handleSerial();
  idle(now);
  if (talking) target.chin = (sin(now / 90.0) > 0.2) ? 0.8 : 0;
  // Ease every part toward its target
  float k = 0.12;
  current.earR += (target.earR - current.earR) * k;
  current.earL += (target.earL - current.earL) * k;
  current.brow += (target.brow - current.brow) * k;
  current.chin += (target.chin - current.chin) * (talking ? 0.5 : k);
  writeServos();
  if (now > nextBlink) { blink(); nextBlink = now + random(2500, 6000); }
  else drawEyes(1);
  delay(20);
}
