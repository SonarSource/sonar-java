/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
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
  private static final MethodMatchers THREADLOCAL_REMOVE = MethodMatchers.create()
    .ofTypes(THREAD_LOCAL).names("remove").addWithoutParametersMatcher().build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.CLASS)) {
      Symbol.TypeSymbol clazz = ((ClassTree) tree).symbol();
      if (clazz.type().isSubtypeOf(THREAD_LOCAL)) {
        return;
      }
      clazz.memberSymbols().stream()
        .filter(Symbol::isVariableSymbol)
        .filter(s -> s.isPrivate() && s.type().is(THREAD_LOCAL))
        .forEach(this::checkThreadLocalField);
    } else {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (THREADLOCAL_SET.matches(mit) && mit.arguments().get(0).is(Tree.Kind.NULL_LITERAL)) {
        reportIssue(mit, "Use \"remove()\" instead of \"set(null)\".");
      }
    }
  }

  private void checkThreadLocalField(Symbol field) {
    if (field.usages().stream().noneMatch(ThreadLocalCleanupCheck::usageIsRemove)) {
      reportIssue(((VariableTree) field.declaration()).simpleName(), "Call \"remove()\" on \"" + field.name() + "\".");
    }
  }

  private static boolean usageIsRemove(IdentifierTree usage) {
    Tree parent = usage.parent();
    if (parent.is(Tree.Kind.MEMBER_SELECT)) {
      Tree mseParent = parent.parent();
      return mseParent != null && mseParent.is(Tree.Kind.METHOD_INVOCATION)
        && THREADLOCAL_REMOVE.matches((MethodInvocationTree) mseParent);
    }
    return false;
  }
}

