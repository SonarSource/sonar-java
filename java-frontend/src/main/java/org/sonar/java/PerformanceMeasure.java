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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.annotations.VisibleForTesting;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PerformanceMeasure {
  private static final Logger LOG = Loggers.get(PerformanceMeasure.class);
  private static final String ACTIVATION_PROPERTY = "sonar.java.performance.measure";
  private static final String FILE_PATH_PROPERTY = "sonar.java.performance.measure.path";
  private static final String DESTINATION_FILE = "sonar.java.performance.measure.json";
  /**
   * In a multi-threaded environment, this variable should be stored in a ThreadLocal
   */
  private static PerformanceMeasure currentMeasure = null;
  private static Path performanceMeasureFile = null;
  private static final IgnoredDuration NO_OP_DURATION = new IgnoredDuration();

  @Nullable
  public final PerformanceMeasure parent;
  public final String name;
  public final Supplier<Long> nanoTimeSupplier;
  private long totalDurationNanos = 0;
  private long callsCount = 0;
  private Map<String, PerformanceMeasure> childrenMap = null;

  public static DurationReport start(Configuration config, String name, Supplier<Long> nanoTimeSupplier) {
    performanceMeasureFile = config.get(PerformanceMeasure.FILE_PATH_PROPERTY)
      .filter(path -> !path.isEmpty())
      .map(path -> path.replace('\\', File.separatorChar).replace('/', File.separatorChar))
      .map(Paths::get)
      .orElse(null);
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
    currentMeasure = currentMeasure.getOrCreateChild(name);
    return new RecordedDuration(currentMeasure);
  }

  public static void setCurrent(@Nullable PerformanceMeasure measure) {
    currentMeasure = measure;
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

  public Collection<PerformanceMeasure> children() {
    return childrenMap != null ? childrenMap.values() : Collections.emptyList();
  }

  private PerformanceMeasure getOrCreateChild(String name) {
    if (childrenMap == null) {
      childrenMap = new HashMap<>();
    }
    return childrenMap.computeIfAbsent(name, n -> new PerformanceMeasure(this, n, nanoTimeSupplier));
  }

  private PerformanceMeasure merge(PerformanceMeasure measure) throws IOException {
    if (!measure.name.equals(name)) {
      throw new IOException("Incompatible name '" + measure.name + "' and '" + name + "'");
    }
    totalDurationNanos += measure.totalDurationNanos;
    callsCount += measure.callsCount;
    for (PerformanceMeasure child : measure.children()) {
      getOrCreateChild(child.name).merge(child);
    }
    return this;
  }

  public interface Duration {
    void stop();
  }

  public interface DurationReport {
    void stopAndLog(@Nullable File workDir, boolean appendMeasurementCost);
  }

  private static final class IgnoredDuration implements Duration, DurationReport {
    @Override
    public void stop() {
      // no op
    }

    @Override
    public void stopAndLog(@Nullable File workDir, boolean appendMeasurementCost) {
      // no op
    }
  }

  private static class RecordedDuration implements Duration, DurationReport {

    private static final String PARENT_OF_THROWAWAY_MEASURES_TO_COMPUTE_OBSERVATION_COST = "#measures to compute observation cost";
    private static final int SAMPLING_COUNT_TO_EVALUATE_OBSERVATION_COST = 99;
    private static final Supplier<IntStream> SAMPLES = () -> IntStream.range(0, SAMPLING_COUNT_TO_EVALUATE_OBSERVATION_COST);

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
        startNanos = -1;
        setCurrent(measure.parent);
      }
    }

    @Override
    public void stopAndLog(@Nullable File workDir, boolean appendMeasurementCost) {
      if (appendMeasurementCost) {
        setCurrent(measure);
        appendMeasurementCost();
      }
      stop();
      saveToFile(workDir, measure);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Performance Measures:\n" + jsonFormat(toJson(measure)));
      }
    }

    private static void appendMeasurementCost() {
      String[] sampleNames = SAMPLES.get().mapToObj(i -> "m" + i).toArray(String[]::new);
      Duration totalDuration = start("#MeasurementCost_v1");
      PerformanceMeasure measurementCost = currentMeasure;
      Duration temporaryDuration = start(PARENT_OF_THROWAWAY_MEASURES_TO_COMPUTE_OBSERVATION_COST);
      measurementCost.getOrCreateChild("nanoTime").add(median(SAMPLES.get().mapToLong(i -> {
        long start = System.nanoTime();
        return System.nanoTime() - start;
      })));
      measurementCost.getOrCreateChild("createChild").add(median(SAMPLES.get().mapToLong(i -> {
        long start = System.nanoTime();
        start(sampleNames[i]).stop();
        return System.nanoTime() - start;
      })));
      measurementCost.getOrCreateChild("observationCost").add(median(Arrays.stream(sampleNames)
        .map(n -> currentMeasure.childrenMap.get(n)).mapToLong(m -> m.totalDurationNanos)));
      start("measure").stop();
      measurementCost.getOrCreateChild("incrementChild").add(median(SAMPLES.get().mapToLong(i -> {
        long start = System.nanoTime();
        start("measure").stop();
        return System.nanoTime() - start;
      })));
      temporaryDuration.stop();
      measurementCost.childrenMap.remove(PARENT_OF_THROWAWAY_MEASURES_TO_COMPUTE_OBSERVATION_COST);
      totalDuration.stop();
    }

    private static long median(LongStream measures) {
      long[] sortedMeasures = measures.sorted().toArray();
      return sortedMeasures[(sortedMeasures.length - 1)/2];
    }

    private static void saveToFile(@Nullable File workDir, PerformanceMeasure measure) {
      Path performanceFile = performanceMeasureFile;
      if (performanceFile == null && workDir == null) {
        return;
      }
      try {
        if (performanceFile == null) {
          if (!workDir.exists()) {
            throw new IOException("Directory does not exist: " + workDir.toString());
          }
          performanceFile = workDir.toPath().resolve(DESTINATION_FILE);
        }
        PerformanceMeasure allMeasures;
        if (Files.exists(performanceFile)) {
          LOG.info("Adding performance measures into: " + performanceFile);
          allMeasures = fromJson(performanceFile).merge(measure);
        } else {
          LOG.info("Saving performance measures into: " + performanceFile);
          allMeasures = measure;
          ensureParentDirectoryExists(performanceFile);
        }
        Files.write(performanceFile, jsonFormat(toJson(allMeasures)).getBytes(UTF_8));
      } catch (IOException e) {
        LOG.error("Can't save performance measure: " + e.getMessage());
      }
    }

    private static JsonObject toJson(PerformanceMeasure measure) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("name", measure.name);
      jsonObject.addProperty("calls", measure.callsCount);
      jsonObject.addProperty("durationNanos", measure.totalDurationNanos);
      Collection<PerformanceMeasure> children = measure.children();
      if (!children.isEmpty()) {
        jsonObject.add("children", children.stream()
          .sorted(Comparator.comparing(e -> e.name))
          .map(RecordedDuration::toJson)
          .collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
      }
      return jsonObject;
    }

    private static PerformanceMeasure fromJson(Path performanceFile) throws IOException {
      JsonObject jsonObject = new Gson().fromJson(new String(Files.readAllBytes(performanceFile), UTF_8), JsonObject.class);
      return fromJson(jsonObject, null);
    }

    private static PerformanceMeasure fromJson(JsonObject jsonObject, @Nullable PerformanceMeasure parent) {
      String name = jsonObject.getAsJsonPrimitive("name").getAsString();
      PerformanceMeasure measure = parent != null ? parent.getOrCreateChild(name) :
        new PerformanceMeasure(null, name, System::nanoTime);
      measure.callsCount = jsonObject.getAsJsonPrimitive("calls").getAsLong();
      measure.totalDurationNanos = jsonObject.getAsJsonPrimitive("durationNanos").getAsLong();
      JsonArray children = jsonObject.getAsJsonArray("children");
      if (children != null) {
        children.forEach(jsonChild -> fromJson(jsonChild.getAsJsonObject(), measure));
      }
      return measure;
    }

    private static String jsonFormat(JsonObject jsonObject) {
      String json = new GsonBuilder()
        .setPrettyPrinting()
        .create()
        .toJson(jsonObject);
      // reduce the number of lines by inlining some of the properties
      return json
        .replaceAll("\n *+(\"(?:name|calls|durationNanos|children)\":)", " $1")
        .replaceAll("(\\d)\n *+\\}", "$1 }");
    }

  }

  @VisibleForTesting
  static void ensureParentDirectoryExists(Path path) throws IOException {
    Path parentDirectory = path.getParent();
    if (parentDirectory != null && !Files.isDirectory(parentDirectory)) {
      Files.createDirectory(parentDirectory);
    }
  }

}
