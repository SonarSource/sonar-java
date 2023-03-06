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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.TestUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentHashCacheTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final File file = new File("src/test/files/api/JavaFileScannerContext.java");
  private final InputFile inputFile = TestUtils.inputFile(file.getAbsoluteFile().getAbsolutePath(), file, InputFile.Type.TEST);

  @Test
  void hasSameHashCached_returns_true_when_content_hash_file_is_in_read_cache() throws IOException, NoSuchAlgorithmException {
    logTester.setLevel(LoggerLevel.TRACE);
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTester());
    Assertions.assertTrue(contentHashCache.hasSameHashCached(inputFile));

    List<String> logs = logTester.getLogs(LoggerLevel.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Reading cache for the file " + inputFile.key(),
        "Copying cache from previous for file " + inputFile.key());
  }

  @Test
  void hasSameHashCached_returns_false_when_content_hash_file_is_not_in_read_cache() {
    logTester.setLevel(LoggerLevel.TRACE);
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(true));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile));

    List<String> logs = logTester.getLogs(LoggerLevel.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Reading cache for the file " + inputFile.key(),
        "Writing to the cache for file " + inputFile.key());
  }

  @Test
  void hasSameHashCached_returns_false_when_cache_is_disabled_and_input_file_status_is_same() {
    logTester.setLevel(LoggerLevel.TRACE);
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.status()).thenReturn(InputFile.Status.SAME);
    when(inputFile1.key()).thenReturn("key");
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(false));
    Assertions.assertTrue(contentHashCache.hasSameHashCached(inputFile1));

    List<String> logs = logTester.getLogs(LoggerLevel.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Cache is disabled. File status is: " + inputFile1.status() + ". File can be skipped.");
  }

  @Test
  void hasSameHashCached_returns_false_cache_is_disabled_and_input_file_status_is_changed() {
    logTester.setLevel(LoggerLevel.TRACE);
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.status()).thenReturn(InputFile.Status.CHANGED);
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(false));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile1));

    List<String> logs = logTester.getLogs(LoggerLevel.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Cache is disabled. File status is: " + inputFile1.status() + ". File can't be skipped.");
  }

  @Test
  void hasSameHashCached_writesToCache_when_key_is_not_present() {
    logTester.setLevel(LoggerLevel.TRACE);
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(true));
    contentHashCache.hasSameHashCached(inputFile);
    Assertions.assertTrue(contentHashCache.writeToCache(inputFile));

    List<String> logs = logTester.getLogs(LoggerLevel.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Writing to the cache for file " + inputFile.key());
  }

  @Test
  void hasSameHashCached_returns_false_when_content_hash_file_is_not_same_as_one_in_cache() {
    logTester.setLevel(LoggerLevel.TRACE);
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

    List<String> logs = logTester.getLogs(LoggerLevel.TRACE).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Reading cache for the file " + inputFile.key(),
        "Writing to the cache for file " + inputFile.key());
  }

  @Test
  void hasSameHashCached_returns_false_when_FileHashingUtils_throws_exception() throws IOException {
    logTester.setLevel(LoggerLevel.WARN);
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

    List<String> logs = logTester.getLogs(LoggerLevel.WARN).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Failed to compute content hash for file " + inputFile1.key());
  }

  @Test
  void contains_returns_true_when_file_is_in_cache() throws IOException, NoSuchAlgorithmException {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTester());
    Assertions.assertTrue(contentHashCache.contains(inputFile));
  }

  @Test
  void contains_returns_false_when_file_is_not_in_cache() {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(true));
    Assertions.assertFalse(contentHashCache.contains(inputFile));
  }

  @Test
  void contains_returns_false_when_cache_is_disabled() {
    ContentHashCache contentHashCache = new ContentHashCache(getSensorContextTesterWithEmptyCache(false));
    Assertions.assertFalse(contentHashCache.contains(inputFile));
  }

  @Test
  void writeToCache_returns_false_when_writing_to_cache_throws_exception() throws IOException, NoSuchAlgorithmException {
    logTester.setLevel(LoggerLevel.WARN);
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setNextCache(writeCache);
    doThrow(new IllegalArgumentException()).when(writeCache).write("java:contentHash:MD5:" + inputFile.key(), FileHashingUtils.inputFileContentHash(file.getPath()));
    ContentHashCache contentHashCache = new ContentHashCache(sensorContext);
    Assertions.assertFalse(contentHashCache.writeToCache(inputFile));

    List<String> logs = logTester.getLogs(LoggerLevel.WARN).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Tried to write multiple times to cache key java:contentHash:MD5:" + inputFile.key() + ". Ignoring writes after the first.");
  }

  @Test
  void writeToCache_returns_false_when_FileHashingUtils_throws_exception() throws IOException {
    logTester.setLevel(LoggerLevel.WARN);
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

    List<String> logs = logTester.getLogs(LoggerLevel.WARN).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
    assertThat(logs).
      contains("Failed to compute content hash for file " + inputFile1.key());
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