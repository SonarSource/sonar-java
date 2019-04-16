package org.sonar.java.ecj;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jdt.internal.formatter.Token;
import org.eclipse.jdt.internal.formatter.TokenManager;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * TODO how to replace internal {@link TerminalTokens} on public {@link org.eclipse.jdt.core.compiler.ITerminalSymbols} given that their values are different?
 *
 * TODO add {@link javax.annotation.ParametersAreNonnullByDefault} so that it can be placed in package-info for all classes ?
 */
public final class EcjParser {

  private char[] sourceChars;
  private CompilationUnit compilationUnit;
  private TokenManager tokenManager;

  private EcjParser() {
  }

  // TODO only for tests
  private static String[] classpath() {
    try (Stream<Path> s = Files.list(Paths.get("target/test-jars"))) {
      return s
        .map(p -> p.toAbsolutePath().toString())
        .toArray(String[]::new);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Tree parse(String source) {
    ASTParser astParser = ASTParser.newParser(AST.JLS11);
    astParser.setResolveBindings(true);
    // TODO check astParser.setStatementsRecovery();
    astParser.setEnvironment(
      classpath(),
      new String[]{},
      new String[]{},
      true
    );
    astParser.setUnitName("Example.java");
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_SOURCE, "11");
    astParser.setCompilerOptions(options);

    char[] sourceChars = source.toCharArray();
    astParser.setSource(sourceChars);
    CompilationUnit astNode = (CompilationUnit) astParser.createAST(null);

    // TODO remove debug
    if (false) {
      Arrays.stream(astNode.getProblems())
        .map(IProblem::getMessage)
        .forEach(System.out::println);
      System.out.println(astNode);
    }
    for (IProblem problem : astNode.getProblems()) {
      if (!problem.isError()) {
        continue;
      }

      // Note that some tests pass even in presence of errors such as "Duplicate method"
      String message = "line " + problem.getSourceLineNumber() + ": " + problem.getMessage();
      System.err.println(message);

      if (problem.getMessage().contains("Syntax error")) {
        throw new UnexpectedAccessException(message);
      }
    }

    List<Token> tokens = new ArrayList<>();
    Scanner scanner = new Scanner(
      true,
      true,
      false,
      CompilerOptions.versionToJdkLevel("11"),
      null,
      null,
      false
    );
    scanner.setSource(sourceChars);
    while (true) {
      try {
        int tokenType = scanner.getNextToken();
        if (tokenType == TerminalTokens.TokenNameEOF) {
          break;
        }
        Token token = Token.fromCurrent(scanner, tokenType);
        tokens.add(token);
      } catch (InvalidInputException e) {
        throw new RuntimeException(e);
      }
    }

    EcjParser converter = new EcjParser();
    converter.tokenManager = new TokenManager(tokens, source, new DefaultCodeFormatterOptions(new HashMap<>()));
    converter.sourceChars = sourceChars;
    Tree tree = converter.convert(astNode);
    setParents(tree);

    // TODO remove:
    tree.accept(new BaseTreeVisitor());
    return tree;
  }

  private static void setParents(Tree node) {
    Iterator<? extends Tree> childrenIterator = ((ETree) node).children();
    while (childrenIterator.hasNext()) {
      Tree child = childrenIterator.next();
      ((ETree) child).parent = node;
      setParents(child);
    }
  }

  private Tree convert(ASTNode node) {
    if (node == null) {
      return null;
    }
    switch (node.getNodeType()) {
      case ASTNode.COMPILATION_UNIT: {
        ECompilationUnit t = new ECompilationUnit();
        CompilationUnit e = (CompilationUnit) node;
        this.compilationUnit = e;

        // TODO HACK
        for (Object o : e.getCommentList()) {
          Comment comment = (Comment) o;
          ESyntaxTrivia trivia = new ESyntaxTrivia();
          trivia.comment = new String(sourceChars, comment.getStartPosition(), comment.getLength());
          trivia.line = compilationUnit.getLineNumber(comment.getStartPosition());
          trivia.column = compilationUnit.getColumnNumber(comment.getStartPosition());
          t.eofToken.trivias.add(trivia);
        }

        if (e.getPackage() != null) {
          t.packageDeclaration = new EPackageDeclaration();
          t.packageDeclaration.name = convertExpression(e.getPackage().getName());
        }

        for (Object o : e.imports()) {
          ImportDeclaration i = (ImportDeclaration) o;
          EImportDeclaration d = new EImportDeclaration();
          d.importKeyword = createSyntaxToken(i, "import");
          d.isStatic = i.isStatic();
          d.qualifiedIdentifier = convertExpression(i.getName());
          d.semicolonToken = lastTokenIn(i, TerminalTokens.TokenNameSEMICOLON);
          t.imports.add(d);
        }

        // TODO
//        convert(e.getModule());

        for (Object type : e.types()) {
          t.types.add(convert((AbstractTypeDeclaration) type));
        }
        return t;
      }
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION:
        return convertExpression((Expression) node);
      case ASTNode.MODIFIER: {
        Modifier e = (Modifier) node;
        return new EModifierKeyword(
          createSyntaxToken(e, e.getKeyword().toString())
        );
      }
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
      case ASTNode.ENUM_DECLARATION:
      case ASTNode.TYPE_DECLARATION: {
        EClass t = new EClass();
        AbstractTypeDeclaration e = (AbstractTypeDeclaration) node;

        for (Object o : e.modifiers()) {
          t.modifiers.elements.add(
            (ModifierTree) convert((ASTNode) o)
          );
        }

        t.ast = e.getAST();
        t.binding = e.resolveBinding();

        if (e.getNodeType() == ASTNode.ENUM_DECLARATION) {
          t.kind = Tree.Kind.ENUM;
        } else if (e.getNodeType() == ASTNode.ANNOTATION_TYPE_DECLARATION) {
          t.kind = Tree.Kind.ANNOTATION_TYPE;
        } else {
          t.kind = ((TypeDeclaration) e).isInterface() ? Tree.Kind.INTERFACE : Tree.Kind.CLASS;

          t.superClass = convertType(
            ((TypeDeclaration) e).getSuperclassType()
          );

          for (Object o : ((TypeDeclaration) e).superInterfaceTypes()) {
            t.superInterfaces.elements.add(convertType(
              (Type) o
            ));
          }
        }

        switch (t.kind) {
          case ENUM:
            t.declarationKeyword = firstTokenBefore(e.getName(), TerminalTokens.TokenNameenum);
            break;
          case CLASS:
            t.declarationKeyword = firstTokenBefore(e.getName(), TerminalTokens.TokenNameclass);
            break;
          case INTERFACE:
          case ANNOTATION_TYPE:
            t.declarationKeyword = firstTokenBefore(e.getName(), TerminalTokens.TokenNameinterface);
            break;
        }

        t.simpleName = convertSimpleName(e.getName());
        t.openBraceToken = firstTokenAfter(e.getName(), TerminalTokens.TokenNameLBRACE);
        for (Object o : e.bodyDeclarations()) {
          processBodyDeclaration((BodyDeclaration) o, t.members);
        }
        t.closeBraceToken = lastTokenIn(e, TerminalTokens.TokenNameRBRACE);

        return t;
      }
      case ASTNode.METHOD_DECLARATION: {
        EMethod t = new EMethod();
        MethodDeclaration e = (MethodDeclaration) node;
        t.ast = e.getAST();
        t.binding = e.resolveBinding();

        for (Object o : e.modifiers()) {
          t.modifiers.elements.add(
            (ModifierTree) convert((ASTNode) o)
          );
        }
        t.returnType = convertType(e.getReturnType2());
        t.simpleName = convertSimpleName(e.getName());
        for (Object o : e.parameters()) {
          t.parameters.add(createVariable((SingleVariableDeclaration) o));
        }
        t.closeParenToken = firstTokenAfter(
          e.parameters().isEmpty() ? e.getName() : (ASTNode) e.parameters().get(e.parameters().size() - 1),
          TerminalTokens.TokenNameRPAREN
        );
        if (!e.thrownExceptionTypes().isEmpty()) {
          t.throwsToken = firstTokenBefore((Type) e.thrownExceptionTypes().get(0), TerminalTokens.TokenNamethrows);
          for (Object o : e.thrownExceptionTypes()) {
            t.throwsClauses.elements.add(convertType((Type) o));
          }
        }
        t.block = convertBlock(e.getBody());
        return t;
      }
      case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
        AnnotationTypeMemberDeclaration e = (AnnotationTypeMemberDeclaration) node;
        EMethod t = new EMethod();
        t.ast = e.getAST();
        t.binding = e.resolveBinding();
        for (Object o : e.modifiers()) {
          t.modifiers.elements.add(
            (ModifierTree) convert((ASTNode) o)
          );
        }
        t.returnType = convertType(e.getType());
        t.simpleName = convertSimpleName(e.getName());
        t.closeParenToken = firstTokenAfter(e.getName(), TerminalTokens.TokenNameRPAREN);
        return t;
      }
      case ASTNode.INITIALIZER: {
        Initializer e = (Initializer) node;
        EBlock t = convertBlock(e.getBody());
        if (Modifier.isStatic(e.getModifiers())) {
          EStaticInitializer t2 = new EStaticInitializer();
          t2.staticKeyword = createSyntaxToken(e, "static");
          t2.body.addAll(t.body);
          t2.openBraceToken = t.openBraceToken();
          t2.closeBraceToken = t.closeBraceToken();
          return t2;
        }
        return t;
      }

      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private void processBodyDeclaration(BodyDeclaration e, List<Tree> members) {
    if (e.getNodeType() == ASTNode.FIELD_DECLARATION) {
      FieldDeclaration fieldDeclaration = (FieldDeclaration) e;
      // modifiers are shared
      EModifiers modifiers = new EModifiers();
      for (Object o : fieldDeclaration.modifiers()) {
        modifiers.elements.add(
          (ModifierTree) convert((ASTNode) o)
        );
      }
      for (Object o : fieldDeclaration.fragments()) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
        EVariable t = new EVariable();
        t.ast = fragment.getAST();
        t.binding = fragment.resolveBinding();
        t.modifiers = modifiers;
        // TODO type is also shared
        t.type = convertType(fieldDeclaration.getType());
        t.simpleName = convertSimpleName(fragment.getName());
        if (fragment.getInitializer() != null) {
          t.equalToken = firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL);
          t.initializer = convertExpression(fragment.getInitializer());
        }
        members.add(t);
      }
    } else {
      members.add(convert(e));
    }
  }

