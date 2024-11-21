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

import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.notifications.AnalysisWarnings;

/**
 * Wrap an {@link AnalysisWarnings} instance, available since SQ API 7.4.
 * AnalysisWarnings are not supported in SonarLint context, hence this wrapper when running in SonarLint.
 * The wrapper allows to avoid null check in every components which requires it.
 */
@ScannerSide
@InstantiationStrategy("PER_BATCH")
public class AnalysisWarningsWrapper {
  private final AnalysisWarnings analysisWarnings;

  /**
   * Noop instance which can be used as placeholder when {@link AnalysisWarnings} is not supported
   */
  public static final AnalysisWarningsWrapper NOOP_ANALYSIS_WARNINGS = new AnalysisWarningsWrapper(null) {
    @Override
    public void addUnique(String text) {
      // no operation
    }
  };

  public AnalysisWarningsWrapper(AnalysisWarnings analysisWarnings) {
    this.analysisWarnings = analysisWarnings;
  }

  public void addUnique(String text) {
    this.analysisWarnings.addUnique(text);
  }
}
