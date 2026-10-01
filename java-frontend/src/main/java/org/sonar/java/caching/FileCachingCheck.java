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
import org.sonar.plugins.java.api.InputFileScannerContext;

/**
 * Per-file caching for checks that aggregate data across a module before reporting.
 * <p>
 * Such a check only sees the files that changed during an incremental analysis. To reason about the
 * whole module it must persist what it learned from each file, and restore it for the files that were
 * skipped. This interface owns that mechanics; implementors supply only the parts that are specific to
 * them — a key prefix, how to (de)serialize their per-file data, and how to restore it into their
 * module-level state.
 * <p>
 * It is a mixin: implement it alongside {@code IssuableSubscriptionVisitor}, {@code JavaFileScanner},
 * or any other scanner base class, and wire it up as
 * <ul>
 *   <li>{@code leaveFile} (or {@code scanFile}) → {@link #writeToCache(InputFileScannerContext, Object)}</li>
 *   <li>{@code scanWithoutParsing} → {@link #restoreFromCache(InputFileScannerContext)}</li>
 * </ul>
 * <p>
 * Cache problems are never fatal, and each degrades differently:
 * <ul>
 *   <li>an entry this check cannot read is trace-logged, and the file is parsed instead</li>
 *   <li>a missing entry does the same, silently</li>
 *   <li>a write colliding with an earlier one is trace-logged and dropped, keeping the earlier entry;
 *       the file had been parsed either way, so nothing is recomputed</li>
 *   <li>failing to carry an entry over is trace-logged and leaves the data restored: it only means
 *       another check sharing the entry has already copied it</li>
 * </ul>
 * An I/O failure while reading the cache is the only problem that propagates, and even that one is not
 * fatal by default: {@code VisitorsBridge} logs it and falls back to parsing the file, failing the
 * analysis only in fail-fast mode.
 * <p>
 * {@link org.sonar.java.serialization.JsonUtils} provides a versioned JSON envelope suitable for most
 * payloads; anything with structure is best described by a Gson {@code TypeAdapter}.
 *
 * @param <T> the data collected for a single file
 */
public interface FileCachingCheck<T> {

  /**
   * Prefix identifying this check's entries, conventionally {@code "java:<ruleKey>:"}. Two checks sharing
   * a prefix share their entries, which is supported: the second write of an analysis is ignored.
   */
  String cacheKeyPrefix();

  /**
   * Encodes the data collected for one file. The file itself needs no representation in the payload, since
   * an entry only ever holds one file's data and is stored under a key identifying it.
   */
  byte[] serialize(T data);

  /**
   * Rebuilds the data cached for one file. The file it belongs to is the one the entry was read for, which
   * {@link #restore(InputFileScannerContext, Object)} receives.
   *
   * @throws RuntimeException if the entry cannot be read, which is reported as a cache miss
   */
  T deserialize(byte[] data);

  /**
   * Restores data from the cache into this check's module-level state, as visiting the file would have.
   *
   * <p>The module-level state must be idempotent per input file. Both this method and the regular parsed-file
   * path must replace the contribution stored under {@code context.getInputFile().key()}, rather than append
   * to shared state.
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
      data = deserialize(bytes);
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
   * <p>A successful restore does not guarantee that the file will be skipped. Parsing is skipped only when
   * every scanner that cannot be skipped successfully scans without parsing. If another scanner returns
   * {@code false} or fails, this check will also visit the parsed file after restoring it. Implementors must
   * therefore keep module-level state idempotent per input file, so the parsed contribution replaces the
   * restored one.
   *
   * @return True if this check restored the file and does not require parsing, false if it requires parsing.
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
