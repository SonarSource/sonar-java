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
package org.sonar.java.model;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.sonar.api.batch.fs.InputFile;

public final class InputFileUtils {

  private InputFileUtils() {
    // utility class
  }

  public static String md5Hash(InputFile inputFile) {
    String contents;
    try {
      contents = inputFile.contents();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return hash(contents.getBytes(inputFile.charset()), "MD5", 32);
  }

  public static String hash(byte[] input, String algorithm, int expectedLength) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(algorithm + " not supported", e);
    }
    md.update(input);
    String suffix = new BigInteger(1,md.digest()).toString(16);
    String prefix = "0".repeat(expectedLength - suffix.length());
    return prefix + suffix;
  }

}
