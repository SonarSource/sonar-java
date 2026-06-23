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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ClassPatternsUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S5810")
public class JUnit5SilentlyIgnoreClassAndMethodCheck extends IssuableSubscriptionVisitor {

  private enum ModifierScope {
    // annotation like @Nested that applies to class
    CLASS,
    // annotation like @BeforeAll that applies to static class method
    CLASS_METHOD,
    // annotation like @BeforeEach that applies to non-static method
    INSTANCE_METHOD
  }

  private static final String WRONG_MODIFIER_ISSUE_MESSAGE = "Remove this '%s' modifier.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.symbol().isAbstract()) {
      return;
    }

    UnitTestUtils.JUnit5MethodGroups groups = UnitTestUtils.groupJUnit5Methods(classTree);
    List<MethodTree> junit5ClassMethods = groups.classMethods();
    List<MethodTree> junit5InstanceMethods = groups.instanceMethods();
    List<MethodTree> nonJunit5Methods = groups.otherMethods();

    raiseIssueOnMethods(junit5ClassMethods, ModifierScope.CLASS_METHOD);
    raiseIssueOnMethods(junit5InstanceMethods, ModifierScope.INSTANCE_METHOD);
    boolean classHasJunit5InstanceMethods = !junit5InstanceMethods.isEmpty();
    if (classHasJunit5InstanceMethods) {
      raiseIssueOnClass(nonJunit5Methods, classTree);
    }
  }

  private void raiseIssueOnMethods(List<MethodTree> methods, ModifierScope scope) {
    for (MethodTree method : methods) {
      raiseIssueOnNotCompliantModifiers(method.modifiers(), scope);
      raiseIssueOnNonCompliantReturnType(method);
    }
  }

  private void raiseIssueOnClass(List<MethodTree> nonJunit5Methods, ClassTree classTree) {
    if (ClassPatternsUtils.shouldBePublicClass(classTree, nonJunit5Methods)) {
      return;
    }
    raiseIssueOnNotCompliantModifiers(classTree.modifiers(), ModifierScope.CLASS);
  }

  private void raiseIssueOnNotCompliantModifiers(ModifiersTree modifierTree, ModifierScope modifierScope) {
    modifierTree.modifiers().stream()
      .filter(modifier -> isNonCompliantModifier(modifier.modifier(), modifierScope))
      .findFirst()
      .ifPresent(this::raiseIssueOnNonCompliantModifier);
  }

  private void raiseIssueOnNonCompliantModifier(ModifierKeywordTree modifier) {
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(modifier)
      .withMessage(WRONG_MODIFIER_ISSUE_MESSAGE, modifier.keyword().text())
      .withQuickFix(() ->
        JavaQuickFix.newQuickFix("Remove \"%s\" modifier", modifier.keyword().text())
          .addTextEdit(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(modifier, true, QuickFixHelper.nextToken(modifier), false)))
        .build())
      .report();
  }

  private static boolean isNonCompliantModifier(Modifier modifier, ModifierScope modifierScope) {
    return modifier == Modifier.PRIVATE || (modifierScope == ModifierScope.INSTANCE_METHOD && modifier == Modifier.STATIC);
  }

  private void raiseIssueOnNonCompliantReturnType(MethodTree methodTree) {
    TypeTree returnType = methodTree.returnType();
    // returnType of METHOD is never null (unlike CONSTRUCTOR)
    Type type = returnType.symbolType();
    boolean methodReturnAValue = !type.isUnknown() && !type.isVoid();
    if (methodReturnAValue && !methodTree.symbol().metadata().isAnnotatedWith("org.junit.jupiter.api.TestFactory")) {
      List<JavaTextEdit> textEdits = new ArrayList<>();
      textEdits.add(JavaTextEdit.replaceTree(returnType, "void"));
      // Make return statements return void
      List<ReturnStatementTree> returnStatementTrees = new ReturnStatementVisitor(methodTree).returnStatementTrees();
      returnStatementTrees.forEach(r -> textEdits.add(JavaTextEdit.removeTree(r.expression())));

      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(methodTree.returnType())
        .withMessage("Replace the return type by void.")
        .withQuickFix(() ->
          JavaQuickFix.newQuickFix("Replace with void")
            .addTextEdits(textEdits)
            .build())
        .report();
    }
  }

  static final class ReturnStatementVisitor extends BaseTreeVisitor {
    private List<ReturnStatementTree> returnStatementTrees = new ArrayList<>();

    ReturnStatementVisitor(MethodTree methodTree) {
      scan(methodTree);
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      returnStatementTrees.add(tree);
    }

    List<ReturnStatementTree> returnStatementTrees() {
      return Collections.unmodifiableList(returnStatementTrees);
    }
  }
}
