#ifndef COLOR_INCLUDED
#define COLOR_INCLUDED

class Color
{
public :
  static int rgbTo16Bit(int r, int g, int b);	
	static const int BLACK  = 0x0000;
	static const int BLUE   = 0x001F;
	static const int RED    = 0xF800;
	static const int GREEN  = 0x07E0;
	static const int CYAN   = 0x07FF;
	static const int MAGENTA = 0xF81F;
	static const int YELLOW = 0xFFE0;
	static const int WHITE  = 0xFFFF;
};

#define MAUVE Color::rgbTo16Bit(118, 96, 137)

#endif

