/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
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
  tags = {"junit", "unused"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class NoTestInTestClassCheck extends SubscriptionBaseVisitor {

  private static final Predicate<SymbolMetadata.AnnotationInstance> PREDICATE_ANNOTATION_TEST_OR_UNKNOWN = new Predicate<SymbolMetadata.AnnotationInstance>() {
    @Override
    public boolean apply(SymbolMetadata.AnnotationInstance input) {
      Type type = input.symbol().type();
      return type.isUnknown() || type.is("org.junit.Test");
    }
  };

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    CompilationUnitTree cut = (CompilationUnitTree) tree;
    for (Tree typeTree : cut.types()) {
      if (typeTree.is(Kind.CLASS)) {
        ClassTree classTree = (ClassTree) typeTree;
        if (!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)) {
          checkJunit3TestClass(classTree);
          checkJunit4TestClass(classTree);
        }
      }
    }
  }

  private void checkJunit3TestClass(ClassTree tree) {
    if (tree.symbol().type().isSubtypeOf("junit.framework.TestCase")) {
      checkMethods(tree, false);
    }
  }

  private void checkJunit4TestClass(ClassTree tree) {
    IdentifierTree name = tree.simpleName();
    if (name != null && name.name().endsWith("Test")) {
      checkMethods(tree, true);
    }
  }

  private void checkMethods(ClassTree classTree, boolean forJunit4) {
    boolean hasNoTest = true;
    for (Tree member : classTree.members()) {
      if (member.is(Kind.METHOD) && isTestMethod(forJunit4, (MethodTree) member)) {
        hasNoTest = false;
        break;
      }
    }
    if (hasNoTest) {
      addIssue(classTree, "Add some tests to this class.");
    }
  }

  private static boolean isTestMethod(boolean forJunit4, MethodTree member) {
    if (forJunit4) {
      return Iterables.any(member.symbol().metadata().annotations(), PREDICATE_ANNOTATION_TEST_OR_UNKNOWN);
    }
    return member.simpleName().name().startsWith("test");
  }
}
