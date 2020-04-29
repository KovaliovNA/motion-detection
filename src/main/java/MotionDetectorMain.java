import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.videoio.Videoio.CAP_PROP_FOURCC;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

import canny_filter.CannyEdgeDetector;
import contour_finder.Contour;
import contour_finder.ContoursFinder;
import contour_finder.RectangleForCountursDrowingTool;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import process_bar.ProcessBar;
import utils.ImageUtils;

@Slf4j
public class MotionDetectorMain {

  private static final int DIFF_FRAMES_COUNT = 4;

  private static final Size MAX_VIDEO_SIZE = new Size(800, 600);

  private static CannyEdgeDetector detector = new CannyEdgeDetector();

  public static final String TEST_PATH = "C:\\Users\\Nikita_Kovalev1\\IdeaProjects\\motion-detector\\src\\main\\resources\\test\\";
  public static final String ORIGINAL_FRAME_PATH = "C:\\Users\\Nikita_Kovalev1\\IdeaProjects\\motion-detector\\src\\main\\resources\\original_frames\\";
  public static final String PROCESSING_RESULT_PATH = "C:\\Users\\Nikita_Kovalev1\\IdeaProjects\\motion-detector\\src\\main\\resources\\execution_result\\";

  @SneakyThrows
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
    LinkedList<BufferedImage> previousFrames = new LinkedList<>();
    while (capture.isOpened()) {
      Mat frame = new Mat();
      if (!capture.read(frame)) {
        processBar.step();
        break;
      }
      frame = ImageUtils.resizeFrameIfNeeded(frame, videoSize);

      BufferedImage frameForEdgesDetection = ImageUtils.mat2BufferedImage(frame);
      BufferedImage detectedEdges = detector.detectEdges(frameForEdgesDetection).getDefaultResult();

      previousFrames.add(detectedEdges);
      if (previousFrames.size() < DIFF_FRAMES_COUNT) {
        writer.write(frame);
        processBar.step();
        continue;
      }

      BufferedImage differenceImage = ImageUtils.getImageAbsDiff(previousFrames);
      Mat frameDelta = ImageUtils.bufferedImage2Mat(differenceImage);

      Mat morphingMatrix = new Mat(new Size(2, 2), CV_8UC1, new Scalar(255));
      Mat clearMat = new Mat();
      Imgproc.morphologyEx(frameDelta, clearMat, Imgproc.MORPH_OPEN, morphingMatrix);
      Mat dilate = new Mat();
      Imgproc.dilate(clearMat, dilate, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

      BufferedImage bufferedImage = ImageUtils.mat2BufferedImage(dilate);
//      int i = processBar.getRemain();
//      ImageIO.write(frameForEdgesDetection, "png", new File(PROCESSING_RESULT_PATH + "_" + i + ".png"));
//      previousFrames.forEach(img ->
//      {
//        try {
//          ImageIO.write(detectedEdges, "png", new File(PROCESSING_RESULT_PATH + "_detection_" + i + "_" + previousFrames.indexOf(img) + ".png"));
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      });
//      ImageIO.write(detectedEdges, "png", new File(PROCESSING_RESULT_PATH + "_detection_" + i + ".png"));
//      ImageIO.write(differenceImage, "png", new File(PROCESSING_RESULT_PATH + "_difference_" + i + ".png"));
      ContoursFinder contoursFinder = new ContoursFinder(500);

      List<Contour> contours = contoursFinder.findContours(bufferedImage);

      RectangleForCountursDrowingTool.drawRectangleForContour(frame, contours);

//      int i = processBar.getRemain();
//      ImageIO.write(detectedEdges, "png", new File(PROCESSING_RESULT_PATH + "_detection_" + i + ".png"));
//      ImageIO.write(differenceImage, "png", new File(PROCESSING_RESULT_PATH + "_difference_" + i + ".png"));
//      ImageIO.write(ImageUtils.mat2BufferedImage(clearMat), "png", new File(PROCESSING_RESULT_PATH + "_opening_" + i + ".png"));
//      ImageIO.write(ImageUtils.mat2BufferedImage(dilate), "png", new File(PROCESSING_RESULT_PATH + "_dilate_" + i + ".png"));
//      ImageIO.write(ImageUtils.mat2BufferedImage(frame), "png", new File(PROCESSING_RESULT_PATH + "final_" + i + ".png"));

      writer.write(frame);
      processBar.step();
    }

    log.info("Video saved in: {}", new File(".").getAbsolutePath());
    log.info("Processing time in minutes: {}", ChronoUnit.MINUTES.between(start, LocalDateTime.now()));

    writer.release();
  }
}
