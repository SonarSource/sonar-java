package org.sonar.java.checks.prettyprint;

import java.util.ArrayList;
import java.util.List;
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
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.GuardedPatternTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
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

  // TODO use a list instead, does not work for list nodes
  protected Tree result;

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
      return (T) result;
    }
  }

  private <T extends Tree> List<T> convert(List<T> ls) {
    return ls.stream().map(this::<T>convert).toList();
  }

  private <T extends Tree> List<T> convert(List<? extends Tree> ls, Class<T> clazz) {
    return ls.stream()
      .map(this::<T>convert)
      .map(clazz::cast)
      .toList();
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    result = new JavaTree.CompilationUnitTreeImpl(
      convert(tree.packageDeclaration()),
      convert(tree.imports()),
      convert(tree.types()),
      convert(tree.moduleDeclaration()),
      convert(tree.eofToken())
    );
  }

  @Override
  public void visitImport(ImportTree tree) {
    result = new JavaTree.ImportTreeImpl(
      convert(tree.importKeyword()),
      convert(tree.staticKeyword()),
      convert(tree.qualifiedIdentifier()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitClass(ClassTree tree) {
    result = new ClassTreeImpl(
      tree.kind(),
      convert(tree.openBraceToken()),
      convert(tree.members()),
      convert(tree.closeBraceToken())
    ).complete(
      convert((Tree) tree.modifiers()),
      convert(tree.declarationKeyword()),
      convert(tree.simpleName())
    ).completeTypeParameters(
      convert((Tree) tree.typeParameters())
    ).completeRecordComponents(
      convert(tree.recordOpenParenToken()),
      convert(tree.recordComponents()),
      convert(tree.recordCloseParenToken())
    ).completeSuperclass(
      convert(tree.extendsKeyword()),
      convert(tree.superClass())
    ).completeInterfaces(
      convert(tree.implementsKeyword()),
      convert((Tree) tree.superInterfaces())
    ).completePermittedTypes(
      convert(tree.permitsKeyword()),
      convert((Tree) tree.permittedTypes())
    );
  }

  @Override
  public void visitMethod(MethodTree tree) {
    var params = new FormalParametersListTreeImpl(convert(tree.openParenToken()), convert(tree.closeParenToken()));
    params.addAll(convert(params, VariableTreeImpl.class));
    result = new MethodTreeImpl(
      convert(tree.returnType()),
      convert(tree.simpleName()),
      params,
      convert(tree.throwsToken()),
      convert((Tree) tree.throwsClauses()),
      convert(tree.block()),
      convert(tree.semicolonToken())
    ).completeWithModifiers(
      convert((Tree) tree.modifiers())
    ).completeWithTypeParameters(
      convert((Tree) tree.typeParameters())
    );
  }

  @Override
  public void visitBlock(BlockTree tree) {
    result = new BlockTreeImpl(
      tree.kind(),
      convert(tree.openBraceToken()),
      convert(tree.body()),
      convert(tree.closeBraceToken())
    );
  }

  @Override
  public void visitEmptyStatement(EmptyStatementTree tree) {
    result = new EmptyStatementTreeImpl(
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitLabeledStatement(LabeledStatementTree tree) {
    result = new LabeledStatementTreeImpl(
      convert(tree.label()),
      convert(tree.colonToken()),
      convert(tree.statement())
    );
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    result = new ExpressionStatementTreeImpl(
      convert(tree.expression()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    result = new IfStatementTreeImpl(
      convert(tree.ifKeyword()),
      convert(tree.openParenToken()),
      convert(tree.condition()),
      convert(tree.closeParenToken()),
      convert(tree.thenStatement()),
      convert(tree.elseKeyword()),
      convert(tree.elseStatement())
    );
  }

  @Override
  public void visitAssertStatement(AssertStatementTree tree) {
    result = new AssertStatementTreeImpl(
      convert(tree.assertKeyword()),
      convert(tree.condition()),
      convert(tree.semicolonToken())
    ).complete(
      convert(tree.colonToken()),
      convert(tree.detail())
    );
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    result = new SwitchStatementTreeImpl(
      convert(tree.switchKeyword()),
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.openBraceToken()),
      convert(tree.cases(), CaseGroupTreeImpl.class),
      convert(tree.closeBraceToken())
    );
  }

  @Override
  public void visitSwitchExpression(SwitchExpressionTree tree) {
    result = new SwitchExpressionTreeImpl(
      convert(tree.switchKeyword()),
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.openBraceToken()),
      convert(tree.cases(), CaseGroupTreeImpl.class),
      convert(tree.closeBraceToken())
    );
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    var statsList = StatementListTreeImpl.emptyList();
    statsList.addAll(tree.body());
    result = new CaseGroupTreeImpl(
      convert(tree.labels(), CaseLabelTreeImpl.class),
      statsList
    );
  }

  @Override
  public void visitCaseLabel(CaseLabelTree tree) {
    result = new CaseLabelTreeImpl(
      convert(tree.caseOrDefaultKeyword()),
      convert(tree.expressions()),
      convert(tree.colonOrArrowToken())
    );
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    result = new WhileStatementTreeImpl(
      convert(tree.whileKeyword()),
      convert(tree.openParenToken()),
      convert(tree.condition()),
      convert(tree.closeParenToken()),
      convert(tree.statement())
    );
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    result = new DoWhileStatementTreeImpl(
      convert(tree.doKeyword()),
      convert(tree.statement()),
      convert(tree.whileKeyword()),
      convert(tree.openParenToken()),
      convert(tree.condition()),
      convert(tree.closeParenToken()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    result = new ForStatementTreeImpl(
      convert(tree.forKeyword()),
      convert(tree.openParenToken()),
      convert((Tree) tree.initializer()),
      convert(tree.firstSemicolonToken()),
      convert(tree.condition()),
      convert(tree.secondSemicolonToken()),
      convert((Tree) tree.update()),
      convert(tree.closeParenToken()),
      convert(tree.statement())
    );
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    result = new ForEachStatementImpl(
      convert(tree.forKeyword()),
      convert(tree.openParenToken()),
      convert(tree.variable()),
      convert(tree.colonToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.statement())
    );
  }

  @Override
  public void visitBreakStatement(BreakStatementTree tree) {
    result = new BreakStatementTreeImpl(
      convert(tree.breakKeyword()),
      convert(tree.label()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitYieldStatement(YieldStatementTree tree) {
    result = new YieldStatementTreeImpl(
      convert(tree.yieldKeyword()),
      convert(tree.expression()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitContinueStatement(ContinueStatementTree tree) {
    result = new ContinueStatementTreeImpl(
      convert(tree.continueKeyword()),
      convert(tree.label()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    result = new ReturnStatementTreeImpl(
      convert(tree.returnKeyword()),
      convert(tree.expression()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    result = new ThrowStatementTreeImpl(
      convert(tree.throwKeyword()),
      convert(tree.expression()),
      convert(tree.semicolonToken())
    );
  }

  @Override
  public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
    result = new SynchronizedStatementTreeImpl(
      convert(tree.synchronizedKeyword()),
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken()),
      convert(tree.block())
    );
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    result = new TryStatementTreeImpl(
      convert(tree.tryKeyword()),
      convert(tree.openParenToken()),
      convert((Tree) tree.resourceList()),
      convert(tree.closeParenToken()),
      convert(tree.block()),
      convert(tree.catches()),
      convert(tree.finallyKeyword()),
      convert(tree.finallyBlock())
    );
  }

  @Override
  public void visitCatch(CatchTree tree) {
    result = new CatchTreeImpl(
      convert(tree.catchKeyword()),
      convert(tree.openParenToken()),
      convert(tree.parameter()),
      convert(tree.closeParenToken()),
      convert(tree.block())
    );
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    result = switch (tree.kind()) {
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
    };
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    result = new BinaryExpressionTreeImpl(
      tree.kind(),
      convert(tree.leftOperand()),
      convert(tree.operatorToken()),
      convert(tree.rightOperand())
    );
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    result = new ConditionalExpressionTreeImpl(
      convert(tree.condition()),
      convert(tree.questionToken()),
      convert(tree.trueExpression()),
      convert(tree.colonToken()),
      convert(tree.falseExpression())
    );
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    result = new ArrayAccessExpressionTreeImpl(
      convert(tree.expression()),
      convert(tree.dimension())
    );
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    result = new MemberSelectExpressionTreeImpl(
      convert(tree.expression()),
      convert(tree.operatorToken()),
      convert(tree.identifier())
    );
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    result = new NewClassTreeImpl(
      convert(tree.identifier()),
      convert((Tree) tree.arguments()),
      convert(tree.classBody())
    ).completeWithNewKeyword(
      convert(tree.newKeyword())
    ).completeWithEnclosingExpression(
      convert(tree.enclosingExpression())
    ).completeWithTypeArguments(
      convert((Tree) tree.typeArguments())
    );
  }

  @Override
  public void visitNewArray(NewArrayTree tree) {
    result = new NewArrayTreeImpl(
      convert(tree.dimensions()),
      convert((Tree) tree.initializers())
    );
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    result = new MethodInvocationTreeImpl(
      convert(tree.methodSelect()),
      convert((Tree) tree.typeArguments()),
      convert((Tree) tree.arguments())
    );
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    result = new TypeCastExpressionTreeImpl(
      convert(tree.openParenToken()),
      convert(tree.type()),
      convert(tree.andToken()),
      convert((Tree) tree.bounds()),
      convert(tree.closeParenToken()),
      convert(tree.expression())
    );
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    result = new InstanceOfTreeImpl(
      convert(tree.expression()),
      convert(tree.instanceofKeyword()),
      (TypeTree) convert(tree.type())
    );
  }

  @Override
  public void visitPatternInstanceOf(PatternInstanceOfTree tree) {
    result = new InstanceOfTreeImpl(
      convert(tree.expression()),
      convert(tree.instanceofKeyword()),
      (PatternTree) convert(tree.pattern())
    );
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    result = new ParenthesizedTreeImpl(
      convert(tree.openParenToken()),
      convert(tree.expression()),
      convert(tree.closeParenToken())
    );
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    result = new AssignmentExpressionTreeImpl(
      tree.kind(),
      convert(tree.variable()),
      convert(tree.operatorToken()),
      convert(tree.expression())
    );
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    result = new LiteralTreeImpl(
      tree.kind(),
      convert(tree.token())
    );
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    result = new IdentifierTreeImpl(
      convert(tree.identifierToken())
    );
  }

  @Override
  public void visitVarType(VarTypeTree tree) {
    result = new VarTypeTreeImpl(
      convert(tree.varToken())
    );
  }

  @Override
  public void visitVariable(VariableTree tree) {
    result = new VariableTreeImpl(
      convert((Tree) tree.modifiers()),
      (TypeTree) null,
      convert(tree.simpleName())
    ).completeTypeAndInitializer(
      convert(tree.type()),
      convert(tree.equalToken()),
      convert(tree.initializer())
    );
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    result = new EnumConstantTreeImpl(
      convert((Tree) tree.modifiers()),
      convert(tree.simpleName()),
      convert(tree.initializer()),
      convert(tree.separatorToken())
    );
  }

  @Override
  public void visitPrimitiveType(PrimitiveTypeTree tree) {
    var typeTree = new JavaTree.PrimitiveTypeTreeImpl(
      convert(tree.keyword())
    );
    typeTree.complete(
      convert(tree.annotations())
    );
    result = typeTree;
  }

  @Override
  public void visitArrayType(ArrayTypeTree tree) {
    result = new JavaTree.ArrayTypeTreeImpl(
      convert(tree.type()),
      convert(tree.annotations(), AnnotationTreeImpl.class),
      convert(tree.openBracketToken()),
      convert(tree.closeBracketToken())
    );
  }

  @Override
  public void visitParameterizedType(ParameterizedTypeTree tree) {
    var typeTree = new JavaTree.ParameterizedTypeTreeImpl(
      convert(tree.type()),
      convert((Tree) tree.typeArguments())
    );
    typeTree.complete(
      convert(tree.annotations())
    );
    result = typeTree;
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
      convert(tree.annotations())
    );
    result = wildcard;
  }

  @Override
  public void visitUnionType(UnionTypeTree tree) {
    result = new JavaTree.UnionTypeTreeImpl(
      convert((Tree) tree.typeAlternatives())
    );
  }

  @Override
  public void visitModifier(ModifiersTree modifiersTree) {
    var modifiersList = new ArrayList<ModifierTree>();
    modifiersList.addAll(convert(modifiersTree.modifiers(), ModifierTree.class));
    modifiersList.addAll(convert(modifiersTree.annotations(), ModifierTree.class));
    result = new ModifiersTreeImpl(modifiersList);
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    result = new AnnotationTreeImpl(
      convert(annotationTree.atToken()),
      convert(annotationTree.annotationType()),
      convert((Tree) annotationTree.arguments())
    );
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    result = new LambdaExpressionTreeImpl(
      convert(lambdaExpressionTree.openParenToken()),
      convert(lambdaExpressionTree.parameters()),
      convert(lambdaExpressionTree.closeParenToken()),
      convert(lambdaExpressionTree.arrowToken()),
      convert(lambdaExpressionTree.body())
    );
  }

  @Override
  public void visitTypeParameter(TypeParameterTree typeParameter) {
    result = new TypeParameterTreeImpl(
      convert(typeParameter.identifier()),
      convert(typeParameter.extendToken()),
      convert((Tree) typeParameter.bounds())
    );
  }

  @Override
  public void visitTypeArguments(TypeArguments trees) {
    var typeArgs = new TypeArgumentListTreeImpl(
      convert(trees.openBracketToken()),
      convert(trees.closeBracketToken())
    );
    typeArgs.addAll(trees);
    result = typeArgs;
  }

  @Override
  public void visitTypeParameters(TypeParameters trees) {
    var typeParams = new TypeParameterListTreeImpl(
      convert(trees.openBracketToken()),
      convert(trees.closeBracketToken())
    );
    typeParams.addAll(trees);
    result = typeParams;
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
      convert((Tree) methodReferenceTree.typeArguments()),
      convert(methodReferenceTree.method())
    );
    result = methodRef;
  }

  @Override
  public void visitPackage(PackageDeclarationTree tree) {
    result = new JavaTree.PackageDeclarationTreeImpl(
      convert(tree.annotations()),
      convert(tree.packageKeyword()),
      convert(tree.packageName()),
      convert(tree.semicolonToken())
    );
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
    result = new ArrayDimensionTreeImpl(
      convert(tree.openBracketToken()),
      convert(tree.expression()),
      convert(tree.closeBracketToken())
    );
  }

  @Override
  public void visitTypePattern(TypePatternTree tree) {
    result = new TypePatternTreeImpl(
      convert(tree.patternVariable())
    );
  }

  @Override
  public void visitNullPattern(NullPatternTree tree) {
    result = new NullPatternTreeImpl(
      convert(tree.nullLiteral())
    );
  }

  @Override
  public void visitDefaultPattern(DefaultPatternTree tree) {
    result = new DefaultPatternTreeImpl(
      convert(tree.defaultToken())
    );
  }

  @Override
  public void visitGuardedPattern(GuardedPatternTree tree) {
    result = new GuardedPatternTreeImpl(
      convert(tree.pattern()),
      convert(tree.whenOperator()),
      convert(tree.expression())
    );
  }

  @Override
  public void visitRecordPattern(RecordPatternTree tree) {
    result = new RecordPatternTreeImpl(
      convert(tree.type()),
      convert(tree.openParenToken()),
      convert(tree.patterns()),
      convert(tree.closeParenToken())
    );
  }

}
