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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.testing.ThreadLocalLogTester;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.caching.JavaReadCache;
import org.sonar.plugins.java.api.caching.JavaWriteCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileCachingCheckTest {

  private static final String FILE_KEY = "module:src/main/java/org/Example.java";
  private static final String CACHE_KEY = "java:S9999:" + FILE_KEY;

  @RegisterExtension
  ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.TRACE);

  private JavaReadCache readCache;
  private JavaWriteCache writeCache;
  private InputFile inputFile;
  private TestCachingCheck check;

  @BeforeEach
  void setUp() {
    readCache = mock(JavaReadCache.class);
    writeCache = mock(JavaWriteCache.class);
    inputFile = mock(InputFile.class);
    when(inputFile.key()).thenReturn(FILE_KEY);
    check = new TestCachingCheck();
  }

  @Test
  void cacheKey_combines_prefix_and_file_key() {
    assertThat(check.cacheKey(context(true))).isEqualTo(CACHE_KEY);
  }

  @Test
  void writeToCache_stores_serialized_data() {
    check.writeToCache(context(true), List.of("a", "b"));

    verify(writeCache).write(CACHE_KEY, "a,b".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void writeToCache_does_nothing_when_caching_is_disabled() {
    check.writeToCache(context(false), List.of("a"));

    verify(writeCache, never()).write(anyString(), any(byte[].class));
  }

  @Test
  void writeToCache_ignores_a_second_write_under_the_same_key() {
    doThrow(new IllegalArgumentException("already present")).when(writeCache).write(anyString(), any(byte[].class));

    check.writeToCache(context(true), List.of("a"));

    assertThat(logTester.logs(Level.TRACE))
      .contains("Tried to write multiple times to cache key '" + CACHE_KEY + "'. Ignoring writes after the first.");
  }

  @Test
  void readFromCache_restores_data_and_carries_the_entry_over() {
    when(readCache.readBytes(CACHE_KEY)).thenReturn("a,b".getBytes(StandardCharsets.UTF_8));

    assertThat(check.readFromCache(context(true))).contains(List.of("a", "b"));
    verify(writeCache).copyFromPrevious(CACHE_KEY);
  }

  @Test
  void readFromCache_is_empty_on_a_cache_miss() {
    when(readCache.readBytes(CACHE_KEY)).thenReturn(null);

    assertThat(check.readFromCache(context(true))).isEmpty();
    verify(writeCache, never()).copyFromPrevious(anyString());
  }

  @Test
  void readFromCache_is_empty_and_keeps_no_entry_when_data_is_unreadable() {
    when(readCache.readBytes(CACHE_KEY)).thenReturn("boom".getBytes(StandardCharsets.UTF_8));

    assertThat(check.readFromCache(context(true))).isEmpty();
    verify(writeCache, never()).copyFromPrevious(anyString());
    assertThat(logTester.logs(Level.TRACE))
      .contains("Failed to deserialize cached data for '" + CACHE_KEY + "', will re-parse.");
  }

  @Test
  void readFromCache_still_restores_data_when_the_entry_was_already_carried_over() {
    when(readCache.readBytes(CACHE_KEY)).thenReturn("a".getBytes(StandardCharsets.UTF_8));
    doThrow(new IllegalArgumentException("already present")).when(writeCache).copyFromPrevious(anyString());

    assertThat(check.readFromCache(context(true))).contains(List.of("a"));
    assertThat(logTester.logs(Level.TRACE))
      .contains("Cache key '" + CACHE_KEY + "' was already carried over to the next analysis.");
  }

  @Test
  void deserialize_receives_the_file_being_restored() {
    when(readCache.readBytes(CACHE_KEY)).thenReturn("a".getBytes(StandardCharsets.UTF_8));

    check.readFromCache(context(true));

    assertThat(check.deserializedFor).isSameAs(inputFile);
  }

  @Test
  void restoreFromCache_merges_data_and_reports_a_hit() {
    when(readCache.readBytes(CACHE_KEY)).thenReturn("a,b".getBytes(StandardCharsets.UTF_8));

    assertThat(check.restoreFromCache(context(true))).isTrue();
    assertThat(check.moduleState).containsExactly("a", "b");
  }

  @Test
  void restoreFromCache_reports_a_miss_without_restoring_anything() {
    when(readCache.readBytes(CACHE_KEY)).thenReturn(null);

    assertThat(check.restoreFromCache(context(true))).isFalse();
    assertThat(check.moduleState).isEmpty();
  }

  @Test
  void round_trip_through_the_cache() {
    check.writeToCache(context(true), List.of("x", "y"));
    when(readCache.readBytes(CACHE_KEY)).thenReturn("x,y".getBytes(StandardCharsets.UTF_8));

    assertThat(check.restoreFromCache(context(true))).isTrue();
    assertThat(check.moduleState).containsExactly("x", "y");
  }

  private InputFileScannerContext context(boolean cacheEnabled) {
    var cacheContext = mock(CacheContext.class);
    when(cacheContext.isCacheEnabled()).thenReturn(cacheEnabled);
    when(cacheContext.getReadCache()).thenReturn(readCache);
    when(cacheContext.getWriteCache()).thenReturn(writeCache);
    var context = mock(InputFileScannerContext.class);
    when(context.getCacheContext()).thenReturn(cacheContext);
    when(context.getInputFile()).thenReturn(inputFile);
    return context;
  }

  private static class TestCachingCheck implements FileCachingCheck<List<String>> {

    private final List<String> moduleState = new ArrayList<>();
    private InputFile deserializedFor;

    @Override
    public String cacheKeyPrefix() {
      return "java:S9999:";
    }

    @Override
    public byte[] serialize(List<String> data) {
      return String.join(",", data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<String> deserialize(InputFile inputFile, byte[] data) {
      deserializedFor = inputFile;
      String content = new String(data, StandardCharsets.UTF_8);
      if (content.contains("boom")) {
        throw new IllegalArgumentException("unreadable entry");
      }
      return List.of(content.split(","));
    }

    @Override
    public void restore(InputFileScannerContext context, List<String> data) {
      moduleState.addAll(data);
    }
  }
}
