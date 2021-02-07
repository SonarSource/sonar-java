/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class PerformanceMeasure {
  private static final Logger LOG = Loggers.get(PerformanceMeasure.class);
  private static final String ACTIVATION_PROPERTY = "sonar.java.performance.measure";
  /**
   * In a multi-threaded environment, this variable should be stored in a ThreadLocal
   */
  private static PerformanceMeasure currentMeasure = null;
  private static final IgnoredDuration NO_OP_DURATION = new IgnoredDuration();
  private static final NumberFormat LONG_FORMATTER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT));
  static final long NANO_PER_MICRO = 1_000;

  @Nullable
  public final PerformanceMeasure parent;
  public final String name;
  public final Supplier<Long> nanoTimeSupplier;
  private long totalDurationNanos = 0;
  private long callsCount = 0;
  private Map<String, PerformanceMeasure> childrenMap = null;

  public static DurationReport start(Configuration config, String name, Supplier<Long> nanoTimeSupplier) {
    if (!config.get(PerformanceMeasure.ACTIVATION_PROPERTY).filter("true"::equals).isPresent()) {
      return NO_OP_DURATION;
    }
    currentMeasure = new PerformanceMeasure(currentMeasure, name, nanoTimeSupplier);
    return new RecordedDuration(currentMeasure);
  }

  public static Duration start(Object object) {
    if (currentMeasure == null)  {
      return NO_OP_DURATION;
    }
    return start(object.getClass().getSimpleName());
  }

  public static Duration start(String name) {
    if (currentMeasure == null)  {
      return NO_OP_DURATION;
    }
    if (currentMeasure.name.equals(name)) {
      // Probably an unexpected second call to start() without a call to stop(), let's ignore the first start()
      return new RecordedDuration(currentMeasure);
    }
    if (currentMeasure.childrenMap == null) {
      currentMeasure.childrenMap = new HashMap<>();
    }
    currentMeasure = currentMeasure.childrenMap
      .computeIfAbsent(name, n -> new PerformanceMeasure(currentMeasure, n, currentMeasure.nanoTimeSupplier));
    return new RecordedDuration(currentMeasure);
  }

  public PerformanceMeasure(@Nullable PerformanceMeasure parent, String name, Supplier<Long> nanoTimeSupplier) {
    this.parent = parent;
    this.name = name;
    this.nanoTimeSupplier = nanoTimeSupplier;
  }

  public final void add(long durationNanos) {
    totalDurationNanos += durationNanos;
    callsCount++;
  }

  public void report(StringBuilder out, String parentContext) {
    String context = parentContext + "/" + name;
    if (totalDurationNanos != 0) {
      String minusChildren = "";
      long childrenDurationNanos = childrenMap == null ? 0 : childrenMap.values().stream().mapToLong(c -> c.totalDurationNanos).sum();
      if (childrenDurationNanos != 0) {
        long diffMicros = (totalDurationNanos - childrenDurationNanos) / NANO_PER_MICRO;
        minusChildren = ", minus children duration " + LONG_FORMATTER.format(diffMicros) + " micro_s ";
      }
      out.append(context)
        .append(" ( ")
        .append(LONG_FORMATTER.format(totalDurationNanos / NANO_PER_MICRO))
        .append(" micro_s during ").append(LONG_FORMATTER.format(callsCount)).append(" call(s)").append(minusChildren).append(")\n");
    }
    if (childrenMap != null) {
      childrenMap.values().stream()
        .sorted(Comparator.comparing(e -> e.name))
        .forEach(child -> child.report(out, context));
    }
  }

  public interface Duration {
    void stop();
  }

  public interface DurationReport {
    void stopAndLog();
  }

  private static final class IgnoredDuration implements Duration, DurationReport {
    @Override
    public void stop() {
      // no op
    }

    @Override
    public void stopAndLog() {
      // no op
    }
  }

  private static class RecordedDuration implements Duration, DurationReport {
    private final PerformanceMeasure measure;
    private long startNanos;

    public RecordedDuration(PerformanceMeasure measure) {
      this.measure = measure;
      this.startNanos = measure.nanoTimeSupplier.get();
    }

    @Override
    public void stop() {
      if (startNanos != -1) {
        measure.add(measure.nanoTimeSupplier.get() - startNanos);
        currentMeasure = measure.parent;
        startNanos = -1;
      }
    }

    @Override
    public void stopAndLog() {
      stop();
      StringBuilder out = new StringBuilder();
      out.append("Performance Measures:\n");
      measure.report(out, "  {perf} ");
      LOG.info(out.toString());
    }

  }

}
