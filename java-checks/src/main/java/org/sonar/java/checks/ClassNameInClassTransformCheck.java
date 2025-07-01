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
import java.util.function.Predicate;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

@Rule(key = "S7477")
public class ClassNameInClassTransformCheck extends IssuableSubscriptionVisitor {
  private static final String CLASS_ENTRY_CLASSNAME = "java.lang.classfile.constantpool.ClassEntry";
  private static final String CLASS_MODEL_CLASSNAME = "java.lang.classfile.ClassModel";
  private static final String CLASS_DESC_CLASSNAME = "java.lang.constant.ClassDesc";

  MethodMatchers classTransformMatcher = MethodMatchers.create()
    .ofTypes("java.lang.classfile.ClassFile")
    .names("transformClass")
    .addParametersMatcher(
      CLASS_MODEL_CLASSNAME,
      CLASS_DESC_CLASSNAME,
      "java.lang.classfile.ClassTransform")
    .addParametersMatcher(
      CLASS_MODEL_CLASSNAME,
      CLASS_ENTRY_CLASSNAME,
      "java.lang.classfile.ClassTransform")
    .build();

  MethodMatchers thisClassMatcher = MethodMatchers.create()
    .ofTypes(CLASS_MODEL_CLASSNAME)
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

  MethodMatchers stringValueMatcher = MethodMatchers.create()
    .ofTypes("java.lang.classfile.constantpool.Utf8Entry")
    .names("stringValue")
    .addWithoutParametersMatcher()
    .build();

  MethodMatchers ofDescriptorMatcher = MethodMatchers.create()
    .ofTypes(CLASS_DESC_CLASSNAME)
    .names("ofDescriptor")
    .addParametersMatcher("java.lang.String")
    .build();

  MethodMatchers ofInternalNameMatcher = MethodMatchers.create()
    .ofTypes(CLASS_DESC_CLASSNAME)
    .names("ofInternalName")
    .addParametersMatcher("java.lang.String")
    .build();

  MethodMatchers descriptorStringMatcher = MethodMatchers.create()
    .ofTypes(CLASS_DESC_CLASSNAME)
    .names("descriptorString")
    .addWithoutParametersMatcher()
    .build();

  static class ExpressionMatcher {
    @Nullable
    private final ExpressionTree expressionTree;

    ExpressionMatcher(@Nullable ExpressionTree expressionTree) {
      this.expressionTree = expressionTree;
    }

    CallMatcher calls(MethodMatchers methodMatchers) {
      if (expressionTree instanceof MethodInvocationTree mit && methodMatchers.matches(mit)) {
        return new CallMatcher(mit);
      }
      return new CallMatcher(null);
    }

    boolean isIdentifier(IdentifierTree identifier) {
      if (expressionTree instanceof IdentifierTree id) {
        return id.symbol().equals(identifier.symbol());
      }
      return false;
    }

    boolean matches(Predicate<ExpressionTree> predicate) {
      return expressionTree != null && predicate.test(expressionTree);
    }
  }

  static class CallMatcher {

    /** When {@link #methodInvocationTree} is null, the matcher doesn't match anything. */
    @Nullable
    private final MethodInvocationTree methodInvocationTree;

    CallMatcher(@Nullable MethodInvocationTree methodInvocationTree) {
      this.methodInvocationTree = methodInvocationTree;
    }

    ExpressionMatcher onExpression() {
      if (methodInvocationTree != null && methodInvocationTree.methodSelect() instanceof MemberSelectExpressionTree mset) {
        return new ExpressionMatcher(mset.expression());
      }
      return new ExpressionMatcher(null);
    }

    ExpressionMatcher withArgument(int argumentIndex) {
      if (methodInvocationTree != null && methodInvocationTree.arguments().size() >= argumentIndex) {
        return new ExpressionMatcher(methodInvocationTree.arguments().get(argumentIndex));
      }
      return new ExpressionMatcher(null);
    }
  }

  static ExpressionMatcher check(ExpressionTree expression) {
    return new ExpressionMatcher(expression);
  }

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
      if (classModel instanceof IdentifierTree classModelId &&
        (isThisClassOf(classModelId).test(classDesc) || isDescriptorOf(classModelId).test(classDesc))) {
        context.reportIssue(this, classDesc, "Use `transformClass` overload without the class name.");
      }
    }
  }

  /** The expression may be of the form `model.thisClass().asInternalName()` or `model.thisClass().name().stringValue(). */
  private Predicate<ExpressionTree> isInternalNameOf(IdentifierTree classModel) {
    return expression -> check(expression).calls(internalNameMatcher).onExpression().matches(isThisClassOf(classModel))
      || check(expression).calls(stringValueMatcher).onExpression().calls(nameMatcher).onExpression().matches(isThisClassOf(classModel));
  }

  /** 
   * The expression may be of the form:
   *   - `model.thisClass().asSymbol()` 
   *   - or `Desc.ofInternalName(internalName)` where `internalName` is the result of `model.thisClass().asInternalName()`
   *   - or `Desc.ofDescriptorString(desc.descriptorString())` where `desc` itself has the form of a descriptor. */
  private Predicate<ExpressionTree> isDescriptorOf(IdentifierTree classModel) {
    return descTree -> check(descTree).calls(asSymbolMatcher).onExpression().matches(isThisClassOf(classModel))
      || check(descTree).calls(ofInternalNameMatcher).withArgument(0).matches(isInternalNameOf(classModel))
      || check(descTree).calls(ofDescriptorMatcher).withArgument(0)
        .calls(descriptorStringMatcher).onExpression().matches(isDescriptorOf(classModel));
  }

  /** Check whether the given expression is of the form: `classModel.thisClass()`. */
  private Predicate<ExpressionTree> isThisClassOf(IdentifierTree classModel) {
    return expression -> check(expression).calls(thisClassMatcher).onExpression().isIdentifier(classModel);
  }
}
