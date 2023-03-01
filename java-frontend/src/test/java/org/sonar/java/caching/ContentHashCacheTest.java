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

import java.io.ByteArrayInputStream;
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
  void hasSameHashCached_returns_false_whenCacheIsDisabledAndInputFileStatusIsSame() {
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.status()).thenReturn(InputFile.Status.SAME);
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(false));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile1));
  }

  @Test
  void hasSameHashCached_returns_false_whenCacheIsDisabledAndInputFileStatusIsChanged() {
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.status()).thenReturn(InputFile.Status.CHANGED);
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(false));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile1));
  }

  @Test
  void hasSameHashCached_writesToCache_whenKeyIsNotPresent() {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(true));
    contentHashCache.hasSameHashCached(inputFile);
    Assertions.assertTrue(contentHashCache.writeToCache(inputFile));
  }

  @Test
  void hasSameHashCached_returns_false_whenContentHashFileIsNotSameAsOneInCache() {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    ReadCache readCache = mock(ReadCache.class);
    when(readCache.read("java:contentHash:MD5:" + inputFile.key())).thenReturn(new ByteArrayInputStream("Dummy content hash".getBytes()));
    when(readCache.contains("java:contentHash:MD5:" + inputFile.key())).thenReturn(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(writeCache);
    ContentHashCache contentHashCache = new ContentHashCache(sensorContext);
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile));
  }

  @Test
  void hasSameHashCached_returns_false_whenFileHashingUtilsThrowsException() throws IOException {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    ReadCache readCache = mock(ReadCache.class);
    // Mocking input file to throw IOException because
    // mocking static method requires mockito-inline, which currently breaks the tests.
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.key()).thenReturn("key");
    when(readCache.read("java:contentHash:MD5:" + inputFile1.key())).thenReturn(new ByteArrayInputStream("string".getBytes()));
    when(readCache.contains("java:contentHash:MD5:" + inputFile1.key())).thenReturn(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(writeCache);
    when(inputFile1.contents()).thenThrow(new IOException());
    ContentHashCache contentHashCache = new ContentHashCache(sensorContext);
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile1));
  }

  @Test
  void hasSameHashCached_returns_true_whenWriteCacheIsNull() throws IOException, NoSuchAlgorithmException {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    ReadCache readCache = mock(ReadCache.class);
    when(readCache.read("java:contentHash:MD5:" + inputFile.key())).thenReturn(new ByteArrayInputStream(FileHashingUtils.inputFileContentHash(inputFile)));
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(null);
    ContentHashCache contentHashCache = new ContentHashCache(sensorContext);
    Assertions.assertTrue(contentHashCache.hasSameHashCached(inputFile));
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

  @Test
  void writeToCache_returns_false_whenWriteCacheIsNull() {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    ReadCache readCache = mock(ReadCache.class);
    when(readCache.read("java:contentHash:MD5:" + inputFile.key())).thenReturn(new ByteArrayInputStream("Dummy content hash".getBytes()));
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(null);
    ContentHashCache contentHashCache = new ContentHashCache(sensorContext);
    Assertions.assertFalse(contentHashCache.writeToCache(inputFile));
  }

  @Test
  void writeToCache_returns_false_whenWritingToCacheThrowsException() throws IOException, NoSuchAlgorithmException {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setNextCache(writeCache);
    doThrow(new IllegalArgumentException()).when(writeCache).write("java:contentHash:MD5:" + inputFile.key(), FileHashingUtils.inputFileContentHash(file.getPath()));
    ContentHashCache contentHashCache = new ContentHashCache(sensorContext);
    Assertions.assertFalse(contentHashCache.writeToCache(inputFile));
  }

  @Test
  void writeToCache_returns_false_whenFileHashingUtilsThrowsException() throws IOException {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setNextCache(writeCache);
    // Mocking input file to throw IOException because
    // mocking static method requires mockito-inline, which currently breaks the tests.
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.key()).thenReturn("key");
    when(inputFile1.contents()).thenThrow(new IOException());

    ContentHashCache contentHashCache = new ContentHashCache(sensorContext);
    Assertions.assertFalse(contentHashCache.writeToCache(inputFile1));
  }

  private SensorContextTester getSensorContextTesterWithEmptyCache(boolean isCacheEnabled) {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(isCacheEnabled);
    ReadCache readCache = mock(ReadCache.class);
    when(readCache.read("java:contentHash:MD5:" + inputFile.key())).thenThrow(new IllegalArgumentException());
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(writeCache);

    return sensorContext;
  }

  private SensorContextTester getSensorContextTester() throws IOException, NoSuchAlgorithmException {
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    ReadCache readCache = mock(ReadCache.class);
    when(readCache.read("java:contentHash:MD5:" + inputFile.key())).thenReturn(new ByteArrayInputStream(FileHashingUtils.inputFileContentHash(inputFile)));
    when(readCache.contains("java:contentHash:MD5:" + inputFile.key())).thenReturn(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(writeCache);

    return sensorContext;
  }

}
