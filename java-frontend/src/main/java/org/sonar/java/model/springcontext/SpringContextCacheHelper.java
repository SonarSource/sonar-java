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
package org.sonar.java.model.springcontext;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer.BeanData;
import org.sonar.java.model.springcontext.TypeToDependenciesIndex.InjectionPoint;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;

/**
 * Shared per-file caching mechanics for {@link SpringContextModelGatherer}s: builds cache keys, writes serialized
 * data to the write cache, and reads/deserializes data from the read cache during incremental analyses.
 *
 * <p>Also owns the concrete cache format for {@link BeanDefinitionGatherer}'s {@link BeanData}.
 */
final class SpringContextCacheHelper {

  private static final String BEAN_CACHE_KEY_PREFIX = "java:spring:bean-definitions:";
  private static final String BEAN_SEPARATOR = "\n";
  private static final String FIELD_SEPARATOR = "|";
  private static final String DEP_SEPARATOR = ",";
  private static final String DEP_KEY_VALUE_SEPARATOR = ":";
  private static final String DEP_NAMES_SEPARATOR = ";";
  private static final String DEP_LOCATION_SEPARATOR = "#";
  private static final String TYPE_HIERARCHY_SEPARATOR = ";";

  private SpringContextCacheHelper() {
  }

  /**
   * Builds the per-file cache key used to store/retrieve a gatherer's data for the file currently being scanned.
   *
   * @param cacheKeyPrefix prefix identifying the gatherer that owns the cache entry
   * @param context        context of the file the key is built for
   * @return the cache key, unique per gatherer and per file
   */
  static String cacheKey(String cacheKeyPrefix, InputFileScannerContext context) {
    return cacheKeyPrefix + context.getInputFile().key();
  }

