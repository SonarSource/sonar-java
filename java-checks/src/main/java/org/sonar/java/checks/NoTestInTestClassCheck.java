/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Rule(key = "S2187")
public class NoTestInTestClassCheck extends IssuableSubscriptionVisitor {

  private final Set<String> testAnnotations = new HashSet<>();
  private final Set<String> seenAnnotations = new HashSet<>();

  private boolean isTestAnnotation(Type type) {
    return testAnnotations.contains(type.fullyQualifiedName()) || isJUnitTestableMetaAnnotated(type);
  }

  private boolean isJUnitTestableMetaAnnotated(Type type) {
    if (seenAnnotations.contains(type.fullyQualifiedName())) {
      return false;
    }
    seenAnnotations.add(type.fullyQualifiedName());
    SymbolMetadata metadata = type.symbol().metadata();
    if (metadata.isAnnotatedWith("org.junit.platform.commons.annotation.Testable")) {
      testAnnotations.add(type.fullyQualifiedName());
      return true;
    }
    for (SymbolMetadata.AnnotationInstance annotation : metadata.annotations()) {
      if (isJUnitTestableMetaAnnotated(annotation.symbol().type())) {
        testAnnotations.add(type.fullyQualifiedName());
        return true;
      }
    }
    return false;
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      resetAnnotationCache();
      CompilationUnitTree cut = (CompilationUnitTree) tree;
      cut.types().stream().filter(typeTree -> typeTree.is(Kind.CLASS)).forEach(typeTree -> checkClass((ClassTree) typeTree));
    }
  }

  private void resetAnnotationCache() {
    testAnnotations.clear();
    seenAnnotations.clear();
    testAnnotations.add("org.junit.Test");
    testAnnotations.add("org.testng.annotations.Test");
    testAnnotations.add("org.junit.jupiter.api.Test");
  }

  private void checkClass(ClassTree classTree) {
    if (!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)) {
      Symbol.TypeSymbol classSymbol = classTree.symbol();
      Stream<Symbol.MethodSymbol> members = getAllMembers(classSymbol);
      IdentifierTree simpleName = classTree.simpleName();
      if (classSymbol.metadata().isAnnotatedWith("org.testng.annotations.Test")) {
        checkTestNGmembers(simpleName, members);
      } else {
        boolean isJunit3TestClass = classSymbol.type().isSubtypeOf("junit.framework.TestCase");
        if (isJunit3TestClass) {
          checkJunit3TestClass(simpleName, members);
        } else {
          checkJunit4AndAboveTestClass(simpleName, classSymbol, members);
        }
      }
    }
  }

  private void checkTestNGmembers(IdentifierTree className, Stream<Symbol.MethodSymbol> members) {
    if (members.noneMatch(member -> member.isPublic() && !member.isStatic() && member.returnType() != null)) {
      reportClass(className);
    }
  }

  private void checkJunit3TestClass(IdentifierTree className, Stream<Symbol.MethodSymbol> members) {
    if (members.noneMatch(m -> m.name().startsWith("test"))) {
      reportClass(className);
    }
  }

  private void checkJunit4AndAboveTestClass(IdentifierTree className, Symbol.TypeSymbol symbol, Stream<Symbol.MethodSymbol> members) {
    if (symbol.name().endsWith("Test")
      && !runWithEnclosedOrCucumberOrSuiteRunner(symbol)
      && members.noneMatch(this::isTestMethod)) {
      reportClass(className);
    }
  }

  private static boolean runWithEnclosedOrCucumberOrSuiteRunner(Symbol.TypeSymbol symbol) {
    List<SymbolMetadata.AnnotationValue> annotationValues = symbol.metadata().valuesForAnnotation("org.junit.runner.RunWith");
    if(annotationValues != null && annotationValues.size() == 1) {
      Object value = annotationValues.get(0).value();
      if (value instanceof MemberSelectExpressionTree) {
        String runnerParam = ExpressionsHelper.concatenate((ExpressionTree) value);
        return runnerParam.endsWith("Enclosed.class") || runnerParam.endsWith("Cucumber.class") || runnerParam.endsWith("Suite.class") || runnerParam.endsWith("Theories.class");
      }
    }
    return false;
  }

  private boolean isTestMethod(Symbol method) {
    return method.metadata().annotations().stream().anyMatch(input -> {
      Type type = input.symbol().type();
      return type.isUnknown() || isTestAnnotation(type);
    });
  }

  private static Stream<Symbol.MethodSymbol> getAllMembers(Symbol.TypeSymbol symbol) {
    if ("java.lang.Object".equals(symbol.type().fullyQualifiedName())) {
      return Stream.empty();
    }
    Stream<Symbol.MethodSymbol> members = symbol.memberSymbols().stream().filter(Symbol::isMethodSymbol).map(Symbol.MethodSymbol.class::cast);
    Type superClass = symbol.superClass();
    if (superClass != null) {
      members = Stream.concat(members, getAllMembers(superClass.symbol()));
    }
    Stream<Symbol.MethodSymbol> defaultMethodsFromInterfaces = symbol.interfaces().stream()
      .flatMap(i -> getAllMembers(i.symbol()))
      .filter(m -> ((JavaSymbol.MethodJavaSymbol) m).isDefault());
    members = Stream.concat(members, defaultMethodsFromInterfaces);
    return members;
  }

  private void reportClass(IdentifierTree className) {
    reportIssue(className, "Add some tests to this class.");
  }
}
