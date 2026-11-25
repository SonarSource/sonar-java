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
package org.sonar.java;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;

/**
 * Benchmark logs of the Java frontend. Every line starts with {@value #KEY}, followed by space separated
 * {@code key=value} pairs, the file path always last. Durations are measured with {@link System#nanoTime()} and written
 * in microseconds ({@code *_us}). The run id and the fields of the file being parsed are kept per thread, as several
 * analyses can share one JVM. The lines are on unless the system property {@value #ENABLED_PROPERTY} is {@code false}.
 */
public final class FrontendBenchLog {

  public static final String KEY = "SJ_FE_BENCH v=1";
  public static final String ENABLED_PROPERTY = "sonar.java.frontend.bench";

  private static final Logger LOG = LoggerFactory.getLogger(FrontendBenchLog.class);
  private static final boolean ENABLED = !"false".equals(System.getProperty(ENABLED_PROPERTY));
  private static final String FRONTEND = "ecj";
  private static final ThreadLocal<Run> RUN = ThreadLocal.withInitial(() -> new Run("none"));
  private static final ThreadLocal<Map<String, Object>> FILE_FIELDS = ThreadLocal.withInitial(LinkedHashMap::new);

  private FrontendBenchLog() {
  }

  public static void startRun() {
    RUN.set(new Run(String.format("%08x", ThreadLocalRandom.current().nextInt())));
    FILE_FIELDS.get().clear();
  }

  public static long micros(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000L;
  }

  public static void log(String event, Object... fields) {
    write(event, null, Map.of(), fields);
  }

  public static void putFileField(String key, Object value) {
    FILE_FIELDS.get().put(key, value);
  }

  public static void resetFile() {
    FILE_FIELDS.get().clear();
  }

  /**
   * Writes one {@code ev=file} line with the fields collected by the parser for this file, then the given ones.
   */
  public static void flushFile(InputFile file, Object... fields) {
    Map<String, Object> collected = FILE_FIELDS.get();
    write("file", file.toString(), collected, fields);
    collected.clear();
  }

  public static long bytes(InputFile file) {
    return file.path().toFile().length();
  }

  /**
   * @return the number of lines, or -1 for the generated files that do not know it
   */
  public static int lines(InputFile file) {
    try {
      return file.lines();
    } catch (UnsupportedOperationException e) {
      return -1;
    }
  }

  public static long threadCpuNanos() {
    ThreadMXBean threads = ManagementFactory.getThreadMXBean();
    return threads.isCurrentThreadCpuTimeSupported() ? threads.getCurrentThreadCpuTime() : 0L;
  }

  public static long gcMillis() {
    return ManagementFactory.getGarbageCollectorMXBeans().stream()
      .mapToLong(GarbageCollectorMXBean::getCollectionTime)
      .filter(time -> time > 0)
      .sum();
  }

  public static long heapMb() {
    return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024L * 1024L);
  }

  private static void write(String event, @Nullable String file, Map<String, Object> collected, Object... fields) {
    if (!ENABLED || !LOG.isInfoEnabled()) {
      return;
    }
    Run run = RUN.get();
    StringBuilder line = new StringBuilder(KEY)
      .append(" run=").append(run.id)
      .append(" fe=").append(FRONTEND)
      .append(" ev=").append(event)
      .append(" seq=").append(run.seq++)
      .append(" wall_ms=").append(System.currentTimeMillis())
      .append(" mono_us=").append(System.nanoTime() / 1_000L)
      .append(" thr=").append(value(Thread.currentThread().getName()));
    collected.forEach((key, value) -> line.append(' ').append(key).append('=').append(value(value)));
    for (int i = 0; i + 1 < fields.length; i += 2) {
      line.append(' ').append(fields[i]).append('=').append(value(fields[i + 1]));
    }
    if (file != null) {
      line.append(" file=").append(file);
    }
    LOG.info("{}", line);
  }

  private static String value(@Nullable Object value) {
    return String.valueOf(value).replace(' ', '_');
  }

  private static final class Run {
    private final String id;
    private long seq = 0L;

    private Run(String id) {
      this.id = id;
    }
  }
}
