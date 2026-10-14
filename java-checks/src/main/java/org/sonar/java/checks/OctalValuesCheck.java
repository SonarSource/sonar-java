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

import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1314")
public class OctalValuesCheck extends BaseTreeVisitor implements JavaFileScanner {

  /**
   * Matches Unix file permission modes such as {@code 0644} or {@code 0755}:
   * owner, group and others digits, where owner has at least read permission
   * and each digit is one of the common values 0, 4, 5, 6 or 7.
   */
  private static final Pattern PERMISSION_PATTERN = Pattern.compile("0[4-7][04-7]{2}");

  private static final String EXPLICIT_PREFIX_MESSAGE = "If an octal value is intended, use the more explicit \"0_\" prefix.";

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    String value = tree.value();
    if (!tree.is(Tree.Kind.INT_LITERAL) || !isOctal(value) || value.length() <= 2 || hasExplicitOctalPrefix(value)) {
      return;
    }
    if (isPermission(value)) {
      context.reportIssue(this, tree, String.format("If this is a file permission mode, use the symbolic permission string \"%s\"; otherwise use the decimal value. %s", toSymbolicPermissions(value), EXPLICIT_PREFIX_MESSAGE));
    } else {
      context.reportIssue(this, tree, "Use decimal values instead of octal ones. " + EXPLICIT_PREFIX_MESSAGE);
    }
  }

  /**
   * Converts an octal permission mode such as {@code 0755} into its symbolic representation such as {@code rwxr-xr-x},
   * as accepted by {@code java.nio.file.attribute.PosixFilePermissions.fromString}.
   */
  private static String toSymbolicPermissions(String value) {
    StringBuilder sb = new StringBuilder();
    for (char digit : value.substring(1).toCharArray()) {
      int bits = digit - '0';
      sb.append((bits & 4) != 0 ? 'r' : '-')
        .append((bits & 2) != 0 ? 'w' : '-')
        .append((bits & 1) != 0 ? 'x' : '-');
    }
    return sb.toString();
  }

  private static boolean isOctal(String value) {
    return value.charAt(0) == '0'  && !"0".equals(value) && !isHexadecimal(value) && !isBinary(value);
  }

  private static boolean hasExplicitOctalPrefix(String value) {
    return value.startsWith("0_");
  }

  private static boolean isPermission(String value) {
    return PERMISSION_PATTERN.matcher(value).matches();
  }

  private static boolean isHexadecimal(String value) {
    return value.startsWith("0x") || value.startsWith("0X");
  }

  private static boolean isBinary(String value) {
    return value.startsWith("0b") || value.startsWith("0B");
  }

}
