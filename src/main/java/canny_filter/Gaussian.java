package canny_filter;

import java.awt.image.BufferedImage;
import utils.ImageUtils;

public class Gaussian {

  private static final double SQRT2PI = Math.sqrt(2 * Math.PI);

  public int[][] blurImage(BufferedImage img, int radius, double intensity) {
    int[][] gs = ImageUtils.rgbImageToGrayscaleArrayOfPixels(img);

    return blur(gs, radius, intensity);
  }

  /**
   * Send this method an int[][][] RGB array, an int radius, and a double intensity to blur the image with a Gaussian
   * filter of that radius and intensity.
   *
   * @param raw       int[][][], an array of RGB values to be blurred
   * @param radius    int, the radius of the Gaussian filter (filter width = 2 * r + 1)
   * @param intensity double, the intensity of the Gaussian blur
   * @return outRGB   int[][][], an array of RGB values from blurring input image with Gaussian filter
   */
  private int[][] blur(int[][] raw, int radius, double intensity) {
    int height = raw.length;
    int width = raw[0].length;
    double norm = 0.;
    //This also seems very costly, do it as little as possible
    double invIntensSqrPi = 1 / (SQRT2PI * intensity);
    double[] mask = new double[2 * radius + 1];
    int[][] outGS = new int[height - 2 * radius][width - 2 * radius];

    //Create Gaussian kernel
    double intensSquared2 = 2 * intensity * intensity;
    for (int x = 0; x < 2 * radius + 1; x++) {
      double exp = Math.exp(-((x * x) / intensSquared2));

      mask[x] = invIntensSqrPi * exp;
      norm += mask[x];
    }

    //Convolve image with kernel horizontally
    for (int r = radius; r < height - radius; r++) {
      for (int c = radius; c < width - radius; c++) {
        double sum = 0.;

        for (int mr = -radius; mr < radius + 1; mr++) {
          sum += (mask[mr + radius] * raw[r][c + mr]);
        }

        //Normalize channel after blur
        sum /= norm;
        outGS[r - radius][c - radius] = (int) Math.round(sum);
      }
    }

    //Convolve image with kernel vertically
    for (int r = radius; r < height - radius; r++) {
      for (int c = radius; c < width - radius; c++) {
        double sum = 0.;

        for (int mr = -radius; mr < radius + 1; mr++) {
          sum += (mask[mr + radius] * raw[r + mr][c]);
        }

        //Normalize channel after blur
        sum /= norm;
        outGS[r - radius][c - radius] = (int) Math.round(sum);
      }
    }

    return outGS;
  }
}
