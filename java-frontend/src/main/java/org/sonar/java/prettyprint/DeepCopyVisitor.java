package org.sonar.java.prettyprint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.ArrayDimensionTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.InternalPostfixUnaryExpression;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.java.model.pattern.DefaultPatternTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.pattern.NullPatternTreeImpl;
import org.sonar.java.model.pattern.RecordPatternTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
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
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ProvidesDirectiveTree;
import org.sonar.plugins.java.api.tree.RecordPatternTree;
import org.sonar.plugins.java.api.tree.RequiresDirectiveTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.UsesDirectiveTree;
import org.sonar.plugins.java.api.tree.VarTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.plugins.java.api.tree.WildcardTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

public class DeepCopyVisitor implements TreeVisitor {

  private final Queue<Tree> results = new LinkedList<>();

  protected void pushResult(Tree result){
    results.add(result);
  }

  protected Tree popUniqueResult(){
    if (results.size() != 1){
      throw new IllegalStateException("size was " + results.size());
    }
    return results.remove();
  }

  protected List<Tree> getResults(){
    var ls = new ArrayList<>(results);
    results.clear();
    return ls;
  }

  public Tree copy(Tree tree) {
    return convert(tree);
  }

  private <T extends Tree> T convert(Tree tree) {
    if (tree == null) {
      return null;
    } else if (tree instanceof SyntaxToken token) {
      // tokens are not covered by the visitor system
      return (T) token;
    } else {
      tree.accept(this);
      return (T) popUniqueResult();
    }
  }

  private <T extends Tree> List<T> convert(List<? extends Tree> ls, Class<T> clazz) {
    for (var tree : ls) {
      tree.accept(this);
    }
    return getResults().stream().map(clazz::cast).toList();
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    results.add(new JavaTree.CompilationUnitTreeImpl(
      convert(tree.packageDeclaration()),
      convert(tree.imports(), ImportClauseTree.class),
      convert(tree.types(), Tree.class),
      convert(tree.moduleDeclaration()),
      convert(tree.eofToken())
    ));
  }

