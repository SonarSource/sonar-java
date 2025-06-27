/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;

class BatchGenerator {
  public final long batchSizeInBytes;
  private final Iterator<InputFile> source;
  private InputFile buffer = null;

  public BatchGenerator(Iterator<InputFile> source, long batchSizeInBytes) {
    this.source = source;
    this.batchSizeInBytes = batchSizeInBytes;
  }

  public boolean hasNext() {
    return buffer != null || source.hasNext();
  }

  public List<InputFile> next() {
    List<InputFile> batch = clearBuffer();
    long batchSize = batch.isEmpty() ? 0L : batch.get(0).file().length();
    while (source.hasNext() && batchSize <= batchSizeInBytes) {
      buffer = source.next();
      batchSize += buffer.file().length();
      if (batchSize > batchSizeInBytes) {
        // If the batch is empty, we clear the value from the buffer and add it to the batch
        if (batch.isEmpty()) {
          batch.add(buffer);
          buffer = null;
        }
        // If the last inputFile does not fit into the non-empty batch, we keep it in the buffer for the next call
        return batch;
      }
      batch.add(buffer);
    }
    buffer = null;
    return batch;
  }

  private List<InputFile> clearBuffer() {
    if (buffer == null) {
      return new ArrayList<>();
    }
    List<InputFile> batch = new ArrayList<>();
    batch.add(buffer);
    buffer = null;
    return batch;
  }
}
