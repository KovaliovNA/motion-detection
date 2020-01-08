package canny_filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class Parameters {

  private boolean enableIntermediateResults;
  @Builder.Default
  private int gaussianRadius = 7;
  @Builder.Default
  private double gaussianIntensity = 1.5;
  @Builder.Default
  private double numberDeviations = 1; //Number of standard deviations above mean for high threshold
  @Builder.Default
  private double fract = 0.04; //Low threshold is this fraction of high threshold
}
