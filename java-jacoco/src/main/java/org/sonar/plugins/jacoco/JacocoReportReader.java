/*
 * SonarQube Java
 * Copyright (C) 2010-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.jacoco;

import com.google.common.base.Preconditions;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.annotation.Nullable;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.sonar.java.AnalysisException;

import static org.sonar.plugins.jacoco.JaCoCoExtensions.LOG;

public class JacocoReportReader {

  @Nullable
  private final File jacocoExecutionData;

  public JacocoReportReader(@Nullable File jacocoExecutionData) {
    checkCurrentReportFormat(jacocoExecutionData);
    this.jacocoExecutionData = jacocoExecutionData;
  }

  /**
   * Read JaCoCo report determining the format to be used.
   * @param executionDataVisitor visitor to store execution data.
   * @param sessionInfoStore visitor to store info session.
   * @return true if binary format is the latest one.
   * @throws IOException in case of error or binary format not supported.
   */
  public JacocoReportReader readJacocoReport(IExecutionDataVisitor executionDataVisitor, ISessionInfoVisitor sessionInfoStore) {
    if (jacocoExecutionData == null) {
      return this;
    }

    LOG.info("Analysing {}", jacocoExecutionData);
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(jacocoExecutionData))) {
      ExecutionDataReader reader = new ExecutionDataReader(inputStream);
      reader.setSessionInfoVisitor(sessionInfoStore);
      reader.setExecutionDataVisitor(executionDataVisitor);
      reader.read();
    } catch (IOException e) {
      throw new AnalysisException(String.format("Unable to read %s", jacocoExecutionData.getAbsolutePath()), e);
    }
    return this;
  }

  private static void checkCurrentReportFormat(@Nullable File jacocoExecutionData) {
    if (jacocoExecutionData == null) {
      return;
    }
    try (DataInputStream dis = new DataInputStream(new FileInputStream(jacocoExecutionData))) {
      byte firstByte = dis.readByte();
      Preconditions.checkState(firstByte == ExecutionDataWriter.BLOCK_HEADER);
      Preconditions.checkState(dis.readChar() == ExecutionDataWriter.MAGIC_NUMBER);
      char version = dis.readChar();
      if (version != ExecutionDataWriter.FORMAT_VERSION) {
        throw new AnalysisException("You are not using the latest JaCoCo binary format version, please consider upgrading to latest JaCoCo version.");
      }
    } catch (IOException | IllegalStateException e) {
      throw new AnalysisException(String.format("Unable to read %s to determine JaCoCo binary format.", jacocoExecutionData.getAbsolutePath()), e);
    }
  }

  /**
   * Caller must guarantee that {@code classFiles} are actually class file.
   */
  public CoverageBuilder analyzeFiles(ExecutionDataStore executionDataStore, Collection<File> classFiles) {
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
    for (File classFile : classFiles) {
      analyzeClassFile(analyzer, classFile);
    }
    logNoMatchClasses(coverageBuilder.getNoMatchClasses());
    return coverageBuilder;
  }

  private static void logNoMatchClasses(Collection<IClassCoverage> noMatchClasses) {
    if (noMatchClasses.isEmpty()) {
      return;
    }
    LOG.warn("The following class(es) did not match with execution data:");
    for (IClassCoverage iClassCoverage : noMatchClasses) {
      LOG.warn(String.format("> '%s'", iClassCoverage.getName()));
    }
    LOG.warn("In order to have accurate coverage measures, the same class files must be used as at runtime for report generation.");
  }


  private static void analyzeClassFile(Analyzer analyzer, File classFile) {
    try (InputStream inputStream = new FileInputStream(classFile)) {
      analyzer.analyzeClass(inputStream, classFile.getPath());
    } catch (IOException e) {
      // (Godin): in fact JaCoCo includes name into exception
      LOG.warn("Exception during analysis of file " + classFile.getAbsolutePath(), e);
    }
  }

}
