package contour_finder;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import utils.ImageUtils;


@RequiredArgsConstructor
public class ContoursFinder {

  private final int maxContourPixelsCount;
  private ReentrantLock lock = new ReentrantLock();
  private final Set<Contour> contours = Collections.synchronizedSet(new HashSet<>());

  public List<Contour> findContours(BufferedImage frame) {
    Integer[][] pixels = Arrays.stream(ImageUtils.rgbImageToGrayscaleArrayOfPixels(frame))
        .map(ArrayUtils::toObject)
        .toArray(Integer[][]::new);

    IntStream.range(0, pixels.length)
        .parallel()
        .forEach(y -> processLine(pixels, y));

    return contours.stream()
        .filter(contour -> contour.getDetectedObjectEdge().size() >= maxContourPixelsCount)
        .collect(Collectors.toList());
  }

  private void processLine(Integer[][] pixels, int y) {
    for (int x = 0; x < pixels[0].length; x++) {
      int currentPixel = pixels[y][x];
      Pair<Integer, Integer> currentPixelCoord = Pair.of(x, y);

      if (currentPixel == 0) {
        continue;
      }

      lock.lock();
      Set<Contour> neighborhoods = getNeighborhoods(currentPixelCoord, pixels);

      if (neighborhoods.size() > 1) {
        List<Pair<Integer, Integer>> mergedContours = neighborhoods.stream()
            .map(Contour::getDetectedObjectEdge)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        mergedContours.add(currentPixelCoord);

        contours.removeAll(neighborhoods);
        contours.add(Contour.builder().detectedObjectEdge(mergedContours).build());
        lock.unlock();
        continue;
      }

      if (neighborhoods.size() == 1) {

        neighborhoods.iterator().next().getDetectedObjectEdge().add(currentPixelCoord);
        lock.unlock();
        continue;
      }

      Contour contour = Contour.builder().build();
      contour.getDetectedObjectEdge().add(currentPixelCoord);
      contours.add(contour);
      lock.unlock();
    }
  }

  private Set<Contour> getNeighborhoods(
      Pair<Integer, Integer> currentPixelCoord,
      Integer[][] pixels) {

    return Direction.VALUES.stream()
        .filter(direction -> direction.getCheckNextPixelExistence().apply(currentPixelCoord, pixels))
        .map(direction -> getContourByPixelCoord(direction.getNextPixel().apply(currentPixelCoord)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
  }

  private Optional<Contour> getContourByPixelCoord(Pair<Integer, Integer> contourPixel) {

    return contours.stream()
        .filter(contour -> isPixelCoordInContour(contour, contourPixel))
        .findFirst();
  }

  private boolean isPixelCoordInContour(Contour contour, Pair<Integer, Integer> contourPixel) {

    return contour.getDetectedObjectEdge().stream()
        .anyMatch(pixel -> pixel.equals(contourPixel));
  }
}
