/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.LiveVariables;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.prettyprint.FileConfig;
import org.sonar.java.checks.prettyprint.PrettyPrintStringBuilder;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.QuickFixHelper.contentForRange;
import static org.sonar.java.se.ProgramState.isField;

/**
 * Current implementation raises the issue only for the fields used in one method
 */
@Rule(key = "S1450")
public class PrivateFieldUsedLocallyCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove the \"%s\" field and declare it as a local variable in the relevant methods.";
  private static final String QUICK_FIX_MESSAGE = "Move this field to the only method where it is used";

  private static final FileConfig FILE_CONFIG = new FileConfig(new FileConfig.IndentMode.Spaces(2), "\n");

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeSymbol classSymbol = ((ClassTree) tree).symbol();
    Set<Symbol> fieldsReadOnAnotherInstance = FieldsReadOnAnotherInstanceVisitor.getFrom(tree);

    classSymbol.memberSymbols().stream()
      .filter(PrivateFieldUsedLocallyCheck::isPrivateField)
      .filter(s -> !isAConstant(s))
      .filter(s -> !hasAnnotation(s))
      .filter(s -> !s.usages().isEmpty())
      .filter(s -> !fieldsReadOnAnotherInstance.contains(s))
      .forEach(s -> checkPrivateField(s, classSymbol));
  }

  private static boolean isAConstant(Symbol s) {
    return s.isFinal() && s.isStatic();
  }

  private static boolean hasAnnotation(Symbol s) {
    return !s.metadata().annotations().isEmpty();
  }

  private void checkPrivateField(Symbol privateFieldSymbol, TypeSymbol classSymbol) {
    MethodTree methodWhereUsed = usedInOneMethodOnly(privateFieldSymbol, classSymbol);

    if (methodWhereUsed != null && !isLiveInMethodEntry(privateFieldSymbol, methodWhereUsed)) {
      VariableTree declaration = (VariableTree) privateFieldSymbol.declaration();
      IdentifierTree declarationIdentifier = declaration.simpleName();
      String message = String.format(MESSAGE, privateFieldSymbol.name());
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(declarationIdentifier)
        .withMessage(message)
        .withQuickFixes(() -> computeQuickFix((Symbol.VariableSymbol) privateFieldSymbol, declaration, methodWhereUsed))
        .report();
    }
  }

  private List<JavaQuickFix> computeQuickFix(Symbol.VariableSymbol symbol, VariableTree declaration, MethodTree methodWhereUsed) {
    if (wouldRelocationClashWithLocalVariables(symbol, methodWhereUsed)) {
      return Collections.emptyList();
    }
    BlockTree block = methodWhereUsed.block();
    String declarationMinusModifiers = contentForRange(declaration.type().firstToken(), declaration.endToken(), context);
    var pps = new PrettyPrintStringBuilder(FILE_CONFIG, block.body().get(0).firstToken(), false);
    pps.newLine().addStripLeading(declarationMinusModifiers);
    return List.of(
      JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
        .addTextEdits(editUsagesWithThis(symbol))
        .addTextEdit(JavaTextEdit.insertAfterTree(block.openBraceToken(), pps.toString()))
        .addTextEdit(JavaTextEdit.removeTree(declaration))
        .build()
    );
  }

  /*
   * Compares the field name against local variables and parameters in the method to ensure that moving the
   * field declaration to the method would not create any clash.
   */
  private static boolean wouldRelocationClashWithLocalVariables(Symbol.VariableSymbol symbol, MethodTree method) {
    LocalVariableCollector collector = new LocalVariableCollector();
    method.accept(collector);
    if (collector.variables.isEmpty()) {
      return false;
    }
    return collector.variables
      .stream()
      .anyMatch(variable -> variable.symbol().name().equals(symbol.name()));
  }

  /**
   * Returns edits to transform all usages in the form of this.myVariable to myVariable.
   */
  private static List<JavaTextEdit> editUsagesWithThis(Symbol symbol) {
    return symbol.usages().stream()
      .map(Tree::parent)
      .filter(parent -> parent.is(Kind.MEMBER_SELECT))
      .map(MemberSelectExpressionTree.class::cast)
      .filter(memberSelect -> ExpressionUtils.isThis(memberSelect.expression()))
      .map(memberSelect -> JavaTextEdit.removeBetweenTree(memberSelect.expression(), memberSelect.operatorToken()))
      .toList();
  }

  private static boolean isLiveInMethodEntry(Symbol privateFieldSymbol, MethodTree methodTree) {
    CFG cfg = (CFG) methodTree.cfg();
    LiveVariables liveVariables = LiveVariables.analyzeWithFields(cfg);
    return liveVariables.getIn(cfg.entryBlock()).contains(privateFieldSymbol);
  }

  private static boolean isPrivateField(Symbol memberSymbol) {
    return memberSymbol.isPrivate() && memberSymbol.isVariableSymbol();
  }

  /**
   * If private field used in several methods then returns null, otherwise returns the method where it's used
   */
  @CheckForNull
  private static MethodTree usedInOneMethodOnly(Symbol privateFieldSymbol, TypeSymbol classSymbol) {
    MethodTree method = null;

    for (IdentifierTree usageIdentifier : privateFieldSymbol.usages()) {
      MethodTree enclosingMethod = (MethodTree) ExpressionUtils.getEnclosingTree(usageIdentifier, Kind.METHOD);

      if (enclosingMethod == null
        || !enclosingMethod.symbol().owner().equals(classSymbol)
        || (method != null && !method.equals(enclosingMethod))) {
        return null;
      } else {
        method = enclosingMethod;
      }
    }

    return method;
  }

  private static class FieldsReadOnAnotherInstanceVisitor extends BaseTreeVisitor {

    private final Set<Symbol> fieldsReadOnAnotherInstance = new HashSet<>();

    static Set<Symbol> getFrom(Tree classTree) {
      FieldsReadOnAnotherInstanceVisitor fieldsReadOnAnotherInstanceVisitor = new FieldsReadOnAnotherInstanceVisitor();
      fieldsReadOnAnotherInstanceVisitor.scan(classTree);
      return fieldsReadOnAnotherInstanceVisitor.fieldsReadOnAnotherInstance;
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      Symbol symbol = tree.identifier().symbol();
      if (isField(symbol) && !symbol.isStatic()) {
        if (tree.expression().is(Kind.IDENTIFIER)) {
          if (!ExpressionUtils.isThis(tree.expression())) {
            fieldsReadOnAnotherInstance.add(symbol);
          }
        } else {
          fieldsReadOnAnotherInstance.add(symbol);
        }
      }

      super.visitMemberSelectExpression(tree);
    }
  }

  private static class LocalVariableCollector extends BaseTreeVisitor {
    private final Set<VariableTree> variables = new HashSet<>();

    @Override
    public void visitVariable(VariableTree tree) {
      variables.add(tree);
    }
  }

}
