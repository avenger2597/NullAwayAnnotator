/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.core.registries.region.generatedcode;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.region.Region;
import java.util.Set;

/**
 * Interface for annotation processors that generate code. These processors can impact the set of
 * potentially impacted regions or copy an annotation from one location to another. Subclasses of
 * this interface can be used to extend the set of impacted regions for a given set of regions or
 * inform Annotator about the copy of an annotation to other locations.
 */
public interface AnnotationProcessorHandler {

  /**
   * Given an input set of impacted regions, returns an extended set of potentially-impacted
   * regions, accounting for the properties of impacted regions in code generated by an annotation
   * processor.
   *
   * @param regions Set of impacted regions.
   */
  Set<Region> extendForGeneratedRegions(Set<Region> regions);

  /**
   * Extends the set of fixes to account for code generated by an annotation processor. Annotation
   * processors might copy an annotation from one location to another. This method can be used to
   * inform Annotator about the copy of an annotation to other locations.
   *
   * @param fixes Given set of fixes that some of them may make the processor to copy to other
   *     locations.
   * @return Set of fixes that includes the input set of fixes and the fixes that are copied to
   *     other locations.
   */
  ImmutableSet<Fix> extendForGeneratedFixes(Set<Fix> fixes);
}