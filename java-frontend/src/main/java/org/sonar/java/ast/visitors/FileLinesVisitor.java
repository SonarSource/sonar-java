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
package org.sonar.java.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.java.SonarComponents;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.BLOCK;
import static org.sonar.plugins.java.api.tree.Tree.Kind.BOOLEAN_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CATCH;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CHAR_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONSTRUCTOR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.DOUBLE_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.DO_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.FLOAT_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.FOR_EACH_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.FOR_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.INITIALIZER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.INT_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.LAMBDA_EXPRESSION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.LONG_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NEW_CLASS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.STATIC_INITIALIZER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.STRING_LITERAL;
import static org.sonar.plugins.java.api.tree.Tree.Kind.TOKEN;
import static org.sonar.plugins.java.api.tree.Tree.Kind.TRY_STATEMENT;
import static org.sonar.plugins.java.api.tree.Tree.Kind.VARIABLE;
import static org.sonar.plugins.java.api.tree.Tree.Kind.WHILE_STATEMENT;

/**
 * Saves information about lines directly into Sonar by using {@link FileLinesContext}.
 */
public class FileLinesVisitor extends SubscriptionVisitor {

  private final SonarComponents sonarComponents;
  private final Set<Integer> linesOfCode = new HashSet<>();
  private final Set<Integer> executableLines = new HashSet<>();

  public FileLinesVisitor(SonarComponents sonarComponents) {
    this.sonarComponents = sonarComponents;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(TOKEN,
      METHOD, CONSTRUCTOR,
      INITIALIZER, STATIC_INITIALIZER,
      VARIABLE,
      FOR_EACH_STATEMENT, FOR_STATEMENT, WHILE_STATEMENT, DO_STATEMENT,
      LAMBDA_EXPRESSION);

  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    InputFile currentFile = context.getInputFile();
    FileLinesContext fileLinesContext = sonarComponents.fileLinesContextFor(currentFile);
    for (int line = 1; line <= currentFile.lines(); line++) {
      fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, linesOfCode.contains(line) ? 1 : 0);
      fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, executableLines.contains(line) ? 1 : 0);
    }
    fileLinesContext.save();

    linesOfCode.clear();
    executableLines.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    List<? extends Tree> trees = Collections.emptyList();
    switch (tree.kind()) {
      case INITIALIZER:
      case STATIC_INITIALIZER:
        trees = ((BlockTree) tree).body();
        break;
      case VARIABLE:
        trees = visitVariable((VariableTree) tree);
        break;
      case LAMBDA_EXPRESSION:
        trees = visitLambda((LambdaExpressionTree) tree);
        break;
      case METHOD:
      case CONSTRUCTOR:
        trees = visitMethod((MethodTree) tree);
        break;
      case FOR_STATEMENT:
      case FOR_EACH_STATEMENT:
      case WHILE_STATEMENT:
      case DO_STATEMENT:
        executableLines.add(tree.lastToken().line());
        break;
      default:
        // Do nothing particular
    }
    computeExecutableLines(trees);
  }

  private List<? extends Tree> visitVariable(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    if(initializer != null && !isConstant(variableTree)) {
      return Lists.newArrayList(initializer);
    }
    if(variableTree.parent().is(CATCH)) {
      // catch variable are counted as executable lines
      new ExecutableLinesTokenVisitor().scanTree(variableTree);
    }
    return Collections.emptyList();
  }

  private static List<? extends Tree> visitLambda(LambdaExpressionTree lambda) {
    Tree body = lambda.body();
    if(body.is(BLOCK)) {
      return ((BlockTree) body).body();
    }
    return Lists.newArrayList(body);
  }

  private List<? extends Tree> visitMethod(MethodTree tree) {
    BlockTree methodBody = tree.block();
    if(methodBody != null) {
      // get the last
      TypeTree returnType = tree.returnType();
      if(returnType == null || "void".equals(returnType.firstToken().text())) {
        executableLines.add(methodBody.closeBraceToken().line());
      }
      return methodBody.body();
    }
    return Collections.emptyList();
  }

  private void computeExecutableLines(List<? extends Tree> trees) {
    if(trees.isEmpty()) {
      return;
    }
    // rely on cfg to get every instructions and get most of the token.
    CFG cfg = CFG.buildCFG(trees);
    cfg.blocks()
      .stream()
      .flatMap(b->b.elements().stream())
      .forEach(
        t -> {
          if (t.is(NEW_CLASS)) {
            NewClassTree newClassTree = (NewClassTree) t;
            new ExecutableLinesTokenVisitor().scanTree(newClassTree.identifier());
            executableLines.add(newClassTree.newKeyword().line());
          } else if (t.is(TRY_STATEMENT)) {
            // add last token of try statements
            executableLines.add(t.lastToken().line());
          } else {
            executableLines.add(t.firstToken().line());
          }
        }
      );
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    linesOfCode.add(syntaxToken.line());
  }

  private static boolean isConstant(VariableTree variableTree) {
    return ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.STATIC)
      &&  ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.FINAL)
      && variableTree.initializer().is(BOOLEAN_LITERAL,
      STRING_LITERAL, LONG_LITERAL,
      CHAR_LITERAL, INT_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL, NULL_LITERAL);
  }

  /**
   * Add lines of token to executable lines only, skips comments and blank lines.
   */
  private class ExecutableLinesTokenVisitor extends SubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(TOKEN);
    }

    @Override
    public void visitToken(SyntaxToken syntaxToken) {
      executableLines.add(syntaxToken.line());
    }
  }
}


