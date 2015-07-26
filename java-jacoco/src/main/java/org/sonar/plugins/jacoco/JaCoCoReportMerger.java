/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacoco;

import org.apache.commons.lang.BooleanUtils;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfoStore;
import org.sonar.api.utils.SonarException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class to merge JaCoCo reports.
 *
 * This class handles two versions of JaCoCo binary format to merge.
 */
public class JaCoCoReportMerger {

  private JaCoCoReportMerger() {
  }

  /**
   * Merge all reports in reportOverall.
   * @param reportOverall destination file of merge.
   * @param reports files to be merged.
   */
  public static void mergeReports(File reportOverall, File... reports) {
    SessionInfoStore infoStore = new SessionInfoStore();
    ExecutionDataStore dataStore = new ExecutionDataStore();
    boolean isCurrentVersionFormat = loadSourceFiles(infoStore, dataStore, reports);

    try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(reportOverall))) {
      Object visitor;
      if (isCurrentVersionFormat) {
        visitor = new ExecutionDataWriter(outputStream);
      } else {
        visitor = new org.jacoco.previous.core.data.ExecutionDataWriter(outputStream);
      }
      infoStore.accept((ISessionInfoVisitor) visitor);
      dataStore.accept((IExecutionDataVisitor) visitor);
    } catch (IOException e) {
      throw new SonarException(String.format("Unable to write overall coverage report %s", reportOverall.getAbsolutePath()), e);
    }
  }

  private static boolean loadSourceFiles(ISessionInfoVisitor infoStore, IExecutionDataVisitor dataStore, File... reports) {
    Boolean isCurrentVersionFormat = null;
    for (File report : reports) {
      if (report.isFile()) {
        JacocoReportReader jacocoReportReader = new JacocoReportReader(report).readJacocoReport(dataStore, infoStore);
        boolean reportFormatIsCurrent = jacocoReportReader.useCurrentBinaryFormat();
        if (isCurrentVersionFormat == null) {
          isCurrentVersionFormat = reportFormatIsCurrent;
        } else if (!isCurrentVersionFormat.equals(reportFormatIsCurrent)) {
          throw new IllegalStateException("You are trying to merge two different JaCoCo binary formats. Please use only one version of JaCoCo.");
        }
      }
    }
    return BooleanUtils.isNotFalse(isCurrentVersionFormat);
  }

}
