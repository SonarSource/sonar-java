/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.java.caching.FileHashingUtils;
import org.sonar.java.checks.verifier.internal.InternalInputFile;
import org.sonar.java.checks.verifier.internal.InternalReadCache;

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
    return new InternalReadCache().put(contentHashKey(cachedFile), cachedHash);
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
