/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.helpers;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.java.caching.FileHashingUtils;
import org.sonar.java.checks.verifier.internal.InternalInputFile;
import org.sonar.java.checks.verifier.internal.InternalReadCache;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

public class HashCacheTestHelper {

  public static InputFile inputFileFromPath(String path) {
    return InternalInputFile
      .inputFile("", new File(path), InputFile.Status.SAME);
  }

  public static String contentHashKey(String path) {
    return contentHashKey(inputFileFromPath(path));
  }

  public static String contentHashKey(InputFile inputFile) {
    return "java:contentHash:MD5:" + inputFile.key();
  }

  public static ReadCache internalReadCacheFromFile(String path) throws NoSuchAlgorithmException, IOException {
    InputFile cachedFile = inputFileFromPath(path);
    byte[] cachedHash = FileHashingUtils.inputFileContentHash(cachedFile);
    InternalReadCache localReadCache = new InternalReadCache().put(contentHashKey(cachedFile), cachedHash);
    return localReadCache;
  }

  public static ReadCache internalReadCacheFromFiles(Collection<String> paths) throws NoSuchAlgorithmException, IOException {
    InternalReadCache localReadCache = new InternalReadCache();
    for (String path : paths) {
      InputFile cachedFile = inputFileFromPath(path);
      byte[] cachedHash = FileHashingUtils.inputFileContentHash(cachedFile);
      localReadCache.put(contentHashKey(cachedFile), cachedHash);
    }
    return localReadCache;
  }
  
  public static byte[] getSlightlyDifferentContentHash(String path) throws NoSuchAlgorithmException, IOException {
    InputFile cachedFile = inputFileFromPath(path);
    byte[] cachedHash = FileHashingUtils.inputFileContentHash(cachedFile);
    byte[] copy = Arrays.copyOf(cachedHash, cachedHash.length+1);
    copy[cachedHash.length] = 10;
    return copy;
  }

}
