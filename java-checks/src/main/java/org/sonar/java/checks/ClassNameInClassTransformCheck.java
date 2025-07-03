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
import org.sonar.java.matcher.TreeMatcher;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.matcher.TreeMatcher.calls;
import static org.sonar.java.matcher.TreeMatcher.invokedOn;
import static org.sonar.java.matcher.TreeMatcher.isIdentifier;
import static org.sonar.java.matcher.TreeMatcher.withArgument;

@Rule(key = "S7477")
public class ClassNameInClassTransformCheck extends IssuableSubscriptionVisitor {
  private static final String CLASS_ENTRY_CLASSNAME = "java.lang.classfile.constantpool.ClassEntry";
  private static final String CLASS_MODEL_CLASSNAME = "java.lang.classfile.ClassModel";
  private static final String CLASS_DESC_CLASSNAME = "java.lang.constant.ClassDesc";
  private static final String ISSUE_MESSAGE = "Use `transformClass` overload without the class name.";

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
        (isThisClassOf(classModelId).or(isDescriptorOf(classModelId)).check(classDesc))) {

        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(classDesc)
          .withMessage(ISSUE_MESSAGE)
          .withQuickFix(() -> computeQuickFix(mit.arguments()))
          .report();
      }
    }
  }

  private static JavaQuickFix computeQuickFix(Arguments arguments) {
    return JavaQuickFix
      .newQuickFix("Remove second argument.")
      .addTextEdit(JavaTextEdit.replaceBetweenTree(
              arguments.get(0), false, arguments.get(2), false, ", "))
      .build();
  }

  /** The expression may be of the form `model.thisClass().asInternalName()` or `model.thisClass().name().stringValue(). */
  private TreeMatcher<ExpressionTree> isInternalNameOf(IdentifierTree classModel) {
    return calls(internalNameMatcher, invokedOn(isThisClassOf(classModel)))
            .or(calls(stringValueMatcher, invokedOn(calls(nameMatcher, invokedOn(isThisClassOf(classModel))))));
  }

  /** 
   * The expression may be of the form:
   *   - `model.thisClass().asSymbol()` 
   *   - or `Desc.ofInternalName(internalName)` where `internalName` is the result of `model.thisClass().asInternalName()`
   *   - or `Desc.ofDescriptorString(desc.descriptorString())` where `desc` itself has the form of a descriptor. */
  private TreeMatcher<ExpressionTree> isDescriptorOf(IdentifierTree classModel) {
    return TreeMatcher.recursive(self ->
      calls(asSymbolMatcher, invokedOn(isThisClassOf(classModel)))
        .or(calls(ofInternalNameMatcher, withArgument(0, isInternalNameOf(classModel))))
        .or(calls(ofDescriptorMatcher,
          withArgument(0, calls(descriptorStringMatcher, invokedOn(self))))));
  }

  /** Check whether the given expression is of the form: `classModel.thisClass()`. */
  private TreeMatcher<ExpressionTree> isThisClassOf(IdentifierTree classModel) {
    return calls(thisClassMatcher, invokedOn(isIdentifier(classModel)));
  }
}
