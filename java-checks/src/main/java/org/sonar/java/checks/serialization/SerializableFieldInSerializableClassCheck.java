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
package org.sonar.java.checks.serialization;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
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
      
      Set<String> constructorInjectedParams = getConstructorInjectedFields(classTree);
      Set<String> setterInjectedParams = getSetterInjectedFields(classTree);
      
      classTree.members().stream()
        .filter(member -> member.is(Tree.Kind.VARIABLE))
        .map(VariableTree.class::cast)
        .filter(variableTree -> 
          !(constructorInjectedParams.contains(variableTree.simpleName().name()) ||
            setterInjectedParams.contains(variableTree.simpleName().name())))
        .forEach(this::checkVariableMember);
    }
  }

  private static Set<String> getSetterInjectedFields(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(SerializableFieldInSerializableClassCheck::isAnnotatedWithInject)
      .map(methodTree -> methodTree.simpleName().name())
      .filter(name -> name.startsWith("set"))
      .map(name -> name.substring(3).toLowerCase(Locale.ROOT))
      .collect(Collectors.toSet());
  }

  private static Set<String> getConstructorInjectedFields(ClassTree classTree) {
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .filter(SerializableFieldInSerializableClassCheck::isAnnotatedWithInject)
      .flatMap(methodTree -> methodTree.parameters().stream())
      .map(variableTree -> variableTree.simpleName().name())
      .collect(Collectors.toSet());
  }

  private static boolean isAnnotatedWithInject(MethodTree methodTree) {
    return methodTree.symbol().metadata().isAnnotatedWith(JAVAX_INJECT)
      || hasAnnotationsWithIncompleteSemantic(methodTree.symbol().metadata());
  }

  private void checkVariableMember(VariableTree variableTree) {
    if (!isExcluded(variableTree)) {
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
    return metadata.isAnnotatedWith(JAVAX_INJECT)
      || metadata.isAnnotatedWith("javax.ejb.EJB")
      || metadata.isAnnotatedWith("org.apache.wicket.spring.injection.annot.SpringBean")
      || hasAnnotationsWithIncompleteSemantic(metadata);
  }

  private static boolean hasAnnotationsWithIncompleteSemantic(SymbolMetadata metadata) {
    return metadata.annotations().stream().anyMatch(annotation -> annotation.symbol().isUnknown());
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
    if (type.isClass() || JUtils.isTypeVar(type)) {
      return type.isSubtypeOf("java.io.Serializable");
    }
    return false;
  }

}
