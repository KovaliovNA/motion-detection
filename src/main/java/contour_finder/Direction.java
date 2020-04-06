package contour_finder;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@RequiredArgsConstructor
public enum Direction {

  E((pixel, pixels) ->
      pixel.getKey() < pixels[0].length - 1
          && pixels[pixel.getValue()][pixel.getKey() + 1] != 0,
      (pixel) -> Pair.of(pixel.getKey() + 1, pixel.getValue())),
  ES((pixel, pixels) ->
      pixel.getKey() < pixels[0].length - 1
          && pixel.getValue() < pixels.length - 1
          && pixels[pixel.getValue() + 1][pixel.getKey() + 1] != 0,
      (pixel) -> Pair.of(pixel.getKey() + 1, pixel.getValue() + 1)),
  S((pixel, pixels) ->
      pixel.getValue() < pixels.length - 1
          && pixels[pixel.getValue() + 1][pixel.getKey()] != 0,
      (pixel) -> Pair.of(pixel.getKey(), pixel.getValue() + 1)),
  SW((pixel, pixels) ->
      pixel.getKey() > 0
          && pixel.getValue() < pixels.length - 1
          && pixels[pixel.getValue() + 1][pixel.getKey() - 1] != 0,
      (pixel) -> Pair.of(pixel.getKey() - 1, pixel.getValue() + 1)),
  W((pixel, pixels) ->
      pixel.getKey() > 0
          && pixels[pixel.getValue()][pixel.getKey() - 1] != 0,
      (pixel) -> Pair.of(pixel.getKey() - 1, pixel.getValue())),
  NW((pixel, pixels) ->
      pixel.getKey() > 0
          && pixel.getValue() > 0
          && pixels[pixel.getValue() - 1][pixel.getKey() - 1] != 0,
      (pixel) -> Pair.of(pixel.getKey() - 1, pixel.getValue() - 1)),
  N((pixel, pixels) ->
      pixel.getValue() > 0
          && pixels[pixel.getValue() - 1][pixel.getKey()] != 0,
      (pixel) -> Pair.of(pixel.getKey(), pixel.getValue() - 1)),
  NE((pixel, pixels) ->
      pixel.getKey() < pixels[0].length - 1
          && pixel.getValue() > 0
          && pixels[pixel.getValue() - 1][pixel.getKey() + 1] != 0,
      (pixel) -> Pair.of(pixel.getKey() + 1, pixel.getValue() - 1));

  private final BiFunction<Pair<Integer, Integer>, Integer[][], Boolean> checkNextPixelExistence;
  private final Function<Pair<Integer, Integer>, Pair<Integer, Integer>> nextPixel;

  public static final List<Direction> VALUES = Arrays.asList(Direction.values());
}
