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

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonarsource.api.sonarlint.SonarLintSide;

@ScannerSide
@SonarLintSide
public class SemanticReportScanner implements JavaFileScanner {

  private final Map<Path, IdentifierCounts> files = new TreeMap<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    IdentifierCounts counts = new IdentifierCounts();
    context.getTree().accept(new BaseTreeVisitor() {
      @Override
      public void visitIdentifier(IdentifierTree tree) {
        counts.total++;
        if (tree.symbol().isUnknown()) {
          counts.unknown++;
        }
        super.visitIdentifier(tree);
      }
    });
    files.put(normalizePath(Path.of(context.getInputFile().uri())), counts);
  }

  public void writeReport(Path reportPath, Path projectRoot) {
    Path normalizedProjectRoot = normalizePath(projectRoot);
    int total = files.values().stream().mapToInt(counts -> counts.total).sum();
    int unknown = files.values().stream().mapToInt(counts -> counts.unknown).sum();
    try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8))) {
      writer.setIndent("  ");
      writer.beginObject();
      writer.name("totalNumberOfIdentifier").value(total);
      writer.name("totalNumberOfUnknownIdentifier").value(unknown);
      writer.name("globalPercentageOfUnknownIdentifier").value(percentage(unknown, total));
      writer.name("files").beginArray();
      for (Map.Entry<Path, IdentifierCounts> file : files.entrySet()) {
        IdentifierCounts counts = file.getValue();
        writer.beginObject();
        writer.name("path").value(normalizedProjectRoot.relativize(file.getKey()).toString().replace('\\', '/'));
        writer.name("numberOfIdentifier").value(counts.total);
        writer.name("numberOfUnknownIdentifier").value(counts.unknown);
        writer.name("percentageOfUnknownIdentifier").value(percentage(counts.unknown, counts.total));
        writer.endObject();
      }
      writer.endArray();
      writer.endObject();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write semantic report to " + reportPath, e);
    }
  }

  private static Path normalizePath(Path path) {
    try {
      return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      return path.toAbsolutePath().normalize();
    }
  }

  private static BigDecimal percentage(int unknown, int total) {
    if (total == 0) {
      return BigDecimal.ZERO.setScale(3);
    }
    return BigDecimal.valueOf(unknown).multiply(BigDecimal.valueOf(100))
      .divide(BigDecimal.valueOf(total), 3, RoundingMode.HALF_UP);
  }

  private static class IdentifierCounts {
    int total;
    int unknown;
  }
}
