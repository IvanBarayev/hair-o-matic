#ifndef IUIController_INCLUDED
#define IUIController_INCLUDED

class IUIController {
protected:
	ProbeState* state;
public:
	virtual ~IUIController() {}

	virtual void initialize () = 0;
	virtual void readInput() = 0;
	virtual void update(double R2, double targetVoltage, bool ended) = 0;
};

#endif