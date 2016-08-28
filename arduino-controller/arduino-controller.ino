#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_TFTLCD.h>
#include <EEPROM.h>
#include <TouchScreen.h>
#include "Color.h"
#include "LcdUtils.h"
#include "DigiPot.h"

#define DEBUG_MODE false

// LCD pins...
#define LCD_CS A3
#define LCD_CD A2
#define LCD_WR A1
#define LCD_RD A0
#define LCD_RESET A4
Adafruit_TFTLCD tft(LCD_CS, LCD_CD, LCD_WR, LCD_RD, LCD_RESET);

// Touch screen pins
#define YP A3
#define XM A2
#define YM 9
#define XP 8

#define MINPRESSURE 4
#define MAXPRESSURE 2000

#define TS_MINX 150
#define TS_MINY 120
#define TS_MAXX 920
#define TS_MAXY 940
TouchScreen touchScreen = TouchScreen(XP, YP, XM, YM, 300);

const int PROBE_ACTIVE_SENSOR_PIN = 15;
const int BUZZER_IO_PIN = 44;
const int DAC_OUTPUT_PIN = 22;

int killCount = 0;
int lifeTimeKillCount = 0;
long probeActiveStartTime = 0;
long lastTickTime = 0;
bool isProbeActive = false;

bool firstLoop = true;
bool isRedrawNeeded = true;
int currentLayoutYPos = 0;
int buttonYStart = 0;
double lastInputVoltage = 1;

int ACCENT_COLOR = MAUVE;

DigiPot powerPot(DAC_OUTPUT_PIN);
int targetMicroAmps = 500;

void setup(void) {
	Serial.begin(9600);

	initializeLcd(tft);
	tft.fillScreen(Color::WHITE);

	lifeTimeKillCount = readInt(0);

	targetMicroAmps = readInt(4);
	if (targetMicroAmps < 1)
		targetMicroAmps = 500;
}

void loop(void) {
	int activeVoltage = analogReadFiltered(PROBE_ACTIVE_SENSOR_PIN);
	bool ended = false;

	if (isInsertStart(activeVoltage)) {
		isProbeActive = true;
		probeActiveStartTime = millis();
	} else if (activeVoltage == 0) {
		if (isInsertEnd()) {
			ended = true;
			isRedrawNeeded = true;

			killCount++;
			lifeTimeKillCount++;
			saveInt(lifeTimeKillCount, 0);
		}

		isProbeActive = false;
	}

	if (isProbeActive && getActiveTime() != lastTickTime)
		isRedrawNeeded = true;

	long time = getActiveTime();
	if (isProbeActive && (time % 10 > 0 || time < 10))
		playBuzzer();
	else
		stopBuzzer();

	double R1 = 480;
	double vIn = lastInputVoltage;
	double vOut = (5.0 * activeVoltage) / 1024.0;

	double buffer = (vIn / vOut) - 1;
	double R2 = (R1 * buffer);
	double current = vIn / (R2 + R1);

	double targetCurrent = 0.000001 * targetMicroAmps;
	double targetVoltage = targetCurrent * (R2 + R1);

	if (targetVoltage > 10)
		targetVoltage = 10;

	// initial read may be off so drop back down if voltage really high so no possible initial painful zap
	if (targetVoltage > 8 && getPreciseActiveTime() < 1.5 && targetMicroAmps < 900)
		targetVoltage = 6.5;

	if (activeVoltage < 1) {
		powerPot.writeVolts(1);
		targetVoltage = 1;
		lastInputVoltage = 1;
		R2 = 0;
		current = 0;
	}

	else if (abs(lastInputVoltage - targetVoltage) >= .01) {
		// divide by 2 since opamp will double our target voltage
		powerPot.writeVolts(targetVoltage / 2.0);
		lastInputVoltage = targetVoltage;

		if (DEBUG_MODE) {
			Serial.print("targetVoltage: "); Serial.print(targetVoltage); Serial.println();
			Serial.print("current: "); Serial.print(current * 1000000); Serial.print(" uA"); Serial.println();
			Serial.print("target current: "); Serial.print(targetMicroAmps); Serial.print(" uA"); Serial.println();
			Serial.print("R2: "); Serial.print(R2); Serial.println();
			Serial.print("vOut: "); Serial.print(vOut); Serial.println();
		}
	}

	readTouchScreen();

	if (isRedrawNeeded) {
		currentLayoutYPos = 0;
		drawHomeScreen(ended);

		char voltLabel[32];
		char volts[6];
		char res[6];
		dtostrf(targetVoltage, 2, 2, volts);
		dtostrf(R2, 3, 0, res);

		sprintf(voltLabel, "%s | %s", res, volts);
		drawInfoBox("R | VOut", voltLabel, true);

		drawInfoBox("Current[uA]", String(targetMicroAmps), true);
		drawButtons(firstLoop);

		isRedrawNeeded = false;
	}


	lastTickTime = getActiveTime();
	firstLoop = false;
	delay(25);
}



const int padding = 7;

