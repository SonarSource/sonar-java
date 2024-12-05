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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3252")
public class StaticMemberAccessCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers LIST_SET_OF = MethodMatchers.create()
    .ofTypes("java.util.List", "java.util.Set")
    .names("of")
    .withAnyParameters()
    .build();

  private String currentPackage = "";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.MEMBER_SELECT, Tree.Kind.COMPILATION_UNIT);
  }


  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof CompilationUnitTree file) {
      PackageDeclarationTree packageDecl = file.packageDeclaration();
      currentPackage = packageDecl == null ? "" : packageDecl.packageName().toString();
      return;
    }

    MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
    Symbol symbol = mse.identifier().symbol();
    if (symbol.isStatic() && !isListOrSetOf(mse)) {
      ExpressionTree expression = mse.expression();

      Symbol owner = symbol.owner();
      Type staticType = owner.type();
      Type expressionType = expression.symbolType();

      if (!staticType.isUnknown() && !expressionType.isUnknown()
        && !expressionType.erasure().equals(staticType.erasure())
        && (currentPackage.equals(classPackage(expressionType)) || owner.isPublic())
      ) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(mse.identifier())
          .withMessage("Use static access with \"%s\" for \"%s\".", staticType.fullyQualifiedName(), symbol.name())
          .withQuickFix(() -> quickFix(expression, staticType))
          .report();
      }
    }
  }

  private static String classPackage(Type classType) {
    int endPackage = classType.fullyQualifiedName().lastIndexOf('.');
    return endPackage == -1 ? "" : classType.fullyQualifiedName().substring(0, endPackage);
  }

  private static boolean isListOrSetOf(MemberSelectExpressionTree mse) {
    // this is necessary because we incorrectly resolve to Set#of List#of methods on JDK11
    // see SONARJAVA-3095
    Tree parent = mse.parent();
    return parent.is(Tree.Kind.METHOD_INVOCATION) && LIST_SET_OF.matches((MethodInvocationTree) parent);
  }

  private static JavaQuickFix quickFix(ExpressionTree expression, Type staticType) {
    String oldType = expression.symbolType().name();
    String newType = staticType.name();
    return JavaQuickFix.newQuickFix(String.format("Use \"%s\" instead of \"%s\"", newType, oldType))
      .addTextEdit(JavaTextEdit.replaceTree(expression, newType))
      .build();
  }
}
