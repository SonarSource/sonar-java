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
package org.sonar.samples.java;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "semanticbindings", priority = Priority.MAJOR, name = "Semantic binding probes", description = "Checks expected bindings in the source-only comparison fixture")
public class SemanticBindingsCheck extends IssuableSubscriptionVisitor {

  @RuleProperty(
    key = "expectProjectBindingsResolved",
    description = "Whether project type, inherited field, and method bindings should resolve in the comparison fixture.",
    defaultValue = "true")
  public boolean expectProjectBindingsResolved = true;

  private static final Set<String> EXPECTED = Set.of("String", "BindingHelper", "select", "inheritedValue", "MissingType");
  private final Set<String> checked = new HashSet<>();
  private final Set<String> failed = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.COMPILATION_UNIT, Tree.Kind.IDENTIFIER, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      checked.clear();
      failed.clear();
    }
    if (!context.getInputFile().filename().equals("BindingFixture.java")) {
      return;
    }
    if (tree instanceof IdentifierTree identifier) {
      Symbol symbol = identifier.symbol();
      switch (identifier.name()) {
        case "String" -> verify(identifier, "String", "type java.lang.String", describe(symbol));
        case "BindingHelper" -> {
          if (identifier.parent().is(Tree.Kind.VARIABLE)) {
            verify(identifier, "BindingHelper", expectedProjectBinding("type bindings.BindingHelper"), describe(symbol));
          }
        }
        case "inheritedValue" -> verify(identifier, "inheritedValue", expectedProjectBinding("variable bindings.BindingParent.inheritedValue:int"), describe(symbol));
        case "MissingType" -> verify(identifier, "MissingType", "unknown", describe(symbol));
        default -> { }
      }
    } else if (tree instanceof MethodInvocationTree invocation && invocation.methodSelect() instanceof MemberSelectExpressionTree select
      && select.identifier().name().equals("select")) {
      verify(invocation, "select", expectedProjectBinding("method bindings.BindingHelper.select(java.lang.String):java.lang.String"), describe(invocation.methodSymbol()));
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT) && context.getInputFile().filename().equals("BindingFixture.java")) {
      for (String probe : EXPECTED.stream().sorted().toList()) {
        if (!checked.contains(probe)) {
          failed.add(probe);
          reportIssue(tree, "Semantic binding mismatch for " + probe + ": expected a probe occurrence, actual missing.");
        }
      }
      reportIssue(tree, "Semantic binding probes: " + checked.size() + " checked, " + failed.size() + " failed.");
    }
  }

  private void verify(Tree tree, String probe, String expected, String actual) {
    checked.add(probe);
    if (!expected.equals(actual)) {
      failed.add(probe);
      reportIssue(tree, "Semantic binding mismatch for " + probe + ": expected " + expected + ", actual " + actual + ".");
    }
  }

  private String expectedProjectBinding(String resolved) {
    return expectProjectBindingsResolved ? resolved : "unknown";
  }

  private static String describe(Symbol symbol) {
    if (symbol.isUnknown()) {
      return "unknown";
    }
    if (symbol.isTypeSymbol()) {
      return "type " + symbol.type().fullyQualifiedName();
    }
    String owner = symbol.owner() == null ? "<none>" : symbol.owner().type().fullyQualifiedName();
    if (symbol instanceof Symbol.MethodSymbol method) {
      String parameters = String.join(",", method.parameterTypes().stream().map(type -> type.fullyQualifiedName()).toList());
      return "method " + owner + "." + symbol.name() + "(" + parameters + "):" + method.returnType().type().fullyQualifiedName();
    }
    return "variable " + owner + "." + symbol.name() + ":" + symbol.type().fullyQualifiedName();
  }
}
