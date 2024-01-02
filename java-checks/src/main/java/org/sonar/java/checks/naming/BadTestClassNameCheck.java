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
package org.sonar.java.checks.naming;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.isTestClass;

@Rule(key = "S3577")
public class BadTestClassNameCheck extends IssuableSubscriptionVisitor {

  private static final String STARTING_WITH_TEST = "(Test|IT)[a-zA-Z0-9_]+";
  private static final String ENDING_WITH_TEST = "[A-Z][a-zA-Z0-9_]*(Test|Tests|TestCase|IT|ITCase)";
  private static final String DEFAULT_FORMAT = "^(" + STARTING_WITH_TEST + "|" + ENDING_WITH_TEST + ")$";

  @RuleProperty(
    key = "format",
    description = "Regular expression against which test class names are checked.",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    IdentifierTree simpleName = classTree.simpleName();
    if (hasInvalidName(simpleName) && isTestClass(classTree)) {
      reportIssue(simpleName, "Rename class \"" + simpleName.name() + "\" to match the regular expression: '" + format + "'");
    }
  }

  private boolean hasInvalidName(@Nullable IdentifierTree className) {
    return className != null && !pattern.matcher(className.name()).matches();
  }

}
