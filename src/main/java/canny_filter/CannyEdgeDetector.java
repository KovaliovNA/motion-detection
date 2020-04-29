package canny_filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import utils.ImageUtils;

@Slf4j
public class CannyEdgeDetector {

  private final static double PI_RAD = 180 / Math.PI;

  private static final int STRONG_PIXEL = 255;
  private static final int WEAK_PIXEL = 25;
  private static final int NON_RELEVANT_PIXEL = 0;

  @Setter
  private Parameters parameters = Parameters.builder().build();

  private int stDev;       //Standard deviation in magnitude of image's pixels
  private int mean;        //Mean of magnitude in image's pixels

  public ResultsContainer detectEdges(BufferedImage source) {
    ResultsContainer resultsContainer = ResultsContainer
        .builder()
        .intermediateResultsEnabled(parameters.isEnableIntermediateResults())
        .build();

    Gaussian gaussian = new Gaussian();
    int[][] blurredImage = gaussian.blurImage(source,
        parameters.getGaussianRadius(), parameters.getGaussianIntensity());

//    Adaptive adaptive = new Adaptive();
//    int[][] gs = ImageUtils.rgbImageToGrayscaleArrayOfPixels(source);
//    int[][] blurredImage = adaptive.blurImage(gs);

    resultsContainer.saveIntermediateResult(blurredImage);

    Sobel sobel = new Sobel();
    int[][] gX = sobel.process(blurredImage, Sobel.MASK_H);
    int[][] gY = sobel.process(blurredImage, Sobel.MASK_V);

    resultsContainer.saveIntermediateResult(gX);
    resultsContainer.saveIntermediateResult(gY);

    double[][] magnitude = nonMaximumSuppression(gX, gY);

    resultsContainer.storeMagnitude(magnitude);

    int[][] threshold = threshold(magnitude);

    resultsContainer.saveIntermediateResult(threshold);

    int[][] result = hysteresis(threshold);

    resultsContainer.saveIntermediateResult(result);
    resultsContainer.saveDefaultValue(result);

    return resultsContainer;
  }


