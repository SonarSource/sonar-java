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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.TestUtils;
import org.sonar.java.checks.verifier.internal.InternalReadCache;
import org.sonar.java.checks.verifier.internal.InternalWriteCache;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.*;

class ContentHashCacheTest {

  private final File file = new File("src/test/files/api/JavaFileScannerContext.java");
  private final InputFile inputFile = TestUtils.inputFile(file.getAbsoluteFile().getAbsolutePath(), file, InputFile.Type.TEST);

  @Test
  void hasSameHashCached_returns_true_whenContentHashFileIsInReadCache() throws IOException, NoSuchAlgorithmException {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTester());
    Assertions.assertTrue(contentHashCache.hasSameHashCached(inputFile));
  }

  @Test
  void hasSameHashCached_returns_false_whenContentHashFileIsNotInReadCache() {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(true));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile));
  }

  @Test
  void hasSameHashCached_returns_false_whenCacheIsDisabled() {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(false));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile));
  }

  @Test
  void hasSameHashCached_writesToCache_whenKeyIsNotPresent() {
    ContentHashCache contentHashCache = spy(new ContentHashCache(getSensorContextTesterWithEmptyCache(true)));
    contentHashCache.hasSameHashCached(inputFile);
    verify(contentHashCache, times(1)).writeToCache(inputFile);
  }

  @Test
  void contains_returns_true_whenFileIsInCache() throws IOException, NoSuchAlgorithmException {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTester());
    Assertions.assertTrue(contentHashCache.contains(inputFile));
  }

  @Test
  void contains_returns_false_whenFileIsNotInCache() {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(true));
    Assertions.assertFalse(contentHashCache.contains(inputFile));
  }

  @Test
  void contains_returns_false_whenCacheIsDisabled() {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(false));
    Assertions.assertFalse(contentHashCache.contains(inputFile));
  }

  private SensorContextTester getSensorContextTesterWithEmptyCache(boolean isCacheEnabled) {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(isCacheEnabled);
    ReadCache readCache = new InternalReadCache();
    WriteCache writeCache = new InternalWriteCache().bind(readCache);
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(writeCache);

    return sensorContext;
  }

  private SensorContextTester getSensorContextTester() throws IOException, NoSuchAlgorithmException {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    ReadCache readCache = new InternalReadCache().put("java:contentHash:MD5:" + inputFile.key(), FileHashingUtils.inputFileContentHash(inputFile));
    WriteCache writeCache = new InternalWriteCache().bind(readCache);
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(writeCache);

    return sensorContext;
  }

}
