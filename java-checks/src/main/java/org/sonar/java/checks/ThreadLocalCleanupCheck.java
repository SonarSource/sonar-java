/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S5164")
public class ThreadLocalCleanupCheck extends IssuableSubscriptionVisitor {

  private static final String THREAD_LOCAL = "java.lang.ThreadLocal";
  private static final MethodMatchers THREADLOCAL_SET = MethodMatchers.create()
    .ofTypes(THREAD_LOCAL).names("set").addParametersMatcher(ANY).build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      Symbol.TypeSymbol clazz = ((ClassTree) tree).symbol();
      Type superClass = clazz.superClass();
      if (clazz.type().isSubtypeOf(THREAD_LOCAL) || (superClass != null && superClass.isUnknown())) {
        return;
      }
      clazz.memberSymbols().stream()
        .filter(Symbol::isVariableSymbol)
        .filter(s -> s.isPrivate() && s.type().is(THREAD_LOCAL))
        .forEach(this::checkThreadLocalField);
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (THREADLOCAL_SET.matches(mit) && mit.arguments().get(0).is(Tree.Kind.NULL_LITERAL)) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(mit)
          .withMessage("Use \"remove()\" instead of \"set(null)\".")
          .withQuickFix(() -> JavaQuickFix.newQuickFix("Replace with \"remove()\"")
            .addTextEdit(JavaTextEdit.replaceBetweenTree(ExpressionUtils.methodName(mit), mit.arguments(), "remove()"))
            .build())
          .report();
      }
    }
  }

  private void checkThreadLocalField(Symbol field) {
    if (field.usages().stream().noneMatch(ThreadLocalCleanupCheck::usageIsRemove)) {
      reportIssue(((VariableTree) field.declaration()).simpleName(), "Call \"remove()\" on \"" + field.name() + "\".");
    }
  }

  private static boolean usageIsRemove(IdentifierTree usage) {
    return MethodTreeUtils.consecutiveMethodInvocation(usage)
      // At this point, we know that "usage" is of type ThreadLocal, we don't have to check the full type, the name is enough.
      .filter(mit -> "remove".equals(ExpressionUtils.methodName(mit).name()))
      .isPresent();
  }
}

