/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.caching;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ContentHashCache {

  private static final Logger LOG = Loggers.get(ContentHashCache.class);
  private static final String CONTENT_HASH_KEY = String.format("java:contentHash:%s:", FileHashingUtils.HASH_ALGORITHM);
  private static final String HASH_COMPUTE_FAIL_MSG = "Failed to compute content hash for file %s";

  private ReadCache readCache;
  private WriteCache writeCache;
  private boolean enabled;

  public ContentHashCache(SensorContext context) {
    enabled = context.isCacheEnabled();
    if (enabled) {
      readCache = context.previousCache();
      writeCache = context.nextCache();
    }
  }

  public boolean hasSameHashCached(InputFile inputFile) {
    if (!enabled) {
      LOG.trace("Cannot read from cache when disabled");
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
      LOG.trace(String.format(HASH_COMPUTE_FAIL_MSG, inputFile.key()));
    }
    return false;
  }

  public boolean contains(InputFile inputFile) {
    if (!enabled) {
      LOG.trace("Cannot read from cache when disabled");
      return false;
    }
    return readCache.contains(getCacheKey(inputFile));
  }

  public boolean writeToCache(InputFile inputFile) {
    if (!enabled) {
      LOG.trace("Cannot write on cache when disabled");
      return false;
    }
    if (writeCache != null) {
      LOG.trace(String.format("Writing to the cache for file %s", inputFile.key()));
      String cacheKey = getCacheKey(inputFile);
      try {
        writeCache.write(cacheKey, FileHashingUtils.inputFileContentHash(inputFile));
        return true;
      } catch (IllegalArgumentException e) {
        LOG.error(String.format("Tried to write multiple times to cache key '%s'. Ignoring writes after the first.", cacheKey));
      } catch (IOException | NoSuchAlgorithmException e) {
        LOG.error(String.format(HASH_COMPUTE_FAIL_MSG, inputFile.key()));
      }
    }
    return false;
  }

  private void copyFromPrevious(InputFile inputFile) {
    if (writeCache != null) {
      LOG.trace("Coping cache from previous for file {}", inputFile.key());
      writeCache.copyFromPrevious(getCacheKey(inputFile));
    }
  }

  private static String getCacheKey(InputFile inputFile) {
    return CONTENT_HASH_KEY + inputFile.key();
  }
}
