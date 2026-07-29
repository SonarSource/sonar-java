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
package org.sonar.java.checks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Cache data structures and serialization logic for rules that accumulate per-file data
 * and decide whether to report issues only at end of analysis.
 *
 * <p>Serialization format:
 * <pre>
 * Line 0: totalCount|noEnumCount
 * Lines 1..N (one per issue):
 *   sl|sc|el|ec|b64(message)|b64(replacement)|hasImport[|importSL|importSC|importEL|importEC|b64(importReplacement)]
 * </pre>
 * Strings are Base64-encoded to safely handle newlines in import replacement text.
 */
class ConditionalRuleCacheUtils {

  private ConditionalRuleCacheUtils() {
    // utility class
  }

  /** Position and text of an import insertion edit — enough to reconstruct the JavaTextEdit without the AST. */
  record ImportEditData(int startLine, int startCol, int endLine, int endCol, String replacement) {}

  /**
   * Everything needed to report an issue and reconstruct its quick fix at endOfAnalysis without the AST.
   * The edit span equals the issue span, so it is not stored separately.
   * The import edit is null when the import was already present at scan time.
   */
  record CachedIssue(
    int startLine, int startCol, int endLine, int endCol,
    String message,
    String replacement,
    @Nullable ImportEditData importEdit
  ) {}

  /** All cache data for a single file. */
  record CachedFileData(int totalCount, int noEnumCount, List<CachedIssue> issues) {}

  static byte[] serialize(int totalCount, int noEnumCount, List<CachedIssue> issues) {
    Base64.Encoder enc = Base64.getEncoder();
    var sb = new StringBuilder();
    sb.append(totalCount).append('|').append(noEnumCount).append('\n');
    for (CachedIssue issue : issues) {
      sb.append(issue.startLine()).append('|')
        .append(issue.startCol()).append('|')
        .append(issue.endLine()).append('|')
        .append(issue.endCol()).append('|')
        .append(enc.encodeToString(issue.message().getBytes(StandardCharsets.UTF_8))).append('|')
        .append(enc.encodeToString(issue.replacement().getBytes(StandardCharsets.UTF_8))).append('|');
      if (issue.importEdit() != null) {
        ImportEditData ie = issue.importEdit();
        sb.append('1').append('|')
          .append(ie.startLine()).append('|').append(ie.startCol()).append('|')
          .append(ie.endLine()).append('|').append(ie.endCol()).append('|')
          .append(enc.encodeToString(ie.replacement().getBytes(StandardCharsets.UTF_8)));
      } else {
        sb.append('0');
      }
      sb.append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  static CachedFileData deserialize(byte[] data) {
    Base64.Decoder dec = Base64.getDecoder();
    String text = new String(data, StandardCharsets.UTF_8);
    String[] lines = text.split("\n", -1);
    if (lines.length == 0 || lines[0].isEmpty()) {
      return new CachedFileData(0, 0, List.of());
    }
    String[] header = lines[0].split("\\|", 2);
    int totalCount = Integer.parseInt(header[0]);
    int noEnumCount = Integer.parseInt(header[1]);
    List<CachedIssue> issues = new ArrayList<>();
    for (int i = 1; i < lines.length; i++) {
      if (lines[i].isEmpty()) {
        continue;
      }
      String[] parts = lines[i].split("\\|", -1);
      int sl = Integer.parseInt(parts[0]);
      int sc = Integer.parseInt(parts[1]);
      int el = Integer.parseInt(parts[2]);
      int ec = Integer.parseInt(parts[3]);
      String message = new String(dec.decode(parts[4]), StandardCharsets.UTF_8);
      String replacement = new String(dec.decode(parts[5]), StandardCharsets.UTF_8);
      boolean hasImport = "1".equals(parts[6]);
      ImportEditData importEdit = null;
      if (hasImport) {
        int isl = Integer.parseInt(parts[7]);
        int isc = Integer.parseInt(parts[8]);
        int iel = Integer.parseInt(parts[9]);
        int iec = Integer.parseInt(parts[10]);
        String importRepl = new String(dec.decode(parts[11]), StandardCharsets.UTF_8);
        importEdit = new ImportEditData(isl, isc, iel, iec, importRepl);
      }
      issues.add(new CachedIssue(sl, sc, el, ec, message, replacement, importEdit));
    }
    return new CachedFileData(totalCount, noEnumCount, issues);
  }
}