  /**
   * Writes already-serialized data to the write cache under the given key. A second write under the same key
   * within the same analysis is silently ignored (only the first write for a given file is kept).
   *
   * @param context  context of the file being scanned, used to access the write cache
   * @param log      logger of the calling gatherer, used to trace ignored duplicate writes
   * @param cacheKey key to write the data under, as built by {@link #cacheKey}
   * @param data     serialized data to persist
   */
  static void writeToCache(InputFileScannerContext context, Logger log, String cacheKey, String data) {
    try {
      context.getCacheContext().getWriteCache().write(cacheKey, data.getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      log.trace("Tried to write multiple times to cache key '{}'. Ignoring writes after the first.", cacheKey);
    }
  }

  /**
   * Reads and deserializes data previously written under the given key during a prior analysis.
   *
   * On successful deserialization, the entry is carried over to the write cache via {@code copyFromPrevious}
   * so it remains available for the next incremental analysis. On deserialization failure, the entry is left out
   * of the write cache so the file is re-parsed and its cache entry rewritten.
   *
   * @param <T>          type of the deserialized data
   * @param context      context of the file being scanned, used to access the read and write caches
   * @param log          logger of the calling gatherer, used to trace deserialization failures
   * @param cacheKey     key to read the data from, as built by {@link #cacheKey}
   * @param deserializer function turning the raw cached content back into data; any {@link RuntimeException} it
   *                     throws is treated as a cache miss
   * @return the deserialized data, or {@link Optional#empty()} if there is no cache entry or it could not be
   *         deserialized
   */
  static <T> Optional<T> readFromCache(InputFileScannerContext context, Logger log, String cacheKey, Function<String, T> deserializer) {
    var bytes = context.getCacheContext().getReadCache().readBytes(cacheKey);
    if (bytes == null) {
      return Optional.empty();
    }
    String content = new String(bytes, StandardCharsets.UTF_8);
    try {
      T result = deserializer.apply(content);
      context.getCacheContext().getWriteCache().copyFromPrevious(cacheKey);
      return Optional.of(result);
    } catch (RuntimeException e) {
      log.trace("Failed to deserialize cached data for '{}', will re-parse.", cacheKey);
      return Optional.empty();
    }
  }

  /**
   * Serializes and writes this file's beans to the write cache, for reuse by {@link #readBeanDefinitionsFromCache}
   * during the next incremental analysis.
   *
   * Beans are serialized as newline-joined lines (one per bean, see {@link #serializeBean}), mirrored on read
   * by {@link #deserializeBean} via {@code String#lines}.
   *
   * @param context Context of the file being scanned, used to build the cache key and access the write cache
   * @param log     Logger of the calling gatherer, used to trace ignored duplicate writes
   * @param beans   The beans collected from this file
   */
  static void writeBeanDefinitionsToCache(JavaFileScannerContext context, Logger log, List<BeanData> beans) {
    var cacheKey = cacheKey(BEAN_CACHE_KEY_PREFIX, context);
    var data = beans.stream()
      .map(SpringContextCacheHelper::serializeBean)
      .collect(Collectors.joining(BEAN_SEPARATOR));
    writeToCache(context, log, cacheKey, data);
  }

  /**
   * Reads and deserializes {@link BeanData} previously written by {@link #writeBeanDefinitionsToCache} during a
   * prior analysis of the same file.
   *
   * A file with zero beans still has a cache entry (an empty string), handled explicitly here rather than
   * relying on {@code String#lines()} returning an empty stream for {@code ""}.
   *
   * @param ctx Context of the file being scanned, used to build the cache key and access the read/write caches
   * @param log Logger of the calling gatherer, used to trace deserialization failures
   * @return The file's beans, or {@link Optional#empty()} if there is no cache entry or it could not be deserialized
   */
  static Optional<List<BeanData>> readBeanDefinitionsFromCache(InputFileScannerContext ctx, Logger log) {
    var cacheKey = cacheKey(BEAN_CACHE_KEY_PREFIX, ctx);
    return readFromCache(ctx, log, cacheKey, content -> content.isEmpty()
      ? List.<BeanData>of()
      : content.lines().map(line -> deserializeBean(line, ctx.getInputFile())).toList());
  }

  /**
   * Serializes one bean into a single "|"-delimited line, reversed by {@link #deserializeBean}.
   *
   * Any string sourced from user code (bean name, {@code @Profile} expression, dependency type keys,
   * injection point names) is Base64-encoded first, since {@code |}, {@code :}, {@code ,}, {@code ;} or
   * {@code #} could otherwise appear in an identifier and be mistaken for a field/entry separator.
   *
   * @param bean The bean to serialize
   * @return The bean encoded as a single "|"-delimited line
   */
  private static String serializeBean(BeanData bean) {
    var deps = bean.dependencyInjectionPoints().entrySet().stream()
      .map(e -> Base64.getEncoder().encodeToString(e.getKey().getBytes(StandardCharsets.UTF_8))
        + DEP_KEY_VALUE_SEPARATOR
        + e.getValue().stream()
          .map(SpringContextCacheHelper::encodeInjectionPoint)
          .collect(Collectors.joining(DEP_NAMES_SEPARATOR)))
      .collect(Collectors.joining(DEP_SEPARATOR));
    var typeHierarchy = String.join(TYPE_HIERARCHY_SEPARATOR, bean.typeHierarchy());
    var span = bean.textSpan();
    var encodedName = Base64.getEncoder().encodeToString(bean.beanName().getBytes(StandardCharsets.UTF_8));
    var encodedProfiles = bean.profiles() != null
      ? Base64.getEncoder().encodeToString(bean.profiles().getBytes(StandardCharsets.UTF_8))
      : "";
    return String.join(FIELD_SEPARATOR,
      encodedName,
      bean.type(),
      bean.beanPackage(),
      span.startLine + ":" + span.startCharacter + ":" + span.endLine + ":" + span.endCharacter,
      Boolean.toString(bean.isPrimary()),
      encodedProfiles,
      deps,
      typeHierarchy);
  }

  /** Encodes one injection point (dependency name, location) as {@code <base64 name>#<span>}; see {@link #serializeBean} for why the name is encoded. */
  private static String encodeInjectionPoint(InjectionPoint point) {
    var span = point.location().mainLocation();
    return Base64.getEncoder().encodeToString(point.name().getBytes(StandardCharsets.UTF_8))
      + DEP_LOCATION_SEPARATOR
      + span.startLine + ":" + span.startCharacter + ":" + span.endLine + ":" + span.endCharacter;
  }

  /**
   * Reverse of {@link #serializeBean}: splits the "|"-delimited line back into a bean's fields, decoding
   * every value that was Base64-encoded on write.
   *
   * @param line      One "|"-delimited line, as produced by {@link #serializeBean}
   * @param inputFile The file the cache entry belongs to, attached to the deserialized bean and to each
   *                   of its injection points
   * @return The deserialized bean
   */
  private static BeanData deserializeBean(String line, InputFile inputFile) {
    // -1 keeps trailing empty fields (e.g. no dependencies/no type hierarchy) so the fixed field indices below stay aligned.
    String[] fields = line.split("\\" + FIELD_SEPARATOR, -1);
    String beanName = new String(Base64.getDecoder().decode(fields[0]), StandardCharsets.UTF_8);
    String type = fields[1];
    String beanPackage = fields[2];
    String[] spanParts = fields[3].split(":");
    var textSpan = new AnalyzerMessage.TextSpan(
      Integer.parseInt(spanParts[0]),
      Integer.parseInt(spanParts[1]),
      Integer.parseInt(spanParts[2]),
      Integer.parseInt(spanParts[3]));
    boolean isPrimary = Boolean.parseBoolean(fields[4]);
    String profiles = !fields[5].isEmpty()
      ? new String(Base64.getDecoder().decode(fields[5]), StandardCharsets.UTF_8)
      : null;
    Map<String, Set<InjectionPoint>> injectionPoints = new LinkedHashMap<>();
    if (!fields[6].isEmpty()) {
      for (String entry : fields[6].split(DEP_SEPARATOR)) {
        // indexOf is safe: Base64 output never contains ':', so the first ':' is unambiguously the key/value boundary.
        int idx = entry.indexOf(DEP_KEY_VALUE_SEPARATOR);
        String typeFqn = new String(Base64.getDecoder().decode(entry.substring(0, idx)), StandardCharsets.UTF_8);
        Set<InjectionPoint> points = Arrays.stream(entry.substring(idx + 1).split(DEP_NAMES_SEPARATOR))
          .map(token -> decodeInjectionPoint(token, inputFile))
          .collect(Collectors.toCollection(LinkedHashSet::new));
        injectionPoints.put(typeFqn, points);
      }
    }
    Map<String, Set<String>> deps = BeanDefinitionGatherer.toNameMap(injectionPoints);
    Set<String> typeHierarchy = !fields[7].isEmpty()
      ? new LinkedHashSet<>(List.of(fields[7].split(TYPE_HIERARCHY_SEPARATOR)))
      : new LinkedHashSet<>();
    return new BeanData(beanName, type, beanPackage, inputFile, textSpan, isPrimary, profiles, deps, injectionPoints, typeHierarchy);
  }

  /** Reverse of {@link #encodeInjectionPoint}. */
  private static InjectionPoint decodeInjectionPoint(String token, InputFile inputFile) {
    int idx = token.indexOf(DEP_LOCATION_SEPARATOR);
    String name = new String(Base64.getDecoder().decode(token.substring(0, idx)), StandardCharsets.UTF_8);
    String[] spanParts = token.substring(idx + 1).split(":");
    var span = new AnalyzerMessage.TextSpan(
      Integer.parseInt(spanParts[0]),
      Integer.parseInt(spanParts[1]),
      Integer.parseInt(spanParts[2]),
      Integer.parseInt(spanParts[3]));
    return new InjectionPoint(name, new BeanLocation(inputFile, span));
  }

}
