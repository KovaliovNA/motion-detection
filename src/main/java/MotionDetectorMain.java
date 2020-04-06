import static org.opencv.videoio.Videoio.CAP_PROP_FOURCC;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

import canny_filter.CannyEdgeDetector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import contour_finder.Contour;
import contour_finder.ContoursFinder;
import contour_finder.RectangleForCountursDrowingTool;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import process_bar.ProcessBar;
import utils.ImageUtils;

@Slf4j
public class MotionDetectorMain {

  private static final Size MAX_VIDEO_SIZE = new Size(800, 600);

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

    int width = (int) capture.get(CAP_PROP_FRAME_WIDTH);
    int height = (int) capture.get(CAP_PROP_FRAME_HEIGHT);

    Size videoSize = new Size(width, height);

    if (videoSize.width > MAX_VIDEO_SIZE.width || videoSize.height > MAX_VIDEO_SIZE.height) {
      videoSize = MAX_VIDEO_SIZE;
    }

    VideoWriter writer = new VideoWriter("result.mp4", codec, fps, videoSize);

    int totalFrames = (int) capture.get(CAP_PROP_FRAME_COUNT);
    ProcessBar processBar = new ProcessBar(totalFrames, "Motion detection progress");

    LocalDateTime start = LocalDateTime.now();
    BufferedImage firstFrame = null;
    while (capture.isOpened()) {
      Mat frame = new Mat();
      if (!capture.read(frame)) {
        processBar.step();
        break;
      }
      frame = ImageUtils.resizeFrameIfNeeded(frame, videoSize);

      BufferedImage frameForEdgesDetection = ImageUtils.mat2BufferedImage(frame);
      BufferedImage detectedEdges = detector.detectEdges(frameForEdgesDetection).getDefaultResult();

      if (firstFrame == null) {
        firstFrame = detectedEdges;
      }

      BufferedImage differenceImage = ImageUtils.getImageAbsDiff(firstFrame, detectedEdges);
      Mat frameDelta = ImageUtils.bufferedImage2Mat(differenceImage);

      Mat dilate = new Mat();
      Imgproc.dilate(frameDelta, dilate, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

      BufferedImage bufferedImage = ImageUtils.mat2BufferedImage(dilate);
      ContoursFinder contoursFinder = new ContoursFinder(500);

      List<Contour> contours = contoursFinder.findContours(bufferedImage);

      RectangleForCountursDrowingTool.drawRectangleForContour(frame, contours);

      firstFrame = detectedEdges;
      writer.write(frame);
      processBar.step();
    }

    log.info("Video saved in: {}", new File(".").getAbsolutePath());
    log.info("Processing time in minutes: {}", ChronoUnit.MINUTES.between(start, LocalDateTime.now()));

    writer.release();
  }
}
