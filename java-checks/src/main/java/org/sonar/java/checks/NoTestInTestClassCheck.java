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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
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

@Rule(key = "S2187")
public class NoTestInTestClassCheck extends IssuableSubscriptionVisitor {

  private final Set<String> testAnnotations = new HashSet<>();
  private final Set<String> seenAnnotations = new HashSet<>();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.COMPILATION_UNIT);
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
      Stream<Symbol.MethodSymbol> members = getAllMembers(classSymbol, checkRunWith(classSymbol, "Enclosed.class"));
      IdentifierTree simpleName = classTree.simpleName();
      if (classSymbol.metadata().isAnnotatedWith("org.testng.annotations.Test")) {
        checkTestNGmembers(simpleName, members);
      } else {
        boolean isJunit3TestClass = classSymbol.type().isSubtypeOf("junit.framework.TestCase");
        if (isJunit3TestClass) {
          checkJunit3TestClass(simpleName, members);
        } else {
          if (runWitZohhak(classSymbol)) {
            testAnnotations.add("com.googlecode.zohhak.api.TestWith");
          }
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
      && !runWithCucumberOrSuiteOrTheoriesRunner(symbol)
      && members.noneMatch(this::isTestMethod)) {
      reportClass(className);
    }
  }

  private static boolean runWithCucumberOrSuiteOrTheoriesRunner(Symbol.TypeSymbol symbol) {
    return checkRunWith(symbol, "Cucumber.class","Suite.class", "Theories.class");
  }

  private static boolean runWitZohhak(Symbol.TypeSymbol symbol) {
    return checkRunWith(symbol, "ZohhakRunner.class");
  }

  private static boolean checkRunWith(Symbol.TypeSymbol symbol, String... runnerClasses) {
    List<SymbolMetadata.AnnotationValue> annotationValues = symbol.metadata().valuesForAnnotation("org.junit.runner.RunWith");
    if (annotationValues != null && annotationValues.size() == 1) {
      Object value = annotationValues.get(0).value();
      if (value instanceof MemberSelectExpressionTree) {
        String runnerParam = ExpressionsHelper.concatenate((ExpressionTree) value);
        for (String runnerClass : runnerClasses) {
          if (runnerParam.endsWith(runnerClass)) {
            return true;
          }
        }
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

  private static Stream<Symbol.MethodSymbol> getAllMembers(Symbol.TypeSymbol symbol, boolean isEnclosed) {
    return getAllMembers(symbol, isEnclosed, new HashSet<>());
  }

  private static Stream<Symbol.MethodSymbol> getAllMembers(Symbol.TypeSymbol symbol, boolean isEnclosed, Set<Symbol> visitedSymbols) {
    if (!visitedSymbols.add(symbol) || symbol.type().is("java.lang.Object")) {
      return Stream.empty();
    }
    Stream<Symbol.MethodSymbol> members = Stream.empty();
    if (!isEnclosed) {
      members = symbol.memberSymbols().stream().filter(Symbol::isMethodSymbol).map(Symbol.MethodSymbol.class::cast);
    }
    Type superClass = symbol.superClass();
    if (superClass != null) {
      members = Stream.concat(members, getAllMembers(superClass.symbol(), isEnclosed, visitedSymbols));
    }
    Stream<Symbol.MethodSymbol> defaultMethodsFromInterfaces = symbol.interfaces().stream()
      .flatMap(i -> getAllMembers(i.symbol(), false, visitedSymbols))
      .filter(m -> ((JavaSymbol.MethodJavaSymbol) m).isDefault());
    members = Stream.concat(members, defaultMethodsFromInterfaces);
    for (Symbol s : symbol.memberSymbols()) {
      if (isNested(s) || isPublicStaticConcrete(s)) {
        members = Stream.concat(members, getAllMembers((Symbol.TypeSymbol) s, false, visitedSymbols));
      }
    }
    return members;
  }

  private static boolean isNested (Symbol s) {
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
