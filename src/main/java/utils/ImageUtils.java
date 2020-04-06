package utils;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.INTER_AREA;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

@UtilityClass
public class ImageUtils {

  private static final int CONS_255 = 0xff;

  /**
   * Send this method a BufferedImage to get a grayscale array (int, value 0-255.
   *
   * @param img BufferedImage, the input image from which to extract grayscale
   * @return gs   int[][] array of grayscale pixel values from image.
   */
  public int[][] rgbImageToGrayscaleArrayOfPixels(BufferedImage img) {
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

  public Mat resizeFrameIfNeeded(Mat frame, Size neededFrameSize) {
    Size currentFrameSize = frame.size();

    if (currentFrameSize.width > neededFrameSize.width || currentFrameSize.height > neededFrameSize.height) {
      Mat resizedFrame = new Mat();
      Imgproc.resize(frame, resizedFrame, neededFrameSize, 0, 0, INTER_AREA);

      return resizedFrame;
    }

    return frame;
  }

  //rewrite for binary image case
  public BufferedImage getImageAbsDiff(BufferedImage img1, BufferedImage img2) {
    int width1 = img1.getWidth(); // Change - getWidth() and getHeight() for BufferedImage
    int width2 = img2.getWidth(); // take no arguments
    int height1 = img1.getHeight();
    int height2 = img2.getHeight();
    if ((width1 != width2) || (height1 != height2)) {
      System.err.println("Error: Images dimensions mismatch");
      System.exit(1);
    }

    // NEW - Create output Buffered image of type RGB
    BufferedImage outImg = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_RGB);

    // Modified - Changed to int as pixels are ints
    int diff;
    int result; // Stores output pixel
    for (int i = 0; i < height1; i++) {
      for (int j = 0; j < width1; j++) {
        int rgb1 = img1.getRGB(j, i);
        int rgb2 = img2.getRGB(j, i);
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = (rgb1) & 0xff;
        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = (rgb2) & 0xff;
        diff = Math.abs(r1 - r2); // Change
        diff += Math.abs(g1 - g2);
        diff += Math.abs(b1 - b2);
        diff /= 3; // Change - Ensure result is between 0 - 255
        // Make the difference image gray scale
        // The RGB components are all the same
        result = (diff << 16) | (diff << 8) | diff;
        outImg.setRGB(j, i, result); // Set result
      }
    }

    // Now return
    return outImg;
  }

  @SneakyThrows
  public Mat bufferedImage2Mat(BufferedImage in) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(in, "png", byteArrayOutputStream);
    byteArrayOutputStream.flush();

    return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), CV_8UC1);
  }

  @SneakyThrows
  public BufferedImage mat2BufferedImage(Mat matrix) {
    MatOfByte mob = new MatOfByte();
    Imgcodecs.imencode(".png", matrix, mob);
    byte[] ba = mob.toArray();

    return ImageIO.read(new ByteArrayInputStream(ba));
  }
}
