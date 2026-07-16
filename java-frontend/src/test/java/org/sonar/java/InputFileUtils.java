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
package org.sonar.java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

class InputFileUtils {
  public static InputFile addFile(Path temp, String code, SensorContextTester context) throws IOException {
    Matcher matcher = Pattern.compile("(?:^|\\s)(?:class|interface|enum|record)\\s++(\\w++)").matcher(code);
    if (matcher.find()) {
      String className = matcher.group(1);
      InputFile.Type type = className.endsWith("Test") ? InputFile.Type.TEST : InputFile.Type.MAIN;
      File file = Files.createFile(temp.resolve(className + ".java")).toFile().getAbsoluteFile();
      return generateInputFile(code, context, file, type);
    } else {
      File file = Files.createFile(temp.resolve("Unnamed.java")).toFile().getAbsoluteFile();
      return generateInputFile(code, context, file, InputFile.Type.MAIN);
    }
  }

  @NotNull
  private static InputFile generateInputFile(String code, SensorContextTester context, File file, InputFile.Type type) throws IOException {
    Files.writeString(file.toPath(), code, StandardCharsets.UTF_8);
    InputFile defaultFile = TestUtils.inputFile(context.fileSystem().baseDir().getAbsolutePath(), file, type);
    context.fileSystem().add(defaultFile);
    return defaultFile;
  }
}
