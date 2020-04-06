package canny_filter;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
public class ResultsContainer {

  private boolean intermediateResultsEnabled;
  @Getter
  @Builder.Default
  private List<BufferedImage> results = new LinkedList<>();

  @Getter
  private BufferedImage defaultResult;

  public void saveIntermediateResult(int[][] img) {
    if (intermediateResultsEnabled) {
      results.add(grayscaleArrayOfPixelsToImage(img));
    }
  }

  public void saveDefaultValue(int[][] img) {
    BufferedImage bufferedImage = grayscaleArrayOfPixelsToImage(img);
    if (!intermediateResultsEnabled) {
      results.add(bufferedImage);
    }

    defaultResult = bufferedImage;
  }

  public void storeMagnitude(double[][] mag) {
    if (intermediateResultsEnabled) {
      results.add(grayscaleArrayOfPixelsToImage(mag));
    }
  }

  private static BufferedImage grayscaleArrayOfPixelsToImage(int[][] raw) {
    BufferedImage img = null;
    int height = raw.length;
    int width = raw[0].length;

    img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        img.setRGB(j, i, (raw[i][j] << 16) | (raw[i][j] << 8) | (raw[i][j]));
      }
    }

    return img;
  }

  private static BufferedImage grayscaleArrayOfPixelsToImage(double[][] raw) {
    BufferedImage img = null;
    int height = raw.length;
    int width = raw[0].length;

    img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        img.setRGB(j, i, (int) raw[i][j] << 16 | (int) raw[i][j] << 8 | (int) raw[i][j]);
      }
    }

    return img;
  }
}
