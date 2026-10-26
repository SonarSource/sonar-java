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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9398")
public class InnerClassInInterfaceCheck extends IssuableSubscriptionVisitor {

  private static final String BUILDER_SUFFIX = "Builder";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree interfaceTree = (ClassTree) tree;
    Set<Symbol> signatureTypes = signatureTypes(interfaceTree);
    interfaceTree.members().stream()
      .filter(member -> member.is(Tree.Kind.CLASS))
      .map(ClassTree.class::cast)
      .filter(classTree -> !ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT))
      .filter(classTree -> implementsInterface(classTree, interfaceTree) || !isHelperType(classTree, signatureTypes))
      .forEach(classTree -> reportIssue(classTree.simpleName(), "Move this class out of this interface."));
  }

  private static boolean implementsInterface(ClassTree classTree, ClassTree interfaceTree) {
    return classTree.symbol().type().isSubtypeOf(interfaceTree.symbol().type());
  }

  /**
   * A nested class is considered a helper type of its enclosing interface when it is a builder,
   * or when it appears in the signature (return type, parameter types or thrown exceptions)
   * of a method declared by the interface.
   */
  private static boolean isHelperType(ClassTree classTree, Set<Symbol> signatureTypes) {
    return classTree.simpleName().name().endsWith(BUILDER_SUFFIX) || signatureTypes.contains(classTree.symbol());
  }

  private static Set<Symbol> signatureTypes(ClassTree interfaceTree) {
    TypeSymbolsCollector collector = new TypeSymbolsCollector();
    interfaceTree.members().stream()
      .filter(MethodTree.class::isInstance)
      .map(MethodTree.class::cast)
      .forEach(collector::scanSignature);
    return collector.symbols;
  }

  private static class TypeSymbolsCollector extends BaseTreeVisitor {

    private final Set<Symbol> symbols = new HashSet<>();

    void scanSignature(MethodTree methodTree) {
      scan(methodTree.returnType());
      methodTree.parameters().forEach(parameter -> scan(parameter.type()));
      scan(methodTree.throwsClauses());
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();
      if (symbol.isTypeSymbol()) {
        symbols.add(symbol);
      }
    }
  }

}
