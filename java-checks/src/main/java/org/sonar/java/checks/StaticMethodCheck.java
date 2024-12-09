/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;

@Rule(key = "S2325")
public class StaticMethodCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String JAVA_IO_SERIALIZABLE = "java.io.Serializable";
  private static final MethodMatchers EXCLUDED_SERIALIZABLE_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes(JAVA_IO_SERIALIZABLE)
      .names("readObject")
      .addParametersMatcher(params -> params.size() == 1 && params.get(0).isSubtypeOf("java.io.ObjectInputStream"))
      .build(),
    MethodMatchers.create()
      .ofSubTypes(JAVA_IO_SERIALIZABLE)
      .names("writeObject")
      .addParametersMatcher(params -> params.size() == 1 && params.get(0).isSubtypeOf("java.io.ObjectOutputStream"))
      .build(),
    MethodMatchers.create()
      .ofSubTypes(JAVA_IO_SERIALIZABLE)
      .names("readObjectNoData", "writeReplace", "readResolve")
      .addWithoutParametersMatcher()
      .build());

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
    ClassTree classTree = (ClassTree) tree.parent();
    if (!Boolean.FALSE.equals(tree.isOverriding()) || classTree.is(Tree.Kind.ENUM)) {
      // In case it cannot be determined (isOverriding returns null), consider as overriding to avoid FP.
      return;
    }
    if ((symbol.isPrivate() || symbol.isFinal() || classTree.symbol().isFinal())
      && !symbol.isStatic()
      && !reference.hasNonStaticReference()
      && !returnRequiresParentTypeParameter(symbol)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(tree.simpleName())
        .withMessage("Make \"%s\" a \"static\" method.", symbol.name())
        .withQuickFix(() -> getQuickFix(tree))
        .report();
    }
  }

  /**
   * Method's return type requires type parameter and therefore cannot be made static.
   */
  private static boolean returnRequiresParentTypeParameter(Symbol.MethodSymbol symbol) {
    List<Type> returnTypeVars = new ArrayList<>();
    collectTypeVars(returnTypeVars, symbol.returnType().type());
    if (returnTypeVars.isEmpty()) {
      return false;
    }
    // called from visitMethod, so we have declaration (NPE not possible)
    ClassTree parent = (ClassTree) symbol.declaration().parent();
    Set<Symbol> parentTypeParam =
      parent
        .typeParameters()
        .stream()
        .map(TypeParameterTree::symbol)
        .collect(Collectors.toUnmodifiableSet());
    return returnTypeVars
      .stream()
      .map(Type::symbol)
      .anyMatch(parentTypeParam::contains);
  }

  private static void collectTypeVars(List<Type> accumulator, Type t) {
    // Consider a type such as `Map<T,Map<String,U>>`, it has two type
    // arguments: `T` and `Map<String,U>`. We collect variables in the `accumulator`,
    // and descend recursively otherwise (which is fine for simple types, such as String,
    // which do not have variables).
    for(Type tt: t.typeArguments()) {
      if (tt.isTypeVar()) {
        accumulator.add(tt);
      } else {
        collectTypeVars(accumulator, tt);
      }
    }
  }

  private static JavaQuickFix getQuickFix(MethodTree tree) {
    Tree insertPosition = QuickFixHelper.nextToken(tree.modifiers());

    for (ModifierKeywordTree modifier: tree.modifiers().modifiers()) {
      if (shouldBePlacedAfterStatic(modifier.modifier())) {
        insertPosition = modifier;
        break;
      }
    }

    return JavaQuickFix.newQuickFix("Make static")
      .addTextEdit(JavaTextEdit.insertBeforeTree(insertPosition, "static "))
      .build();
  }

  private static boolean shouldBePlacedAfterStatic(Modifier modifier) {
    return modifier.ordinal() > Modifier.STATIC.ordinal();
  }

  private static boolean isExcluded(MethodTree tree) {
    return tree.is(Tree.Kind.CONSTRUCTOR) || EXCLUDED_SERIALIZABLE_METHODS.matches(tree) || hasEmptyBody(tree);
  }

  private static boolean hasEmptyBody(MethodTree tree) {
    return tree.block() != null && tree.block().body().isEmpty();
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
