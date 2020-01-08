package canny_filter;

import java.awt.image.BufferedImage;

public class Gaussian {

  private static final double SQRT2PI = Math.sqrt(2 * Math.PI);
  private static final int CONS_255 = 0xff;

  public int[][] blurImage(BufferedImage img, int radius, double intensity) {
    int[][] gs = rgbImageToGrayscaleArrayOfPixels(img);

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

  /**
   * Send this method a BufferedImage to get a grayscale array (int, value 0-255.
   *
   * @param img BufferedImage, the input image from which to extract grayscale
   * @return gs   int[][] array of grayscale pixel values from image.
   */
  private int[][] rgbImageToGrayscaleArrayOfPixels(BufferedImage img) {
    int[][] gs;
    int height = img.getHeight();
    int width = img.getWidth();

    gs = new int[height][width];

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        int pixel = img.getRGB(j, i);

        int r = (pixel >> 16) & CONS_255;
        int g = (pixel >> 8) & CONS_255;
        int b = pixel & CONS_255;

        long avg = Math.round((r + g + b) / 3.0);
        gs[i][j] = (int) avg;
      }
    }

    return gs;
  }
}
