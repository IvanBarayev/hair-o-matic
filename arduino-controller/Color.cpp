#include <Arduino.h>
#include "Color.h"

int Color::rgbTo16Bit(int r, int g, int b)
{

  int red5Bit = (r / 256.0) * 32;
  int green5Bit = (g / 256.0) * 32;
  int blue5Bit = (b / 256.0) * 32;

  int color = (red5Bit & 0xff) << 11;
  color |= (green5Bit & 0xff) << 5;
  color |= (blue5Bit & 0xff) << 0;

  return color;
}

