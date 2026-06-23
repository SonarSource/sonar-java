/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.AnnotationsHelper.hasUnknownAnnotation;

@Rule(key = "S6206")
public class RecordInsteadOfClassCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final String JAVA_IO_EXTERNALIZABLE = "java.io.Externalizable";
  private static final String JAVA_IO_SERIALIZABLE = "java.io.Serializable";

  private static final Set<String> JACKSON_ANNOTATION_PACKAGES = Set.of(
    "com.fasterxml.jackson.annotation.",
    "com.fasterxml.jackson.databind.annotation.");
  private static final Set<String> GSON_ANNOTATION_PACKAGES = Set.of("com.google.gson.annotations.");
  private static final Set<String> MICRONAUT_ANNOTATION_PACKAGES = Set.of(
    "io.micronaut.core.annotation.",
    "io.micronaut.data.annotation.",
    "io.micronaut.serde.annotation.");
  private static final Set<String> JAKARTA_EE_ANNOTATION_PACKAGES = Set.of(
    "jakarta.inject.",
    "jakarta.persistence.",
    "jakarta.xml.bind.annotation.");
  private static final Set<String> JAVA_EE_ANNOTATION_PACKAGES = Set.of(
    "javax.inject.",
    "javax.persistence.",
    "javax.xml.bind.annotation.");
  private static final Set<String> LOMBOK_ANNOTATION_PACKAGES = Set.of("lombok.");
  private static final Set<String> SPRING_ANNOTATION_PACKAGES = Set.of(
    SpringUtils.BEANS_FACTORY_ANNOTATION_PACKAGE,
    SpringUtils.BOOT_CONTEXT_PROPERTIES_PACKAGE,
    SpringUtils.DATA_PACKAGE + "annotation.");

  private static final Set<String> FRAMEWORK_ANNOTATION_PREFIXES = Stream.of(
    JACKSON_ANNOTATION_PACKAGES,
    GSON_ANNOTATION_PACKAGES,
    MICRONAUT_ANNOTATION_PACKAGES,
    JAKARTA_EE_ANNOTATION_PACKAGES,
    JAVA_EE_ANNOTATION_PACKAGES,
    LOMBOK_ANNOTATION_PACKAGES,
    SPRING_ANNOTATION_PACKAGES)
    .flatMap(Set::stream)
    .collect(Collectors.toUnmodifiableSet());

  private static final MethodMatchers SERIALIZATION_CONTRACT_METHODS = MethodMatchers.or(
    methodMatcher("readObject", "java.io.ObjectInputStream"),
    methodMatcher("writeObject", "java.io.ObjectOutputStream"),
    methodMatcher("readExternal", "java.io.ObjectInput"),
    methodMatcher("writeExternal", "java.io.ObjectOutput"),
    MethodMatchers.create()
      .ofAnyType()
      .names("readObjectNoData", "writeReplace", "readResolve")
      .addWithoutParametersMatcher()
      .build());

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
    if (classTree.simpleName() == null) {
      // Anonymous classes can not be converted to records.
      return;
    }
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
    if (hasSerializationContract(classTree)) {
      // records have special serialization behavior, so this refactoring is not behavior-preserving.
      return;
    }

    List<Symbol.VariableSymbol> fields = classFields(classSymbol);
    if (fields.isEmpty() || !hasOnlyPrivateFinalFields(fields)) {
      return;
    }
    List<Symbol.MethodSymbol> methods = classMethods(classSymbol);
    List<Symbol.MethodSymbol> constructors = classConstructors(methods);
    if (constructors.size() != 1) {
      return;
    }
    Symbol.MethodSymbol constructor = constructors.get(0);
    Map<String, Type> fieldsNameToType = fields.stream().collect(Collectors.toMap(Symbol::name, Symbol::type));
    if (hasFrameworkContract(classSymbol, fields, methods, fieldsNameToType, constructor)) {
      return;
    }

    if (!hasGetterForEveryField(methods, fieldsNameToType)) {
      return;
    }
    if (hasParameterForEveryField(constructor, fieldsNameToType.keySet()) && !constructorHasSmallerVisibility(constructor, classSymbol)) {
      reportIssue(classTree.simpleName(), String.format("Refactor this class declaration to use 'record %s'.", recordName(classTree, constructor)));
    }
  }

  private static boolean hasSerializationContract(ClassTree classTree) {
    Type type = classTree.symbol().type();
    return type.isSubtypeOf(JAVA_IO_SERIALIZABLE)
      || type.isSubtypeOf(JAVA_IO_EXTERNALIZABLE)
      || classTree.members().stream().anyMatch(RecordInsteadOfClassCheck::isSerializationContractMember);
  }

  private static boolean isSerializationContractMember(Tree member) {
    if (member.is(Tree.Kind.METHOD)) {
      return SERIALIZATION_CONTRACT_METHODS.matches((MethodTree) member);
    }
    if (member.is(Tree.Kind.VARIABLE)) {
      return isSerialPersistentFields(((VariableTree) member).symbol());
    }
    return false;
  }

  private static boolean isSerialPersistentFields(Symbol field) {
    return "serialPersistentFields".equals(field.name())
      && field.isPrivate()
      && field.isStatic()
      && field.isFinal()
      && field.type().is("java.io.ObjectStreamField[]");
  }

  private static MethodMatchers methodMatcher(String methodName, String parameterType) {
    return MethodMatchers.create()
      .ofAnyType()
      .names(methodName)
      .addParametersMatcher(parameterType)
      .build();
  }

  private static boolean hasFrameworkContract(
    Symbol.TypeSymbol classSymbol,
    List<Symbol.VariableSymbol> fields,
    List<Symbol.MethodSymbol> methods,
    Map<String, Type> fieldsNameToType,
    Symbol.MethodSymbol constructor) {

    return hasFrameworkAnnotation(classSymbol.metadata())
      || hasFrameworkAnnotation(constructor.metadata())
      || hasFrameworkAnnotationOnConstructorParameters(constructor)
      || fields.stream().anyMatch(field -> hasFrameworkAnnotation(field.metadata()))
      || methods.stream()
        .filter(method -> isGetter(method, fieldsNameToType))
        .anyMatch(method -> hasFrameworkAnnotation(method.metadata()));
  }

  private static boolean hasFrameworkAnnotationOnConstructorParameters(Symbol.MethodSymbol constructor) {
    return constructor.declaration().parameters().stream()
      .anyMatch(parameter -> hasFrameworkAnnotation(parameter.symbol().metadata()));
  }

  private static boolean hasFrameworkAnnotation(SymbolMetadata metadata) {
    return hasUnknownAnnotation(metadata) || metadata.annotations().stream().anyMatch(RecordInsteadOfClassCheck::isFrameworkAnnotation);
  }

  private static boolean isFrameworkAnnotation(SymbolMetadata.AnnotationInstance annotation) {
    Type annotationType = annotation.symbol().type();
    return !annotationType.isUnknown()
      && FRAMEWORK_ANNOTATION_PREFIXES.stream().anyMatch(annotationType.fullyQualifiedName()::startsWith);
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
      .toList();
  }

  private static List<Symbol.VariableSymbol> classFields(Symbol.TypeSymbol classSymbol) {
    return classSymbol
      .memberSymbols()
      .stream()
      .filter(Symbol::isVariableSymbol)
      // records can have constant, so discarding them
      .filter(s -> !isConstant(s))
      .map(Symbol.VariableSymbol.class::cast)
      .toList();
  }

  private static List<Symbol.MethodSymbol> classConstructors(List<Symbol.MethodSymbol> methods) {
    return methods.stream()
      .filter(m -> "<init>".equals(m.name()))
      // only explicit constructors
      .filter(m -> m.declaration() != null)
      .toList();
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
