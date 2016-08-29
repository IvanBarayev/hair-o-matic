#ifndef TouchUIController_INCLUDED
#define TouchUIController_INCLUDED
#include <Adafruit_GFX.h>
#include <Adafruit_TFTLCD.h>
#include <TouchScreen.h>
#include "Color.h"
#include "ProbeState.h"

class TouchUIController {
public :
	void initialize (ProbeState state);
	void initializeLcd (Adafruit_TFTLCD& tft);
	void drawHomeScreen(bool ended);
	void drawInfoBox (String title, String data, bool redraw);
	void readInput();
	void drawButtons(bool redraw);
	
	bool isRedrawNeeded;
	int currentLayoutYPos = 0;
	int buttonYStart = 0;
	ProbeState state;
};

#endif

