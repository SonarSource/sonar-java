package org.sonar.java.checks.prettyprint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.precedence.Associativity;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DefaultPatternTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExportsDirectiveTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.NullPatternTree;
import org.sonar.plugins.java.api.tree.OpensDirectiveTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ProvidesDirectiveTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.RequiresDirectiveTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.UsesDirectiveTree;
import org.sonar.plugins.java.api.tree.VarTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.plugins.java.api.tree.WildcardTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

import static org.sonar.java.checks.prettyprint.KindsPrinter.printExprKind;

public final class Prettyprinter implements TreeVisitor {

  private final PrettyPrintStringBuilder ppsb;

  public Prettyprinter(PrettyPrintStringBuilder ppsb) {
    this.ppsb = ppsb;
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    var packageDecl = tree.packageDeclaration();
    if (packageDecl != null) {
      packageDecl.accept(this);
      ppsb.newLine().newLine();
      // TODO maybe configure the number of newLines in config
    }
    var imports = tree.imports();
    if (imports != null) {
      for (ImportClauseTree anImport : imports) {
        anImport.accept(this);
        ppsb.newLine();
      }
    }
    for (Tree type : tree.types()) {
      type.accept(this);
      ppsb.newLine().newLine();
      // TODO maybe configure the number of newLines in config
    }
    var module = tree.moduleDeclaration();
    if (module != null) {
      module.accept(this);
    }
  }

  @Override
  public void visitImport(ImportTree tree) {
    unsupported();
  }

