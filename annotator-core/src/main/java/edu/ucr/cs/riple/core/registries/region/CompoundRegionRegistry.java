/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.core.registries.region;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.region.generatedcode.AnnotationProcessorHandler;
import edu.ucr.cs.riple.injector.location.Location;

/**
 * Container class for all region registries. This region registry can identify impacted regions for
 * all fix types.
 */
public class CompoundRegionRegistry implements RegionRegistry {

  /** List of all region registries. */
  private final ImmutableSet<RegionRegistry> registries;

  /** Module where this registry belongs to. */
  private final ModuleInfo moduleInfo;

  /**
   * Method region registry. This registry is used by other registries to identify impacted regions
   * specifically {@link AnnotationProcessorHandler}. To avoid recreating this instance, it is
   * stored here and passed to other registries.
   */
  private final MethodRegionRegistry methodRegionRegistry;

  public CompoundRegionRegistry(ModuleInfo moduleInfo, Context context) {
    this.moduleInfo = moduleInfo;
    this.methodRegionRegistry = new MethodRegionRegistry(moduleInfo, context);
    this.registries =
        ImmutableSet.of(
            new FieldRegionRegistry(moduleInfo, context),
            methodRegionRegistry,
            new ParameterRegionRegistry(moduleInfo, methodRegionRegistry));
  }

  @Override
  public ImmutableSet<Region> getImpactedRegions(Location location) {
    ImmutableSet.Builder<Region> fromRegistriesBuilder = ImmutableSet.builder();
    this.registries.forEach(
        registry -> fromRegistriesBuilder.addAll(registry.getImpactedRegions(location)));
    ImmutableSet<Region> fromRegionRegistries = fromRegistriesBuilder.build();
    ImmutableSet.Builder<Region> extendedRegionsBuilder = ImmutableSet.builder();
    extendedRegionsBuilder.addAll(fromRegionRegistries);
    this.moduleInfo
        .getAnnotationProcessorHandlers()
        .forEach(
            handler ->
                extendedRegionsBuilder.addAll(
                    handler.extendForGeneratedRegions(fromRegionRegistries)));
    return extendedRegionsBuilder.build();
  }

  @Override
  public ImmutableSet<Region> getImpactedRegionsByUse(Location location) {
    ImmutableSet.Builder<Region> fromRegistriesBuilder = ImmutableSet.builder();
    this.registries.forEach(
        registry -> fromRegistriesBuilder.addAll(registry.getImpactedRegionsByUse(location)));
    return fromRegistriesBuilder.build();
  }

  /**
   * Returns the method region registry created by this instance.
   *
   * @return Method region registry instance.
   */
  public MethodRegionRegistry getMethodRegionRegistry() {
    return methodRegionRegistry;
  }
}
