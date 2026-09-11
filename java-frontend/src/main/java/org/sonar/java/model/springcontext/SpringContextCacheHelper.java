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

import com.google.gson.JsonElement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer.BeanData;
import org.sonar.java.serialization.BeanDataTypeAdapter;
import org.sonar.java.serialization.InjectionPointTypeAdapter;
import org.sonar.java.serialization.TextSpanTypeAdapter;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.sonar.java.serialization.JsonUtils.BEANS;
import static org.sonar.java.serialization.JsonUtils.PACKAGES;
import static org.sonar.java.serialization.JsonUtils.deserializeStrings;
import static org.sonar.java.serialization.JsonUtils.parseDocument;
import static org.sonar.java.serialization.JsonUtils.requiredArray;
import static org.sonar.java.serialization.JsonUtils.writeDocument;
import static org.sonar.java.serialization.JsonUtils.writeStrings;

/**
 * Shared per-file caching mechanics for Spring context gatherers.
 *
 * <p>Every cache entry is a JSON object carrying a {@code version} field, currently
 * {@value #CACHE_FORMAT_VERSION}. The version is shared by all gatherers: bumping it whenever any entry's
 * shape changes invalidates all previously cached entries, which are then recomputed on the next analysis.
 *
 * <p>This class only owns that versioned envelope. The shape of the beans it wraps is defined by
 * {@link BeanDataTypeAdapter}, {@link InjectionPointTypeAdapter} and {@link TextSpanTypeAdapter}.
 */
final class SpringContextCacheHelper {

  private static final int CACHE_FORMAT_VERSION = 1;
  private static final String BEAN_CACHE_KEY_PREFIX = "java:spring:bean-definitions:";
  private static final String COMPONENT_SCAN_CACHE_KEY_PREFIX = "java:spring:component-scan-packages:";

  private SpringContextCacheHelper() {
  }

  /**
   * Builds the per-file cache key used to store/retrieve a gatherer's data for the file currently being scanned.
   */
  private static String cacheKey(String cacheKeyPrefix, InputFileScannerContext context) {
    return cacheKeyPrefix + context.getInputFile().key();
  }

  /**
   * Writes a serialized entry to the write cache. A second write under the same key within the same analysis
   * is silently ignored: only the first write for a given file is kept.
   */
  private static void writeToCache(InputFileScannerContext context, Logger log, String cacheKey, String data) {
    try {
      context.getCacheContext().getWriteCache().write(cacheKey, data.getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      log.trace("Tried to write multiple times to cache key '{}'. Ignoring writes after the first.", cacheKey);
    }
  }

  /**
   * Reads and deserializes an entry written during a prior analysis.
   *
   * <p>Any {@link RuntimeException} thrown by {@code deserializer} is treated as a cache miss. The entry is
   * carried over to the write cache via {@code copyFromPrevious} only on successful deserialization, so a
   * corrupt entry is deliberately dropped and rewritten once the file has been re-parsed.
   *
   * @return The deserialized data, or {@link Optional#empty()} if there is no entry or it could not be read.
   */
  private static <T> Optional<T> readFromCache(InputFileScannerContext context, Logger log, String cacheKey, Function<String, T> deserializer) {
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
      log.trace("Failed to deserialize cached data for '{}', will re-parse.", cacheKey, e);
      return Optional.empty();
    }
  }

  /**
   * Serializes and writes this file's beans to the write cache, for reuse by {@link #readBeanDefinitionsFromCache}
   * during the next incremental analysis.
   *
   * @param context Context of the file being scanned, used to build the cache key and access the write cache.
   * @param log     Logger of the calling gatherer, used to trace ignored duplicate writes.
   * @param beans   The beans collected from this file.
   */
  static void writeBeanDefinitionsToCache(JavaFileScannerContext context, Logger log, List<BeanData> beans) {
    var adapter = new BeanDataTypeAdapter(context.getInputFile());
    String document = writeDocument(CACHE_FORMAT_VERSION, out -> {
      out.name(BEANS);
      out.beginArray();
      for (BeanData bean : beans) {
        adapter.write(out, bean);
      }
      out.endArray();
    });
    writeToCache(context, log, cacheKey(BEAN_CACHE_KEY_PREFIX, context), document);
  }

  /**
   * Restores bean definitions from their JSON representation, associating every location with the current file.
   *
   * @param context Context of the file being scanned, used to build the cache key and access the read cache.
   * @param log     Logger of the calling gatherer, used to trace failed accesses to cached data.
   */
  static Optional<List<BeanData>> readBeanDefinitionsFromCache(InputFileScannerContext context, Logger log) {
    var cacheKey = cacheKey(BEAN_CACHE_KEY_PREFIX, context);
    var adapter = new BeanDataTypeAdapter(context.getInputFile());
    return readFromCache(context, log, cacheKey, content -> deserializeBeans(content, adapter));
  }

  /**
   * Serializes and writes the packages covered by component scan annotations found in this file to the write cache, for reuse by
   * {@link #readComponentScanPackagesFromCache} during the next incremental analysis.
   *
   * @param context  Context of the file being scanned, used to build the cache key and access the write cache.
   * @param log      Logger of the calling gatherer, used to trace ignored duplicate writes.
   * @param packages The package names collected from this file.
   */
  static void writeComponentScanPackagesToCache(InputFileScannerContext context, Logger log, Collection<String> packages) {
    String document = writeDocument(CACHE_FORMAT_VERSION, out -> {
      out.name(PACKAGES);
      writeStrings(out, packages);
    });
    writeToCache(context, log, cacheKey(COMPONENT_SCAN_CACHE_KEY_PREFIX, context), document);
  }

  /**
   * Restores package names from their JSON representation.
   *
   * @param context Context of the file being scanned, used to build the cache key and access the read cache.
   * @param log     Logger of the calling gatherer, used to trace failed accesses to cached data.
   */
  static Optional<List<String>> readComponentScanPackagesFromCache(InputFileScannerContext context, Logger log) {
    var cacheKey = cacheKey(COMPONENT_SCAN_CACHE_KEY_PREFIX, context);
    return readFromCache(context, log, cacheKey, SpringContextCacheHelper::deserializePackages);
  }

  private static List<BeanData> deserializeBeans(String content, BeanDataTypeAdapter adapter) {
    var beans = requiredArray(parseDocument(content, CACHE_FORMAT_VERSION), BEANS);
    List<BeanData> result = new ArrayList<>();
    for (JsonElement bean : beans) {
      result.add(adapter.fromJsonTree(bean));
    }
    return result;
  }

  private static List<String> deserializePackages(String content) {
    return List.copyOf(deserializeStrings(requiredArray(parseDocument(content, CACHE_FORMAT_VERSION), PACKAGES)));
  }
}
