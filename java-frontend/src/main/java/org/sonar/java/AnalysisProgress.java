/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

public class AnalysisProgress {
  private final int totalFileCount;
  private int currentBatchSize;
  private int analysedFileCount;

  public AnalysisProgress(int totalFileCount) {
    this.totalFileCount = totalFileCount;
    currentBatchSize = 0;
    analysedFileCount = 0;
  }

  public void startBatch(int currentBatchSize) {
    this.currentBatchSize = currentBatchSize;
  }

  public void endBatch() {
    this.analysedFileCount += currentBatchSize;
    this.currentBatchSize = 0;
  }

  public boolean isFirstBatch() {
    return analysedFileCount == 0;
  }

  public boolean isLastBatch() {
    return analysedFileCount + currentBatchSize == totalFileCount;
  }

  public double toGlobalPercentage(double currentBatchPercentage) {
    if (totalFileCount == 0) {
      return 0;
    }
    double percentageDoneInPreviousBatches = analysedFileCount / (double) totalFileCount;
    double currentBatchFactor = currentBatchSize / (double) totalFileCount;
    return percentageDoneInPreviousBatches + currentBatchFactor * currentBatchPercentage;
  }

}
