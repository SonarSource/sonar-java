package org.sonar.plugins.jacoco;

import com.google.common.base.Preconditions;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class JacocoBinaryReader {

  /**
   * Read JaCoCo report determining the format to be used.
   * @param jacocoExecutionData binary report file.
   * @param executionDataVisitor visitor to store execution data.
   * @param sessionInfoStore visitor to store info session.
   * @return true if binary format is the latest one.
   * @throws IOException in case of error or binary format not supported.
   */
  public static boolean readJacocoReport(File jacocoExecutionData, IExecutionDataVisitor executionDataVisitor, ISessionInfoVisitor sessionInfoStore) throws IOException {
    JaCoCoExtensions.LOG.info("Analysing {}", jacocoExecutionData);
    boolean currentReportFormat;
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(jacocoExecutionData))) {
      currentReportFormat = isCurrentReportFormat(jacocoExecutionData);
      if (currentReportFormat) {
        ExecutionDataReader reader = new ExecutionDataReader(inputStream);
        reader.setSessionInfoVisitor(sessionInfoStore);
        reader.setExecutionDataVisitor(executionDataVisitor);
        reader.read();
      } else {
        org.jacoco.previous.core.data.ExecutionDataReader reader = new org.jacoco.previous.core.data.ExecutionDataReader(inputStream);
        reader.setSessionInfoVisitor(sessionInfoStore);
        reader.setExecutionDataVisitor(executionDataVisitor);
        reader.read();
      }
    }
    return currentReportFormat;
  }

  private static boolean isCurrentReportFormat(File jacocoExecutionData) throws IOException {
    try (DataInputStream dis = new DataInputStream(new FileInputStream(jacocoExecutionData))) {
      byte firstByte = dis.readByte();
      Preconditions.checkState(firstByte == ExecutionDataWriter.BLOCK_HEADER);
      Preconditions.checkState(dis.readChar() == ExecutionDataWriter.MAGIC_NUMBER);
      char version = dis.readChar();
      boolean isCurrentFormat = version == ExecutionDataWriter.FORMAT_VERSION;
      if (!isCurrentFormat) {
        JaCoCoExtensions.LOG.warn("You are not using the latest JaCoCo binary format version, please consider upgrading to latest JaCoCo version.");
      }
      return isCurrentFormat;
    }
  }

  /**
   * Caller must guarantee that {@code classFiles} are actually class file.
   */
  public static CoverageBuilder analyzeFiles(ExecutionDataStore executionDataStore, Collection<File> classFiles, boolean previousReportFormat) {
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    if (previousReportFormat) {
      org.jacoco.previous.core.analysis.Analyzer analyzer = new org.jacoco.previous.core.analysis.Analyzer(executionDataStore, coverageBuilder);
      for (File classFile : classFiles) {
        analyzeClassFile(analyzer, classFile);
      }
    } else {
      Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
      for (File classFile : classFiles) {
        analyzeClassFile(analyzer, classFile);
      }
    }
    return coverageBuilder;
  }

  /**
   * Caller must guarantee that {@code classFile} is actually class file.
   */
  private static void analyzeClassFile(org.jacoco.previous.core.analysis.Analyzer analyzer, File classFile) {
    try (InputStream inputStream = new FileInputStream(classFile)) {
      analyzer.analyzeClass(inputStream, classFile.getPath());
    } catch (IOException e) {
      // (Godin): in fact JaCoCo includes name into exception
      JaCoCoExtensions.LOG.warn("Exception during analysis of file " + classFile.getAbsolutePath(), e);
    }
  }

  private static void analyzeClassFile(Analyzer analyzer, File classFile) {
    try (InputStream inputStream = new FileInputStream(classFile)) {
      analyzer.analyzeClass(inputStream, classFile.getPath());
    } catch (IOException e) {
      // (Godin): in fact JaCoCo includes name into exception
      JaCoCoExtensions.LOG.warn("Exception during analysis of file " + classFile.getAbsolutePath(), e);
    }
  }


}
