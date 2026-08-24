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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Flags a {@code hashCode()} implementation that reads an instance field which the class's
 * {@code equals(Object)} implementation never reads, breaking the {@code Object.hashCode()} contract.
 */
@Rule(key = "S9362")
public class HashCodeMismatchedFieldsCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE =
    "This hashCode() implementation is inconsistent with equals(): it reads \"%s\", which equals() never reads, so equal objects may hash differently.";
  private static final String SECONDARY_MESSAGE = "Not compared in equals()";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol owner = classTree.symbol();
    if (owner.isUnknown() || owner.type().isUnknown()) {
      return;
    }

    MethodTree equalsMethod = null;
    MethodTree hashCodeMethod = null;
    List<MethodTree> otherMethods = new ArrayList<>();
    for (Tree member : classTree.members()) {
      if (!(member instanceof MethodTree methodTree) || methodTree.block() == null) {
        continue;
      }
      if (MethodTreeUtils.isEqualsMethod(methodTree)) {
        equalsMethod = methodTree;
      } else if (MethodTreeUtils.isHashCodeMethod(methodTree)) {
        hashCodeMethod = methodTree;
      } else {
        otherMethods.add(methodTree);
      }
    }
    if (equalsMethod == null || hashCodeMethod == null) {
      return;
    }

    Map<Symbol.MethodSymbol, Map<Symbol, Tree>> fieldsByHelper = collectHelperFields(owner, otherMethods);

    FieldReadCollector equalsFields = scan(equalsMethod, owner, fieldsByHelper, Role.EQUALS);
    FieldReadCollector hashCodeFields = scan(hashCodeMethod, owner, fieldsByHelper, Role.HASH_CODE);
    if (equalsFields.failed || hashCodeFields.failed || equalsFields.fields.isEmpty()) {
      // Bail out on unresolved members, or when equals() compares no state (likely reference equality).
      return;
    }

    Map<Symbol, Tree> extraFields = new LinkedHashMap<>(hashCodeFields.fields);
    extraFields.keySet().removeAll(equalsFields.fields.keySet());
    // A field caching a previously computed hash value does not add new identity state: recognize it either by
    // name, or because hashCode() itself assigns to it (the memoization pattern), regardless of its name.
    extraFields.keySet().removeIf(field -> field.name().toLowerCase(Locale.ROOT).contains("hash") || hashCodeFields.assignedFields.contains(field));
    if (extraFields.isEmpty()) {
      return;
    }

    reportMismatch(hashCodeMethod, extraFields);
  }

  private static Map<Symbol.MethodSymbol, Map<Symbol, Tree>> collectHelperFields(Symbol owner, List<MethodTree> otherMethods) {
    Map<Symbol.MethodSymbol, Map<Symbol, Tree>> fieldsByHelper = new HashMap<>();
    for (MethodTree helper : otherMethods) {
      Symbol.MethodSymbol helperSymbol = helper.symbol();
      if (helperSymbol.isUnknown() || !helper.parameters().isEmpty()) {
        continue;
      }
      FieldReadCollector collector = scan(helper, owner, Map.of(), Role.HELPER);
      if (!collector.failed) {
        fieldsByHelper.put(helperSymbol, collector.fields);
      }
    }
    return fieldsByHelper;
  }

  private static FieldReadCollector scan(MethodTree method, Symbol owner, Map<Symbol.MethodSymbol, Map<Symbol, Tree>> fieldsByHelper, Role role) {
    FieldReadCollector collector = new FieldReadCollector(owner, fieldsByHelper, role);
    method.block().accept(collector);
    return collector;
  }

  private void reportMismatch(MethodTree hashCodeMethod, Map<Symbol, Tree> extraFields) {
    List<String> names = extraFields.keySet().stream()
      .map(Symbol::name)
      .sorted()
      .toList();
    List<JavaFileScannerContext.Location> secondaryLocations = extraFields.values().stream()
      .map(location -> new JavaFileScannerContext.Location(SECONDARY_MESSAGE, location))
      .toList();
    reportIssue(hashCodeMethod.simpleName(), String.format(ISSUE_MESSAGE, String.join("\", \"", names)), secondaryLocations, null);
  }

  private enum Role {
    /** Pre-scanning a candidate getter/helper method: no instance calls are trusted, and helpers are not chained. */
    HELPER,
    EQUALS,
    HASH_CODE
  }

  /**
   * Collects same-owner, non-static field reads inside a method body. Any unresolved symbol, or any
   * instance-method call that is not on the small allow-list for the method's role, marks the scan as
   * failed: callers must then skip reporting entirely instead of guessing from a partial field set.
   */
  private static final class FieldReadCollector extends BaseTreeVisitor {

    private final Symbol enclosingClass;
    private final Map<Symbol.MethodSymbol, Map<Symbol, Tree>> fieldsByHelper;
    private final Role role;
    private final Map<Symbol, Tree> fields = new LinkedHashMap<>();
    private final Set<Symbol> assignedFields = new HashSet<>();
    private boolean failed;

    private FieldReadCollector(Symbol enclosingClass, Map<Symbol.MethodSymbol, Map<Symbol, Tree>> fieldsByHelper, Role role) {
      this.enclosingClass = enclosingClass;
      this.fieldsByHelper = fieldsByHelper;
      this.role = role;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not attribute field reads from a nested or anonymous class to the enclosing equals()/hashCode().
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (!failed) {
        String name = tree.name();
        if (!"this".equals(name) && !"super".equals(name)) {
          Symbol symbol = tree.symbol();
          if (symbol.isUnknown()) {
            failed = true;
          } else if (symbol.isVariableSymbol() && !symbol.isStatic() && ownedByEnclosing(symbol)) {
            fields.putIfAbsent(symbol, tree);
          }
        }
      }
      super.visitIdentifier(tree);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (tree.variable() instanceof IdentifierTree identifier) {
        Symbol symbol = identifier.symbol();
        if (!symbol.isUnknown() && symbol.isVariableSymbol() && !symbol.isStatic() && ownedByEnclosing(symbol)) {
          assignedFields.add(symbol);
        }
      }
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (!failed) {
        Symbol.MethodSymbol symbol = tree.methodSymbol();
        if (symbol.isUnknown()) {
          failed = true;
        } else {
          Map<Symbol, Tree> helperFields = fieldsByHelper.get(symbol);
          if (helperFields != null) {
            helperFields.forEach(fields::putIfAbsent);
          } else if (symbol.isStatic()) {
            // A same-class static helper we could not pre-scan (e.g. it takes parameters) may hide field
            // reads: bail out rather than silently ignoring it. An external static utility (e.g. Objects.hash)
            // is assumed side-effect free and is not owned by the enclosing class.
            failed = ownedByEnclosing(symbol);
          } else if (ownedByEnclosing(symbol) || !isAllowedInstanceCall(symbol)) {
            // A same-class instance method other than the trusted getClass()/equals()/hashCode() allow-list
            // (e.g. a differently-parameterized equals(SpecificType) overload) may hide field reads.
            failed = true;
          }
        }
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isAllowedInstanceCall(Symbol.MethodSymbol symbol) {
      String name = symbol.name();
      if ("getClass".equals(name) && symbol.parameterTypes().isEmpty()) {
        return true;
      }
      return switch (role) {
        case EQUALS -> "equals".equals(name) && symbol.parameterTypes().size() == 1;
        case HASH_CODE -> "hashCode".equals(name) && symbol.parameterTypes().isEmpty();
        case HELPER -> false;
      };
    }

    private boolean ownedByEnclosing(Symbol symbol) {
      Symbol symbolOwner = symbol.owner();
      // Compare erasures so a field of Holder<?> still belongs to Holder<T>.
      return symbolOwner != null && !symbolOwner.isUnknown() && symbolOwner.isTypeSymbol()
        && enclosingClass.type().erasure().equals(symbolOwner.type().erasure());
    }
  }
}
