/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1068",
  name = "Unused private fields should be removed",
  tags = {"unused"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class UnusedPrivateFieldCheck extends SubscriptionBaseVisitor {

  private static final List<String> EXCLUDED_ANNOTATIONS_TYPE = ImmutableList.<String>builder()
      .add("lombok.Getter")
      .add("lombok.Setter")
      .add("lombok.Value")
      .add("lombok.Data")
      .add("lombok.Builder")
      .add("lombok.ToString")
      .add("lombok.EqualsAndHashCode")
      .add("lombok.AllArgsConstructor")
      .add("lombok.NoArgsConstructor")
      .add("lombok.RequiredArgsConstructor")
      .build();


  private static final Tree.Kind[] ASSIGNMENT_KINDS = {
    Tree.Kind.ASSIGNMENT,
    Tree.Kind.MULTIPLY_ASSIGNMENT,
    Tree.Kind.DIVIDE_ASSIGNMENT,
    Tree.Kind.REMAINDER_ASSIGNMENT,
    Tree.Kind.PLUS_ASSIGNMENT,
    Tree.Kind.MINUS_ASSIGNMENT,
    Tree.Kind.LEFT_SHIFT_ASSIGNMENT,
    Tree.Kind.RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.AND_ASSIGNMENT,
    Tree.Kind.XOR_ASSIGNMENT,
    Tree.Kind.OR_ASSIGNMENT};

  private List<ClassTree> classes = Lists.newArrayList();
  private ListMultimap<Symbol, IdentifierTree> assignments = ArrayListMultimap.create();
  private boolean hasNativeMethod = false;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.COMPILATION_UNIT, Tree.Kind.METHOD, Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void leaveNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.METHOD)) {
        MethodTree method = (MethodTree) tree;
        if (ModifiersUtils.hasModifier(method.modifiers(), Modifier.NATIVE)) {
          hasNativeMethod = true;
        }
      } else if (tree.is(Tree.Kind.CLASS)) {
        classes.add((ClassTree) tree);
      } else if (tree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        ExpressionTree expression = ((ExpressionStatementTree) tree).expression();
        if (expression.is(ASSIGNMENT_KINDS)) {
          addAssignment(((AssignmentExpressionTree) expression).variable());
        }
      } else {
        leaveCompilationUnit();
      }
    }
  }

  private void leaveCompilationUnit() {
    if (!hasNativeMethod) {
      for (ClassTree classTree : classes) {
        checkClassFields(classTree);
      }
    }
    classes.clear();
    assignments.clear();
    hasNativeMethod = false;
  }

  private void checkClassFields(ClassTree classTree) {
    if (!hasExcludedAnnotation(classTree)) {
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.VARIABLE)) {
          checkIfUnused((VariableTree) member);
        }
      }
    }
  }


  public void checkIfUnused(VariableTree tree) {
    if (tree.modifiers().annotations().isEmpty()) {
      Symbol symbol = tree.symbol();
      String name = symbol.name();
      if (symbol.isPrivate() && !"serialVersionUID".equals(name) && symbol.usages().size() == assignments.get(symbol).size()) {
        addIssue(tree, "Remove this unused \"" + name + "\" private field.");
      }
    }
  }
  private static boolean hasExcludedAnnotation(ClassTree classTree) {
    for (String excludedAnnotation : EXCLUDED_ANNOTATIONS_TYPE) {
      if (hasAnnotation(classTree.modifiers(), excludedAnnotation)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAnnotation(ModifiersTree modifiers, String annotationName) {
    for (AnnotationTree annotation : modifiers.annotations()) {
      if (annotation.symbolType().is(annotationName)) {
        return true;
      }
    }
    return false;
  }

  private void addAssignment(ExpressionTree tree) {
    ExpressionTree variable = ExpressionsHelper.skipParentheses(tree);
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      addAssignment((IdentifierTree) variable);
    } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      addAssignment(((MemberSelectExpressionTree) variable).identifier());
    }
  }

  private void addAssignment(IdentifierTree identifier) {
    Symbol reference = identifier.symbol();
    if (!reference.isUnknown()) {
      assignments.put(reference, identifier);
    }
  }

}
