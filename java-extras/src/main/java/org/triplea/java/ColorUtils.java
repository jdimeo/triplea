package org.triplea.java;

import java.awt.Color;
import java.util.Random;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ColorUtils {
  /**
   * Returns a color parsed from the provided input hex string.
   *
   * @param colorString EG: 00FF00, FF00FF, 000000
   */
  public Color fromHexString(final String colorString) {
    try {
      return Color.decode("#" + colorString);
    } catch (final NumberFormatException nfe) {
      throw new IllegalArgumentException(
          "Colors must be 6 digit hex numbers (without the preceding hash), eg FF0011, not: "
              + colorString
              + ", "
              + nfe.getMessage(),
          nfe);
    }
  }

  /** Returns a randomly generated color using a fixed random seed. */
  public Color randomColor(final long randomSeed) {
    final Random random = new Random(randomSeed);
    return Color.getHSBColor(random.nextFloat(), random.nextFloat(), 0.5f);
  }
}
