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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

@Rule(
  key = EqualsNotOverridenWithCompareToCheck.RULE_KEY,
  priority = Priority.CRITICAL,
  tags={"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class EqualsNotOverridenWithCompareToCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1210";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    if ((tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM)) && isComparable(tree)) {
      boolean hasEquals = false;
      Tree compare = null;

      for (Tree member : tree.members()) {
        if (member.is(Tree.Kind.METHOD)) {
          MethodTree method = (MethodTree) member;

          if (isEqualsMethod(method)) {
            hasEquals = true;
          } else if (isCompareToMethod(method)) {
            compare = member;
          }
        }
      }

      if (compare != null && !hasEquals) {
        context.addIssue(compare, ruleKey, "Override \"equals(Object obj)\" to comply with the contract of the \"compareTo(T o)\" method.");
      }
    }
    super.visitClass(tree);
  }

  private boolean isCompareToMethod(MethodTree method) {
    String name = method.simpleName().name();
    return "compareTo".equals(name) && returnsInt(method) && method.parameters().size() == 1;
  }

  private boolean isEqualsMethod(MethodTree method) {
    String name = method.simpleName().name();
    return "equals".equals(name) && hasObjectParam(method) && returnsBoolean(method);
  }


  private boolean isComparable(ClassTree tree) {
    Symbol.TypeSymbol typeSymbol = ((ClassTreeImpl) tree).getSymbol();
    if (typeSymbol == null) {
      return false;
    }
    for (Type type : typeSymbol.getInterfaces()) {
      if ("Comparable".equals(((Type.ClassType) type).getSymbol().getName())) {
        return true;
      }
    }
    return false;
  }

  private boolean hasObjectParam(MethodTree tree) {
    boolean result = false;
    if (tree.parameters().size() == 1 && tree.parameters().get(0).type().is(Tree.Kind.IDENTIFIER)) {
      result = ((IdentifierTree) tree.parameters().get(0).type()).name().endsWith("Object");
    }
    return result;
  }

  private boolean returnsBoolean(MethodTree tree) {
    Symbol.MethodSymbol methodSymbol = ((MethodTreeImpl) tree).getSymbol();
    return methodSymbol != null && methodSymbol.getReturnType().getType().isTagged(Type.BOOLEAN);
  }

  private boolean returnsInt(MethodTree tree) {
    Symbol.MethodSymbol methodSymbol = ((MethodTreeImpl) tree).getSymbol();
    return methodSymbol != null && methodSymbol.getReturnType().getType().isTagged(Type.INT);
  }

}
