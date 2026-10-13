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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9436")
public class MoreThanOneLoggerCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this logger and use the one already declared in this class.";
  private static final String SECONDARY_MESSAGE = "Logger already declared here.";

  private static final Set<String> LOGGER_TYPES = Set.of(
    "org.slf4j.Logger",
    "org.apache.logging.log4j.Logger",
    "org.apache.log4j.Logger",
    "java.util.logging.Logger",
    "org.apache.commons.logging.Log");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.RECORD, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    List<VariableTree> loggerFields = loggerFields((ClassTree) tree);
    if (loggerFields.size() < 2) {
      return;
    }
    List<JavaFileScannerContext.Location> secondary =
      Collections.singletonList(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, loggerFields.get(0).simpleName()));
    loggerFields.subList(1, loggerFields.size())
      .forEach(extraLogger -> reportIssue(extraLogger.simpleName(), MESSAGE, secondary, null));
  }

  private static List<VariableTree> loggerFields(ClassTree classTree) {
    List<VariableTree> loggerFields = new ArrayList<>();
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE) && isLoggerType(((VariableTree) member).type().symbolType())) {
        loggerFields.add((VariableTree) member);
      }
    }
    return loggerFields;
  }

  private static boolean isLoggerType(Type type) {
    return LOGGER_TYPES.stream().anyMatch(type::is);
  }
}
