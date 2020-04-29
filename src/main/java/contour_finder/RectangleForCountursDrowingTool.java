package contour_finder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class RectangleForCountursDrowingTool {

  public static void drawRectangleForContour(Mat frame, List<Contour> contours) {
    Scalar green = new Scalar(0, 255, 0);

    List<Pair<Point, Point>> rectanglesPoints = new ArrayList<>();

    for (Contour contour : contours) {
      List<Pair<Integer, Integer>> detectedObjectEdge = contour.getDetectedObjectEdge();

      Pair<Integer, Integer> maxXCoord = detectedObjectEdge.stream()
          .max(Entry.comparingByKey())
          .orElse(null);

      Pair<Integer, Integer> maxYCoord = detectedObjectEdge.stream()
          .max(Entry.comparingByValue())
          .orElse(null);

      Pair<Integer, Integer> minXCoord = detectedObjectEdge.stream()
          .min(Entry.comparingByKey())
          .orElse(null);

      Pair<Integer, Integer> minYCoord = detectedObjectEdge.stream()
          .min(Entry.comparingByValue())
          .orElse(null);


      Point rectangleLeftPoint = new Point(new double[]{minXCoord.getKey(), minYCoord.getValue()});
      Point rectangleRightPoint =
          new Point(new double[]{minXCoord.getKey() + (maxXCoord.getKey() - minXCoord.getKey()), minYCoord.getValue() + (maxYCoord.getValue() - minYCoord.getValue())});
      Imgproc.rectangle(frame, rectangleLeftPoint, rectangleRightPoint, green, 1);
    }

//    for (Pair<Point, Point> rectanglesPoint : rectanglesPoints) {
//      Point left = rectanglesPoint.getLeft();
//      Point right = rectanglesPoint.getRight();
//
//
//    }
  }
}
