package contour_finder;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

@Builder
public class Contour {

  @Getter
  @Builder.Default
  private List<Pair<Integer, Integer>> detectedObjectEdge = new ArrayList<>();
}
