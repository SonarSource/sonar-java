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
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.sonar.plugins.java.api.InputFileScannerContext;

/**
 * Shared per-file caching mechanics for {@link SpringContextModelGatherer}s: builds cache keys, writes serialized
 * data to the write cache, and reads/deserializes data from the read cache during incremental analyses.
 */
final class SpringContextCacheHelper {

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
   * <p>On successful deserialization, the entry is carried over to the write cache via {@code copyFromPrevious}
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

}
