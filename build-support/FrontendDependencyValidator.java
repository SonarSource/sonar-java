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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FrontendDependencyValidator {
  private FrontendDependencyValidator() {
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expected the packaged plugin JAR path");
    }
    try (var input = Files.newInputStream(Path.of(args[0]))) {
      inspect(input, args[0]);
    }
    System.out.println("Packaged frontend contains no ECJ or JDT core classes");
  }

  private static void inspect(InputStream input, String path) throws IOException {
    try (var zip = new ZipInputStream(input)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        String location = path + "!/" + name;
        if (name.contains("jdt-package") || name.contains("org.eclipse.jdt.core") || name.matches(".*(?:^|/)ecj[-./].*")) {
          throw new IllegalStateException("Forbidden compiler artifact: " + location);
        }
        if (name.endsWith(".class") && name.contains("org/eclipse/jdt/") && !name.contains("org/eclipse/jdt/annotation/")) {
          throw new IllegalStateException("Forbidden compiler class: " + location);
        }
        if (name.endsWith(".jar")) {
          inspect(new ByteArrayInputStream(zip.readAllBytes()), location);
        } else if (name.endsWith(".class")) {
          String bytes = new String(zip.readAllBytes(), StandardCharsets.ISO_8859_1);
          for (String prefix : java.util.List.of("org/eclipse/jdt/core/", "org/eclipse/jdt/internal/", "org.eclipse.jdt.core.", "org.eclipse.jdt.internal.")) {
            if (bytes.contains(prefix)) {
              throw new IllegalStateException("Forbidden compiler reference: " + location);
            }
          }
        }
      }
    }
  }
}
