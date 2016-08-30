#ifndef TouchUIController_INCLUDED
#define TouchUIController_INCLUDED
#include <Adafruit_GFX.h>
#include <Adafruit_TFTLCD.h>
#include <TouchScreen.h>
#include "Color.h"
#include "ProbeState.h"
#include "IUIController.h"

class TouchUIController : public IUIController {
public :
	TouchUIController(ProbeState* state);
	void initialize ();
	void update(double R2, double targetVoltage, bool ended);
	void readInput();

protected :
	void initializeLcd (Adafruit_TFTLCD& tft);
	void drawHomeScreen(bool ended);
	void drawInfoBox (String title, String data, bool redraw);
	void drawButtons(bool redraw);

	int currentLayoutYPos = 0;
	int buttonYStart = 0;
};

#endif

