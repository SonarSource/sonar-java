/*
 * SonarQube Java
 * Copyright (C) 2024-2024 SonarSource SA
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
package org.sonar.java.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.check.Rule;
import org.sonar.java.GeneratedCheckList;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.plugins.java.api.JavaFileScanner;

public class QuickFixTestUtils {

  public static final List<String> RULE_KEYS_IMPLEMENTING_QUICKFIXES;
  public static final List<String> RULE_KEYS_WITH_QUICKFIX_METADATA;
  public static final List<JavaFileScanner> CHECKS_WITH_QUICKFIX;

  private static final Pattern METADATA_PATTERN = Pattern.compile("\"quickfix\"(\\s*:\\s*)(\"covered\"|\"partial\")");
  private static final Pattern RULE_KEY_PATTERN = Pattern.compile("@Rule\\(key\\s*=\\s*\"(.*?)\"\\)");

  static {
    try {
      RULE_KEYS_IMPLEMENTING_QUICKFIXES = getRuleKeysImplementingQuickFixes();
      RULE_KEYS_WITH_QUICKFIX_METADATA = getRuleKeysWithQuickfixMetadata();
      CHECKS_WITH_QUICKFIX = getScannersImplementingQuickFixes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<String> getRuleKeysImplementingQuickFixes() throws IOException {
    Path javaChecksPath = Paths.get("../../java-checks/src/main/java/org/sonar/java/checks/");
    List<String> actualQuickfixImplementations = new ArrayList<>();
    List<Path> paths = Files.walk(javaChecksPath).toList();
    for (Path path : paths) {
      String ruleKey = getRuleKeyFromContent(path);
      if (ruleKey != null) {
        actualQuickfixImplementations.add(ruleKey);
      }
    }
    return actualQuickfixImplementations;
  }

  private static String getRuleKeyFromContent(Path path) throws IOException {
    if (path.toString().endsWith(".java")) {
      String content = new String(Files.readAllBytes(path));
      if (content.contains("QuickFixHelper") || content.contains("JavaQuickFix")) {
        var matcher = RULE_KEY_PATTERN.matcher(content);
        if (matcher.find()) {
          return matcher.group(1);
        }
      }
    }
    return null;
  }

  private static List<String> getRuleKeysWithQuickfixMetadata() throws IOException {
    String metadataFolder = "../../sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java";
    List<String> ruleKeys = new ArrayList<>();
    List<Path> paths = Files.walk(Paths.get(metadataFolder)).toList();
    for (Path path : paths) {
      if (path.toString().endsWith(".json")) {
        String content = new String(Files.readAllBytes(path));
        if (METADATA_PATTERN.matcher(content).find()) {
          ruleKeys.add(path.getFileName().toString().replace(".json", ""));
        }
      }
    }
    return ruleKeys;
  }

  private static List<JavaFileScanner> getScannersImplementingQuickFixes(){
    var checks = new ArrayList<JavaFileScanner>();
    var checkClassesWithQuickfixes = GeneratedCheckList.getJavaChecks().stream()
        .filter(c -> c.isAnnotationPresent(Rule.class) && hasQuickFixCovered(c.getAnnotation(Rule.class)))
        .toList();
    for (var checkClass : checkClassesWithQuickfixes) {
      try {
        checks.add((JavaFileScanner) checkClass.getDeclaredConstructor().newInstance());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return checks;
  }

  private static boolean hasQuickFixCovered(Rule rule) {
    return RULE_KEYS_WITH_QUICKFIX_METADATA.contains(rule.key());
  }

  public static List<InputFile> collectJavaFiles(String directory) throws IOException {
    Path start = Paths.get(directory);
    try (Stream<Path> stream = Files.walk(start)) {
      return stream
        .filter(path -> path.toString().endsWith(".java"))
        .map(path -> TestUtils.inputFile(path.toFile()))
        .toList();
    }
  }

}
