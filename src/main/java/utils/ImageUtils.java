package utils;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.INTER_AREA;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
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

  private final int CONS_255 = 0xff;
  private final int RGB_WHITE = (CONS_255 << 16) | (CONS_255 << 8) | CONS_255;

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

  public BufferedImage getImageAbsDiff(LinkedList<BufferedImage> prevFrames) {
    int width = prevFrames.get(0).getWidth();
    int height = prevFrames.get(0).getHeight();
    BufferedImage outImg =
        new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {

        int rgb1 = prevFrames.getFirst().getRGB(j, i);
        int rgb4 = prevFrames.getLast().getRGB(j, i);

        int v1 = (rgb1) & 0xff;
        int v2 = (rgb4) & 0xff;

        int diff = v1 == v2 ? 0 : 255;

        outImg.setRGB(j, i, (diff << 16) | (diff << 8) | diff);
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
