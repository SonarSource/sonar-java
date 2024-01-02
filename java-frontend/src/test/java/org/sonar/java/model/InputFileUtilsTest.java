/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


class InputFileUtilsTest {

  @Test
  void md5_hash_from_bytes() throws Exception {
    byte[] bytes = "content".getBytes(UTF_8);
    assertEquals("9a0364b9e99bb480dd25e1f0284c8555", InputFileUtils.hash(bytes, "MD5", 32));
    bytes = "363".getBytes(UTF_8);
    assertEquals("00411460f7c92d2124a67ea0f4cb5f85", InputFileUtils.hash(bytes, "MD5", 32));
  }

  @Test
  void md5_hash_from_input_file() throws Exception {
    InputFile inputFile = Mockito.mock(InputFile.class);
    Mockito.when(inputFile.contents()).thenReturn("abc");
    Mockito.when(inputFile.charset()).thenReturn(UTF_8);
    assertEquals("900150983cd24fb0d6963f7d28e17f72", InputFileUtils.md5Hash(inputFile));
  }

  @Test
  void md5_hash_from_invalid_input_file() throws Exception {
    InputFile inputFile = Mockito.mock(InputFile.class);
    Mockito.when(inputFile.contents()).thenThrow(new IOException("Boom!"));
    Mockito.when(inputFile.charset()).thenReturn(UTF_8);
    assertThatThrownBy(() -> InputFileUtils.md5Hash(inputFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("java.io.IOException: Boom!");
  }

  @Test
  void hash_using_invalid_algorithm() throws Exception {
    byte[] bytes = "363".getBytes(UTF_8);
    assertThatThrownBy(() -> InputFileUtils.hash(bytes, "invalid-algorithm", 32))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("invalid-algorithm not supported");
  }

}