  private double[][] nonMaximumSuppression(int[][] gX, int[][] gY) {
    int height = gX.length;
    int width = gX[0].length;
    double pixelTotal = height * width;
    double[][] mag = new double[height][width];

    double sum = 0;
    double deviation = 0;

    for (int x = 1; x < height - 1; x++) {
      for (int y = 1; y < width - 1; y++) {
        double gradMag = getMagnitude(gX[x][y], gY[x][y]);

        switch (getDirection(gX[x][y], gY[x][y])) {
          case 0:
            double magS = getMagnitude(gX[x][y - 1], gY[x][y - 1]);
            double magN = getMagnitude(gX[x][y + 1], gY[x][y + 1]);
            mag[x - 1][y - 1] = defineMagnitude(() -> gradMag < magS && gradMag < magN, gradMag);
            break;
          case 45:
            double magNE = getMagnitude(gX[x - 1][y + 1], gY[x - 1][y + 1]);
            double magSW = getMagnitude(gX[x + 1][y - 1], gY[x + 1][y - 1]);
            mag[x - 1][y - 1] = defineMagnitude(() -> gradMag < magNE && gradMag < magSW, gradMag);
            break;
          case 90:
            double magE = getMagnitude(gX[x - 1][y], gY[x - 1][y]);
            double magW = getMagnitude(gX[x + 1][y - 1], gY[x + 1][y - 1]);
            mag[x - 1][y - 1] = defineMagnitude(() -> gradMag < magE && gradMag < magW, gradMag);
            break;
          case 135:
            double magSE = getMagnitude(gX[x - 1][y - 1], gY[x - 1][y - 1]);
            double magNW = getMagnitude(gX[x + 1][y + 1], gY[x + 1][y + 1]);
            mag[x - 1][y - 1] = defineMagnitude(() -> gradMag < magSE && gradMag < magNW, gradMag);
            break;
        }

        sum += mag[x - 1][y - 1];
      }
    }

    mean = (int) Math.round(sum / pixelTotal);

    //Get variance
    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        double diff = mag[r][c] - mean;

        deviation += (diff * diff);
      }
    }

    stDev = (int) Math.sqrt(deviation / pixelTotal);

    return mag;
  }

  private double defineMagnitude(Supplier<Boolean> gradCheck, double gradMag) {
    return gradCheck.get() ? 0 : gradMag;
  }

  private double getMagnitude(int x, int y) {
    return Math.hypot(x, y);
  }

  private int getDirection(int x, int y) {
    double angle = Math.atan2(y, x) * PI_RAD;    //Convert radians to degrees

    //Check for negative angles
    if (angle < 0) {
      angle += 360.;
    }

    //Each pixels ACTUAL angle is examined and placed in 1 of four groups (for the four searched 45-degree neighbors)
    //Reorder this for optimization
    if (angle <= 22.5 || (angle >= 157.5 && angle <= 202.5) || angle >= 337.5) {
      return 0;      //Check left and right neighbors
    } else if ((angle >= 22.5 && angle <= 67.5) || (angle >= 202.5 && angle <= 247.5)) {
      return 45;     //Check diagonal (upper right and lower left) neighbors
    } else if ((angle >= 67.5 && angle <= 112.5) || (angle >= 247.5 && angle <= 292.5)) {
      return 90;     //Check top and bottom neighbors
    } else {
      return 135;    //Check diagonal (upper left and lower right) neighbors
    }
  }

  private int[][] threshold(double[][] mag) {
    int height = mag.length - 1;
    int width = mag[0].length - 1;
    int[][] threshold = new int[height - 1][width - 1];

    double highThreshold = mean + (parameters.getNumberDeviations() * stDev);
    double lowThreshold = highThreshold * parameters.getFract();

    for (int i = 1; i < height; i++) {
      for (int j = 1; j < width; j++) {
        double magnitude = mag[i][j];

        if (magnitude >= highThreshold) {
          threshold[i - 1][j - 1] = STRONG_PIXEL;
        } else if (magnitude < lowThreshold) {
          threshold[i - 1][j - 1] = NON_RELEVANT_PIXEL;
        } else {    //This could be separate method or lambda
          boolean connected = false;

          for (int i1 = -1; i1 < 2; i1++) {
            for (int j1 = -1; j1 < 2; j1++) {
              if (mag[i + i1][j + j1] >= highThreshold) {
                connected = true;
                break;
              }
            }
          }

          threshold[i - 1][j - 1] = connected ? WEAK_PIXEL : NON_RELEVANT_PIXEL;
        }
      }
    }

    return threshold;
  }

  private int[][] hysteresis(int[][] img) {
    int height = img.length - 1;
    int width = img[0].length - 1;

    for (int i = 1; i < height - 1; i++) {
      for (int j = 1; j < width - 1; j++) {
        if (img[i][j] == WEAK_PIXEL) {
          boolean n = img[i + 1][j] == STRONG_PIXEL;
          boolean nw = img[i + 1][j + 1] == STRONG_PIXEL;
          boolean w = img[i][j + 1] == STRONG_PIXEL;
          boolean sw = img[i + 1][j - 1] == STRONG_PIXEL;
          boolean s = img[i][j - 1] == STRONG_PIXEL;
          boolean se = img[i - 1][j - 1] == STRONG_PIXEL;
          boolean e = img[i][j - 1] == STRONG_PIXEL;
          boolean ne = img[i - 1][j + 1] == STRONG_PIXEL;

          if (n || nw || w || sw || s || se || e || ne) {
            img[i][j] = STRONG_PIXEL;
          } else {
            img[i][j] = NON_RELEVANT_PIXEL;
          }
        }
      }
    }

    return img;
  }

  /**
   * Entry point for canny filter.
   *
   * @param args must contains next data in next order: /path/to/file/without/file/name/ imageNameWithoutExtension
   *             .imageExtension
   */
  public static void main(String[] args) {
    CannyEdgeDetector detector = new CannyEdgeDetector();
    detector.setParameters(Parameters.builder()
        .enableIntermediateResults(true)
        .gaussianRadius(7)
        .gaussianIntensity(2)
        .numberDeviations(1)
        .fract(0.1)
        .build());

    String path = args[0];
    String imageName = args[1];
    String fileExt = args[2];
    String originalImagePath = path + imageName + "." + fileExt;
    String detectedEdgesPath = path + imageName + "-result";

    try {
      BufferedImage img = ImageIO.read(new File(originalImagePath));
      ResultsContainer detectedEdges = detector.detectEdges(img);
      List<BufferedImage> results = detectedEdges.getResults();

      for (int i = 0; i < results.size(); i++) {
        ImageIO.write(results.get(i), fileExt, new File(detectedEdgesPath + i + "." + fileExt));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
