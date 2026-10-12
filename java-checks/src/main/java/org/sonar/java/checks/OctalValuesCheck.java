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
   * Matches Unix file permission modes such as {@code 0644}, {@code 0755} or {@code 04755}:
   * an optional special bits digit (setuid, setgid, sticky) followed by owner, group and others digits,
   * where owner has at least read permission and each digit is one of the common values 0, 4, 5, 6 or 7.
   */
  private static final Pattern PERMISSION_PATTERN = Pattern.compile("0[1-7]?[4-7][04-7]{2}");

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    String value = tree.value();
    if (tree.is(Tree.Kind.INT_LITERAL) && isOctal(value) && value.length() > 2 && !isPermission(value)) {
      context.reportIssue(this, tree, "Use decimal values instead of octal ones.");
    }
  }

  private static boolean isOctal(String value) {
    return value.charAt(0) == '0'  && !"0".equals(value) && !isHexadecimal(value) && !isBinary(value);
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
