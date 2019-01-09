/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.regex.Pattern;

@Rule(key = "S1312")
public class LoggersDeclarationCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String DEFAULT_FORMAT = "LOG(?:GER)?";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the logger names against.",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    if (context.getSemanticModel() != null) {
      if (pattern == null) {
        pattern = Pattern.compile(format);
      }
      this.context = context;
      scan(context.getTree());
    }
  }

  private static boolean isPrivateStaticFinal(ModifiersTree tree) {
    return ModifiersUtils.hasModifier(tree, Modifier.PRIVATE) &&
      ModifiersUtils.hasModifier(tree, Modifier.STATIC) &&
      ModifiersUtils.hasModifier(tree, Modifier.FINAL);
  }

  private boolean isValidLoggerName(String name) {
    return pattern.matcher(name).matches();
  }

  @Override
  public void visitMethod(MethodTree tree) {
    // only scan body of the method and avoid looking at parameters
    scan(tree.block());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    if (tree.symbol().type().is("org.apache.maven.plugin.logging.Log")) {
      return;
    }
    if (isLoggerType(tree.type())) {
      boolean isPrivateStaticFinal = isPrivateStaticFinal(tree.modifiers());
      IdentifierTree variableIdentifier = tree.simpleName();
      String name = variableIdentifier.name();
      boolean hasValidLoggerName = isValidLoggerName(name);

      if (!isPrivateStaticFinal && !hasValidLoggerName) {
        context.reportIssue(this, variableIdentifier, getPrivateStaticFinalMessage(name) + " and rename it to comply with the format \"" + format + "\".");
      } else if (!isPrivateStaticFinal) {
        context.reportIssue(this, variableIdentifier, getPrivateStaticFinalMessage(name) + ".");
      } else if (!hasValidLoggerName) {
        context.reportIssue(this, variableIdentifier, "Rename the \"" + variableIdentifier + "\" logger to comply with the format \"" + format + "\".");
      }
    }
  }

  private static String getPrivateStaticFinalMessage(String simpleName) {
    return "Make the \"" + simpleName + "\" logger private static final";
  }

  private static boolean isLoggerType(Tree tree) {
    IdentifierTree identifierTree = null;
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      identifierTree = (IdentifierTree) tree;
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      identifierTree = ((MemberSelectExpressionTree) tree).identifier ();
    } else {
      return false;
    }

    return "Log".equals(identifierTree.name()) ||
      "Logger".equals(identifierTree.name());
  }

}
