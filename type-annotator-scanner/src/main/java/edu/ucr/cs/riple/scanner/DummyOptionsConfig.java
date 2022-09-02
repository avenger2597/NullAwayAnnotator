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

package edu.ucr.cs.riple.scanner;

import java.nio.file.Path;
import javax.annotation.Nonnull;

public class DummyOptionsConfig implements Config {

  static final String ERROR_MESSAGE =
      "Error in configuring Scanner Checker, a path must be passed via Error Prone Flag (-XepOpt:Scanner:ConfigPath) where output directory is in XML format.";

  @Override
  public boolean callTrackerIsActive() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public boolean fieldTrackerIsActive() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public boolean methodTrackerIsActive() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public boolean classTrackerIsActive() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Override
  public Serializer getSerializer() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }

  @Nonnull
  @Override
  public Path getOutputDirectory() {
    throw new IllegalStateException(ERROR_MESSAGE);
  }
}