void readTouchScreen() {
	digitalWrite(13, HIGH);
	TSPoint p = touchScreen.getPoint();
	digitalWrite(13, LOW);

	pinMode(XM, OUTPUT);
	pinMode(YP, OUTPUT);

	if (p.z < MINPRESSURE || p.z > MAXPRESSURE)
		return;

	// scale w/h from 0->1023 and take inverse for standard portrait orientation
	p.x = tft.width() - map(p.x, TS_MINX, TS_MAXX, tft.width(), 0);
	p.y = tft.height() - (tft.height() - map(p.y, TS_MINY, TS_MAXY, tft.height(), 0));

	int width = tft.width() - (padding * 2);

	// touch screen very bottom guves like 312 Y so - 35 for some leeway..
	// possibly need to get precise resistance for specific one and put it in for TouchScreen constr
	if (p.y > buttonYStart - 35 && p.y < buttonYStart + 60) {
		if (p.x < width / 2)
			targetMicroAmps -= 50;
		else if (targetMicroAmps <= 1000)
			targetMicroAmps += 50;

		saveInt(targetMicroAmps, 4);
		isRedrawNeeded = true;
	}
}

void drawHomeScreen(bool ended) {
	drawInfoBox("Insert Timer", String(getActiveTime()), true);
	drawInfoBox("Kill Count", String(killCount), firstLoop || ended);
	drawInfoBox("Lifetime Kills", String(lifeTimeKillCount), firstLoop || ended);
}

void drawButtons(bool redraw) {
	int y = currentLayoutYPos + padding;
	int height = 26;
	int width = tft.width() - (padding * 2);
	currentLayoutYPos += height + padding;
	buttonYStart = currentLayoutYPos;

	if (!redraw)
		return;

	tft.fillRect(padding, y, width / 2, height, Color::RED);
	tft.setTextColor(Color::WHITE); tft.setTextSize(2);
	tft.setCursor(width / 4, y + 5);
	tft.println("-");

	tft.fillRect(padding + (width / 2), y, width / 2, height, Color::BLACK);
	tft.setCursor((width / 4) + (width / 2), y + 5);
	tft.println("+");
}

void drawInfoBox (String title, String data, bool redraw) {
	int y = currentLayoutYPos + padding;
	int height = 50;
	int width = tft.width() - (padding * 2);

	currentLayoutYPos += height + padding;
	if (!redraw)
		return;

	tft.setCursor(padding * 2, y + 5);
	tft.setTextColor(Color::WHITE); tft.setTextSize(2);
	// hack to get refresh fine switch to actual objects that know when to redraw like below class
	// eventually
	if (firstLoop) {
		tft.fillRect(padding, y, width, height, ACCENT_COLOR);
		tft.println(title);
	} else
		tft.fillRect(width / 4, y + height / 2, width - (padding * 2) - (width / 4), height / 2, ACCENT_COLOR);


	int dataLabelStart = width / 2;
	if (data.length() > 5)
		dataLabelStart = width / 4;
	tft.setCursor(dataLabelStart, y + (padding * 4));
	tft.setTextColor(Color::WHITE); tft.setTextSize(2);
	tft.println(data);

}

void saveInt(int value, int position) {
	int a = value / 256;
	int b = value % 256;

	EEPROM.write(position, a);
	EEPROM.write(position + 1, b);
}

int readInt(int position) {
	int a = EEPROM.read(position);
	int b = EEPROM.read(position + 1);

	return a * 256 + b;
}

bool isInsertStart(int activeVoltage) {
	return activeVoltage > 0 && !isProbeActive;
}

bool isInsertEnd() {
	return isProbeActive && getActiveTime() > 4;
}

long getActiveTime() {
	if (isProbeActive)
		return (millis() - probeActiveStartTime) / 1000;

	return 0;
}

double getPreciseActiveTime() {
	if (isProbeActive)
		return (millis() - probeActiveStartTime) / 1000.0;

	return 0;
}

void playBuzzer() {
	int hz = 110;

	tone(BUZZER_IO_PIN, hz);
}

void stopBuzzer() {
	noTone(BUZZER_IO_PIN);
}

#define NUM_READS 100
float analogReadFiltered(int sensorpin) {
	int sortedValues[NUM_READS];
	for (int i = 0; i < NUM_READS; i++) {
		int value = analogRead(sensorpin);
		sortedValues[i] = value;
	}

	bubbleSort(sortedValues, NUM_READS);

	//return scaled mode of 10 values
	return scaledMode(sortedValues, 10);
}

int scaledMode(int* array, int scale) {
	float returnval = 0;
	for (int i = NUM_READS / 2 - (scale/2); i < (NUM_READS / 2 + (scale/2)); i++)
		returnval += array[i];

	return returnval / scale;
}

void bubbleSort(int* array, int size)
{
	int i, j, swap; 
	for (i = 0 ; i < ( size - 1 ); i++)
	{
		for (j = 0 ; j < size - i - 1; j++)
		{
			if (array[j] > array[j + 1]) 
			{
				swap = array[j];
				array[j] = array[j + 1];
				array[j + 1] = swap;
			}
		}
	}
}


// class InfoBox {
// public:
// 	InfoBox(String title, int data);
// 	setData(int data);
// private:
// 	int data;
// 	String title;

// };

// InfoBox::InfoBox(String title, int data)
// {
// 	this->title = title;
// 	this->data = data;
// }

// void InfoBox::setData(int data)
// {
// 	this->data = data;
// }
