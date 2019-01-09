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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

@Rule(key = "S2325")
public class StaticMethodCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String JAVA_IO_SERIALIZABLE = "java.io.Serializable";
  private static final MethodMatcherCollection EXCLUDED_SERIALIZABLE_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("readObject").addParameter(TypeCriteria.subtypeOf("java.io.ObjectInputStream")),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("writeObject").addParameter(TypeCriteria.subtypeOf("java.io.ObjectOutputStream")),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("readObjectNoData").withoutParameter(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("writeReplace").withoutParameter(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf(JAVA_IO_SERIALIZABLE)).name("readResolve").withoutParameter()
  );

  private JavaFileScannerContext context;
  private Deque<MethodReference> methodReferences = new LinkedList<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (isExcluded(tree)) {
      return;
    }
    Symbol.MethodSymbol symbol = tree.symbol();
    methodReferences.push(new MethodReference(symbol));
    scan(tree.parameters());
    scan(tree.block());
    MethodReference reference = methodReferences.pop();
    if (symbol.isPrivate() && !symbol.isStatic() && !reference.hasNonStaticReference()) {
      context.reportIssue(this, tree.simpleName(), "Make \"" + symbol.name() + "\" a \"static\" method.");
    }
  }

  private static boolean isExcluded(MethodTree tree) {
    return tree.is(Tree.Kind.CONSTRUCTOR) || EXCLUDED_SERIALIZABLE_METHODS.anyMatch(tree);
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    super.visitIdentifier(tree);
    if ("class".equals(tree.name()) || methodReferences.isEmpty()) {
      return;
    }
    if (parentIs(tree, Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree parent = (MemberSelectExpressionTree) tree.parent();
      // Exclude identifiers used in member select, except for instance creation
      // New class may use member select to denote an inner class
      if (tree.equals(parent.identifier()) && !parentIs(parent, Tree.Kind.NEW_CLASS) && !refToEnclosingClass(tree)) {
        return;
      }
    }
    visitTerminalIdentifier(tree);
  }

  private static boolean refToEnclosingClass(IdentifierTree tree) {
    String identifier = tree.name();
    return "this".equals(identifier) || "super".equals(identifier);
  }

  private void visitTerminalIdentifier(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    MethodReference currentMethod = methodReferences.peek();
    if (symbol.isUnknown()) {
      currentMethod.setNonStaticReference();
      return;
    }
    for (MethodReference methodReference : methodReferences) {
      methodReference.checkSymbol(symbol);
    }
  }

  private static boolean parentIs(Tree tree, Tree.Kind kind) {
    return tree.parent() != null && tree.parent().is(kind);
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    if (tree.expression().is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) tree.expression();
      Symbol owner = identifier.symbol().owner();
      if (owner != null && owner.isMethodSymbol()) {
        // No need to investigate selection on local symbols
        return;
      }
    }
    super.visitMemberSelectExpression(tree);
  }

  private static class MethodReference {

    private final Symbol.MethodSymbol methodSymbol;
    private final Symbol methodScopeOwner;
    private boolean nonStaticReference = false;

    MethodReference(Symbol.MethodSymbol symbol) {
      methodSymbol = symbol;
      methodScopeOwner = methodSymbol.owner();
      if (methodScopeOwner != null && methodScopeOwner.isTypeSymbol()) {
        nonStaticReference = !methodScopeOwner.isStatic() && !methodScopeOwner.owner().isPackageSymbol();
      }
    }

    @CheckForNull
    private static Symbol getPackage(Symbol symbol) {
      Symbol owner = symbol.owner();
      while (owner != null) {
        if (owner.isPackageSymbol()) {
          break;
        }
        owner = owner.owner();
      }
      return owner;
    }

    void setNonStaticReference() {
      nonStaticReference = true;
    }

    boolean hasNonStaticReference() {
      return nonStaticReference;
    }

    void checkSymbol(Symbol symbol) {
      if (nonStaticReference || methodSymbol.equals(symbol) || symbol.isStatic()) {
        return;
      }
      Symbol scopeOwner = symbol.owner();
      if (isConstructor(symbol)) {
        checkConstructor(scopeOwner);
      } else if (scopeOwner != null) {
        checkNonConstructor(scopeOwner);
      }
    }

    private void checkConstructor(Symbol constructorClass) {
      if (!constructorClass.isStatic()) {
        Symbol methodPackage = getPackage(methodScopeOwner);
        Symbol constructorPackage = getPackage(constructorClass);
        if (Objects.equals(methodPackage, constructorPackage) && !constructorClass.owner().isPackageSymbol()) {
          setNonStaticReference();
        }
      }
    }

    private void checkNonConstructor(Symbol scopeOwner) {
      if (scopeOwner.isMethodSymbol()) {
        return;
      }
      if (hasLocalAccess(methodScopeOwner, scopeOwner)) {
        setNonStaticReference();
      }
    }

    private static boolean isConstructor(Symbol symbol) {
      return "<init>".equals(symbol.name());
    }

    private static boolean hasLocalAccess(Symbol scope, Symbol symbol) {
      if (scope.equals(symbol)) {
        return true;
      }
      if (scope.isTypeSymbol() && symbol.isTypeSymbol()) {
        Type scopeType = scope.type().erasure();
        Type symbolType = symbol.type().erasure();
        if (scopeType.isSubtypeOf(symbolType)) {
          return true;
        }
      }
      return false;
    }
  }
}
