#include <SPI.h>
#include <Adafruit_GFX.h>
#include <Adafruit_TFTLCD.h>
#include <EEPROM.h>
#include <TouchScreen.h>
#include "ProbeState.h"
#include "Color.h"
#include "DigiPot.h"
#include "TouchUIController.h"
#include "IOUtils.h"

#define DEBUG_MODE false

const int PROBE_ACTIVE_SENSOR_PIN = 15;
const int BUZZER_IO_PIN = 44;
const int DAC_OUTPUT_PIN = 22;

bool isRedrawNeeded = true;
ProbeState state;
TouchUIController uiController;

DigiPot powerPot(DAC_OUTPUT_PIN);
long lastTickTime = 0;
double lastInputVoltage = 1;

void setup(void) {
	Serial.begin(9600);
	state.initialize();
	uiController.initialize(state);
}

void loop(void) {
	int activeVoltage = IOUtils::analogReadFiltered(PROBE_ACTIVE_SENSOR_PIN);
	bool ended = false;

	if (isInsertStart(activeVoltage)) {
		state.setIsProbeActive(true);
	} else if (activeVoltage == 0) {
		if (isInsertEnd()) {
			ended = true;
			isRedrawNeeded = true;

			state.incrementKillCount();
			state.incrementLifeTimeCount();
		}

		state.setIsProbeActive(false);
	}

	if (state.getIsProbeActive() && state.getActiveTime() != lastTickTime)
		isRedrawNeeded = true;

	long time = state.getActiveTime();
	if (state.getIsProbeActive() && (time % 10 > 0 || time < 10))
		playBuzzer();
	else
		stopBuzzer();

	double R1 = 480;
	double vIn = lastInputVoltage;
	double vOut = (5.0 * activeVoltage) / 1024.0;

	double buffer = (vIn / vOut) - 1;
	double R2 = (R1 * buffer);
	double current = vIn / (R2 + R1);

	double targetCurrent = 0.000001 * state.getTargetMicroAmps();
	double targetVoltage = targetCurrent * (R2 + R1);

	if (targetVoltage > 10)
		targetVoltage = 10;

	// initial read may be off so drop back down if voltage really high so no possible initial painful zap
	if (targetVoltage > 8 && state.getPreciseActiveTime() < 1.5 && state.getTargetMicroAmps() < 900)
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
			Serial.print("target current: "); Serial.print(state.getTargetMicroAmps()); Serial.print(" uA"); Serial.println();
			Serial.print("R2: "); Serial.print(R2); Serial.println();
			Serial.print("vOut: "); Serial.print(vOut); Serial.println();
		}
	}

	uiController.readInput();

	if (isRedrawNeeded) {
		uiController.currentLayoutYPos = 0;
		uiController.drawHomeScreen(ended);

		char voltLabel[32];
		char volts[6];
		char res[6];
		dtostrf(targetVoltage, 2, 2, volts);
		dtostrf(R2, 3, 0, res);

		sprintf(voltLabel, "%s | %s", res, volts);
		uiController.drawInfoBox("R | VOut", voltLabel, true);

		uiController.drawInfoBox("Current[uA]", String(state.getTargetMicroAmps()), true);
		uiController.drawButtons(state.isFirstLoop);

		isRedrawNeeded = false;
	}

	lastTickTime = state.getActiveTime();
	state.isFirstLoop = false;
	delay(25);
}

bool isInsertStart(int activeVoltage) {
	return activeVoltage > 0 && !state.getIsProbeActive();
}

bool isInsertEnd() {
	return state.getIsProbeActive() && state.getActiveTime() > 4;
}

void playBuzzer() {
	int hz = 110;
	tone(BUZZER_IO_PIN, hz);
}

void stopBuzzer() {
	noTone(BUZZER_IO_PIN);
}
