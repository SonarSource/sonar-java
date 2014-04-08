/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.regex.Pattern;

@Rule(
  key = LoggersDeclarationCheck.KEY,
  priority = Priority.MAJOR,
  tags={"convention"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class LoggersDeclarationCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String KEY = "S1312";
  private static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  private static final String DEFAULT_FORMAT = "LOG(?:GER)?";

  @RuleProperty(
    key = "format",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format);
    }
    this.context = context;
    scan(context.getTree());
  }

  private static boolean isPrivateStaticFinal(ModifiersTree tree) {
    return hasModifier(tree, Modifier.PRIVATE) &&
      hasModifier(tree, Modifier.STATIC) &&
      hasModifier(tree, Modifier.FINAL);
  }

  private static boolean hasModifier(ModifiersTree tree, Modifier expectedModifier) {
    for (Modifier modifier : tree.modifiers()) {
      if (modifier.equals(expectedModifier)) {
        return true;
      }
    }

    return false;
  }

  private boolean isValidLoggerName(String name) {
    return pattern.matcher(name).matches();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);

    if (isLoggerType(tree.type())) {
      boolean isPrivateStaticFinal = isPrivateStaticFinal(tree.modifiers());
      boolean hasValidLoggerName = isValidLoggerName(tree.simpleName().name());

      if (!isPrivateStaticFinal && !hasValidLoggerName) {
        context.addIssue(tree, RULE_KEY, getPrivateStaticFinalMessage(tree.simpleName().name()) + " and rename it to comply with the format \"" + format + "\".");
      } else if (!isPrivateStaticFinal) {
        context.addIssue(tree, RULE_KEY, getPrivateStaticFinalMessage(tree.simpleName().name()) + ".");
      } else if (!hasValidLoggerName) {
        context.addIssue(tree, RULE_KEY, "Rename the \"" + tree.simpleName() + "\" logger to comply with the format \"" + format + "\".");
      }
    }
  }

  private static String getPrivateStaticFinalMessage(String simpleName) {
    return "Make the \"" + simpleName + "\" logger private static final";
  }

  private static boolean isLoggerType(Tree tree) {
    if (!tree.is(Tree.Kind.IDENTIFIER)) {
      return false;
    }
    IdentifierTree identifierTree = (IdentifierTree) tree;

    return "Log".equals(identifierTree.name()) ||
      "Logger".equals(identifierTree.name());
  }

}
