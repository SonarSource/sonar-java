/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Rule(key = "S1711")
public class StandardFunctionalInterfaceCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcherCollection OBJECT_METHODS = MethodMatcherCollection.create(
    methodMatcherWithName("equals", "java.lang.Object"),
    methodMatcherWithName("getClass"),
    methodMatcherWithName("hashcode"),
    methodMatcherWithName("notify"),
    methodMatcherWithName("notifyAll"),
    methodMatcherWithName("toString"),
    methodMatcherWithName("wait"),
    methodMatcherWithName("wait", "long"),
    methodMatcherWithName("wait", "long", "int"));

  private static final Set<String> STD_INTERFACE_NAMES = new HashSet<>();

  private static final Map<Integer, List<FunctionalInterface>> STD_INTERFACE_BY_PARAMETER_COUNT = new HashMap<>();

  static {
    registerInterface("java.util.function.BiConsumer<T,U>", "void", "T", "U");
    registerInterface("java.util.function.BiFunction<T,U,R>", "R", "T", "U");
    registerInterface("java.util.function.BinaryOperator<T>", "T", "T", "T");
    registerInterface("java.util.function.BiPredicate<T,U>", "boolean", "T", "U");
    registerInterface("java.util.function.BooleanSupplier", "boolean");
    registerInterface("java.util.function.Consumer<T>", "void", "T");
    registerInterface("java.util.function.DoubleBinaryOperator", "double", "double", "double");
    registerInterface("java.util.function.DoubleConsumer", "void", "double");
    registerInterface("java.util.function.DoubleFunction<R>", "R", "double");
    registerInterface("java.util.function.DoublePredicate", "boolean", "double");
    registerInterface("java.util.function.DoubleSupplier", "double");
    registerInterface("java.util.function.DoubleToIntFunction", "int", "double");
    registerInterface("java.util.function.DoubleToLongFunction", "long", "double");
    registerInterface("java.util.function.DoubleUnaryOperator", "double", "double");
    registerInterface("java.util.function.Function<T,R>", "R", "T");
    registerInterface("java.util.function.IntBinaryOperator", "int", "int", "int");
    registerInterface("java.util.function.IntConsumer", "void", "int");
    registerInterface("java.util.function.IntFunction<R>", "R", "int");
    registerInterface("java.util.function.IntPredicate", "boolean", "int");
    registerInterface("java.util.function.IntSupplier", "int");
    registerInterface("java.util.function.IntToDoubleFunction", "double", "int");
    registerInterface("java.util.function.IntToLongFunction", "long", "int");
    registerInterface("java.util.function.IntUnaryOperator", "int", "int");
    registerInterface("java.util.function.LongBinaryOperator", "long", "long", "long");
    registerInterface("java.util.function.LongConsumer", "void", "long");
    registerInterface("java.util.function.LongFunction<R>", "R", "long");
    registerInterface("java.util.function.LongPredicate", "boolean", "long");
    registerInterface("java.util.function.LongSupplier", "long");
    registerInterface("java.util.function.LongToDoubleFunction", "double", "long");
    registerInterface("java.util.function.LongToIntFunction", "int", "long");
    registerInterface("java.util.function.LongUnaryOperator", "long", "long");
    registerInterface("java.util.function.ObjDoubleConsumer<T>", "void", "T", "double");
    registerInterface("java.util.function.ObjIntConsumer<T>", "void", "T", "int");
    registerInterface("java.util.function.ObjLongConsumer<T>", "void", "T", "long");
    registerInterface("java.util.function.Predicate<T>", "boolean", "T");
    registerInterface("java.util.function.Supplier<T>", "T");
    registerInterface("java.util.function.ToDoubleBiFunction<T,U>", "double", "T", "U");
    registerInterface("java.util.function.ToDoubleFunction<T>", "double", "T");
    registerInterface("java.util.function.ToIntBiFunction<T,U>", "int", "T", "U");
    registerInterface("java.util.function.ToIntFunction<T>", "int", "T");
    registerInterface("java.util.function.ToLongBiFunction<T,U>", "long", "T", "U");
    registerInterface("java.util.function.ToLongFunction<T>", "long", "T");
    registerInterface("java.util.function.UnaryOperator<T>", "T", "T");

    // Each list of FunctionalInterface has to be sorted ascending by number of parametrized types so that smallest number
    // of parametrized types take precedence. For example UnaryOperator<String> and Function<String,String> are equivalent,
    // but UnaryOperator is preferred.
    STD_INTERFACE_BY_PARAMETER_COUNT.values().forEach(list -> list.sort((a, b) -> Integer.compare(a.getGenericTypeCount(), b.getGenericTypeCount())));
  }

  private static void registerInterface(String name, String returnType, String... parameters) {
    FunctionalInterface functionalInterface = new FunctionalInterface(name, returnType, parameters);
    STD_INTERFACE_NAMES.add(functionalInterface.getName());
    STD_INTERFACE_BY_PARAMETER_COUNT.computeIfAbsent(functionalInterface.getParameterCount(), key -> new ArrayList<>()).add(functionalInterface);
  }

  private static MethodMatcher methodMatcherWithName(String name, String... parameters) {
    MethodMatcher methodMatcher = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name(name);
    if(parameters.length == 0) {
      methodMatcher.withoutParameter();
    }
    for (String parameter : parameters) {
      methodMatcher.addParameter(parameter);
    }
    return methodMatcher;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    // classTree.simpleName() never null for Tree.Kind.INTERFACE
    IdentifierTree issueLocation = classTree.simpleName();
    // The question "Why we raise issue only for interface annotated with @FunctionalInterface?"
    // is discussed in comments of https://jira.sonarsource.com/browse/SONARJAVA-504
    Optional.of(classTree)
      .filter(StandardFunctionalInterfaceCheck::isFunctionalInterface)
      .filter(StandardFunctionalInterfaceCheck::isNonStandardFunctionalInterface)
      .filter(StandardFunctionalInterfaceCheck::hasNoExtension)
      .flatMap(StandardFunctionalInterfaceCheck::lookupFunctionalMethod)
      .flatMap(StandardFunctionalInterfaceCheck::lookupMatchingStandardInterface)
      .ifPresent(standardInterface -> reportIssue(issueLocation, buildIssueMessage(classTree, standardInterface)));
  }

  private static boolean isFunctionalInterface(ClassTree tree) {
    return tree.symbol().metadata().isAnnotatedWith("java.lang.FunctionalInterface");
  }

  private static boolean isNonStandardFunctionalInterface(ClassTree tree) {
    return !STD_INTERFACE_NAMES.contains(tree.symbol().type().fullyQualifiedName());
  }

  private static boolean hasNoExtension(ClassTree tree) {
    return tree.superInterfaces().isEmpty();
  }

  private static Optional<MethodSymbol> lookupFunctionalMethod(ClassTree interfaceTree) {
    return interfaceTree.symbol().memberSymbols().stream()
        .filter(Symbol::isMethodSymbol)
        .map(MethodSymbol.class::cast)
        .filter(MethodSymbol::isAbstract)
        .filter(StandardFunctionalInterfaceCheck::isNotObjectMethod)
        .findFirst();
  }

  private static Optional<String> lookupMatchingStandardInterface(MethodSymbol functionalMethod) {
    MethodTree declaration = functionalMethod.declaration();
    if (!functionalMethod.thrownTypes().isEmpty() || (declaration != null && !declaration.typeParameters().isEmpty())) {
      return Optional.empty();
    }
    Type returnType = declaration != null ? declaration.returnType().symbolType() : functionalMethod.returnType().type();
    return STD_INTERFACE_BY_PARAMETER_COUNT.getOrDefault(functionalMethod.parameterTypes().size(), Collections.emptyList()).stream()
        .map(standardInterface -> standardInterface.matchingSpecialization(functionalMethod, returnType))
        .filter(Objects::nonNull)
        .findFirst();
  }

  private static String buildIssueMessage(ClassTree interfaceTree, String standardInterface) {
    if (interfaceTree.members().size() <= 1) {
      return "Drop this interface in favor of \"" + standardInterface + "\".";
    } else {
      return "Make this interface extend \"" + standardInterface + "\" and remove the functional method declaration.";
    }
  }

  private static boolean isNotObjectMethod(MethodSymbol method) {
    MethodTree declaration = method.declaration();
    return declaration == null || !OBJECT_METHODS.anyMatch(declaration);
  }

  private static class FunctionalInterface {

    private final String name;
    private final List<String> genericTypes;
    private final String returnType;
    private final List<String> parameters;

    private FunctionalInterface(String name, String returnType, String... parameters) {
      int genericStart = name.indexOf('<');
      if (genericStart != -1) {
        this.name = name.substring(0, genericStart);
        this.genericTypes = Arrays.asList(name.substring(genericStart + 1, name.length() - 1).split(","));
      } else {
        this.name = name;
        this.genericTypes = Collections.emptyList();
      }
      this.returnType = returnType;
      this.parameters = Arrays.asList(parameters);
    }

    private String getName() {
      return name;
    }

    private int getGenericTypeCount() {
      return genericTypes.size();
    }

    private int getParameterCount() {
      return parameters.size();
    }

    @CheckForNull
    private String matchingSpecialization(MethodSymbol method, Type actualReturnType) {
      Map<String, String> genericTypeMapping = genericTypes.isEmpty() ? Collections.emptyMap() : new HashMap<>();
      String expectedReturnType = convertGenericType(returnType, actualReturnType, genericTypeMapping);
      if (!expectedReturnType.equals(actualReturnType.fullyQualifiedName())) {
        return null;
      }
      List<Type> methodParameters = method.parameterTypes();
      for (int i = 0; i < parameters.size(); i++) {
        Type actualType = methodParameters.get(i);
        String expectedType = convertGenericType(parameters.get(i), actualType, genericTypeMapping);
        if (!expectedType.equals(actualType.fullyQualifiedName())) {
          return null;
        }
      }
      return buildSpecializationName(genericTypeMapping);
    }

    private String convertGenericType(String expectedType, Type actualType, Map<String, String> genericTypeMapping) {
      if (genericTypes.isEmpty() || !genericTypes.contains(expectedType)) {
        return expectedType;
      }
      String convertedType = genericTypeMapping.get(expectedType);
      if (convertedType == null) {
        if (actualType.isPrimitive() || actualType.isVoid() || actualType.isArray() || actualType.isUnknown()) {
          return "!unknown!";
        }
        convertedType = actualType.fullyQualifiedName();
        genericTypeMapping.put(expectedType, convertedType);
      }
      return convertedType;
    }

    private String buildSpecializationName(Map<String, String> genericTypeMapping) {
      if (genericTypes.isEmpty()) {
        return name;
      }
      StringBuilder genericName = new StringBuilder();
      genericName.append(name);
      genericName.append('<');
      boolean addComma = false;
      for (String genericType : genericTypes) {
        if (addComma) {
          genericName.append(',');
        } else {
          addComma = true;
        }
        String typeName = genericTypeMapping.getOrDefault(genericType, genericType);
        int packageEnd = typeName.lastIndexOf('.');
        if (packageEnd != -1) {
          typeName = typeName.substring(packageEnd + 1);
        }
        genericName.append(typeName);
      }
      genericName.append('>');
      return genericName.toString();
    }

  }

}
