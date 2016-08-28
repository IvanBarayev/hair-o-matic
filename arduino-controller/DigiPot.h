#ifndef DIGIPOT_INCLUDED
#define DIGIPOT_INCLUDED

class DigiPot
{
private :
	int outputPin;
public :
	DigiPot (int ioPin);
	void write (word outputValue);
	void writeVolts (double volts);
};

#endif