  @Override
  public void visitClass(ClassTree tree) {
    switch (tree.kind()) {
      case CLASS -> {
        tree.modifiers().accept(this);
        ppsb.add("class ");
        tree.simpleName().accept(this);
        printTypeParamsListIfAny(tree.typeParameters());
        var superClass = tree.superClass();
        if (superClass != null) {
          ppsb.add("extends ");
          superClass.accept(this);
          ppsb.addSpace();
        }
        var superInterfaces = tree.superInterfaces();
        if (!superInterfaces.isEmpty()) {
          ppsb.add("implements ");
          join(superInterfaces.iterator(), () -> ppsb.add(", "));
          ppsb.addSpace();
        }
        ppsb.addSpace().blockStart();
        // TODO maybe parametrize the number of newLines in config
        for (Tree member : tree.members()) {
          ppsb.newLine();
          member.accept(this);
          ppsb.newLine();
        }
        ppsb.blockEnd();
      }
      default -> unsupported(); // TODO other cases
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    switch (tree.kind()) {
      case METHOD -> {
        tree.modifiers().accept(this);
        printTypeParamsListIfAny(tree.typeParameters());
        tree.returnType().accept(this);
        ppsb.addSpace();
        tree.simpleName().accept(this);
        printArgsList(tree.parameters().iterator());
        var throwsClauses = tree.throwsClauses();
        if (!throwsClauses.isEmpty()) {
          ppsb.add("throws ");
          join(throwsClauses.iterator(), () -> ppsb.add(", "));
        }
        var bodyBlock = tree.block();
        if (bodyBlock == null) {
          ppsb.addSemicolon();
        } else {
          ppsb.addSpace();
          bodyBlock.accept(this);
        }
      }
      default -> unsupported(); // TODO constructor and annotation parameter
    }
  }

  @Override
  public void visitBlock(BlockTree tree) {
    ppsb.blockStart();
    join(tree.body().iterator(), () -> ppsb.forceSemicolon().newLine(), null, ppsb::forceSemicolon);
    ppsb.blockEnd();
  }

  @Override
  public void visitEmptyStatement(EmptyStatementTree tree) {
    ppsb.addSemicolon();
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    // TODO maybe fileconfig should capture the way labels are formatted
    tree.label().accept(this);
    ppsb.add(":").newLine();
    tree.statement().accept(this);
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    tree.expression().accept(this);
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    // TODO fileconfig should know whether else should be on another line or not
    ppsb.add("if (");
    tree.condition().accept(this);
    ppsb.add(") ");
    tree.thenStatement().accept(this);
    var elseStat = tree.elseStatement();
    if (elseStat != null) {
      elseStat.accept(this);
    }
  }

  @Override
  public void visitAssertStatement(AssertStatementTree tree) {
    ppsb.add("assert ");
    tree.condition().accept(this);
    var detail = tree.detail();
    if (detail != null) {
      ppsb.add(" : ");
      detail.accept(this);
    }
    ppsb.addSemicolon();
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    printSwitch(tree);
  }

  @Override
  public void visitSwitchExpression(SwitchExpressionTree tree) {
    printSwitch(tree);
  }

  private void printSwitch(SwitchTree switchTree) {
    ppsb.add("switch (");
    switchTree.expression().accept(this);
    ppsb.add(") ").blockStart();
    join(switchTree.cases().iterator(), ppsb::newLine);
    ppsb.blockEnd();
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    // TODO should be subject to configuration in config
    if (tree.labels().get(0).colonOrArrowToken().text().equals(":")) {
      for (CaseLabelTree label : tree.labels()) {
        label.accept(this);
        ppsb.newLine();
      }
      join(tree.body().iterator(), ppsb::newLine);
    } else {
      if (tree.labels().size() > 1){
        throw new IllegalArgumentException();
      }
      tree.labels().get(0).accept(this);
      if (tree.body().size() == 1) {
        ppsb.addSpace();
        tree.body().get(0).accept(this);
        ppsb.forceSemicolon();
      } else {
        ppsb.blockStart();
        join(tree.body().iterator(), ppsb::newLine);
        ppsb.blockEnd();
      }
    }
  }

  @Override
  public void visitCaseLabel(CaseLabelTree tree) {
    var isDefault = tree.expressions().isEmpty();
    if (isDefault) {
      ppsb.add("default");
    } else {
      ppsb.add("case ");
      join(tree.expressions().iterator(), () -> ppsb.add(", "));
    }
    ppsb.addSpace().add(tree.colonOrArrowToken().text());
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    ppsb.add("while (");
    tree.condition().accept(this);
    ppsb.add(") ");
    tree.statement().accept(this);
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    ppsb.add("break");
    printLabelIfAny(tree.label());
    ppsb.addSemicolon();
  }

  @Override
  public void visitYieldStatement(YieldStatementTree tree) {
    var yieldToken = tree.yieldKeyword();
    if (yieldToken != null) {
      ppsb.add("yield ");
    }
    tree.expression().accept(this);
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    ppsb.add("continue");
    printLabelIfAny(tree.label());
    ppsb.addSemicolon();
  }

  private void printLabelIfAny(@Nullable IdentifierTree label) {
    if (label != null) {
      ppsb.addSpace();
      label.accept(this);
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    ppsb.add("return");
    var expr = tree.expression();
    if (expr != null) {
      ppsb.addSpace();
      expr.accept(this);
    }
    ppsb.addSemicolon();
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitCatch(CatchTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (tree.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT)) {
      tree.expression().accept(this);
      ppsb.add(printExprKind(tree.kind()));
    } else {
      ppsb.add(printExprKind(tree.kind()));
      var parenthesize = tree.hasPrecedenceOver(tree.expression());
      printMaybeWithParentheses(tree.expression(), parenthesize);
    }
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    var lhs = tree.leftOperand();
    var rhs = tree.rightOperand();
    var parenthesizeLhs = tree.hasPrecedenceOver(lhs);
    printMaybeWithParentheses(lhs, parenthesizeLhs);
    ppsb.addSpace().add(printExprKind(tree.kind())).addSpace();
    var parenthesizeRhs = !rhs.hasPrecedenceOver(tree)
      && !(tree.kind() == rhs.kind() && Associativity.isKnownAssociativeOperator(lhs.symbolType(), tree.kind(), rhs.symbolType()));
    printMaybeWithParentheses(rhs, parenthesizeRhs);
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    tree.expression().accept(this);
    ppsb.add(".");
    tree.identifier().accept(this);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    var typeArgs = tree.typeArguments();
    if (typeArgs != null && !typeArgs.isEmpty()) {
      var methodSelect = (MemberSelectExpressionTree) tree.methodSelect();
      methodSelect.expression().accept(this);
      ppsb.add(".");
      printTypeParamsListIfAny(tree.typeArguments());
      methodSelect.identifier().accept(this);
    } else {
      tree.methodSelect().accept(this);
    }
    printArgsList(tree.arguments().iterator());
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    ppsb.add("(");
    tree.type().accept(this);
    ppsb.add(") ");
    tree.expression().accept(this);
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    tree.expression().accept(this);
    ppsb.add(" instanceof ");
    tree.type().accept(this);
  }

  @Override
  public void visitPatternInstanceOf(PatternInstanceOfTree tree) {
    tree.expression().accept(this);
    ppsb.add(" instanceof ");
    tree.pattern().accept(this);
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    printMaybeWithParentheses(tree.expression(), true);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    tree.variable().accept(this);
    ppsb.addSpace().add(printExprKind(tree.kind())).addSpace();
    tree.expression().accept(this);
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    switch (tree.kind()) {
      case INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL, BOOLEAN_LITERAL, CHAR_LITERAL, STRING_LITERAL -> ppsb.add(tree.value());
      case NULL_LITERAL -> ppsb.add("null");
      case TEXT_BLOCK -> {
        ppsb.incIndent();
        ppsb.add(tree.value());
        ppsb.decIndent();
      }
      default -> throw new AssertionError("unexpected: " + tree.kind());
    }
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    ppsb.add(tree.name());
  }

  @Override
  public void visitVarType(VarTypeTree tree) {
    ppsb.add("var");
  }

  @Override
  public void visitVariable(VariableTree tree) {
    for (ModifierTree mod : tree.modifiers()) {
      mod.accept(this);
      ppsb.addSpace();
    }
    tree.type().accept(this);
    ppsb.addSpace();
    tree.simpleName().accept(this);
    var initializer = tree.initializer();
    if (initializer != null) {
      ppsb.add(" = ");
      initializer.accept(this);
    }
    var endToken = tree.endToken();
    if (endToken != null) {
      ppsb.add(endToken.text());
    }
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    ppsb.add(tree.keyword().text());
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    tree.type().accept(this);
    ppsb.add(tree.ellipsisToken() == null ? "[]" : "...");
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    tree.type().accept(this);
    ppsb.add("<");
    join(tree.typeArguments().iterator(), () -> ppsb.add(", "));
    ppsb.add(">");
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitModifier(ModifiersTree modifiersTree) {
    for (AnnotationTree annotation : modifiersTree.annotations()) {
      annotation.accept(this);
      ppsb.addSpace();
    }
    join(modifiersTree.modifiers().iterator(), ppsb::addSpace, null, ppsb::addSpace);
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    ppsb.add("@");
    annotationTree.annotationType().accept(this);
    var args = annotationTree.arguments();
    if (!args.isEmpty()) {
      ppsb.add("(");
      join(args.iterator(), () -> ppsb.add(", "));
      ppsb.add(")");
    }
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    unsupported();  // TODO
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    unsupported();  // TODO
  }

  @Override
  public void visitTypeArguments(TypeArguments trees) {
    unsupported();  // TODO
  }

  @Override
  public void visitTypeParameters(TypeParameters trees) {
    unsupported();  // TODO
  }

  @Override
  public void visitOther(Tree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
    unsupported();  // TODO
  }

  @Override
  public void visitPackage(PackageDeclarationTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitModule(ModuleDeclarationTree module) {
    unsupported();  // TODO
  }

  @Override
  public void visitRequiresDirectiveTree(RequiresDirectiveTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitExportsDirectiveTree(ExportsDirectiveTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitOpensDirective(OpensDirectiveTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitUsesDirective(UsesDirectiveTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitProvidesDirective(ProvidesDirectiveTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitArrayDimension(ArrayDimensionTree tree) {
    unsupported();  // TODO
  }

  @Override
  public void visitTypePattern(TypePatternTree tree) {
    tree.patternVariable().accept(this);
    // TODO  '->' or ':', if not included in patternVariable
  }

  @Override
  public void visitNullPattern(NullPatternTree tree) {
    ppsb.add("null");
  }

  @Override
  public void visitDefaultPattern(DefaultPatternTree tree) {
    ppsb.add("default");
    // TODO  '->' or ':' ?
  }

  @Override
  public void visitGuardedPattern(GuardedPatternTree tree) {
    tree.pattern().accept(this);
    ppsb.add(" when ");
    tree.expression().accept(this);
    // TODO  '->' or ':' ?
  }

  @Override
  public void visitRecordPattern(RecordPatternTree tree) {
    tree.type().accept(this);
    ppsb.add("(");
    join(tree.patterns().iterator(), () -> ppsb.add(", "));
    ppsb.add(")");
  }

  private void printArgsList(Iterator<? extends Tree> iterator) {
    ppsb.add("(");
    join(iterator, () -> ppsb.add(", "));
    ppsb.add(")");
  }

  private void printTypeParamsListIfAny(@Nullable List<? extends Tree> typeParams) {
    if (typeParams != null && !typeParams.isEmpty()) {
      ppsb.add("<");
      join(typeParams.iterator(), () -> ppsb.add(", "));
      ppsb.add("> ");
    }
  }

  private void join(Iterator<? extends Tree> iterator, Runnable makeSep) {
    while (iterator.hasNext()) {
      var tree = iterator.next();
      tree.accept(this);
      if (iterator.hasNext()) {
        makeSep.run();
      }
    }
  }

  private void join(Iterator<? extends Tree> iterator, Runnable makeSep, @Nullable Runnable beginningIfNonEmpty,
                    @Nullable Runnable terminationIfNonEmpty) {
    if (iterator.hasNext()) {
      if (beginningIfNonEmpty != null) {
        beginningIfNonEmpty.run();
      }
      join(iterator, makeSep);
      if (terminationIfNonEmpty != null) {
        terminationIfNonEmpty.run();
      }
    }
  }

  private void printMaybeWithParentheses(ExpressionTree expr, boolean parenthesize) {
    if (parenthesize) {
      ppsb.add("(");
    }
    expr.accept(this);
    if (parenthesize) {
      ppsb.add(")");
    }
  }

  private void unsupported() {
    throw new UnsupportedOperationException();
  }

}
