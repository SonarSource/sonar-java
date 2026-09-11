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
package org.sonar.java.caching;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.InputFileScannerContext;

/**
 * Per-file caching for checks that aggregate data across a module before reporting.
 * <p>
 * Such a check only sees the files that changed during an incremental analysis. To reason about the
 * whole module it must persist what it learned from each file, and restore it for the files that were
 * skipped. This interface owns that mechanics; implementors supply only the parts that are specific to
 * them — a key prefix, how to (de)serialize their per-file data, and how to merge it back into their
 * module-level state.
 * <p>
 * It is a mixin: implement it alongside {@code IssuableSubscriptionVisitor}, {@code JavaFileScanner},
 * or any other scanner base class, and wire it up as
 * <ul>
 *   <li>{@code leaveFile} (or {@code scanFile}) → {@link #writeToCache(InputFileScannerContext, Object)}</li>
 *   <li>{@code scanWithoutParsing} → {@link #restoreFromCache(InputFileScannerContext)}</li>
 * </ul>
 * <p>
 * Cache problems are never fatal, with one exception: a missing entry, an entry this check cannot
 * read, or a write that collides with an earlier one are all trace-logged and degrade to re-parsing the
 * file, but an I/O failure while reading the cache still aborts the analysis.
 * <p>
 * {@link JsonCacheFormat} provides a versioned JSON encoding suitable for most payloads.
 *
 * @param <T> the data collected for a single file
 */
public interface FileCachingCheck<T> {

  /**
   * Prefix identifying this check's entries, conventionally {@code "java:<ruleKey>:"}. Two checks sharing
   * a prefix share their entries, which is supported: the second write of an analysis is ignored.
   */
  String cacheKeyPrefix();

  byte[] serialize(T data);

  /**
   * Rebuilds the data cached for one file.
   *
   * @param inputFile the file the entry belongs to <em>in the current analysis</em>; source locations
   *                  restored from the entry must be anchored to it rather than to any file recorded when it was written
   * @throws RuntimeException if the entry cannot be read, which is reported as a cache miss
   */
  T deserialize(InputFile inputFile, byte[] data);

  /**
   * Merges data restored from the cache into this check's module-level state, as visiting the file would have.
   *
   * @param context context of the file the data was restored for, for checks that report against it later
   */
  void restore(InputFileScannerContext context, T data);

  default String cacheKey(InputFileScannerContext context) {
    return cacheKeyPrefix() + context.getInputFile().key();
  }

  /**
   * Stores the data collected for the file being scanned. Does nothing when caching is disabled.
   */
  default void writeToCache(InputFileScannerContext context, T data) {
    var cacheContext = context.getCacheContext();
    if (!cacheContext.isCacheEnabled()) {
      return;
    }
    var cacheKey = cacheKey(context);
    var bytes = serialize(data);
    try {
      cacheContext.getWriteCache().write(cacheKey, bytes);
    } catch (IllegalArgumentException e) {
      logger().trace("Tried to write multiple times to cache key '{}'. Ignoring writes after the first.", cacheKey);
    }
  }

  /**
   * Reads back the entry written for this file during a previous analysis, and carries it over to the
   * next one.
   *
   * <p>The entry is carried over only once it has been read successfully, so an unreadable one is
   * dropped and rewritten after the file has been re-parsed. Failing to carry it over is not treated as
   * a miss: it means another check already copied a shared entry, which is exactly the desired state.
   *
   * @return the restored data, or empty if there is no entry or this check cannot read it
   */
  default Optional<T> readFromCache(InputFileScannerContext context) {
    var cacheContext = context.getCacheContext();
    var cacheKey = cacheKey(context);
    var bytes = cacheContext.getReadCache().readBytes(cacheKey);
    if (bytes == null) {
      return Optional.empty();
    }
    T data;
    try {
      data = deserialize(context.getInputFile(), bytes);
    } catch (RuntimeException e) {
      logger().trace("Failed to deserialize cached data for '{}', will re-parse.", cacheKey, e);
      return Optional.empty();
    }
    try {
      cacheContext.getWriteCache().copyFromPrevious(cacheKey);
    } catch (IllegalArgumentException e) {
      logger().trace("Cache key '{}' was already carried over to the next analysis.", cacheKey);
    }
    return Optional.of(data);
  }

  /**
   * Restores this file's contribution from the cache, so that it does not need to be parsed.
   *
   * @return true if the file was restored and can be skipped, false if it must be parsed
   */
  default boolean restoreFromCache(InputFileScannerContext context) {
    return readFromCache(context).map(data -> {
      restore(context, data);
      return true;
    }).orElse(false);
  }

  private Logger logger() {
    return LoggerFactory.getLogger(getClass());
  }
}
