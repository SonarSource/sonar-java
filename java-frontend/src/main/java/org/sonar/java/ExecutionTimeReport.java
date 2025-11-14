/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.InputFileUtils;

public class ExecutionTimeReport {
  private static final Logger LOG = LoggerFactory.getLogger(ExecutionTimeReport.class);

  private static final int MAX_REPORTED_FILES = 10;
  private static final long MIN_REPORTED_ANALYSIS_TIME_MS = 1000;
  private static final long MIN_TOTAL_ANALYSIS_TIME_TO_REPORT_MS = 20_000;

  private static class ExecutionTime {
    private final String file;
    private final long analysisTime;
    private final long lengthInBytes;

    public ExecutionTime(String file, long analysisTime, long lengthInBytes) {
      this.file = file;
      this.analysisTime = analysisTime;
      this.lengthInBytes = lengthInBytes;
    }
  }

  private static final Comparator<ExecutionTime> ORDER_BY_ANALYSIS_TIME_DESCENDING_AND_FILE_ASCENDING = (a, b) -> {
    int compare = Long.compare(b.analysisTime, a.analysisTime);
    return compare != 0 ? compare : a.file.compareTo(b.file);
  };

  private final LinkedList<ExecutionTime> recordedOrderedExecutionTime = new LinkedList<>();
  private long minRecordedOrderedExecutionTime = MIN_REPORTED_ANALYSIS_TIME_MS;

  private final Clock clock;
  private final long analysisStartTimeMS;
  private InputFile currentFile;
  private long currentFileStartTimeMS;

  public ExecutionTimeReport() {
    this(Clock.systemUTC());
  }

  @VisibleForTesting
  ExecutionTimeReport(Clock clock) {
    this.clock = clock;
    analysisStartTimeMS = clock.millis();
  }

  public void start(InputFile inputFile) {
    this.currentFile = inputFile;
    currentFileStartTimeMS = clock.millis();
  }

  public void end() {
    long currentAnalysisTime = clock.millis() - currentFileStartTimeMS;
    if (LOG.isTraceEnabled()) {
      LOG.trace("Analysis time of {} ({}ms)", currentFile, currentAnalysisTime);
    } else if (currentAnalysisTime >= MIN_REPORTED_ANALYSIS_TIME_MS && LOG.isDebugEnabled()) {
      LOG.debug("Analysis time of {} ({}ms)", currentFile, currentAnalysisTime);
    }
    if (currentAnalysisTime >= minRecordedOrderedExecutionTime) {
      long currentFileLength = InputFileUtils.charCount(currentFile, -1);
      recordedOrderedExecutionTime.add(new ExecutionTime(currentFile.toString(), currentAnalysisTime, currentFileLength));
      recordedOrderedExecutionTime.sort(ORDER_BY_ANALYSIS_TIME_DESCENDING_AND_FILE_ASCENDING);
      if (recordedOrderedExecutionTime.size() > MAX_REPORTED_FILES) {
        recordedOrderedExecutionTime.removeLast();
        minRecordedOrderedExecutionTime = recordedOrderedExecutionTime.stream()
          .mapToLong(e -> e.analysisTime)
          .min()
          .orElse(MIN_REPORTED_ANALYSIS_TIME_MS);
      }
    }
    this.currentFile = null;
  }

  public void reportAsBatch() {
    report("Slowest analyzed files (batch mode enabled):");
  }

  public void report() {
    report("Slowest analyzed files:");
  }

  private void report(String message) {
    if (currentFile != null) {
      end();
    }
    long analysisEndTimeMS = clock.millis() - analysisStartTimeMS;
    if (LOG.isInfoEnabled() && analysisEndTimeMS >= MIN_TOTAL_ANALYSIS_TIME_TO_REPORT_MS && !recordedOrderedExecutionTime.isEmpty()) {
      LOG.info("{}{}{}", message, System.lineSeparator(), this);
    }
  }

  @Override
  public String toString() {
    return recordedOrderedExecutionTime.stream()
      .map(e -> "    " + e.file + " (" + e.analysisTime + "ms, " + e.lengthInBytes + "B)")
      .collect(Collectors.joining(System.lineSeparator()));
  }

}
