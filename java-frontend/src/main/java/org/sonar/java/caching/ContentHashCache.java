/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.java.SonarComponents;

public class ContentHashCache {

  private static final Logger LOG = LoggerFactory.getLogger(ContentHashCache.class);
  private static final String CONTENT_HASH_KEY = String.format("java:contentHash:%s:", FileHashingUtils.HASH_ALGORITHM);
  private static final String HASH_COMPUTE_FAIL_MSG = "Failed to compute content hash for file %s";

  private ReadCache readCache;
  private WriteCache writeCache;
  private final boolean enabled;

  public ContentHashCache(SonarComponents sonarComponents) {
    CacheContextImpl cacheContext = CacheContextImpl.of(sonarComponents);
    enabled = cacheContext.isCacheEnabled();

    var sensorContext = sonarComponents.context();
    if (enabled) {
      readCache = sensorContext.previousCache();
      writeCache = sensorContext.nextCache();
    }
  }

  public boolean hasSameHashCached(InputFile inputFile) {
    if (!enabled) {
      if (inputFile.status() == InputFile.Status.SAME) {
        LOG.trace("Cache is disabled. File status is: {}. File can be skipped.", inputFile.status());
        return true;
      }
      LOG.trace("Cache is disabled. File status is: {}. File can't be skipped.", inputFile.status());
      return false;
    }
    String cacheKey = getCacheKey(inputFile);
    try {
      LOG.trace("Reading cache for the file {}", inputFile.key());
      byte[] cachedHash = readCache.read(cacheKey).readAllBytes();
      byte[] fileHash = FileHashingUtils.inputFileContentHash(inputFile);
      boolean isHashEqual = MessageDigest.isEqual(fileHash, cachedHash);
      if (isHashEqual) {
        copyFromPrevious(inputFile);
      } else {
        writeToCache(inputFile);
      }
      return isHashEqual;
    } catch (IllegalArgumentException e) {
      LOG.trace(String.format("Could not find key %s in the cache", cacheKey));
      writeToCache(inputFile);
    } catch (IOException | NoSuchAlgorithmException e) {
      LOG.warn(String.format(HASH_COMPUTE_FAIL_MSG, inputFile.key()));
    }
    return false;
  }

  public boolean contains(InputFile inputFile) {
    if (!enabled) {
      LOG.trace("Cannot lookup cached hashes when the cache is disabled ({}).", inputFile.key());
      return false;
    }
    return readCache.contains(getCacheKey(inputFile));
  }

  public boolean writeToCache(InputFile inputFile) {
    if (!enabled) {
      LOG.trace("Cannot write hashes to the cache when the cache is disabled ({}).", inputFile.key());
      return false;
    }
    LOG.trace("Writing to the cache for file {}", inputFile.key());
    String cacheKey = getCacheKey(inputFile);
    try {
      writeCache.write(cacheKey, FileHashingUtils.inputFileContentHash(inputFile));
      return true;
    } catch (IllegalArgumentException e) {
      LOG.trace(String.format("Tried to write multiple times to cache key %s. Ignoring writes after the first.", cacheKey));
    } catch (IOException | NoSuchAlgorithmException e) {
      LOG.warn(String.format(HASH_COMPUTE_FAIL_MSG, inputFile.key()));
    }
    return false;
  }

  private void copyFromPrevious(InputFile inputFile) {
    LOG.trace("Copying cache from previous for file {}", inputFile.key());
    writeCache.copyFromPrevious(getCacheKey(inputFile));
  }

  private static String getCacheKey(InputFile inputFile) {
    return CONTENT_HASH_KEY + inputFile.key();
  }
}
