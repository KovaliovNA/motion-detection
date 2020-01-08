import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

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

  private static CannyEdgeDetector detector = new CannyEdgeDetector();

  public static void main(String[] args) {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    String videoPath = args[0];

    VideoCapture capture = new VideoCapture(videoPath);
    capture.set(CAP_PROP_FRAME_WIDTH, 800);
    capture.set(CAP_PROP_FRAME_HEIGHT, 600);

    Size videoSize = new Size(capture.get(CAP_PROP_FRAME_WIDTH), capture.get(CAP_PROP_FRAME_HEIGHT));
    int codec = VideoWriter.fourcc('D', 'I', 'V', 'X');
    VideoWriter writer = new VideoWriter("test-result.mp4", codec, 30, videoSize);

    int totalFrames = (int) capture.get(CAP_PROP_FRAME_COUNT);

    ProcessBar processBar = new ProcessBar(totalFrames, "Motion detection progress");

    Mat firstFrame = null;

    while (capture.isOpened()) {
      Mat frame = new Mat();
      if (!capture.read(frame)) {
        processBar.step();
        break;
      }

      BufferedImage frameForEdgesDetection = mat2BufferedImage(frame);
      BufferedImage defaultResult = detector.detectEdges(frameForEdgesDetection).getDefaultResult();
      Mat detectedEdgesMat = bufferedImage2Mat(defaultResult);

      if (firstFrame == null) {
        firstFrame = detectedEdgesMat;
      }

      Mat frameDelta = new Mat();
      Core.absdiff(firstFrame, detectedEdgesMat, frameDelta);

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
        if (Imgproc.contourArea(contour) < 700) {
          continue;
        }

        Rect rect = Imgproc.boundingRect(contour);
        Point xAndY = new Point(new double[]{rect.x, rect.y});
        Point xPlusWAndYPlusH = new Point(new double[]{rect.x + rect.width, rect.y + rect.height});
        Imgproc.rectangle(frame, xAndY, xPlusWAndYPlusH, green, 1);
      }

      firstFrame = detectedEdgesMat;
      writer.write(frame);
      processBar.step();
    }

    log.info("Video saved in: {}", new File(".").getAbsolutePath());

    writer.release();
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
