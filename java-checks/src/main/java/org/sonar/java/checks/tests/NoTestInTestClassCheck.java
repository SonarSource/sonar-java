/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2187")
public class NoTestInTestClassCheck extends IssuableSubscriptionVisitor {

  public static final String ARCH_UNIT_RUNNER = "ArchUnitRunner";
  public static final String ARCH_UNIT_ANALYZE_CLASSES = "com.tngtech.archunit.junit.AnalyzeClasses";
  public static final String ARCH_UNIT_TEST = "com.tngtech.archunit.junit.ArchTest";
  private static final String TEST_NG_TEST = "org.testng.annotations.Test";

  private static final List<String> PACT_UNIT_TEST = Arrays.asList("au.com.dius.pact.provider.junit.State", "au.com.dius.pact.provider.junitsupport.State");

  private final Set<String> testMethodAnnotations = new HashSet<>();
  private final Set<String> testFieldAnnotations = new HashSet<>();
  private final Set<String> seenAnnotations = new HashSet<>();

  private static final String DEFAULT_TEST_CLASS_NAME_PATTERN = ".*(Test|Tests|TestCase)";

  @RuleProperty(key = "TestClassNamePattern",
    description = "Test class name pattern (regular expression)",
    defaultValue = "" + DEFAULT_TEST_CLASS_NAME_PATTERN)
  public String testClassNamePattern = DEFAULT_TEST_CLASS_NAME_PATTERN;
  private Pattern testClassNamePatternRegEx;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    resetAnnotationCache();
    CompilationUnitTree cut = (CompilationUnitTree) tree;
    cut.types().stream()
      .filter(typeTree -> typeTree.is(Tree.Kind.CLASS))
      .forEach(typeTree -> checkClass((ClassTree) typeTree));
  }

  private void resetAnnotationCache() {
    Arrays.asList(testFieldAnnotations, testMethodAnnotations, seenAnnotations).forEach(Set::clear);
    testMethodAnnotations.addAll(Arrays.asList("org.junit.Test", TEST_NG_TEST, "org.junit.jupiter.api.Test"));
  }

  private void checkClass(ClassTree classTree) {
    boolean hasUnknownParent = Optional.ofNullable(classTree.superClass())
      .map(parent -> parent.symbolType().isUnknown())
      // If the superClass is null, then the class has no parent, so has no unknownParent.
      .orElse(false);
    boolean knownImplementedInterfaces = classTree.superInterfaces().stream()
      .noneMatch(i -> i.symbolType().isUnknown());

    if (!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)
      && !hasUnknownParent
      && knownImplementedInterfaces
    ) {
      Symbol.TypeSymbol classSymbol = classTree.symbol();
      Stream<Symbol> members = getAllMembers(classSymbol, checkRunWith(classSymbol, "Enclosed"));
      IdentifierTree simpleName = classTree.simpleName();
      if (classSymbol.metadata().isAnnotatedWith(TEST_NG_TEST)) {
        checkTestNGmembers(simpleName, members);
      } else {
        boolean isJunit3TestClass = classSymbol.type().isSubtypeOf("junit.framework.TestCase");
        List<Symbol> membersList = members.toList();
        if (isJunit3TestClass && containsJUnit3Tests(membersList)) {
          return;
        }
        if (isJunit3TestClass || isTestClassName(classSymbol.name())) {
          checkJunit4AndAboveTestClass(simpleName, classSymbol, membersList);
        }
      }
    }
  }

  private boolean isTestClassName(String className) {
    if (StringUtils.isEmpty(testClassNamePattern)) {
      return false;
    }
    if (testClassNamePatternRegEx == null) {
      testClassNamePatternRegEx = Pattern.compile(testClassNamePattern);
    }
    return testClassNamePatternRegEx.matcher(className).matches();
  }

  private static boolean isArchUnitTestClass(Symbol.TypeSymbol classSymbol) {
    return checkRunWith(classSymbol, ARCH_UNIT_RUNNER) ||
      classSymbol.metadata().isAnnotatedWith(ARCH_UNIT_ANALYZE_CLASSES);
  }

  private void checkTestNGmembers(IdentifierTree className, Stream<Symbol> members) {
    Predicate<SymbolMetadata.AnnotationInstance> isTestNgAnnotation = ann -> {
      Type type = ann.symbol().type();
      return type.isUnknown() || type.is(TEST_NG_TEST);
    };
    Predicate<Symbol> isTestMethod = member -> {
      if(!member.isMethodSymbol()){
        return false;
      }

      // we know member is a method.
      boolean annotatedWithTest = member.metadata().annotations().stream().anyMatch(isTestNgAnnotation);
      boolean publicMethod = member.isPublic() && !member.isStatic() && !"<init>".equals(member.name());
      return annotatedWithTest || publicMethod;
    };

    if (members.noneMatch(isTestMethod)) {
      reportClass(className);
    }
  }

  private static boolean containsJUnit3Tests(List<Symbol> members) {
    return members.stream().anyMatch(m -> m.isMethodSymbol() && m.name().startsWith("test"));
  }

  private void checkJunit4AndAboveTestClass(IdentifierTree className, Symbol.TypeSymbol symbol, List<Symbol> members) {
    addUsedAnnotations(symbol);

    if (!runWithCucumberOrSuiteOrTheoriesRunner(symbol)
      && members.stream().noneMatch(this::isTestFieldOrMethod)) {
      reportClass(className);
    }
  }

  private void addUsedAnnotations(Symbol.TypeSymbol classSymbol) {
    if (runWithZohhak(classSymbol)) {
      testMethodAnnotations.add("com.googlecode.zohhak.api.TestWith");
    } else if (isArchUnitTestClass(classSymbol)) {
      testMethodAnnotations.add(ARCH_UNIT_TEST);
      testFieldAnnotations.add(ARCH_UNIT_TEST);
    } else if (runWithPact(classSymbol)) {
      testMethodAnnotations.addAll(PACT_UNIT_TEST);
    }
  }

  private static boolean runWithCucumberOrSuiteOrTheoriesRunner(Symbol.TypeSymbol symbol) {
    return annotatedIncludeEnginesCucumber(symbol)
      || checkRunWith(symbol, "Cucumber", "Suite", "Theories");
  }

  private static boolean runWithZohhak(Symbol.TypeSymbol symbol) {
    return checkRunWith(symbol, "ZohhakRunner");
  }

  private static boolean runWithPact(Symbol.TypeSymbol symbol) {
    return checkRunWith(symbol, "PactRunner") || checkRunWith(symbol, "RestPactRunner");
  }

  private static boolean checkRunWith(Symbol.TypeSymbol symbol, String... runnerClasses) {
    List<SymbolMetadata.AnnotationValue> annotationValues = symbol.metadata().valuesForAnnotation("org.junit.runner.RunWith");
    if (annotationValues != null && annotationValues.size() == 1) {
      Object value = annotationValues.get(0).value();
      return value instanceof Symbol.TypeSymbol typeSymbol && checkRunWithType(typeSymbol, runnerClasses);
    }
    return false;
  }

  private static boolean checkRunWithType(Symbol.TypeSymbol value, String... runnerClasses) {
    for (String runnerClass : runnerClasses) {
      if (runnerClass.equals(value.name())) {
        return true;
      }
    }
    return false;
  }

  /**
   * True is the symbol is annotated {@code @IncludeEngines("cucumber")}.
   */
  private static boolean annotatedIncludeEnginesCucumber(Symbol.TypeSymbol symbol) {
    SymbolMetadata metadata = symbol.metadata();

    List<SymbolMetadata.AnnotationInstance> annotations = metadata.annotations();
    for (SymbolMetadata.AnnotationInstance annotation: annotations) {
      if (annotation.symbol().type().fullyQualifiedName().endsWith("IncludeEngines")) {
        // values are not available in automatic analysis, so assume "cucumber" is there
        if (annotation.values().isEmpty()) {
          return true;
        }
        // otherwise check the list
        boolean containsCucumber = annotation.values().stream().anyMatch(annotationValue ->
          annotationValue.value() instanceof Object[] vals
            && vals.length == 1
            && "cucumber".equals(vals[0]));
        if (containsCucumber) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isTestFieldOrMethod(Symbol member) {
    return member.metadata().annotations().stream().anyMatch(input -> {
      Type type = input.symbol().type();
      return type.isUnknown() ||
        (member.isMethodSymbol() && isTestMethodAnnotation(type)) ||
        (member.isVariableSymbol() && testFieldAnnotations.contains(type.fullyQualifiedName()));
    });
  }

  private boolean isTestMethodAnnotation(Type type) {
    return testMethodAnnotations.contains(type.fullyQualifiedName()) || isJUnitTestableMetaAnnotated(type);
  }

  private boolean isJUnitTestableMetaAnnotated(Type type) {
    if (seenAnnotations.contains(type.fullyQualifiedName())) {
      return false;
    }
    seenAnnotations.add(type.fullyQualifiedName());
    SymbolMetadata metadata = type.symbol().metadata();
    if (metadata.isAnnotatedWith("org.junit.platform.commons.annotation.Testable")) {
      testMethodAnnotations.add(type.fullyQualifiedName());
      return true;
    }
    for (SymbolMetadata.AnnotationInstance annotation : metadata.annotations()) {
      if (isJUnitTestableMetaAnnotated(annotation.symbol().type())) {
        testMethodAnnotations.add(type.fullyQualifiedName());
        return true;
      }
    }
    return false;
  }

  private static Stream<Symbol> getAllMembers(Symbol.TypeSymbol symbol, boolean isEnclosed) {
    return getAllMembers(symbol, isEnclosed, new HashSet<>());
  }

  private static Stream<Symbol> getAllMembers(Symbol.TypeSymbol symbol, boolean isEnclosed, Set<Symbol> visitedSymbols) {
    if (!visitedSymbols.add(symbol) || symbol.type().is("java.lang.Object")) {
      return Stream.empty();
    }
    Stream<Symbol> members = Stream.empty();
    if (!isEnclosed) {
      members = symbol.memberSymbols().stream().filter(m -> m.isMethodSymbol() || m.isVariableSymbol());
    }
    Type superClass = symbol.superClass();
    if (superClass != null) {
      members = Stream.concat(members, getAllMembers(superClass.symbol(), isEnclosed, visitedSymbols));
    }
    Stream<Symbol> defaultMethodsFromInterfaces = symbol.interfaces().stream()
      .flatMap(i -> getAllMembers(i.symbol(), false, visitedSymbols))
      .filter(m -> m.isMethodSymbol() && ((Symbol.MethodSymbol) m).isDefaultMethod());
    members = Stream.concat(members, defaultMethodsFromInterfaces);
    for (Symbol s : symbol.memberSymbols()) {
      if (isNested(s) || isPublicStaticConcrete(s)) {
        members = Stream.concat(members, getAllMembers((Symbol.TypeSymbol) s, false, visitedSymbols));
      }
    }
    return members;
  }

  private static boolean isNested(Symbol s) {
    return s.isTypeSymbol() && s.metadata().isAnnotatedWith("org.junit.jupiter.api.Nested");
  }

  private static boolean isPublicStaticConcrete(Symbol s) {
    return isPublicStaticClass(s) && !s.isAbstract();
  }

  private static boolean isPublicStaticClass(Symbol symbol) {
    return symbol.isTypeSymbol() && symbol.isPublic() && symbol.isStatic();
  }

  private void reportClass(IdentifierTree className) {
    reportIssue(className, "Add some tests to this class.");
  }
}
