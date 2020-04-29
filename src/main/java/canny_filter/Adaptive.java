package canny_filter;

public class Adaptive {

  public int[][] blurImage(int[][] gs) {

    double h = 0.5;

    return adaptiveBlur(gs, h);
  }

  private int[][] adaptiveBlur(int[][] gs, double h) {
    int height = gs.length;
    int width = gs[0].length;

    int[][] result = new int[height][width];

    Sobel sobel = new Sobel();

    for (int y = 2; y < height - 4; y++) {
      for (int x = 2; x < width - 4; x++) {
        double N = 0;
        double sum = 0;

        for (int j = -1; j < 2; j++) {
          for (int i = -1; i < 2; i++) {
            int gx = sobel.getGradientInPoint(gs, Sobel.MASK_H, y + j, x + i);
            int gy = sobel.getGradientInPoint(gs, Sobel.MASK_V, y + j, x + i);

            double d = Math.sqrt(gx * gx + gy * gy);
            double w = Math.exp(-(Math.sqrt(d) / (2 * h * h)));

            N += w;

            sum += gs[y + j][x + i] * w;
          }
        }

        result[y][x] = (int) Math.floor(sum / N);
      }
    }

    return result;
  }

  private int findN(int[][] d, double h) {
    int height = d.length;
    int width = d[0].length;
    int N = 0;

    for (int i = 0; i < height - 1; i++) {
      for (int j = 0; j < width - 1; j++) {
        N += Math.exp(-((Math.sqrt(d[i + 1][j + 1])) / (2 * h * h)));
      }
    }

    return N;
  }

  private int[][] calculateWight(int[][] gx, int[][] gy) {
    int height = gx.length;
    int width = gx[0].length;

    int[][] d = new int[height][width];

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        d[i][j] = (int) Math.sqrt(gx[i][j] * gx[i][j] + gy[i][j] * gy[i][j]);
      }
    }

    return d;
  }

}
