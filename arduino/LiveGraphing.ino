//Analog pins
int SALINITY_PIN = 1;

//Digital Pins
int LED_PIN = 9;

//Salinity Calibration Values
//     123
//1%   463
//2%   573
//3.5% 610

//ETC
int SAFE_SALINITY = 150;
int DELAY = 500;

void setup() {
  pinMode(LED_PIN, OUTPUT);
  Serial.begin(9600);
  int i = 0;
  while(Serial.available() <= 0) {
    i++;
    delay(1000);
    if(i > 2) break;
  }
  if(Serial.available() > 0) { //Handshake with pc
    DELAY = (int)(1000.0/(Serial.read()/60));
    Serial.println('Y');
  } else //or set default 1s time if disconnected
    DELAY = 1000;
  Serial.flush();
}

void loop() {
  int salinity = analogRead(SALINITY_PIN);
  Serial.println(salinity);
  
  if(salinity > 0 && salinity < SAFE_SALINITY)
    digitalWrite(LED_PIN, HIGH);
  else
    digitalWrite(LED_PIN, LOW);
  
  delay(DELAY);
}