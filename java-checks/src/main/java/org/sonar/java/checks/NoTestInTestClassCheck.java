/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.tag.Tag;
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
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2187",
  name = "TestCases should contain tests",
  priority = Priority.MAJOR,
  tags = {Tag.CONFUSING, Tag.TESTS, Tag.UNUSED})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class NoTestInTestClassCheck extends IssuableSubscriptionVisitor {

  private static final Predicate<SymbolMetadata.AnnotationInstance> PREDICATE_ANNOTATION_TEST_OR_UNKNOWN = new Predicate<SymbolMetadata.AnnotationInstance>() {
    @Override
    public boolean apply(SymbolMetadata.AnnotationInstance input) {
      Type type = input.symbol().type();
      return type.isUnknown() || type.is("org.junit.Test") || type.is("org.testng.annotations.Test");
    }
  };

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      CompilationUnitTree cut = (CompilationUnitTree) tree;
      for (Tree typeTree : cut.types()) {
        if (typeTree.is(Kind.CLASS)) {
          checkClass((ClassTree) typeTree);
        }
      }
    }
  }

  private void checkClass(ClassTree classTree) {
    if (!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)) {
      JavaSymbol.TypeJavaSymbol symbol = (JavaSymbol.TypeJavaSymbol) classTree.symbol();
      Iterable<Symbol> members = getAllMembers(symbol);
      IdentifierTree simpleName = classTree.simpleName();
      if (classTree.symbol().metadata().isAnnotatedWith("org.testng.annotations.Test")) {
        checkTestNGmembers(simpleName, members);
      } else {
        boolean isJunit3TestClass = symbol.type().isSubtypeOf("junit.framework.TestCase");
        if (isJunit3TestClass) {
          checkJunit3TestClass(simpleName, members);
        } else {
          checkJunit4TestClass(simpleName, symbol, members);
        }
      }
    }
  }

  private void checkTestNGmembers(IdentifierTree className, Iterable<Symbol> members) {
    for (Symbol member : members) {
      if (member.isMethodSymbol() && member.isPublic() && !member.isStatic() && ((Symbol.MethodSymbol) member).returnType() != null) {
        return;
      }
    }
    reportIssue(className, "Add some tests to this class.");
  }

  private void checkJunit3TestClass(IdentifierTree className, Iterable<Symbol> members) {
    checkMethods(className, members, false);
  }

  private void checkJunit4TestClass(IdentifierTree className, JavaSymbol.TypeJavaSymbol symbol, Iterable<Symbol> members) {
    if (symbol.name().endsWith("Test") && !runWithEnclosedRunner(symbol)) {
      checkMethods(className, members, true);
    }
  }

  private static boolean runWithEnclosedRunner(JavaSymbol.TypeJavaSymbol symbol) {
    List<SymbolMetadata.AnnotationValue> annotationValues = symbol.metadata().valuesForAnnotation("org.junit.runner.RunWith");
    if(annotationValues != null && annotationValues.size() == 1) {
      Object value = annotationValues.get(0).value();
      return value instanceof MemberSelectExpressionTree && ExpressionsHelper.concatenate((ExpressionTree) value).endsWith("Enclosed.class");
    }
    return false;
  }

  private void checkMethods(IdentifierTree simpleName, Iterable<Symbol> members, boolean forJunit4) {
    for (Symbol member : members) {
      if (member.isMethodSymbol() && isTestMethod(forJunit4, member)) {
        return;
      }
    }
    reportIssue(simpleName, "Add some tests to this class.");
  }

  private static boolean isTestMethod(boolean forJunit4, Symbol member) {
    if (forJunit4) {
      return Iterables.any(member.metadata().annotations(), PREDICATE_ANNOTATION_TEST_OR_UNKNOWN);
    }
    return member.name().startsWith("test");
  }

  private static Iterable<Symbol> getAllMembers(JavaSymbol.TypeJavaSymbol symbol) {
    Iterable<Symbol> members = symbol.memberSymbols();
    JavaType superclass = symbol.getSuperclass();
    while (superclass != null && !"java.lang.Object".equals(superclass.fullyQualifiedName())) {
      members = Iterables.concat(members, superclass.symbol().memberSymbols());
      superclass = superclass.getSymbol().getSuperclass();
    }
    return members;
  }
}
