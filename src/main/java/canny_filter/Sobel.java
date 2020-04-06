/**
 * Copyright 2016 Robert Streetman
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package canny_filter;

/**
 * This class contains methods for masking an image array with horizontal and vertical Sobel masks.
 *
 * @author robert
 */

public class Sobel {

  //The masks for each Sobel convolution
  public static final int[][] MASK_H = {
      {2, 2, 4, 2, 2},
      {1, 1, 2, 1, 1},
      {0, 0, 0, 0, 0},
      {-1, -1, -2, -1, -1},
      {-2, -2, -4, -2, -2}
  };
  public static final int[][] MASK_V = {
      {2, 1, 0, -1, -2},
      {2, 1, 0, -1, -2},
      {4, 2, 0, -2, -4},
      {2, 1, 0, -1, -2},
      {2, 1, 0, -1, -2}
  };

  /**
   * Send this method an int[][] array of grayscale pixel values to get a an image resulting
   * from the convolution of this image with the horizontal Sobel mask.
   *
   * @param raw int[][], array of grayscale pixel values 0-255
   * @return out  int[][], output array of convolved image.
   */
  public int[][] process(int[][] raw, int[][] mask) {
    int height = raw.length;
    int width = raw[0].length;
    int[][] out = new int[height - 4][width - 4];

    for (int r = 3; r < height - 3; r++) {
      for (int c = 3; c < width - 3; c++) {
        int sum = 0;

        for (int kr = -1; kr < 4; kr++) {
          for (int kc = -1; kc < 4; kc++) {
            sum += (mask[kr + 1][kc + 1] * raw[r + kr][c + kc]);
          }
        }

        out[r - 1][c - 1] = sum;
      }
    }

    return out;
  }
}