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
package org.sonar.java.checks.verifier.internal;

import org.junit.jupiter.api.Test;
import org.sonar.java.caching.JavaReadCacheImpl;
import org.sonar.java.caching.JavaWriteCacheImpl;

import static org.assertj.core.api.Assertions.assertThat;

class InternalCacheContextTest {
  @Test
  void constructor_sets_fields_properly() {
    JavaReadCacheImpl readCache = new JavaReadCacheImpl(new InternalReadCache());
    JavaWriteCacheImpl writeCache = new JavaWriteCacheImpl(new InternalWriteCache());
    InternalCacheContext internalCacheContext = new InternalCacheContext(
      true,
      readCache,
      writeCache
    );
    assertThat(internalCacheContext.isCacheEnabled()).isTrue();
    assertThat(internalCacheContext.getReadCache()).isEqualTo(readCache);
    assertThat(internalCacheContext.getWriteCache()).isEqualTo(writeCache);
  }

  @Test
  void test_equality() {
    JavaReadCacheImpl readCache = new JavaReadCacheImpl(new InternalReadCache());
    JavaWriteCacheImpl writeCache = new JavaWriteCacheImpl(new InternalWriteCache());
    InternalCacheContext a = new InternalCacheContext(true, readCache, writeCache);
    InternalCacheContext b = new InternalCacheContext(true, readCache, writeCache);
    assertThat(a)
      .isEqualTo(a)
      .isEqualTo(b)
      .hasSameHashCodeAs(b)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isNotEqualTo(new InternalCacheContext(false, readCache, writeCache))
      .isNotEqualTo(new InternalCacheContext(true, null, writeCache))
      .isNotEqualTo(new InternalCacheContext(true, readCache, null));
  }
}
