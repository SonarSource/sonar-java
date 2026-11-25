/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.model.JParser;
import org.sonarsource.analyzer.commons.ProgressReport;

/**
 * Progress of the files one call of {@link SemSitterParser#parse} goes through. File by file it is a
 * {@link ProgressReport} over these files. In batch mode the calls are the batches of one analysis, so, like the
 * default frontend, it logs the start on the first batch, the end on the last one, and in between the share of the
 * whole analysis that is done.
 */
abstract class ParseProgress {

  static final long PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(10);

  abstract void nextFile();

  abstract void end(boolean completed);

  static ParseProgress start(JParser.Mode mode, List<InputFile> files, AnalysisProgress analysisProgress) {
    return mode == JParser.Mode.BATCH ? new Batch(analysisProgress, files.size(), PERIOD_MILLIS) : new FileByFile(files);
  }

  private static final class FileByFile extends ParseProgress {
    private final ProgressReport report = new ProgressReport("Report about progress of Java AST analyzer", PERIOD_MILLIS);

    private FileByFile(List<InputFile> files) {
      report.start(files.stream().map(InputFile::toString).toList());
    }

    @Override
    void nextFile() {
      report.nextFile();
    }

    @Override
    void end(boolean completed) {
      if (completed) {
        report.stop();
      } else {
        report.cancel();
      }
    }
  }

  static final class Batch extends ParseProgress {
    private static final Logger LOG = LoggerFactory.getLogger(Batch.class);

    private final AnalysisProgress analysisProgress;
    private final int files;
    private final long periodNanos;
    private int done;
    private long lastLog;

    Batch(AnalysisProgress analysisProgress, int files, long periodMillis) {
      this.analysisProgress = analysisProgress;
      this.files = files;
      this.periodNanos = TimeUnit.MILLISECONDS.toNanos(periodMillis);
      this.lastLog = System.nanoTime();
      if (analysisProgress.isFirstBatch()) {
        LOG.info("Starting batch processing.");
      }
    }

    @Override
    void nextFile() {
      done++;
      long now = System.nanoTime();
      if (now - lastLog >= periodNanos) {
        lastLog = now;
        LOG.info("{}% analyzed", (int) (analysisProgress.toGlobalPercentage(done / (double) files) * 100));
      }
    }

    @Override
    void end(boolean completed) {
      if (completed && analysisProgress.isLastBatch()) {
        LOG.info("100% analyzed");
        LOG.info("Batch processing: Done.");
      }
    }
  }
}
