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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
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
    return Tree.Kind.CLASS_KINDS;
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
    List<VariableTree> loggerFields = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(MoreThanOneLoggerCheck::isDeclaredLogger)
      .toList();
    return withoutDerivedLoggers(loggerFields);
  }

  /**
   * A logger field without an initializer is a configurable property, assigned at startup or
   * injected from outside, rather than a category the class gives itself.
   */
  private static boolean isDeclaredLogger(VariableTree field) {
    return field.initializer() != null && isLoggerType(field.type().symbolType());
  }

  /**
   * Drops the loggers whose name is derived from another logger of the same class, such as
   * {@code LoggerFactory.getLogger(LOG.getName() + ".config")}. Those create a child category,
   * which inherits the level and the appenders configured for the parent.
   */
  private static List<VariableTree> withoutDerivedLoggers(List<VariableTree> loggerFields) {
    Set<Symbol> loggerSymbols = loggerFields.stream()
      .map(VariableTree::symbol)
      .collect(Collectors.toSet());
    return loggerFields.stream()
      .filter(field -> !referencesAny(field.initializer(), loggerSymbols))
      .toList();
  }

  private static boolean referencesAny(Tree initializer, Set<Symbol> symbols) {
    LoggerReferenceVisitor visitor = new LoggerReferenceVisitor(symbols);
    initializer.accept(visitor);
    return visitor.found;
  }

  private static boolean isLoggerType(Type type) {
    return LOGGER_TYPES.stream().anyMatch(type::is);
  }

  private static class LoggerReferenceVisitor extends BaseTreeVisitor {

    private final Set<Symbol> symbols;
    private boolean found = false;

    private LoggerReferenceVisitor(Set<Symbol> symbols) {
      this.symbols = symbols;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (symbols.contains(tree.symbol())) {
        found = true;
      }
      super.visitIdentifier(tree);
    }
  }
}
