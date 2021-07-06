/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6206")
public class RecordInsteadOfClassCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava16Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.superClass() != null) {
      // records can not extends other classes
      return;
    }
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (classSymbol.isAbstract()) {
      // records can not be abstract
      return;
    }

    List<Symbol.VariableSymbol> fields = classFields(classSymbol);
    if (fields.isEmpty() || !hasOnlyPrivateFinalFields(fields)) {
      return;
    }
    List<Symbol.MethodSymbol> methods = classMethods(classSymbol);
    Set<String> fieldNames = fields.stream()
      .map(Symbol::name)
      .collect(Collectors.toSet());

    if (hasSetter(methods, fieldNames) || !hasGetterForEveryField(methods, fieldNames)) {
      return;
    }
    List<Symbol.MethodSymbol> constructors = classConstructors(methods);
    if (constructors.size() != 1) {
      return;
    }
    Symbol.MethodSymbol constructor = constructors.get(0);
    if (hasParameterForEveryField(constructor, fieldNames)) {
      reportIssue(classTree.simpleName(), String.format("Refactor this class declaration to use 'record %s'.", recordName(classTree, constructor)));
    }
  }

  private static List<Symbol.MethodSymbol> classMethods(Symbol.TypeSymbol classSymbol) {
    return classSymbol
      .memberSymbols()
      .stream()
      .filter(Symbol::isMethodSymbol)
      .map(Symbol.MethodSymbol.class::cast)
      .collect(Collectors.toList());
  }

  private static List<Symbol.VariableSymbol> classFields(Symbol.TypeSymbol classSymbol) {
    return classSymbol
      .memberSymbols()
      .stream()
      .filter(Symbol::isVariableSymbol)
      // records can have constant, so discarding them
      .filter(s -> !isConstant(s))
      .map(Symbol.VariableSymbol.class::cast)
      .collect(Collectors.toList());
  }

  private static List<Symbol.MethodSymbol> classConstructors(List<Symbol.MethodSymbol> methods) {
    return methods.stream()
      .filter(m -> "<init>".equals(m.name()))
      // only explicit constructors
      .filter(m -> m.declaration() != null)
      .collect(Collectors.toList());
  }

  private static boolean hasOnlyPrivateFinalFields(List<Symbol.VariableSymbol> fields) {
    return fields.stream()
      .allMatch(RecordInsteadOfClassCheck::isPrivateFinal);
  }

  private static boolean isConstant(Symbol symbol) {
    return symbol.isStatic() && symbol.isFinal();
  }

  private static boolean isPrivateFinal(Symbol symbol) {
    return symbol.isPrivate() && symbol.isFinal();
  }

  private static boolean hasSetter(List<Symbol.MethodSymbol> methods, Set<String> fieldNames) {
    return methods.stream().anyMatch(m -> isSetter(m, fieldNames));
  }

  private static boolean isSetter(Symbol.MethodSymbol method, Set<String> fieldNames) {
    String methodName = method.name();
    return methodName.startsWith("set")
      && fieldNames.contains(toFieldName(methodName))
      && method.parameterTypes().size() == 1;
  }

  private static boolean hasGetterForEveryField(List<Symbol.MethodSymbol> methods, Set<String> fieldNames) {
    Set<String> gettersForField = methods.stream()
      .filter(m -> isGetter(m, fieldNames))
      .map(Symbol::name)
      .map(RecordInsteadOfClassCheck::toFieldName)
      .collect(Collectors.toSet());
    return gettersForField.containsAll(fieldNames);
  }

  private static boolean isGetter(Symbol.MethodSymbol method, Set<String> fieldNames) {
    String methodName = method.name();
    return (methodName.startsWith("get") || methodName.startsWith("is"))
      && fieldNames.contains(toFieldName(methodName))
      && method.parameterTypes().isEmpty();
  }

  private static String toFieldName(String methodName) {
    int index = methodName.startsWith("is") ? 2 : /* get/set...() */ 3;
    return lowerCaseFirstLetter(methodName.substring(index));
  }

  private static String lowerCaseFirstLetter(String methodName) {
    return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
  }

  private static boolean hasParameterForEveryField(Symbol.MethodSymbol constructor, Set<String> fieldNames) {
    Set<String> parameterNames = constructor.declaration()
      .parameters()
      .stream()
      .map(VariableTree::simpleName)
      .map(IdentifierTree::name)
      .collect(Collectors.toSet());
    return parameterNames.size() == fieldNames.size() && parameterNames.containsAll(fieldNames);
  }

  private static String recordName(ClassTree classTree, Symbol.MethodSymbol constructor) {
    String typeName = classTree.simpleName().name();
    return String.format("%s(%s)", typeName, parametersAsString(constructor.declaration().parameters()));
  }

  private static String parametersAsString(List<VariableTree> parameters) {
    String parametersAsString = parameters.stream()
      .map(p -> String.format("%s %s", typeAsString(p.type()), p.simpleName().name()))
      .collect(Collectors.joining(", "));
    if (parametersAsString.length() > 50) {
      return parametersAsString.substring(0, 50) + "...";
    }
    return parametersAsString;
  }

  /**
   * Extract type name from tree to not limit impact of unresolved types
   */
  private static String typeAsString(TypeTree type) {
    switch (type.kind()) {
      case PARAMETERIZED_TYPE:
        return typeAsString(((ParameterizedTypeTree) type).type()) + "<...>";
      case IDENTIFIER:
        return ((IdentifierTree) type).name();
      case ARRAY_TYPE:
        ArrayTypeTree arrayTypeTree = (ArrayTypeTree) type;
        String arrayText = arrayTypeTree.ellipsisToken() != null ? " ..." : "[]";
        return typeAsString(arrayTypeTree.type()) + arrayText;
      case PRIMITIVE_TYPE:
        return ((PrimitiveTypeTree) type).keyword().text();
      case MEMBER_SELECT:
        return typeAsString(((MemberSelectExpressionTree) type).identifier());
      default:
        // This should not be possible. The Remaining TypeTrees are UnionType, WildcardType and VarType, which can not be used in such context
        return "?";
    }
  }
}
