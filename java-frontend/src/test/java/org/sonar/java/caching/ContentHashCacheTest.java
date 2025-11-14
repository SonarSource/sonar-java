/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.java.SonarComponents;
import org.sonar.java.TestUtils;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.caching.SonarLintCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentHashCacheTest {

  @RegisterExtension
  ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.DEBUG);

  private final File file = new File("src/test/files/api/JavaFileScannerContext.java");
  private final InputFile inputFile = TestUtils.inputFile(file.getAbsoluteFile().getAbsolutePath(), file, InputFile.Type.TEST);

  @Test
  void hasSameHashCached_returns_true_when_content_hash_file_is_in_read_cache() throws IOException, NoSuchAlgorithmException {
    logTester.setLevel(Level.TRACE);
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSonarComponentsTester()));
    Assertions.assertTrue(contentHashCache.hasSameHashCached(inputFile));

    assertThat(logTester.logs(Level.TRACE)).
      contains("Reading cache for the file " + inputFile.key(),
        "Copying cache from previous for file " + inputFile.key());
  }

  @Test
  void hasSameHashCached_returns_false_when_content_hash_file_is_not_in_read_cache_with_proper_logging() {
    String[] messages = new String[]{
      "Could not find key java:contentHash:MD5:" + inputFile.key() + " in the cache",
      "Reading cache for the file " + inputFile.key(),
      "Writing to the cache for file " + inputFile.key()
    };
    assertThat(hasSameHashCached_returns_false_when_content_hash_file_is_not_in_read_cache(Level.TRACE)).contains(messages);
    assertThat(hasSameHashCached_returns_false_when_content_hash_file_is_not_in_read_cache(Level.WARN)).doesNotContain(messages);
  }

  private List<String> hasSameHashCached_returns_false_when_content_hash_file_is_not_in_read_cache(Level level) {
    logTester.setLevel(level);
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSensorContextTesterWithEmptyCache(true)));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile));
    return logTester.logs(level);
  }

  @Test
  void hasSameHashCached_returns_false_when_cache_is_disabled_and_input_file_status_is_same() {
    logTester.setLevel(Level.TRACE);
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.status()).thenReturn(InputFile.Status.SAME);
    when(inputFile1.key()).thenReturn("key");
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSensorContextTesterWithEmptyCache(false)));
    Assertions.assertTrue(contentHashCache.hasSameHashCached(inputFile1));

    assertThat(logTester.logs(Level.TRACE)).
      contains("Cache is disabled. File status is: " + inputFile1.status() + ". File can be skipped.");
  }

  @Test
  void hasSameHashCached_returns_false_cache_is_disabled_and_input_file_status_is_changed() {
    logTester.setLevel(Level.TRACE);
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.status()).thenReturn(InputFile.Status.CHANGED);
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSensorContextTesterWithEmptyCache(false)));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile1));

    assertThat(logTester.logs(Level.TRACE)).
      contains("Cache is disabled. File status is: " + inputFile1.status() + ". File can't be skipped.");
  }

  @Test
  void hasSameHashCached_writesToCache_when_key_is_not_present() {
    logTester.setLevel(Level.TRACE);
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSensorContextTesterWithEmptyCache(true)));
    contentHashCache.hasSameHashCached(inputFile);
    Assertions.assertTrue(contentHashCache.writeToCache(inputFile));

    assertThat(logTester.logs(Level.TRACE)).
      contains("Writing to the cache for file " + inputFile.key());
  }

  @Test
  void hasSameHashCached_returns_false_when_content_hash_file_is_not_same_as_one_in_cache() {
    logTester.setLevel(Level.TRACE);
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    ReadCache readCache = mock(ReadCache.class);
    when(readCache.read("java:contentHash:MD5:" + inputFile.key())).thenReturn(new ByteArrayInputStream("Dummy content hash".getBytes()));
    when(readCache.contains("java:contentHash:MD5:" + inputFile.key())).thenReturn(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setPreviousCache(readCache);
    sensorContext.setNextCache(writeCache);
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(sensorContext));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile));

    assertThat(logTester.logs(Level.TRACE)).
      contains("Reading cache for the file " + inputFile.key(),
        "Writing to the cache for file " + inputFile.key());
  }

  @Test
  void hasSameHashCached_returns_false_when_FileHashingUtils_throws_exception() throws IOException {
    logTester.setLevel(Level.WARN);
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
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(sensorContext));
    Assertions.assertFalse(contentHashCache.hasSameHashCached(inputFile1));

    assertThat(logTester.logs(Level.WARN)).
      contains("Failed to compute content hash for file " + inputFile1.key());
  }

  @Test
  void contains_returns_true_when_file_is_in_cache() throws IOException, NoSuchAlgorithmException {
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSonarComponentsTester()));
    Assertions.assertTrue(contentHashCache.contains(inputFile));
  }

  @Test
  void contains_returns_false_when_file_is_not_in_cache() {
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSensorContextTesterWithEmptyCache(true)));
    Assertions.assertFalse(contentHashCache.contains(inputFile));
  }

  @Test
  void contains_returns_false_when_cache_is_disabled() {
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(getSensorContextTesterWithEmptyCache(false)));
    Assertions.assertFalse(contentHashCache.contains(inputFile));
  }

  @Test
  void writeToCache_returns_false_when_writing_to_cache_throws_exception_with_proper_logging() throws IOException,
    NoSuchAlgorithmException {
    String message = "Tried to write multiple times to cache key java:contentHash:MD5:" + inputFile.key() + ". Ignoring writes after the " +
      "first.";
    assertThat(writeToCache_returns_false_when_writing_to_cache_throws_exception(Level.TRACE)).contains(message);
    assertThat(writeToCache_returns_false_when_writing_to_cache_throws_exception(Level.WARN)).doesNotContain(message);
  }

  private List<String> writeToCache_returns_false_when_writing_to_cache_throws_exception(Level level) throws IOException,
    NoSuchAlgorithmException {
    logTester.setLevel(level);
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setNextCache(writeCache);
    doThrow(new IllegalArgumentException()).when(writeCache).write("java:contentHash:MD5:" + inputFile.key(),
      FileHashingUtils.inputFileContentHash(file.getPath()));
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(sensorContext));
    Assertions.assertFalse(contentHashCache.writeToCache(inputFile));
    return logTester.logs(level);
  }

  @Test
  void writeToCache_returns_false_when_FileHashingUtils_throws_exception() throws IOException {
    logTester.setLevel(Level.WARN);
    SensorContextTester sensorContext = SensorContextTester.create(file.getAbsoluteFile());
    sensorContext.setCacheEnabled(true);
    WriteCache writeCache = mock(WriteCache.class);
    sensorContext.setNextCache(writeCache);
    // Mocking input file to throw IOException because
    // mocking static method requires mockito-inline, which currently breaks the tests.
    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.key()).thenReturn("key");
    when(inputFile1.contents()).thenThrow(new IOException());
    ContentHashCache contentHashCache = new ContentHashCache(mockSonarComponents(sensorContext));
    Assertions.assertFalse(contentHashCache.writeToCache(inputFile1));

    assertThat(logTester.logs(Level.WARN)).
      contains("Failed to compute content hash for file " + inputFile1.key());
  }

  @Test
  void should_not_enable_content_hash_cache_when_using_sonarlint_cache() {
    logTester.setLevel(Level.TRACE);

    var sensorContext = getSensorContextTesterWithEmptyCache(true);
    var sonarLintCache = mock(SonarLintCache.class);

    var sonarComponents = mockSonarComponents(sensorContext);
    doReturn(sonarLintCache).when(sonarComponents).sonarLintCache();

    var contentHashCache = new ContentHashCache(sonarComponents);

    InputFile inputFile1 = mock(InputFile.class);
    assertThat(contentHashCache.contains(inputFile1)).isFalse();

    assertThat(logTester.logs(Level.TRACE)).
      contains("Cannot lookup cached hashes when the cache is disabled (null).");
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

  private SensorContextTester getSonarComponentsTester() throws IOException, NoSuchAlgorithmException {
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

  private static SonarComponents mockSonarComponents(SensorContext sensorContext) {
    var sonarComponents = mock(SonarComponents.class);
    doReturn(sensorContext).when(sonarComponents).context();

    return sonarComponents;
  }
}
