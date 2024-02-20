/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.serialization;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

@Rule(key = "S1948")
public class SerializableFieldInSerializableClassCheck extends IssuableSubscriptionVisitor {

  private static final String JAVAX_INJECT = "javax.inject.Inject";
  private static final String JAKARTA_INJECT = "jakarta.inject.Inject";
  private static final String[] EXCLUDED_ANNOTATIONS = new String[]{
    "jakarta.annotation.Resource",
    "javax.annotation.Resource",
    "javax.ejb.EJB",
    JAVAX_INJECT,
    JAKARTA_INJECT,
    "org.apache.wicket.spring.injection.annot.SpringBean"
  };

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (isSerializable(classTree)
      && !SerializableContract.hasSpecialHandlingSerializationMethods(classTree)
      && !classTree.symbol().type().isSubtypeOf("javax.servlet.http.HttpServlet")) {

      Set<String> constructorInjectedParams = getConstructorOrMethodInjectedFields(classTree);

      classTree.members().stream()
        .filter(member -> member.is(Tree.Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .filter(variableTree -> !(isExcluded(variableTree) ||
          constructorInjectedParams.contains(variableTree.simpleName().name())))
        .forEach(this::checkVariableMember);
    }
  }
  
  private static Set<String> getConstructorOrMethodInjectedFields(ClassTree classTree) {
    AssignmentsVisitor assignmentsVisitor = new AssignmentsVisitor();
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(methodTree -> isAnnotatedWith(methodTree.symbol().metadata(), JAVAX_INJECT))
      .forEach(methodTree -> methodTree.accept(assignmentsVisitor));
    
    return assignmentsVisitor.getAssignedVariables();
  }

  private void checkVariableMember(VariableTree variableTree) {
    IdentifierTree simpleName = variableTree.simpleName();
    if (isCollectionOfSerializable(variableTree.type())) {
      if (!ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.PRIVATE)
        && !implementsSerializable(variableTree.type().symbolType())) {
        reportIssue(simpleName, "Make \"" + simpleName.name() + "\" private or transient.");
      } else if (isUnserializableCollection(variableTree.type().symbolType())
        || isUnserializableCollection(variableTree.initializer())) {
        reportIssue(simpleName);
      }
      checkCollectionAssignments(variableTree.symbol().usages());
    } else {
      ExpressionTree initializer = variableTree.initializer();
      Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) variableTree.symbol();
      if (initializer == null || !(variableSymbol.isFinal() && implementsSerializable(initializer.symbolType()))) {
        reportIssue(simpleName);
      }
    }
  }

  private static boolean isUnserializableCollection(@Nullable ExpressionTree expression) {
    return expression != null && !expression.is(Tree.Kind.NULL_LITERAL) && isUnserializableCollection(expression.symbolType());
  }

  private static boolean isUnserializableCollection(Type type) {
    return !type.symbol().isInterface() && isSubtypeOfCollectionApi(type) && !implementsSerializable(type);
  }

  private void checkCollectionAssignments(List<IdentifierTree> usages) {
    for (IdentifierTree usage : usages) {
      Tree parentTree = usage.parent();
      if (parentTree.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) parentTree;
        if (usage.equals(assignment.variable()) && isUnserializableCollection(assignment.expression())) {
          reportIssue(usage);
        }
      }
    }
  }

  private void reportIssue(IdentifierTree tree) {
    reportIssue(tree, "Make \"" + tree.name() + "\" transient or serializable.");
  }

  private static boolean isExcluded(VariableTree variableTree) {
    return isStatic(variableTree) || isTransientSerializableOrInjected(variableTree);
  }

  private static boolean isCollectionOfSerializable(Tree tree) {
    if (tree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      ParameterizedTypeTree typeTree = (ParameterizedTypeTree) tree;
      if (isSubtypeOfCollectionApi(typeTree.symbolType())) {
        return typeTree.typeArguments().stream().allMatch(SerializableFieldInSerializableClassCheck::isCollectionOfSerializable);
      }
    }
    return isSerializable(tree);
  }

  private static boolean isSubtypeOfCollectionApi(Type type) {
    return type.isSubtypeOf("java.util.Collection") || type.isSubtypeOf("java.util.Map");
  }

  private static boolean isStatic(VariableTree member) {
    return ModifiersUtils.hasModifier(member.modifiers(), Modifier.STATIC);
  }

  private static boolean isTransientSerializableOrInjected(VariableTree member) {
    if (ModifiersUtils.hasModifier(member.modifiers(), Modifier.TRANSIENT) || (isSerializable(member.type()) && !isSubtypeOfCollectionApi(member.type().symbolType()))) {
      return true;
    }
    SymbolMetadata metadata = member.symbol().metadata();
    return isAnnotatedWith(metadata, EXCLUDED_ANNOTATIONS);
  }

  private static boolean isAnnotatedWith(SymbolMetadata metadata, String... fullyQualifiedNames) {
    Set<String> fullyQualifiedNamesSet = new HashSet<>(Arrays.asList(fullyQualifiedNames));
    return metadata.annotations().stream()
      .map(a -> a.symbol().type())
      .anyMatch(t -> t.isUnknown() || fullyQualifiedNamesSet.contains(t.fullyQualifiedName()));
  }

  private static boolean isSerializable(Tree tree) {
    if (tree.is(Tree.Kind.ENUM, Tree.Kind.PRIMITIVE_TYPE)) {
      return true;
    } else if (tree.is(Tree.Kind.CLASS)) {
      Symbol.TypeSymbol symbol = ((ClassTree) tree).symbol();
      return implementsSerializable(symbol.type());
    } else if (tree.is(Tree.Kind.EXTENDS_WILDCARD, Tree.Kind.SUPER_WILDCARD, Tree.Kind.UNBOUNDED_WILDCARD)) {
      TypeTree bound = ((WildcardTree) tree).bound();
      return bound != null && implementsSerializable(bound.symbolType());
    }
    return implementsSerializable(((TypeTree) tree).symbolType());
  }

  private static boolean implementsSerializable(@Nullable Type type) {
    if (type == null || type.isUnknown() || type.isPrimitive()) {
      // do not raise an issue if type is unknown
      return true;
    }
    if (type.isArray()) {
      return implementsSerializable(((Type.ArrayType) type).elementType());
    }
    if (type.isClass() || type.isTypeVar()) {
      return type.isSubtypeOf("java.io.Serializable");
    }
    return false;
  }

  private static final class AssignmentsVisitor extends BaseTreeVisitor {
    private final Set<String> assignedVariables = new HashSet<>();

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (ExpressionUtils.isSelectOnThisOrSuper(tree)) {
        MemberSelectExpressionTree select = ((MemberSelectExpressionTree) tree.variable());
        assignedVariables.add(select.identifier().name());
      } else if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = ((IdentifierTree) tree.variable());
        assignedVariables.add(identifier.name());
      }
    }

    public Set<String> getAssignedVariables() {
      return Collections.unmodifiableSet(assignedVariables);
    }
  }
}
