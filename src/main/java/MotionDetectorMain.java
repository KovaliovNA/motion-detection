import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.videoio.Videoio.CAP_PROP_FOURCC;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;

import canny_filter.CannyEdgeDetector;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import process_bar.ProcessBar;

@Slf4j
public class MotionDetectorMain {

  private static final Size VIDEO_SIZE = new Size(800, 600);

  private static CannyEdgeDetector detector = new CannyEdgeDetector();

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    String videoPath = args[0];
    VideoCapture capture = new VideoCapture(videoPath);

    double fps = capture.get(CAP_PROP_FPS);
    int codec = (int) capture.get(CAP_PROP_FOURCC);
    if (fps > 30) {
      fps = 30;
    }
    VideoWriter writer = new VideoWriter("test-result.mp4", codec, fps, VIDEO_SIZE);

    int totalFrames = (int) capture.get(CAP_PROP_FRAME_COUNT);
    ProcessBar processBar = new ProcessBar(totalFrames, "Motion detection progress");

    BufferedImage firstFrame = null;
    while (capture.isOpened()) {
      Mat frame = new Mat();
      if (!capture.read(frame)) {
        processBar.step();
        break;
      }
      frame = resizeFrameIfNeeded(frame);

      BufferedImage frameForEdgesDetection = mat2BufferedImage(frame);
      BufferedImage detectedEdges = detector.detectEdges(frameForEdgesDetection).getDefaultResult();

      if (firstFrame == null) {
        firstFrame = detectedEdges;
      }

      BufferedImage differenceImage = getDifferenceImage(firstFrame, detectedEdges);
      Mat frameDelta = bufferedImage2Mat(differenceImage);

      Mat dilate = new Mat();
      Imgproc.dilate(frameDelta, dilate, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));

      List<MatOfPoint> contours = new ArrayList<>();
      Imgproc.findContours(
          dilate.clone(),
          contours,
          new Mat(),
          Imgproc.RETR_EXTERNAL,
          Imgproc.CHAIN_APPROX_SIMPLE
      );

      Scalar green = new Scalar(0, 255, 0);
      for (MatOfPoint contour : contours) {
        if (Imgproc.contourArea(contour) < 500) {
          continue;
        }

        Rect rect = Imgproc.boundingRect(contour);
        Point xAndY = new Point(new double[]{rect.x, rect.y});
        Point xPlusWAndYPlusH = new Point(new double[]{rect.x + rect.width, rect.y + rect.height});
        Imgproc.rectangle(frame, xAndY, xPlusWAndYPlusH, green, 1);
      }

      firstFrame = detectedEdges;
      writer.write(frame);
      processBar.step();
    }

    log.info("Video saved in: {}", new File(".").getAbsolutePath());

    writer.release();
  }

  private static Mat resizeFrameIfNeeded(Mat frame) {
    Size currentFrameSize = frame.size();

    if (currentFrameSize.width > VIDEO_SIZE.width || currentFrameSize.height > VIDEO_SIZE.height) {
      Mat resizedFrame = new Mat();
      Imgproc.resize(frame, resizedFrame, VIDEO_SIZE, 0, 0, INTER_AREA);

      return resizedFrame;
    }

    return frame;
  }

  //rewrite for binary image case
  public static BufferedImage getDifferenceImage(BufferedImage img1, BufferedImage img2) {
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
  private static Mat bufferedImage2Mat(BufferedImage in) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(in, "png", byteArrayOutputStream);
    byteArrayOutputStream.flush();

    return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), CV_8UC1);
  }

  @SneakyThrows
  private static BufferedImage mat2BufferedImage(Mat matrix) {
    MatOfByte mob = new MatOfByte();
    Imgcodecs.imencode(".png", matrix, mob);
    byte[] ba = mob.toArray();

    return ImageIO.read(new ByteArrayInputStream(ba));
  }
}
