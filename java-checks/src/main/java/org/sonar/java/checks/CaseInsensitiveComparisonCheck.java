/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.*;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1157",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CaseInsensitiveComparisonCheck extends SquidCheck<LexerlessGrammar> implements JavaTreeVisitorProvider {

  @Override
  public JavaTreeVisitor createJavaTreeVisitor() {
    return new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree tree) {
        if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree.methodSelect();
          if ("equals".equals(memberSelect.identifier().name())) {
            if (isToUpperCaseOrToLowerCase(memberSelect.expression()) || (tree.arguments().size() == 1 && isToUpperCaseOrToLowerCase(tree.arguments().get(0)))) {
              getContext().createLineViolation(
                CaseInsensitiveComparisonCheck.this,
                "Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.",
                ((JavaTree) tree).getLine()
              );
            }
          }
        }

        super.visitMethodInvocation(tree);
      }

      private boolean isToUpperCaseOrToLowerCase(ExpressionTree expression) {
        if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
          MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
          if (methodInvocation.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
            MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) methodInvocation.methodSelect();
            String name = memberSelect.identifier().name();
            return "toUpperCase".equals(name) || "toLowerCase".equals(name);
          }
        }
        return false;
      }

    };
  }

}
