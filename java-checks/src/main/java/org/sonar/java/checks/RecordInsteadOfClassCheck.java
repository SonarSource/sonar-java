/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
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
    if (!classSymbol.isFinal()) {
      // records can not be extended
      return;
    }

    List<Symbol.VariableSymbol> fields = classFields(classSymbol);
    if (fields.isEmpty() || !hasOnlyPrivateFinalFields(fields)) {
      return;
    }
    List<Symbol.MethodSymbol> methods = classMethods(classSymbol);
    Map<String, Type> fieldsNameToType = fields.stream().collect(Collectors.toMap(Symbol::name, Symbol::type));

    if (!hasGetterForEveryField(methods, fieldsNameToType)) {
      return;
    }
    List<Symbol.MethodSymbol> constructors = classConstructors(methods);
    if (constructors.size() != 1) {
      return;
    }
    Symbol.MethodSymbol constructor = constructors.get(0);
    if (hasParameterForEveryField(constructor, fieldsNameToType.keySet()) && !constructorHasSmallerVisibility(constructor, classSymbol)) {
      reportIssue(classTree.simpleName(), String.format("Refactor this class declaration to use 'record %s'.", recordName(classTree, constructor)));
    }
  }

  private static boolean constructorHasSmallerVisibility(Symbol.MethodSymbol constructor, Symbol.TypeSymbol classSymbol) {
    boolean constructorIsPrivate = constructor.isPrivate();
    boolean constructorIsPackageVisibility = constructor.isPackageVisibility();
    if (classSymbol.isPublic()) {
      return constructorIsPrivate || constructorIsPackageVisibility || constructor.isProtected();
    } else if (classSymbol.isProtected()) {
      return constructorIsPrivate || constructorIsPackageVisibility;
    } else if (classSymbol.isPackageVisibility()) {
      return constructorIsPrivate;
    }
    return false;
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

  private static boolean hasGetterForEveryField(List<Symbol.MethodSymbol> methods, Map<String, Type> fieldsNameToType) {
    Set<String> gettersForField = methods.stream()
      .filter(m -> isGetter(m, fieldsNameToType))
      .map(Symbol::name)
      .map(RecordInsteadOfClassCheck::toFieldName)
      .collect(Collectors.toSet());
    return gettersForField.containsAll(fieldsNameToType.keySet());
  }

  private static boolean isGetter(Symbol.MethodSymbol method, Map<String, Type> fieldsNameToType) {
    String methodName = method.name();
    if (!method.parameterTypes().isEmpty()) {
      return false;
    }
    if (matchNameAndType(methodName, method, fieldsNameToType)) {
      // simple more recent 'myField()' form for getters
      return true;
    }
    if ("get".equals(methodName) || "is".equals(methodName)) {
      return false;
    }
    // traditional getters: 'getMyField()' or 'isMyBooleanField()'
    return (methodName.startsWith("get") || methodName.startsWith("is"))
      && matchNameAndType(toFieldName(methodName), method, fieldsNameToType);
  }

  private static boolean matchNameAndType(String methodName, Symbol.MethodSymbol method, Map<String, Type> fieldsNameToType) {
    return method.returnType().type().equals(fieldsNameToType.get(methodName));
  }

  private static String toFieldName(String methodName) {
    if (methodName.startsWith("is")) {
      return lowerCaseFirstLetter(methodName.substring(2));
    }
    if (methodName.startsWith("get")) {
      return lowerCaseFirstLetter(methodName.substring(3));
    }
    return methodName;
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
    return parameterNames.equals(fieldNames);
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
      return parametersAsString.substring(0, 47) + "...";
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
