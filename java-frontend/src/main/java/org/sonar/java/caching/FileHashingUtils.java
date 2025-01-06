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
package org.sonar.java.caching;

import org.sonar.api.batch.fs.InputFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileHashingUtils {

  private FileHashingUtils() {
  }

  public static final String HASH_ALGORITHM = "MD5";

  public static byte[] inputFileContentHash(InputFile inputFile) throws IOException, NoSuchAlgorithmException {
    byte[] contentBytes = inputFile.contents().getBytes(StandardCharsets.UTF_8);
    MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
    return messageDigest.digest(contentBytes);
  }

  public static byte[] inputFileContentHash(String filepath) throws IOException, NoSuchAlgorithmException {
    File file = new File(filepath);
    String contents = new String(Files.readAllBytes(file.toPath()), UTF_8);
    MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
    return messageDigest.digest(contents.getBytes(UTF_8));
  }

}
