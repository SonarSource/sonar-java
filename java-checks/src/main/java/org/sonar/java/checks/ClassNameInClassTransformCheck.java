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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.*;

@Rule(key = "S7477")
public class ClassNameInClassTransformCheck extends IssuableSubscriptionVisitor {
  private static final String CLASS_ENTRY_CLASSNAME = "java.lang.classfile.constantpool.ClassEntry";

  MethodMatchers classTransformMatcher = MethodMatchers.create()
    .ofTypes("java.lang.classfile.ClassFile")
    .names("transformClass")
    .addParametersMatcher(
      "java.lang.classfile.ClassModel",
      "java.lang.constant.ClassDesc",
      "java.lang.classfile.ClassTransform")
    .build();

  MethodMatchers thisClassMatcher = MethodMatchers.create()
    .ofTypes("java.lang.classfile.ClassModel")
    .names("thisClass")
    .addWithoutParametersMatcher()
    .build();

  MethodMatchers internalNameMatcher = MethodMatchers.create()
    .ofTypes(CLASS_ENTRY_CLASSNAME)
    .names("asInternalName")
    .addWithoutParametersMatcher()
    .build();

  MethodMatchers asSymbolMatcher = MethodMatchers.create()
    .ofTypes(CLASS_ENTRY_CLASSNAME)
    .names("asSymbol")
    .addWithoutParametersMatcher()
    .build();

  MethodMatchers nameMatcher = MethodMatchers.create()
    .ofTypes(CLASS_ENTRY_CLASSNAME)
    .names("name")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (classTransformMatcher.matches(mit)) {
      ExpressionTree classModel = mit.arguments().get(0);
      ExpressionTree classDesc = mit.arguments().get(1);
      // TODO we only handle case where classModel is a variable for now
      if (classModel instanceof IdentifierTree classModelId && isClassNameOf(classModelId, classDesc)) {
        context.reportIssue(this, classDesc, "Unchanged class name in transformClass call.");
      }
    }
  }

  private boolean isClassNameOf(IdentifierTree classModel, ExpressionTree classTree) {
    if (classTree instanceof MethodInvocationTree mit) {
      if (asSymbolMatcher.matches(mit) || internalNameMatcher.matches(mit)) {
        // TODO complete
        if (mit.methodSelect() instanceof MemberSelectExpressionTree mset) {
          return isThisClassOf(classModel, mset.expression());
        }
      }
    }
    return false;
  }

  private boolean isThisClassOf(IdentifierTree classModel, ExpressionTree expression) {
    if (expression instanceof MethodInvocationTree mit && thisClassMatcher.matches(mit)) {
      if (mit.methodSelect() instanceof MemberSelectExpressionTree mset
        && mset.expression() instanceof IdentifierTree identifier) {
        return identifier.symbol().equals(classModel.symbol());
      }
    }
    return false;
  }
}
