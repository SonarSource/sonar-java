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
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9345")
public class FinalizerAttackCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() == null ||
      ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL) ||
      isLocalClass(classTree) ||
      isSafelySealedClass(classTree) ||
      hasFinalFinalizer(classTree)) {
      return;
    }
    List<JavaFileScannerContext.Location> secondaryLocations = Collections.singletonList(
      new JavaFileScannerContext.Location("Non-final class", classTree.simpleName()));

    checkMembers(classTree, secondaryLocations);
  }

  private void checkMembers(ClassTree classTree, List<JavaFileScannerContext.Location> secondaryLocations) {
    boolean hasExplicitConstructor = false;
    boolean hasThrowingInitializers = false;
    List<Tree> throwingInitializers = new ArrayList<>();

    for (Tree member : classTree.members()) {
      if (member.is(Kind.CONSTRUCTOR)) {
        hasExplicitConstructor = true;
      } else if (member.is(Kind.INITIALIZER) && containsThrowStatementInBlock((BlockTree) member)) {
        throwingInitializers.add(member);
        hasThrowingInitializers = true;
      } else if (member.is(Kind.VARIABLE) && hasThrowingFieldInitializer((VariableTree) member)) {
        throwingInitializers.add(member);
        hasThrowingInitializers = true;
      }
    }

    if (hasExplicitConstructor) {
      reportVulnerableConstructors(classTree, hasThrowingInitializers, secondaryLocations);
    } else if (hasThrowingInitializers) {
      reportThrowingInitializers(classTree, throwingInitializers);
    }
  }

  private void reportVulnerableConstructors(ClassTree classTree, boolean hasThrowingInitializers,
    List<JavaFileScannerContext.Location> secondaryLocations) {
    for (Tree member : classTree.members()) {
      if (member.is(Kind.CONSTRUCTOR)) {
        MethodTree constructor = (MethodTree) member;
        if (isVulnerableConstructor(constructor, hasThrowingInitializers)) {
          reportIssue(constructor.simpleName(),
            "Make this class \"final\" or make this throwing constructor \"private\".",
            secondaryLocations, null);
        }
      }
    }
  }

  private void reportThrowingInitializers(ClassTree classTree, List<Tree> throwingInitializers) {
    List<JavaFileScannerContext.Location> locations = new ArrayList<>();
    for (Tree init : throwingInitializers) {
      locations.add(new JavaFileScannerContext.Location("Throwing initializer", init));
    }
    reportIssue(classTree.simpleName(),
      "Make this class \"final\" or add a private constructor, because initializers can throw.",
      locations, null);
  }

  private static boolean isLocalClass(ClassTree classTree) {
    Tree parent = classTree.parent();
    while (parent != null) {
      if (parent.is(Kind.METHOD, Kind.CONSTRUCTOR)) {
        return true;
      }
      if (parent.is(Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.RECORD, Kind.ANNOTATION_TYPE)) {
        return false;
      }
      parent = parent.parent();
    }
    return false;
  }

  private static boolean isSafelySealedClass(ClassTree classTree) {
    if (!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.SEALED)) {
      return false;
    }
    for (TypeTree permitted : classTree.permittedTypes()) {
      if (isNonSealedPermittedType(classTree, permitted)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isNonSealedPermittedType(ClassTree context, TypeTree permitted) {
    ClassTree permittedDecl = resolvePermittedDeclaration(context, permitted);
    return permittedDecl != null && ModifiersUtils.hasModifier(permittedDecl.modifiers(), Modifier.NON_SEALED);
  }

  private static ClassTree resolvePermittedDeclaration(ClassTree context, TypeTree permitted) {
    Type permittedType = permitted.symbolType();
    if (!permittedType.isUnknown()) {
      return permittedType.symbol().declaration();
    }
    return findClassByName(context, getSimpleName(permitted));
  }

  private static String getSimpleName(TypeTree typeTree) {
    if (typeTree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) typeTree).name();
    }
    return "";
  }

  private static ClassTree findClassByName(ClassTree context, String name) {
    if (name.isEmpty()) {
      return null;
    }
    Tree parent = context.parent();
    while (parent != null && !parent.is(Kind.COMPILATION_UNIT)) {
      parent = parent.parent();
    }
    if (parent == null) {
      return null;
    }
    return findClassInTree(parent, name);
  }

  private static ClassTree findClassInTree(Tree tree, String name) {
    if (tree.is(Kind.CLASS, Kind.INTERFACE)) {
      ClassTree classTree = (ClassTree) tree;
      if (classTree.simpleName() != null && name.equals(classTree.simpleName().name())) {
        return classTree;
      }
      for (Tree member : classTree.members()) {
        ClassTree found = findClassInTree(member, name);
        if (found != null) {
          return found;
        }
      }
    } else if (tree.is(Kind.COMPILATION_UNIT)) {
      for (Tree child : ((CompilationUnitTree) tree).types()) {
        ClassTree found = findClassInTree(child, name);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static boolean hasFinalFinalizer(ClassTree classTree) {
    for (Tree member : classTree.members()) {
      if (member.is(Kind.METHOD)) {
        MethodTree method = (MethodTree) member;
        if ("finalize".equals(method.simpleName().name()) &&
          method.parameters().isEmpty() &&
          ModifiersUtils.hasModifier(method.modifiers(), Modifier.FINAL)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isVulnerableConstructor(MethodTree constructor, boolean hasThrowingInitializers) {
    if (ModifiersUtils.hasModifier(constructor.modifiers(), Modifier.PRIVATE)) {
      return false;
    }
    return hasThrowingInitializers || !constructor.throwsClauses().isEmpty() || containsThrowStatement(constructor);
  }

  private static boolean containsThrowStatement(MethodTree method) {
    BlockTree block = method.block();
    if (block == null) {
      return false;
    }
    return containsThrowStatementInBlock(block);
  }

  private static boolean containsThrowStatementInBlock(BlockTree block) {
    ThrowStatementVisitor visitor = new ThrowStatementVisitor();
    block.accept(visitor);
    return visitor.hasThrow;
  }

  private static boolean hasThrowingFieldInitializer(VariableTree variable) {
    if (variable.initializer() == null) {
      return false;
    }
    ThrowStatementVisitor visitor = new ThrowStatementVisitor();
    variable.initializer().accept(visitor);
    return visitor.hasThrow;
  }

  private static class ThrowStatementVisitor extends BaseTreeVisitor {
    boolean hasThrow;

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      hasThrow = true;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip nested classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      // skip lambdas
    }
  }
}