  private EVariable createVariable(SingleVariableDeclaration e) {
    EVariable t = new EVariable();
    t.ast = e.getAST();
    t.binding = e.resolveBinding();
    for (Object o : e.modifiers()) {
      t.modifiers.elements.add(
        (ModifierTree) convert((ASTNode) o)
      );
    }

    t.type = convertType(e.getType());
    if (e.isVarargs()) {
      EArrayType a = new EArrayType();
      a.ast = e.getAST();
      a.binding = e.resolveBinding() == null ? null : e.resolveBinding().getType();
      a.type = t.type;
      a.ellipsisToken = firstTokenAfter(e.getType(), TerminalTokens.TokenNameELLIPSIS);
      t.type = a;
    }

    t.simpleName = convertSimpleName(e.getName());
    if (e.getInitializer() != null) {
      t.equalToken = firstTokenAfter(e.getName(), TerminalTokens.TokenNameEQUAL);
      t.initializer = convertExpression(e.getInitializer());
    }
    return t;
  }

  private EVariable createVariable(VariableDeclarationStatement declaration, VariableDeclarationFragment fragment) {
    EVariable t = new EVariable();
    t.ast = fragment.getAST();
    t.binding = fragment.resolveBinding();
    for (Object o : declaration.modifiers()) {
      t.modifiers.elements.add(
        (ModifierTree) convert((ASTNode) o)
      );
    }
    t.type = convertType(declaration.getType());
    t.simpleName = convertSimpleName(fragment.getName());
    if (fragment.getInitializer() != null) {
      t.equalToken = firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL);
      t.initializer = convertExpression(fragment.getInitializer());
    }
    return t;
  }

  private TypeTree convertType(Type node) {
    if (node == null) {
      return null;
    }
    switch (node.getNodeType()) {
      case ASTNode.PRIMITIVE_TYPE: {
        PrimitiveType e = (PrimitiveType) node;
        EPrimitiveType t = new EPrimitiveType();
        t.ast = e.getAST();
        t.binding = e.resolveBinding();
        t.keyword = createSyntaxToken(node, e.getPrimitiveTypeCode().toString());
        return t;
      }
      case ASTNode.SIMPLE_TYPE: {
        SimpleType e = (SimpleType) node;
        return (TypeTree) convertExpression(e.getName());
      }
      case ASTNode.UNION_TYPE: {
        UnionType e = (UnionType) node;
        EUnionType t = new EUnionType();
        t.ast = e.getAST();
        t.binding = e.resolveBinding();
        for (Object o : e.types()) {
          t.typeAlternatives.elements.add(convertType((Type) o));
        }
        return t;
      }
      case ASTNode.ARRAY_TYPE: {
        // TODO vararg
        ArrayType e = (ArrayType) node;
        EArrayType t = new EArrayType();
        t.ast = e.getAST();
        t.binding = e.resolveBinding();
        t.type = convertType(e.getElementType());
        t.openBracketToken = firstTokenAfter(e.getElementType(), TerminalTokens.TokenNameLBRACKET);
        // TODO e.dimensions()
        return t;
      }
      case ASTNode.PARAMETERIZED_TYPE: {
        ParameterizedType e = (ParameterizedType) node;
        EParameterizedType t = new EParameterizedType();
        t.ast = e.getAST();
        t.binding = e.resolveBinding();
        t.type = convertType(e.getType());
        t.typeArguments = new EClassInstanceCreation.ETypeArguments();
        // TODO e.typeArguments()
        return t;
      }
      case ASTNode.QUALIFIED_TYPE: {
//        QualifiedType e = (QualifiedType) node;
      }
      case ASTNode.WILDCARD_TYPE: {
//        WildcardType e = (WildcardType) node;
      }
      case ASTNode.NAME_QUALIFIED_TYPE: {
//        NameQualifiedType e = (NameQualifiedType) node;
        throw new UnexpectedAccessException();
      }
      case ASTNode.INTERSECTION_TYPE: {
        IntersectionType e = (IntersectionType) node;
        // FIXME
        EIdentifier t = new EIdentifier();
        t.ast = e.getAST();
        t.typeBinding = e.resolveBinding();
        t.identifierToken = createSyntaxToken(e, "");
        return t;
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private StatementTree convertStatement(Statement node) {
    if (node == null) {
      return null;
    }
    switch (node.getNodeType()) {
      case ASTNode.BLOCK:
        return convertBlock((Block) node);
      case ASTNode.EMPTY_STATEMENT: {
        EmptyStatement e = (EmptyStatement) node;
        EEmptyStatement t = new EEmptyStatement();
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.RETURN_STATEMENT: {
        ReturnStatement e = (ReturnStatement) node;
        EReturnStatement t = new EReturnStatement();
        t.returnKeyword = createSyntaxToken(e, "return");
        t.expression = convertExpression(e.getExpression());
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.FOR_STATEMENT: {
        ForStatement e = (ForStatement) node;
        EForStatement t = new EForStatement();
        t.forKeyword = createSyntaxToken(e, "for");
        for (Object o : e.initializers()) {
          if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == ((Expression) o).getNodeType()) {
            VariableDeclarationExpression e2 = (VariableDeclarationExpression) o;
            // TODO only one?
            VariableDeclarationFragment e3 = (VariableDeclarationFragment) e2.fragments().get(0);
            EVariable t2 = new EVariable();
            t2.ast = e3.getAST();
            t2.binding = e3.resolveBinding();
            t2.type = convertType(e2.getType());
            t2.simpleName = convertSimpleName(e3.getName());
            t2.initializer = convertExpression(e3.getInitializer());
          } else {
            EExpressionStatement t2 = new EExpressionStatement();
            t2.expression = convertExpression((Expression) o);
            t.initializer.elements.add(t2);
          }
        }
        t.condition = convertExpression(e.getExpression());
        for (Object o : e.updaters()) {
          EExpressionStatement t2 = new EExpressionStatement();
          t2.expression = convertExpression((Expression) o);
          t.update.elements.add(t2);
        }
        t.closeParenToken = firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN);
        t.statement = convertStatement(e.getBody());
        return t;
      }
      case ASTNode.WHILE_STATEMENT: {
        WhileStatement e = (WhileStatement) node;
        EWhileStatement t = new EWhileStatement();
        t.whileKeyword = createSyntaxToken(e, "while");
        t.condition = convertExpression(e.getExpression());
        t.closeParenToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN);
        t.statement = convertStatement(e.getBody());
        return t;
      }
      case ASTNode.IF_STATEMENT: {
        IfStatement e = (IfStatement) node;
        EIfStatement t = new EIfStatement();
        t.ifKeyword = createSyntaxToken(e, "if");
        t.condition = convertExpression(e.getExpression());
        t.closeParenToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN);
        t.thenStatement = convertStatement(e.getThenStatement());
        if (e.getElseStatement() != null) {
          t.elseKeyword = firstTokenAfter(e.getThenStatement(), TerminalTokens.TokenNameelse);
          t.elseStatement = convertStatement(e.getElseStatement());
        }
        return t;
      }
      case ASTNode.BREAK_STATEMENT: {
        BreakStatement e = (BreakStatement) node;
        EBreakStatement t = new EBreakStatement();
        t.breakKeyword = createSyntaxToken(node, "break");
        t.label = convertSimpleName(e.getLabel());
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.DO_STATEMENT: {
        DoStatement e = (DoStatement) node;
        EDoStatement t = new EDoStatement();
        t.doKeyword = createSyntaxToken(e, "do");
        t.statement = convertStatement(e.getBody());
        t.whileKeyword = firstTokenAfter(e.getBody(), TerminalTokens.TokenNamewhile);
        t.condition = convertExpression(e.getExpression());
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.ASSERT_STATEMENT: {
        AssertStatement e = (AssertStatement) node;
        EAssertStatement t = new EAssertStatement();
        t.assertKeyword = createSyntaxToken(e, "assert");
        t.condition = convertExpression(e.getExpression());
        t.detail = convertExpression(e.getMessage());
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.SWITCH_STATEMENT: {
        SwitchStatement e = (SwitchStatement) node;
        ESwitchStatement t = new ESwitchStatement();

        t.switchExpression.switchKeyword = createSyntaxToken(e, "switch");
        t.switchExpression.expression = convertExpression(e.getExpression());
        t.switchExpression.closeParenToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN);
        t.switchExpression.openBraceToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameLBRACE);

        ESwitchExpression.ECaseGroup group = null;
        boolean nextCaseStartsGroup = true;
        for (Object o : e.statements()) {
          if (o instanceof SwitchCase) {
            if (nextCaseStartsGroup) {
              group = new ESwitchExpression.ECaseGroup();
              t.switchExpression.groups.add(group);
            }
            nextCaseStartsGroup = false;

            SwitchCase c = (SwitchCase) o;
            ESwitchExpression.ECaseLabel l = new ESwitchExpression.ECaseLabel();
            l.caseOrDefaultKeyword = createSyntaxToken(c, c.isDefault() ? "default" : "case");
            l.expression = convertExpression(c.getExpression());
            l.colonToken = lastTokenIn(c, TerminalTokens.TokenNameCOLON);
            group.labels.add(l);
          } else {
            nextCaseStartsGroup = true;
            group.body.add(convertStatement((Statement) o));
          }
        }

        t.switchExpression.closeBraceToken = lastTokenIn(e, TerminalTokens.TokenNameRBRACE);

        return t;
      }
      case ASTNode.SYNCHRONIZED_STATEMENT: {
        SynchronizedStatement e = (SynchronizedStatement) node;
        ESynchronizedStatement t = new ESynchronizedStatement();
        t.synchronizedKeyword = createSyntaxToken(e, "synchronized");
        t.expression = convertExpression(e.getExpression());
        t.closeParenToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN);
        t.block = convertBlock(e.getBody());
        return t;
      }
      case ASTNode.EXPRESSION_STATEMENT: {
        ExpressionStatement e = (ExpressionStatement) node;
        EExpressionStatement t = new EExpressionStatement();
        t.expression = convertExpression(e.getExpression());
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.CONTINUE_STATEMENT: {
        ContinueStatement e = (ContinueStatement) node;
        EContinueStatement t = new EContinueStatement();
        t.continueKeyword = createSyntaxToken(node, "continue");
        t.label = convertSimpleName(e.getLabel());
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.LABELED_STATEMENT: {
        LabeledStatement e = (LabeledStatement) node;
        ELabeledStatement t = new ELabeledStatement();
        t.label = convertSimpleName(e.getLabel());
        t.colonToken = firstTokenAfter(e.getLabel(), TerminalTokens.TokenNameCOLON);
        t.statement = convertStatement(e.getBody());
        return t;
      }
      case ASTNode.ENHANCED_FOR_STATEMENT: {
        EnhancedForStatement e = (EnhancedForStatement) node;
        EEnhancedForStatement t = new EEnhancedForStatement();
        t.forKeyword = createSyntaxToken(e, "for");
        t.variable = createVariable(e.getParameter());
        t.expression = convertExpression(e.getExpression());
        t.closeParenToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN);
        t.statement = convertStatement(e.getBody());
        return t;
      }
      case ASTNode.THROW_STATEMENT: {
        ThrowStatement e = (ThrowStatement) node;
        EThrowStatement t = new EThrowStatement();
        t.throwKeyword = createSyntaxToken(e, "throw");
        t.expression = convertExpression(e.getExpression());
        t.semicolonToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.TRY_STATEMENT: {
        TryStatement e = (TryStatement) node;
        ETryStatement t = new ETryStatement();
        t.tryKeyword = createSyntaxToken(e, "try");

        for (Object o : e.resources()) {
          if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == ((Expression) o).getNodeType()) {
            VariableDeclarationExpression e2 = (VariableDeclarationExpression) o;
            // TODO only one?
            VariableDeclarationFragment e3 = (VariableDeclarationFragment) e2.fragments().get(0);
            EVariable t2 = new EVariable();
            t2.ast = e3.getAST();
            t2.binding = e3.resolveBinding();
            t2.type = convertType(e2.getType());
            t2.simpleName = convertSimpleName(e3.getName());
            t2.initializer = convertExpression(e3.getInitializer());
            t.resources.elements.add(t2);
          } else {
            t.resources.elements.add(
              convertExpression((Expression) o)
            );
          }
        }
        if (!e.resources().isEmpty()) {
          t.closeParenToken = firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN);
        }

        t.body = convertBlock(e.getBody());

        for (Object o : e.catchClauses()) {
          CatchClause e2 = (CatchClause) o;
          ECatchClause t2 = new ECatchClause();
          t2.catchKeyword = createSyntaxToken(e2, "catch");
          t2.parameter = createVariable(e2.getException());
          t2.closeParenToken = firstTokenAfter(e2.getException(), TerminalTokens.TokenNameRPAREN);
          t2.block = convertBlock(e2.getBody());
          t.catches.add(t2);
        }

        if (e.getFinally() != null) {
          t.finallyKeyword = firstTokenBefore(e.getFinally(), TerminalTokens.TokenNamefinally);
          t.finallyBlock = convertBlock(e.getFinally());
        }

        return t;
      }
      case ASTNode.VARIABLE_DECLARATION_STATEMENT: {
        VariableDeclarationStatement e = (VariableDeclarationStatement) node;
        // TODO all fragments
        EVariable t = createVariable(e, (VariableDeclarationFragment) e.fragments().get(0));
        return t;
      }
      case ASTNode.TYPE_DECLARATION_STATEMENT: {
        TypeDeclarationStatement e = (TypeDeclarationStatement) node;
        return (EClass) convert(
          e.getDeclaration()
        );
      }
      case ASTNode.CONSTRUCTOR_INVOCATION: {
        // TODO unused ?
        ConstructorInvocation e = (ConstructorInvocation) node;
        EIdentifier i = new EIdentifier();
        i.identifierToken = createSyntaxToken(e, "this");
        EMethodInvocation mi = new EMethodInvocation();
        mi.ast = e.getAST();
        mi.binding = e.resolveConstructorBinding();
        mi.methodSelect = i;
        for (Object o : e.arguments()) {
          mi.arguments.elements.add(convertExpression((Expression) o));
        }
        EExpressionStatement t = new EExpressionStatement();
        t.expression = mi;
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION: {
        SuperConstructorInvocation e = (SuperConstructorInvocation) node;
        EIdentifier i = new EIdentifier();
        i.identifierToken = createSyntaxToken(e, "super");
        EMethodInvocation mi = new EMethodInvocation();
        mi.ast = e.getAST();
        mi.binding = e.resolveConstructorBinding();
        mi.methodSelect = i;
        for (Object o : e.arguments()) {
          mi.arguments.elements.add(convertExpression((Expression) o));
        }
        EExpressionStatement t = new EExpressionStatement();
        t.expression = mi;
        t.semicolonToken = lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON);
        return t;
      }

      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private SyntaxToken firstTokenBefore(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstTokenBefore(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private SyntaxToken firstTokenAfter(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstTokenAfter(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private SyntaxToken lastTokenIn(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.lastTokenIn(e, tokenType));
  }

  private SyntaxToken createSyntaxToken(Token token) {
    return new ESyntaxToken(
      compilationUnit.getLineNumber(token.originalStart),
      compilationUnit.getColumnNumber(token.originalStart),
      // Inefficient in terms of memory consumption:
      token.toString(tokenManager.getSource())
    );
  }

  private SyntaxToken createSyntaxToken(ASTNode node, String text) {
    return new ESyntaxToken(
      compilationUnit.getLineNumber(node.getStartPosition()),
      compilationUnit.getColumnNumber(node.getStartPosition()),
      text
    );
  }

  private EIdentifier convertSimpleName(SimpleName e) {
    if (e == null) {
      return null;
    }
    EIdentifier t = new EIdentifier();
    t.ast = e.getAST();
    t.typeBinding = e.resolveTypeBinding();
    t.binding = e.resolveBinding();
    t.identifierToken = createSyntaxToken(e, e.getIdentifier());
    return t;
  }

  private ExpressionTree convertExpression(Expression node) {
    if (node == null) {
      return null;
    }
    EExpression t = createExpression(node);
    t.ast = node.getAST();
    t.typeBinding = node.resolveTypeBinding();
    return t;
  }

  private EExpression createExpression(Expression node) {
    if (node == null) {
      return null;
    }
    switch (node.getNodeType()) {
      case ASTNode.SIMPLE_NAME:
        return convertSimpleName((SimpleName) node);
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        EMemberSelect t = new EMemberSelect();
        t.lhs = convertExpression(e.getQualifier());
        t.rhs = convertSimpleName(e.getName());
        return t;
      }
      case ASTNode.FIELD_ACCESS: {
        FieldAccess e = (FieldAccess) node;
        EMemberSelect t = new EMemberSelect();
        t.lhs = convertExpression(e.getExpression());
        t.rhs = convertSimpleName(e.getName());
        return t;
      }
      case ASTNode.SUPER_FIELD_ACCESS: {
        SuperFieldAccess e = (SuperFieldAccess) node;
        EMemberSelect t = new EMemberSelect();
        // FIXME
        // qualifier.super.name
        // super.name
        t.lhs = convertExpression(e.getQualifier());
        t.rhs = convertSimpleName(e.getName());
        return t;
      }
      case ASTNode.THIS_EXPRESSION: {
        ThisExpression e = (ThisExpression) node;
        if (e.getQualifier() == null) {
          EIdentifier t = new EIdentifier();
          t.identifierToken = createSyntaxToken(node, "this");
          return t;
        } else {
          EMemberSelect t = new EMemberSelect();
          t.lhs = convertExpression(e.getQualifier());
          t.rhs = new EIdentifier(); // TODO bindings ?
          t.rhs.ast = e.getAST();
          t.rhs.typeBinding = e.resolveTypeBinding();
          t.rhs.identifierToken = createSyntaxToken(node, "this");
          return t;
        }
      }
      case ASTNode.TYPE_LITERAL: {
        // FIXME void.class
        TypeLiteral e = (TypeLiteral) node;
        EIdentifier t = new EIdentifier();
        t.identifierToken = createSyntaxToken(node, "class");
        return t;
      }
      case ASTNode.ARRAY_ACCESS: {
        ArrayAccess e = (ArrayAccess) node;
        EArrayAccess t = new EArrayAccess();
        t.expression = convertExpression(e.getArray());
        t.dimension.expression = convertExpression(e.getIndex());
        t.dimension.closeBracketToken = firstTokenAfter(e.getIndex(), TerminalTokens.TokenNameRBRACKET);
        return t;
      }
      case ASTNode.ARRAY_CREATION: {
        ArrayCreation e = (ArrayCreation) node;
        EArrayCreation t = new EArrayCreation();
        t.newKeyword = createSyntaxToken(e, "new");
        t.type = convertType(e.getType());
        // TODO
        // e.dimensions()
        // FIXME
        t.openBraceToken = createSyntaxToken(e, "{");
        if (e.getInitializer() != null) {
          for (Object o : e.getInitializer().expressions()) {
            t.initializers.elements.add(convertExpression((Expression) o));
          }
        }
        return t;
      }
      case ASTNode.ARRAY_INITIALIZER: {
        ArrayInitializer e = (ArrayInitializer) node;
        EArrayCreation t = new EArrayCreation();
        t.openBraceToken = createSyntaxToken(e, "{");
        for (Object o : e.expressions()) {
          t.initializers.elements.add(convertExpression((Expression) o));
        }
        return t;
      }
      case ASTNode.ASSIGNMENT: {
        Assignment e = (Assignment) node;
        EAssignment t = new EAssignment();
        Op op = operators.get(e.getOperator());
        t.kind = op.kind;
        t.variable = convertExpression(e.getLeftHandSide());
        t.operatorToken = firstTokenAfter(e.getLeftHandSide(), op.tokenType);
        t.expression = convertExpression(e.getRightHandSide());
        return t;
      }
      case ASTNode.CAST_EXPRESSION: {
        CastExpression e = (CastExpression) node;
        ECastExpression t = new ECastExpression();
        t.openParenToken = createSyntaxToken(e, "(");
        t.type = convertType(e.getType());
        t.expression = convertExpression(e.getExpression());
        return t;
      }
      case ASTNode.CLASS_INSTANCE_CREATION: {
        ClassInstanceCreation e = (ClassInstanceCreation) node;
        EClassInstanceCreation t = new EClassInstanceCreation();
        t.ast = e.getAST();
        t.binding = e.resolveConstructorBinding();
        // TODO position
        t.newKeyword = createSyntaxToken(e, "new");
        t.identifier = convertType(e.getType());
        t.arguments.openParenToken = firstTokenAfter(e.getType(), TerminalTokens.TokenNameLPAREN);
        for (Object o : e.arguments()) {
          t.arguments.elements.add(convertExpression((Expression) o));
        }
        t.arguments.closeParenToken = firstTokenAfter(e.arguments().isEmpty() ? e.getType() : (ASTNode) e.arguments().get(0), TerminalTokens.TokenNameRPAREN);

        if (e.getAnonymousClassDeclaration() != null) {
          t.classBody = new EClass();
          t.classBody.ast = e.getAnonymousClassDeclaration().getAST();
          t.classBody.binding = e.getAnonymousClassDeclaration().resolveBinding();
          // TODO always class?
          t.classBody.kind = Tree.Kind.CLASS;
          t.classBody.openBraceToken = createSyntaxToken(e.getAnonymousClassDeclaration(), "{");
          for (Object o : e.getAnonymousClassDeclaration().bodyDeclarations()) {
            processBodyDeclaration((BodyDeclaration) o, t.classBody.members);
          }
          t.classBody.closeBraceToken = lastTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameRBRACE);
        }

        return t;
      }
      case ASTNode.CONDITIONAL_EXPRESSION: {
        ConditionalExpression e = (ConditionalExpression) node;
        EConditionalExpression t = new EConditionalExpression();
        t.condition = convertExpression(e.getExpression());
        t.questionToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameQUESTION);
        t.trueExpression = convertExpression(e.getThenExpression());
        t.colonToken = firstTokenAfter(e.getThenExpression(), TerminalTokens.TokenNameCOLON);
        t.falseExpression = convertExpression(e.getElseExpression());
        return t;
      }
      case ASTNode.INFIX_EXPRESSION: {
        InfixExpression e = (InfixExpression) node;
        EBinaryExpression t = new EBinaryExpression();
        t.ast = e.getAST();
        t.typeBinding = e.resolveTypeBinding();
        Op op = operators.get(e.getOperator());
        t.kind = op.kind;
        t.leftOperand = convertExpression(e.getLeftOperand());
        t.operatorToken = firstTokenAfter(e.getLeftOperand(), op.tokenType);
        t.rightOperand = convertExpression(e.getRightOperand());
        for (Object o : e.extendedOperands()) {
          Expression e2 = (Expression) o;
          EBinaryExpression t2 = new EBinaryExpression();
          t2.ast = e2.getAST();
          t2.typeBinding = e2.resolveTypeBinding();
          t2.kind = op.kind;
          t2.leftOperand = t;
          t2.operatorToken = firstTokenBefore(e2, op.tokenType);
          t2.rightOperand = convertExpression(e2);
          t = t2;
        }
        return t;
      }
      case ASTNode.METHOD_INVOCATION: {
        MethodInvocation e = (MethodInvocation) node;
        EMethodInvocation t = new EMethodInvocation();
        t.ast = e.getAST();
        t.binding = e.resolveMethodBinding();

        if (e.getExpression() == null) {
          t.methodSelect = convertSimpleName(e.getName());
        } else {
          EMemberSelect t2 = new EMemberSelect();
          t.methodSelect = t2; // TODO typeBinding ?
          t2.lhs = convertExpression(e.getExpression());
          t2.rhs = convertSimpleName(e.getName());
        }

        t.arguments.openParenToken = firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN);
        for (Object o : e.arguments()) {
          t.arguments.elements.add(convertExpression((Expression) o));
        }
        t.arguments.closeParenToken = lastTokenIn(e, TerminalTokens.TokenNameRPAREN);
        return t;
      }
      case ASTNode.SUPER_METHOD_INVOCATION: {
        SuperMethodInvocation e = (SuperMethodInvocation) node;
        EMethodInvocation t = new EMethodInvocation();
        t.ast = e.getAST();
        t.binding = e.resolveMethodBinding();

        if (e.getQualifier() == null) {
          t.methodSelect = convertSimpleName(e.getName());
        } else {
          EMemberSelect t2 = new EMemberSelect();
          t.methodSelect = t2; // TODO typeBinding ?
          t2.lhs = convertExpression(e.getQualifier());
          t2.rhs = convertSimpleName(e.getName());
        }

        t.arguments.openParenToken = firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN);
        for (Object o : e.arguments()) {
          t.arguments.elements.add(convertExpression((Expression) o));
        }
        t.arguments.closeParenToken = lastTokenIn(e, TerminalTokens.TokenNameRPAREN);
        return t;
      }
      case ASTNode.PARENTHESIZED_EXPRESSION: {
        ParenthesizedExpression e = (ParenthesizedExpression) node;
        EParenthesizedExpression t = new EParenthesizedExpression();
        t.openParenToken = createSyntaxToken(e, "(");
        t.expression = convertExpression(e.getExpression());
        t.closeParenToken = firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN);
        return t;
      }
      case ASTNode.POSTFIX_EXPRESSION: {
        PostfixExpression e = (PostfixExpression) node;
        EUnaryExpression.Postfix t = new EUnaryExpression.Postfix();
        Op op = operators.get(e.getOperator());
        t.kind = op.kind;
        t.expression = convertExpression(e.getOperand());
        t.operatorToken = firstTokenAfter(e.getOperand(), op.tokenType);
        return t;
      }
      case ASTNode.PREFIX_EXPRESSION: {
        PrefixExpression e = (PrefixExpression) node;
        EUnaryExpression.Prefix t = new EUnaryExpression.Prefix();
        Op op = operators.get(e.getOperator());
        t.kind = op.kind;
        t.operatorToken = createSyntaxToken(e, e.getOperand().toString());
        t.expression = convertExpression(e.getOperand());
        return t;
      }
      case ASTNode.INSTANCEOF_EXPRESSION: {
        InstanceofExpression e = (InstanceofExpression) node;
        EInstanceof t = new EInstanceof();
        t.expression = convertExpression(e.getLeftOperand());
        t.instanceofKeyword = firstTokenAfter(e.getLeftOperand(), TerminalTokens.TokenNameinstanceof);
        t.type = convertType(e.getRightOperand());
        return t;
      }
      case ASTNode.LAMBDA_EXPRESSION: {
        LambdaExpression e = (LambdaExpression) node;
        ELambdaExpression t = new ELambdaExpression();
        if (e.hasParentheses()) {
          t.openParenToken = createSyntaxToken(e, "(");
        }
        for (Object o : e.parameters()) {
          VariableDeclaration ev = (VariableDeclaration) o;
          if (ev.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
            EVariable tv = new EVariable();
            tv.ast = ev.getAST();
            tv.binding = ev.resolveBinding();
            tv.type = new EInferedType();
            tv.simpleName = convertSimpleName(ev.getName());
            t.parameters.add(tv);
          } else {
            t.parameters.add(
              createVariable((SingleVariableDeclaration) o)
            );
          }
        }
        t.arrowToken = firstTokenBefore(e.getBody(), TerminalTokens.TokenNameARROW);
        if (e.getBody().getNodeType() == ASTNode.BLOCK) {
          t.body = convertBlock((Block) e.getBody());
        } else {
          t.body = convertExpression((Expression) e.getBody());
        }
        return t;
      }
      case ASTNode.CREATION_REFERENCE: {
        CreationReference e = (CreationReference) node;
        EMethodReference t = new EMethodReference();
        t.expression = convertType(e.getType());
        t.method = new EIdentifier();
        // FIXME position, bindings
        t.method.identifierToken = createSyntaxToken(e, "new");
        return t;
      }
      case ASTNode.EXPRESSION_METHOD_REFERENCE: {
        ExpressionMethodReference e = (ExpressionMethodReference) node;
        EMethodReference t = new EMethodReference();
        t.expression = convertExpression(e.getExpression());
        t.method = convertSimpleName(e.getName());
        return t;
      }
      case ASTNode.SUPER_METHOD_REFERENCE: {
        SuperMethodReference e = (SuperMethodReference) node;
        EMethodReference t = new EMethodReference();
        t.expression = convertExpression(e.getQualifier());
        t.method = convertSimpleName(e.getName());
        return t;
      }
      case ASTNode.TYPE_METHOD_REFERENCE: {
        TypeMethodReference e = (TypeMethodReference) node;
        EMethodReference t = new EMethodReference();
        t.expression = convertType(e.getType());
        t.method = convertSimpleName(e.getName());
        return t;
      }
      case ASTNode.NULL_LITERAL: {
        ELiteral t = new ELiteral();
        t.token = createSyntaxToken(node, "null");
        t.kind = Tree.Kind.NULL_LITERAL;
        return t;
      }
      case ASTNode.NUMBER_LITERAL: {
        NumberLiteral e = (NumberLiteral) node;
        ELiteral t = new ELiteral();
        t.token = createSyntaxToken(e, e.getToken());
        t.kind = Tree.Kind.INT_LITERAL;
        return t;
      }
      case ASTNode.CHARACTER_LITERAL: {
        CharacterLiteral e = (CharacterLiteral) node;
        ELiteral t = new ELiteral();
        t.token = createSyntaxToken(e, e.getEscapedValue());
        t.kind = Tree.Kind.CHAR_LITERAL;
        return t;
      }
      case ASTNode.BOOLEAN_LITERAL: {
        BooleanLiteral e = (BooleanLiteral) node;
        ELiteral t = new ELiteral();
        t.token = createSyntaxToken(e, e.booleanValue() ? "true" : "false");
        t.kind = Tree.Kind.BOOLEAN_LITERAL;
        return t;
      }
      case ASTNode.STRING_LITERAL: {
        StringLiteral e = (StringLiteral) node;
        ELiteral t = new ELiteral();
        t.token = createSyntaxToken(e, e.getEscapedValue());
        t.kind = Tree.Kind.STRING_LITERAL;
        return t;
      }
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION: {
        Annotation e = (Annotation) node;
        EAnnotation t = new EAnnotation();
        t.ast = e.getAST();
        t.typeBinding = e.resolveTypeBinding();

        // TODO IdentifierTree implements TypeTree
        t.atToken = createSyntaxToken(e, "@");
        t.annotationType = (TypeTree) convertExpression(e.getTypeName());

        if (e.getNodeType() == ASTNode.SINGLE_MEMBER_ANNOTATION) {
          SingleMemberAnnotation e2 = (SingleMemberAnnotation) e;
          t.arguments.elements.add(convertExpression(e2.getValue()));
          t.arguments.closeParenToken = lastTokenIn(e, TerminalTokens.TokenNameRPAREN);
        } else if (e.getNodeType() == ASTNode.NORMAL_ANNOTATION) {
          NormalAnnotation e2 = (NormalAnnotation) e;
          for (Object o : e2.values()) {
            MemberValuePair p = (MemberValuePair) o;
            EAssignment t2 = new EAssignment();
            t2.variable = convertSimpleName(p.getName());
            t2.kind = Tree.Kind.ASSIGNMENT;
            t2.operatorToken = firstTokenAfter(p.getName(), TerminalTokens.TokenNameEQUAL);
            t2.expression = convertExpression(p.getValue());
            t.arguments.elements.add(t2);
          }
          t.arguments.closeParenToken = lastTokenIn(e, TerminalTokens.TokenNameRPAREN);
        }
        return t;
      }

      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private EBlock convertBlock(Block e) {
    if (e == null) {
      return null;
    }
    EBlock t = new EBlock();
    t.openBraceToken = createSyntaxToken(e, "{");
    for (Object o : e.statements()) {
      t.body.add(convertStatement((Statement) o));
    }
    t.closeBraceToken = lastTokenIn(e, TerminalTokens.TokenNameRBRACE);
    return t;
  }

  private static final Map<Object, Op> operators = new HashMap<>();

  static class Op {
    final Tree.Kind kind;

    /**
     * {@link TerminalTokens}
     */
    final int tokenType;

    Op(Tree.Kind kind, int tokenType) {
      this.kind = kind;
      this.tokenType = tokenType;
    }
  }

  static {
    operators.put(PrefixExpression.Operator.PLUS, new Op(Tree.Kind.UNARY_PLUS, TerminalTokens.TokenNamePLUS));
    operators.put(PrefixExpression.Operator.MINUS, new Op(Tree.Kind.UNARY_MINUS, TerminalTokens.TokenNameMINUS));
    operators.put(PrefixExpression.Operator.NOT, new Op(Tree.Kind.LOGICAL_COMPLEMENT, TerminalTokens.TokenNameNOT));
    operators.put(PrefixExpression.Operator.COMPLEMENT, new Op(Tree.Kind.BITWISE_COMPLEMENT, TerminalTokens.TokenNameTWIDDLE));
    operators.put(PrefixExpression.Operator.DECREMENT, new Op(Tree.Kind.PREFIX_DECREMENT, TerminalTokens.TokenNameMINUS_MINUS));
    operators.put(PrefixExpression.Operator.INCREMENT, new Op(Tree.Kind.POSTFIX_DECREMENT, TerminalTokens.TokenNamePLUS_PLUS));

    operators.put(PostfixExpression.Operator.DECREMENT, new Op(Tree.Kind.POSTFIX_DECREMENT, TerminalTokens.TokenNameMINUS_MINUS));
    operators.put(PostfixExpression.Operator.INCREMENT, new Op(Tree.Kind.POSTFIX_INCREMENT, TerminalTokens.TokenNamePLUS_PLUS));

    operators.put(InfixExpression.Operator.TIMES, new Op(Tree.Kind.MULTIPLY, TerminalTokens.TokenNameMULTIPLY));
    operators.put(InfixExpression.Operator.DIVIDE, new Op(Tree.Kind.DIVIDE, TerminalTokens.TokenNameDIVIDE));
    operators.put(InfixExpression.Operator.REMAINDER, new Op(Tree.Kind.REMAINDER, TerminalTokens.TokenNameREMAINDER));
    operators.put(InfixExpression.Operator.PLUS, new Op(Tree.Kind.PLUS, TerminalTokens.TokenNamePLUS));
    operators.put(InfixExpression.Operator.MINUS, new Op(Tree.Kind.MINUS, TerminalTokens.TokenNameMINUS));
    operators.put(InfixExpression.Operator.LEFT_SHIFT, new Op(Tree.Kind.LEFT_SHIFT, TerminalTokens.TokenNameLEFT_SHIFT));
    operators.put(InfixExpression.Operator.RIGHT_SHIFT_SIGNED, new Op(Tree.Kind.RIGHT_SHIFT, TerminalTokens.TokenNameRIGHT_SHIFT));
    operators.put(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, new Op(Tree.Kind.UNSIGNED_RIGHT_SHIFT, TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT));
    operators.put(InfixExpression.Operator.LESS, new Op(Tree.Kind.LESS_THAN, TerminalTokens.TokenNameLESS));
    operators.put(InfixExpression.Operator.GREATER, new Op(Tree.Kind.GREATER_THAN, TerminalTokens.TokenNameGREATER));
    operators.put(InfixExpression.Operator.LESS_EQUALS, new Op(Tree.Kind.LESS_THAN_OR_EQUAL_TO, TerminalTokens.TokenNameLESS_EQUAL));
    operators.put(InfixExpression.Operator.GREATER_EQUALS, new Op(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, TerminalTokens.TokenNameGREATER_EQUAL));
    operators.put(InfixExpression.Operator.EQUALS, new Op(Tree.Kind.EQUAL_TO, TerminalTokens.TokenNameEQUAL_EQUAL));
    operators.put(InfixExpression.Operator.NOT_EQUALS, new Op(Tree.Kind.NOT_EQUAL_TO, TerminalTokens.TokenNameNOT_EQUAL));
    operators.put(InfixExpression.Operator.XOR, new Op(Tree.Kind.XOR, TerminalTokens.TokenNameXOR));
    operators.put(InfixExpression.Operator.OR, new Op(Tree.Kind.OR, TerminalTokens.TokenNameOR));
    operators.put(InfixExpression.Operator.AND, new Op(Tree.Kind.AND, TerminalTokens.TokenNameAND));
    operators.put(InfixExpression.Operator.CONDITIONAL_OR, new Op(Tree.Kind.CONDITIONAL_OR, TerminalTokens.TokenNameOR_OR));
    operators.put(InfixExpression.Operator.CONDITIONAL_AND, new Op(Tree.Kind.CONDITIONAL_AND, TerminalTokens.TokenNameAND_AND));

    operators.put(Assignment.Operator.ASSIGN, new Op(Tree.Kind.ASSIGNMENT, TerminalTokens.TokenNameEQUAL));
    operators.put(Assignment.Operator.PLUS_ASSIGN, new Op(Tree.Kind.PLUS_ASSIGNMENT, TerminalTokens.TokenNamePLUS_EQUAL));
    operators.put(Assignment.Operator.MINUS_ASSIGN, new Op(Tree.Kind.MINUS_ASSIGNMENT, TerminalTokens.TokenNameMINUS_EQUAL));
    operators.put(Assignment.Operator.TIMES_ASSIGN, new Op(Tree.Kind.MULTIPLY_ASSIGNMENT, TerminalTokens.TokenNameMULTIPLY_EQUAL));
    operators.put(Assignment.Operator.DIVIDE_ASSIGN, new Op(Tree.Kind.DIVIDE_ASSIGNMENT, TerminalTokens.TokenNameDIVIDE_EQUAL));
    operators.put(Assignment.Operator.BIT_AND_ASSIGN, new Op(Tree.Kind.AND_ASSIGNMENT, TerminalTokens.TokenNameAND_EQUAL));
    operators.put(Assignment.Operator.BIT_OR_ASSIGN, new Op(Tree.Kind.OR_ASSIGNMENT, TerminalTokens.TokenNameOR_EQUAL));
    operators.put(Assignment.Operator.BIT_XOR_ASSIGN, new Op(Tree.Kind.XOR_ASSIGNMENT, TerminalTokens.TokenNameXOR_EQUAL));
    operators.put(Assignment.Operator.REMAINDER_ASSIGN, new Op(Tree.Kind.REMAINDER_ASSIGNMENT, TerminalTokens.TokenNameREMAINDER_EQUAL));
    operators.put(Assignment.Operator.LEFT_SHIFT_ASSIGN, new Op(Tree.Kind.LEFT_SHIFT_ASSIGNMENT, TerminalTokens.TokenNameLEFT_SHIFT_EQUAL));
    operators.put(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN, new Op(Tree.Kind.RIGHT_SHIFT_ASSIGNMENT, TerminalTokens.TokenNameRIGHT_SHIFT_EQUAL));
    operators.put(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN, new Op(Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT, TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL));
  }

}