  @Override
  public void visitImport(ImportTree tree) {
    results.add(new JavaTree.ImportTreeImpl(
      convert(tree.importKeyword()),
      convert(tree.staticKeyword()),
      convert(tree.qualifiedIdentifier()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitClass(ClassTree tree) {
    results.add(new ClassTreeImpl(
      tree.kind(),
      convert(tree.openBraceToken()),
      convert(tree.members(), Tree.class),
      convert(tree.closeBraceToken())
    ).complete(
      convert(tree.modifiers()),
      convert(tree.declarationKeyword()),
      convert(tree.simpleName())
    ).completeTypeParameters(
      convert(tree.typeParameters())
    ).completeRecordComponents(
      convert(tree.recordOpenParenToken()),
      convert(tree.recordComponents(), VariableTree.class),
      convert(tree.recordCloseParenToken())
    ).completeSuperclass(
      convert(tree.extendsKeyword()),
      convert(tree.superClass())
    ).completeInterfaces(
      convert(tree.implementsKeyword()),
      convert(tree.superInterfaces())
    ).completePermittedTypes(
      convert(tree.permitsKeyword()),
      convert(tree.permittedTypes())
    ));
  }

  @Override
  public void visitMethod(MethodTree tree) {
    var params = new FormalParametersListTreeImpl(convert(tree.openParenToken()), convert(tree.closeParenToken()));
    params.addAll(convert(params, VariableTreeImpl.class));
    results.add(new MethodTreeImpl(
      convert(tree.returnType()),
      convert(tree.simpleName()),
      params,
      convert(tree.throwsToken()),
      convert(tree.throwsClauses()),
      convert(tree.block()),
      convert(tree.semicolonToken())
    ).completeWithModifiers(
      convert(tree.modifiers())
    ).completeWithTypeParameters(
      convert(tree.typeParameters())
    ));
  }

  @Override
  public void visitBlock(BlockTree tree) {
    results.add(new BlockTreeImpl(
      tree.kind(),
      convert(tree.openBraceToken()),
      convert(tree.body(), StatementTree.class),
      convert(tree.closeBraceToken())
    ));
  }

  @Override
  public void visitEmptyStatement(EmptyStatementTree tree) {
    results.add(new EmptyStatementTreeImpl(
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    results.add(new LabeledStatementTreeImpl(
      convert(tree.label()),
      convert(tree.colonToken()),
      convert(tree.statement())
    ));
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    results.add(new ExpressionStatementTreeImpl(
      convert(tree.expression()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    results.add(new IfStatementTreeImpl(
      convert(tree.ifKeyword()),
      convert(tree.openParenToken()),
      convert(tree.condition()),
      convert(tree.closeParenToken()),
      convert(tree.thenStatement()),
      convert(tree.elseKeyword()),
      convert(tree.elseStatement())
    ));
  }

  @Override
  public void visitAssertStatement(AssertStatementTree tree) {
    results.add(new AssertStatementTreeImpl(
      convert(tree.assertKeyword()),
      convert(tree.condition()),
      convert(tree.semicolonToken())
    ).complete(
      convert(tree.colonToken()),
      convert(tree.detail())
    ));
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    results.add(new SwitchStatementTreeImpl(
      convert(tree.switchKeyword()),
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.openBraceToken()),
      convert(tree.cases(), CaseGroupTreeImpl.class),
      convert(tree.closeBraceToken())
    ));
  }

  @Override
  public void visitSwitchExpression(SwitchExpressionTree tree) {
    results.add(new SwitchExpressionTreeImpl(
      convert(tree.switchKeyword()),
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.openBraceToken()),
      convert(tree.cases(), CaseGroupTreeImpl.class),
      convert(tree.closeBraceToken())
    ));
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    var statsList = StatementListTreeImpl.emptyList();
    statsList.addAll(tree.body());
    results.add(new CaseGroupTreeImpl(
      convert(tree.labels(), CaseLabelTreeImpl.class),
      statsList
    ));
  }

  @Override
  public void visitCaseLabel(CaseLabelTree tree) {
    results.add(new CaseLabelTreeImpl(
      convert(tree.caseOrDefaultKeyword()),
      convert(tree.expressions(), ExpressionTree.class),
      convert(tree.colonOrArrowToken())
    ));
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    results.add(new WhileStatementTreeImpl(
      convert(tree.whileKeyword()),
      convert(tree.openParenToken()),
      convert(tree.condition()),
      convert(tree.closeParenToken()),
      convert(tree.statement())
    ));
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    results.add(new DoWhileStatementTreeImpl(
      convert(tree.doKeyword()),
      convert(tree.statement()),
      convert(tree.whileKeyword()),
      convert(tree.openParenToken()),
      convert(tree.condition()),
      convert(tree.closeParenToken()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    results.add(new ForStatementTreeImpl(
      convert(tree.forKeyword()),
      convert(tree.openParenToken()),
      convert(tree.initializer()),
      convert(tree.firstSemicolonToken()),
      convert(tree.condition()),
      convert(tree.secondSemicolonToken()),
      convert(tree.update()),
      convert(tree.closeParenToken()),
      convert(tree.statement())
    ));
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    results.add(new ForEachStatementImpl(
      convert(tree.forKeyword()),
      convert(tree.openParenToken()),
      convert(tree.variable()),
      convert(tree.colonToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.statement())
    ));
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    results.add(new BreakStatementTreeImpl(
      convert(tree.breakKeyword()),
      convert(tree.label()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitYieldStatement(YieldStatementTree tree) {
    results.add(new YieldStatementTreeImpl(
      convert(tree.yieldKeyword()),
      convert(tree.expression()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    results.add(new ContinueStatementTreeImpl(
      convert(tree.continueKeyword()),
      convert(tree.label()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    results.add(new ReturnStatementTreeImpl(
      convert(tree.returnKeyword()),
      convert(tree.expression()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    results.add(new ThrowStatementTreeImpl(
      convert(tree.throwKeyword()),
      convert(tree.expression()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    results.add(new SynchronizedStatementTreeImpl(
      convert(tree.synchronizedKeyword()),
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.block())
    ));
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    results.add(new TryStatementTreeImpl(
      convert(tree.tryKeyword()),
      convert(tree.openParenToken()),
      convert(tree.resourceList()),
      convert(tree.closeParenToken()),
      convert(tree.block()),
      convert(tree.catches(), CatchTree.class),
      convert(tree.finallyKeyword()),
      convert(tree.finallyBlock())
    ));
  }

  @Override
  public void visitCatch(CatchTree tree) {
    results.add(new CatchTreeImpl(
      convert(tree.catchKeyword()),
      convert(tree.openParenToken()),
      convert(tree.parameter()),
      convert(tree.closeParenToken()),
      convert(tree.block())
    ));
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    results.add(switch (tree.kind()) {
      case POSTFIX_INCREMENT, POSTFIX_DECREMENT -> new InternalPostfixUnaryExpression(
        tree.kind(),
        convert(tree.expression()),
        convert(tree.operatorToken())
      );
      default -> new InternalPrefixUnaryExpression(
        tree.kind(),
        convert(tree.operatorToken()),
        convert(tree.expression())
      );
    });
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    results.add(new BinaryExpressionTreeImpl(
      tree.kind(),
      convert(tree.leftOperand()),
      convert(tree.operatorToken()),
      convert(tree.rightOperand())
    ));
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    results.add(new ConditionalExpressionTreeImpl(
      convert(tree.condition()),
      convert(tree.questionToken()),
      convert(tree.trueExpression()),
      convert(tree.colonToken()),
      convert(tree.falseExpression())
    ));
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    results.add(new ArrayAccessExpressionTreeImpl(
      convert(tree.expression()),
      convert(tree.dimension())
    ));
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    results.add(new MemberSelectExpressionTreeImpl(
      convert(tree.expression()),
      convert(tree.operatorToken()),
      convert(tree.identifier())
    ));
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    results.add(new NewClassTreeImpl(
      convert(tree.identifier()),
      convert(tree.arguments()),
      convert(tree.classBody())
    ).completeWithNewKeyword(
      convert(tree.newKeyword())
    ).completeWithEnclosingExpression(
      convert(tree.enclosingExpression())
    ).completeWithTypeArguments(
      convert(tree.typeArguments())
    ));
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    results.add(new NewArrayTreeImpl(
      convert(tree.dimensions(), ArrayDimensionTree.class),
      convert(tree.initializers())
    ));
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    results.add(new MethodInvocationTreeImpl(
      convert(tree.methodSelect()),
      convert(tree.typeArguments()),
      convert(tree.arguments())
    ));
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    results.add(new TypeCastExpressionTreeImpl(
      convert(tree.openParenToken()),
      convert(tree.type()),
      convert(tree.andToken()),
      convert(tree.bounds()),
      convert(tree.closeParenToken()),
      convert(tree.expression())
    ));
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    results.add(new InstanceOfTreeImpl(
      convert(tree.expression()),
      convert(tree.instanceofKeyword()),
      (TypeTree) convert(tree.type())
    ));
  }

  @Override
  public void visitPatternInstanceOf(PatternInstanceOfTree tree) {
    results.add(new InstanceOfTreeImpl(
      convert(tree.expression()),
      convert(tree.instanceofKeyword()),
      (PatternTree) convert(tree.pattern())
    ));
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    results.add(new ParenthesizedTreeImpl(
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken())
    ));
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    results.add(new AssignmentExpressionTreeImpl(
      tree.kind(),
      convert(tree.variable()),
      convert(tree.operatorToken()),
      convert(tree.expression())
    ));
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    results.add(new LiteralTreeImpl(
      tree.kind(),
      convert(tree.token())
    ));
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    results.add(new IdentifierTreeImpl(
      convert(tree.identifierToken())
    ));
  }

  @Override
  public void visitVarType(VarTypeTree tree) {
    results.add(new VarTypeTreeImpl(
      convert(tree.varToken())
    ));
  }

  @Override
  public void visitVariable(VariableTree tree) {
    results.add(new VariableTreeImpl(
      convert((Tree) tree.modifiers()),
      (TypeTree) null,
      convert(tree.simpleName())
    ).completeTypeAndInitializer(
      convert(tree.type()),
      convert(tree.equalToken()),
      convert(tree.initializer())
    ));
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    results.add(new EnumConstantTreeImpl(
      convert((Tree) tree.modifiers()),
      convert(tree.simpleName()),
      convert(tree.initializer()),
      convert(tree.separatorToken())
    ));
  }

  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    var typeTree = new JavaTree.PrimitiveTypeTreeImpl(
      convert(tree.keyword())
    );
    typeTree.complete(
      convert(tree.annotations(), AnnotationTree.class)
    );
    results.add(typeTree);
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    results.add(new JavaTree.ArrayTypeTreeImpl(
      convert(tree.type()),
      convert(tree.annotations(), AnnotationTreeImpl.class),
      convert(tree.openBracketToken()),
      convert(tree.closeBracketToken())
    ));
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    var typeTree = new JavaTree.ParameterizedTypeTreeImpl(
      convert(tree.type()),
      convert(tree.typeArguments())
    );
    typeTree.complete(
      convert(tree.annotations(), AnnotationTree.class)
    );
    results.add(typeTree);
  }

  @Override
  public void visitWildcard(WildcardTree tree) {
    var wildcard = new JavaTree.WildcardTreeImpl(
      tree.kind(),
      convert(tree.extendsOrSuperToken()),
      convert(tree.bound())
    );
    wildcard.complete(
      (InternalSyntaxToken) convert(tree.queryToken())
    );
    wildcard.complete(
      convert(tree.annotations(), AnnotationTree.class)
    );
    results.add(wildcard);
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    results.add(new JavaTree.UnionTypeTreeImpl(
      convert(tree.typeAlternatives())
    ));
  }

  @Override
  public void visitModifier(ModifiersTree modifiersTree) {
    var modifiersList = new ArrayList<ModifierTree>();
    modifiersList.addAll(convert(modifiersTree.modifiers(), ModifierTree.class));
    modifiersList.addAll(convert(modifiersTree.annotations(), ModifierTree.class));
    results.add(new ModifiersTreeImpl(modifiersList));
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    results.add(new AnnotationTreeImpl(
      convert(annotationTree.atToken()),
      convert(annotationTree.annotationType()),
      convert(annotationTree.arguments())
    ));
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    results.add(new LambdaExpressionTreeImpl(
      convert(lambdaExpressionTree.openParenToken()),
      convert(lambdaExpressionTree.parameters(), VariableTree.class),
      convert(lambdaExpressionTree.closeParenToken()),
      convert(lambdaExpressionTree.arrowToken()),
      convert(lambdaExpressionTree.body())
    ));
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    results.add(new TypeParameterTreeImpl(
      convert(typeParameter.identifier()),
      convert(typeParameter.extendToken()),
      convert(typeParameter.bounds())
    ));
  }

  @Override
  public void visitArguments(Arguments arguments) {
    var args = ArgumentListTreeImpl.emptyList();
    args.complete(
      convert(arguments.openParenToken()),
      convert(arguments.closeParenToken())
    );
    args.addAll(convert(arguments, ExpressionTree.class));
    results.add(args);
  }

  @Override
  public void visitTypeArguments(TypeArguments trees) {
    var typeArgs = new TypeArgumentListTreeImpl(
      convert(trees.openBracketToken()),
      convert(trees.closeBracketToken())
    );
    typeArgs.addAll(trees);
    results.add(typeArgs);
  }

  @Override
  public void visitTypeParameters(TypeParameters trees) {
    var typeParams = new TypeParameterListTreeImpl(
      convert(trees.openBracketToken()),
      convert(trees.closeBracketToken())
    );
    typeParams.addAll(trees);
    results.add(typeParams);
  }

  @Override
  public void visitOther(Tree tree) {
    throw new AssertionError();
  }

  @Override
  public void visitMethodReference(MethodReferenceTree methodReferenceTree) {
    var methodRef = new MethodReferenceTreeImpl(
      convert(methodReferenceTree.expression()),
      convert(methodReferenceTree.doubleColon())
    );
    methodRef.complete(
      convert(methodReferenceTree.typeArguments()),
      convert(methodReferenceTree.method())
    );
    results.add(methodRef);
  }

  @Override
  public void visitPackage(PackageDeclarationTree tree) {
    results.add(new JavaTree.PackageDeclarationTreeImpl(
      convert(tree.annotations(), AnnotationTree.class),
      convert(tree.packageKeyword()),
      convert(tree.packageName()),
      convert(tree.semicolonToken())
    ));
  }

  @Override
  public void visitModule(ModuleDeclarationTree module) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitRequiresDirectiveTree(RequiresDirectiveTree tree) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitExportsDirectiveTree(ExportsDirectiveTree tree) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitOpensDirective(OpensDirectiveTree tree) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitUsesDirective(UsesDirectiveTree tree) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitProvidesDirective(ProvidesDirectiveTree tree) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void visitArrayDimension(ArrayDimensionTree tree) {
    results.add(new ArrayDimensionTreeImpl(
      convert(tree.openBracketToken()),
      convert(tree.expression()),
      convert(tree.closeBracketToken())
    ));
  }

  @Override
  public void visitTypePattern(TypePatternTree tree) {
    results.add(new TypePatternTreeImpl(
      convert(tree.patternVariable())
    ));
  }

  @Override
  public void visitNullPattern(NullPatternTree tree) {
    results.add(new NullPatternTreeImpl(
      convert(tree.nullLiteral())
    ));
  }

  @Override
  public void visitDefaultPattern(DefaultPatternTree tree) {
    results.add(new DefaultPatternTreeImpl(
      convert(tree.defaultToken())
    ));
  }

  @Override
  public void visitGuardedPattern(GuardedPatternTree tree) {
    results.add(new GuardedPatternTreeImpl(
      convert(tree.pattern()),
      convert(tree.whenOperator()),
      convert(tree.expression())
    ));
  }

  @Override
  public void visitRecordPattern(RecordPatternTree tree) {
    results.add(new RecordPatternTreeImpl(
      convert(tree.type()),
      convert(tree.openParenToken()),
      convert(tree.patterns(), PatternTree.class),
      convert(tree.closeParenToken())
    ));
  }

}
