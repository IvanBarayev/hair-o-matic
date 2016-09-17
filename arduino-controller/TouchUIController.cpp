/*
	UI controller for a touch screen LCD connected directly to the Arduino.
	Rather sloppy implementation but it is being phased out for an Android phone UI
	controller. Leaving it here though so others making this can use a LCD instead
	of an Android phone if they desire.
*/
#include <Arduino.h>
#include <SPI.h>
#include "TouchUIController.h"

// LCD pins...
#define LCD_CS A3
#define LCD_CD A2
#define LCD_WR A1
#define LCD_RD A0
#define LCD_RESET A4

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

const int padding = 7;
const int ACCENT_COLOR = MAUVE;

Adafruit_TFTLCD tft(LCD_CS, LCD_CD, LCD_WR, LCD_RD, LCD_RESET);
TouchScreen touchScreen(XP, YP, XM, YM, 300);

TouchUIController::TouchUIController(ProbeState* state) {
	this->state = state;
}

void TouchUIController::initialize() {
	useSlowRefresh = true;
	initializeLcd(tft);
	tft.fillScreen(Color::WHITE);
}

void TouchUIController::drawHomeScreen(bool ended) {
	drawInfoBox("Insert Timer", String(state->getActiveTime()), true);
	drawInfoBox("Kill Count", String(state->getKillCount()), state->isFirstLoop || ended);
	drawInfoBox("Lifetime Kills", String(state->getLifeTimeCount()), state->isFirstLoop || ended);
}

void TouchUIController::drawButtons(bool redraw) {
	int y = currentLayoutYPos + padding;
	int height = 26;
	int width = tft.width() - (padding * 2);
	currentLayoutYPos += height + padding;
	buttonYStart = currentLayoutYPos;

	if (!redraw)
		return;

	tft.fillRect(padding, y, width / 2, height, Color::RED);
	tft.setTextColor(Color::WHITE);
	tft.setTextSize(2);
	tft.setCursor(width / 4, y + 5);
	tft.println("-");

	tft.fillRect(padding + (width / 2), y, width / 2, height, Color::BLACK);
	tft.setCursor((width / 4) + (width / 2), y + 5);
	tft.println("+");
}

void TouchUIController::drawInfoBox (String title, String data, bool redraw) {
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
	if (state->isFirstLoop) {
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

void TouchUIController::update(bool ended) {
	if (state->getIsRefreshNeeded()) {
		currentLayoutYPos = 0;
		drawHomeScreen(ended);

		char voltLabel[32];
		char volts[6];
		char res[6];
		dtostrf(state->lastInputVoltage, 2, 2, volts);
		dtostrf(state->resistance, 3, 0, res);

		sprintf(voltLabel, "%s | %s", res, volts);
		drawInfoBox("R | VOut", voltLabel, true);

		drawInfoBox("Current[uA]", String(state->getTargetMicroAmps()), true);
		drawButtons(state->isFirstLoop);

		state->setIsRefreshNeeded(false);
	}
}

void TouchUIController::readInput() {
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
			state->decreaseTargetCurrent();
		else if (state->getTargetMicroAmps() <= 1000)
			state->increaseTargetCurrent();

		state->setIsRefreshNeeded(true);
	}
}

void TouchUIController::initializeLcd (Adafruit_TFTLCD& tft) {
	Serial.begin(9600);
	Serial.print("TFT size is "); Serial.print(tft.width()); Serial.print("x"); Serial.println(tft.height());

	tft.reset();

	uint16_t identifier = tft.readID();
	if (identifier == 0x9325) {
		Serial.println(F("Found ILI9325 LCD driver"));
	} else if (identifier == 0x9328) {
		Serial.println(F("Found ILI9328 LCD driver"));
	} else if (identifier == 0x4535) {
		Serial.println(F("Found LGDP4535 LCD driver"));
	} else if (identifier == 0x7575) {
		Serial.println(F("Found HX8347G LCD driver"));
	} else if (identifier == 0x9341) {
		Serial.println(F("Found ILI9341 LCD driver"));
	} else if (identifier == 0x8357) {
		Serial.println(F("Found HX8357D LCD driver"));
	} else if (identifier == 0x0101)
		identifier = 0x9341;
	else
		identifier = 0x9341;

	tft.begin(identifier);
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
