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
package org.sonar.java.model;

import com.sonar.sslr.api.RecognitionException;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExportsDirective;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
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
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.ModuleModifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.OpensDirective;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.RequiresDirective;
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
import org.eclipse.jdt.core.dom.SwitchExpression;
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
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jdt.internal.formatter.Token;
import org.eclipse.jdt.internal.formatter.TokenManager;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.BlockStatementListTreeImpl;
import org.sonar.java.ast.parser.BoundListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementExpressionListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.ast.parser.TypeUnionListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
import org.sonar.java.model.declaration.ModuleNameListTreeImpl;
import org.sonar.java.model.declaration.OpensDirectiveTreeImpl;
import org.sonar.java.model.declaration.ProvidesDirectiveTreeImpl;
import org.sonar.java.model.declaration.RequiresDirectiveTreeImpl;
import org.sonar.java.model.declaration.UsesDirectiveTreeImpl;
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
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class JParser {

  /**
   * @param unitName see {@link ASTParser#setUnitName(String)}
   * @throws RecognitionException in case of syntax errors
   */
  public static CompilationUnitTree parse(String version, String unitName, String source, List<File> classpath) {
    ASTParser astParser = ASTParser.newParser(AST.JLS12);
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_SOURCE, version);
    options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    astParser.setCompilerOptions(options);

    astParser.setEnvironment(
      classpath.stream().map(File::getAbsolutePath).toArray(String[]::new),
      new String[]{},
      new String[]{},
      true
    );
    astParser.setUnitName(unitName);

    astParser.setResolveBindings(false);
    astParser.setBindingsRecovery(true);

    char[] sourceChars = source.toCharArray();
    astParser.setSource(sourceChars);

    CompilationUnit astNode = (CompilationUnit) astParser.createAST(null);
    for (IProblem problem : astNode.getProblems()) {
      if (!problem.isError()) {
        continue;
      }
      final int line = problem.getSourceLineNumber();
      final int column = astNode.getColumnNumber(problem.getSourceStart());
      throw new RecognitionException(line, "Parse error at line " + line + " column " + column + ": " + problem.getMessage());
    }

    JParser converter = new JParser();
    converter.compilationUnit = astNode;
    converter.tokenManager = new TokenManager(lex(version, unitName, sourceChars), source, new DefaultCodeFormatterOptions(new HashMap<>()));

    CompilationUnitTree tree = converter.convertCompilationUnit(astNode);
    setParents(tree);
    return tree;
  }

  private static void setParents(Tree node) {
    Iterator<Tree> childrenIterator = iteratorFor(node);
    while (childrenIterator.hasNext()) {
      Tree child = childrenIterator.next();
      ((JavaTree) child).setParent(node);
      setParents(child);
    }
  }

  private static Iterator<Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TOKEN) {
      // getChildren throws exception in this case
      return Collections.emptyIterator();
    }
    return ((JavaTree) node).getChildren().iterator();
  }

  private static List<Token> lex(String version, String unitName, char[] sourceChars) {
    List<Token> tokens = new ArrayList<>();
    Scanner scanner = new Scanner(
      true,
      false,
      false,
      CompilerOptions.versionToJdkLevel(version),
      null,
      null,
      false
    );
    scanner.fakeInModule = "module-info.java".equals(unitName);
    scanner.setSource(sourceChars);
    while (true) {
      try {
        int tokenType = scanner.getNextToken();
        Token token = Token.fromCurrent(scanner, tokenType);
        tokens.add(token);
        if (tokenType == TerminalTokens.TokenNameEOF) {
          break;
        }
      } catch (InvalidInputException e) {
        throw new IllegalStateException(e);
      }
    }
    return tokens;
  }

  private CompilationUnit compilationUnit;

  private TokenManager tokenManager;

  private int firstTokenIndexAfter(ASTNode e) {
    int index = tokenManager.firstIndexAfter(e, ANY_TOKEN);
    while (tokenManager.get(index).isComment()) {
      index++;
    }
    return index;
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private int nextTokenIndex(int tokenIndex, int tokenType) {
    assert tokenType != ANY_TOKEN;
    do {
      tokenIndex += 1;
    } while (tokenManager.get(tokenIndex).tokenType != tokenType);
    return tokenIndex;
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken firstTokenBefore(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstIndexBefore(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken firstTokenAfter(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstIndexAfter(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken firstTokenIn(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstIndexIn(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken lastTokenIn(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.lastIndexIn(e, tokenType));
  }

  private InternalSyntaxToken createSyntaxToken(int tokenIndex) {
    Token t = tokenManager.get(tokenIndex);
    if (t.tokenType == TerminalTokens.TokenNameEOF) {
      if (t.originalStart == 0) {
        return new InternalSyntaxToken(1, 0, "", collectComments(tokenIndex), 0, 0, true);
      }
      final int position = t.originalStart - 1;
      final char c = tokenManager.getSource().charAt(position);
      int line = compilationUnit.getLineNumber(position);
      int column = compilationUnit.getColumnNumber(position);
      if (c == '\n' || c == '\r') {
        line++;
        column = 0;
      } else {
        column++;
      }
      return new InternalSyntaxToken(line, column, "", collectComments(tokenIndex), 0, 0, true);
    }
    return new InternalSyntaxToken(
      compilationUnit.getLineNumber(t.originalStart),
      compilationUnit.getColumnNumber(t.originalStart),
      t.toString(tokenManager.getSource()),
      collectComments(tokenIndex),
      0, 0, false
    );
  }

  private InternalSyntaxToken createSpecialToken(int tokenIndex) {
    Token t = tokenManager.get(tokenIndex);
    List<SyntaxTrivia> comments = t.tokenType == TerminalTokens.TokenNameGREATER
      ? collectComments(tokenIndex)
      : Collections.emptyList();
    return new InternalSyntaxToken(
      compilationUnit.getLineNumber(t.originalEnd),
      compilationUnit.getColumnNumber(t.originalEnd),
      ">",
      comments,
      0, 0, false
    );
  }

  private List<SyntaxTrivia> collectComments(int tokenIndex) {
    int commentIndex = tokenIndex;
    while (commentIndex > 0 && tokenManager.get(commentIndex - 1).isComment()) {
      commentIndex--;
    }
    List<SyntaxTrivia> comments = new ArrayList<>();
    for (int i = commentIndex; i < tokenIndex; i++) {
      Token t = tokenManager.get(i);
      comments.add(new InternalSyntaxTrivia(
        t.toString(tokenManager.getSource()),
        compilationUnit.getLineNumber(t.originalStart),
        compilationUnit.getColumnNumber(t.originalStart)
      ));
    }
    return comments;
  }

  private void addEmptyDeclarationsToList(int tokenIndex, List list) {
    while (true) {
      Token token;
      do {
        tokenIndex++;
        token = tokenManager.get(tokenIndex);
      } while (token.isComment());
      if (token.tokenType == TerminalTokens.TokenNameSEMICOLON) {
        list.add(
          new EmptyStatementTreeImpl(createSyntaxToken(tokenIndex))
        );
      } else {
        break;
      }
    }
  }

  private CompilationUnitTree convertCompilationUnit(CompilationUnit e) {
    PackageDeclarationTree packageDeclaration = null;
    if (e.getPackage() != null) {
      packageDeclaration = new JavaTree.PackageDeclarationTreeImpl(
        convertAnnotations(e.getPackage().annotations()),
        firstTokenIn(e.getPackage(), TerminalTokens.TokenNamepackage),
        convertExpression(e.getPackage().getName()),
        firstTokenIn(e.getPackage(), TerminalTokens.TokenNameSEMICOLON)
      );
    }

    List<ImportClauseTree> imports = new ArrayList<>();
    for (int i = 0; i < e.imports().size(); i++) {
      ImportDeclaration e2 = (ImportDeclaration) e.imports().get(i);
      ExpressionTree name = convertExpression(e2.getName());
      if (e2.isOnDemand()) {
        name = new MemberSelectExpressionTreeImpl(
          name,
          lastTokenIn(e2, TerminalTokens.TokenNameDOT),
          new IdentifierTreeImpl(lastTokenIn(e2, TerminalTokens.TokenNameMULTIPLY))
        );
      }
      imports.add(new JavaTree.ImportTreeImpl(
        firstTokenIn(e2, TerminalTokens.TokenNameimport),
        e2.isStatic() ? firstTokenIn(e2, TerminalTokens.TokenNamestatic) : null,
        name,
        lastTokenIn(e2, TerminalTokens.TokenNameSEMICOLON)
      ));

      addEmptyDeclarationsToList(
        tokenManager.lastIndexIn(e2, TerminalTokens.TokenNameSEMICOLON),
        imports
      );
    }

    List<Tree> types = new ArrayList<>();
    for (Object type : e.types()) {
      processBodyDeclaration((AbstractTypeDeclaration) type, types);
    }

    if (e.imports().isEmpty() && e.types().isEmpty()) {
      addEmptyDeclarationsToList(-1, imports);
    }

    return new JavaTree.CompilationUnitTreeImpl(
      packageDeclaration,
      imports,
      types,
      convertModuleDeclaration(compilationUnit.getModule()),
      firstTokenAfter(e, TerminalTokens.TokenNameEOF)
    );
  }

  @Nullable
  private ModuleDeclarationTree convertModuleDeclaration(@Nullable ModuleDeclaration e) {
    if (e == null) {
      return null;
    }
    List<ModuleDirectiveTree> moduleDirectives = new ArrayList<>();
    for (Object o : e.moduleStatements()) {
      moduleDirectives.add(
        convertModuleDirective((ModuleDirective) o)
      );
    }
    return new ModuleDeclarationTreeImpl(
      convertAnnotations(e.annotations()),
      e.isOpen() ? firstTokenIn(e, TerminalTokens.TokenNameopen) : null,
      firstTokenBefore(e.getName(), TerminalTokens.TokenNamemodule),
      convertModuleName(e.getName()),
      firstTokenAfter(e.getName(), TerminalTokens.TokenNameLBRACE),
      moduleDirectives,
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    );
  }

  private ModuleNameTreeImpl convertModuleName(Name node) {
    switch (node.getNodeType()) {
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        ModuleNameTreeImpl t = convertModuleName(e.getQualifier());
        t.add(
          new IdentifierTreeImpl(firstTokenIn(e.getName(), TerminalTokens.TokenNameIdentifier))
        );
        return t;
      }
      case ASTNode.SIMPLE_NAME: {
        SimpleName e = (SimpleName) node;
        ModuleNameTreeImpl t = new ModuleNameTreeImpl(new ArrayList<>(), Collections.emptyList());
        t.add(
          new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNameIdentifier))
        );
        return t;
      }
    }
  }

  private ModuleNameListTreeImpl convertModuleNames(List list) {
    ModuleNameListTreeImpl t = new ModuleNameListTreeImpl(new ArrayList<>(), new ArrayList<>());
    for (int i = 0; i < list.size(); i++) {
      Name o = (Name) list.get(i);
      if (i > 0) {
        t.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      t.add(convertModuleName(o));
    }
    return t;
  }

  private ModuleDirectiveTree convertModuleDirective(ModuleDirective node) {
    switch (node.getNodeType()) {
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
      case ASTNode.REQUIRES_DIRECTIVE: {
        RequiresDirective e = (RequiresDirective) node;
        ModifiersTreeImpl modifiers = new ModifiersTreeImpl(new ArrayList<>());
        for (Object o : e.modifiers()) {
          switch (((ModuleModifier) o).getKeyword().toString()) {
            default:
              throw new IllegalStateException();
            case "static":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
            case "transitive":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.TRANSITIVE, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
          }
        }
        return new RequiresDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamerequires),
          modifiers,
          convertModuleName(e.getName()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.EXPORTS_DIRECTIVE: {
        ExportsDirective e = (ExportsDirective) node;
        return new ExportsDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameexports),
          convertExpression(e.getName()),
          e.modules().isEmpty() ? null : firstTokenAfter(e.getName(), TerminalTokens.TokenNameto),
          convertModuleNames(e.modules()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.OPENS_DIRECTIVE: {
        OpensDirective e = (OpensDirective) node;
        return new OpensDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameopens),
          convertExpression(e.getName()),
          e.modules().isEmpty() ? null : firstTokenAfter(e.getName(), TerminalTokens.TokenNameto),
          convertModuleNames(e.modules()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.USES_DIRECTIVE: {
        UsesDirective e = (UsesDirective) node;
        return new UsesDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameuses),
          (TypeTree) convertExpression(e.getName()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.PROVIDES_DIRECTIVE: {
        ProvidesDirective e = (ProvidesDirective) node;
        QualifiedIdentifierListTreeImpl typeNames = new QualifiedIdentifierListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.implementations().size(); i++) {
          Name o = (Name) e.implementations().get(i);
          if (i > 0) {
            typeNames.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          typeNames.add((TypeTree) convertExpression(o));
        }
        return new ProvidesDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameprovides),
          (TypeTree) convertExpression(e.getName()),
          firstTokenAfter(e.getName(), TerminalTokens.TokenNamewith),
          typeNames,
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
    }
  }

  private ClassTreeImpl convertTypeDeclaration(AbstractTypeDeclaration e) {
    List<Tree> members = new ArrayList<>();
    if (e.getNodeType() == ASTNode.ENUM_DECLARATION) {
      for (Object o : ((EnumDeclaration) e).enumConstants()) {
        members.add(processEnumConstantDeclaration((EnumConstantDeclaration) o));
      }
    }

    // TODO try to simplify, note that type annotations can contain LBRACE
    final int leftBraceTokenIndex;
    if (e.getNodeType() == ASTNode.ENUM_DECLARATION) {
      EnumDeclaration enumDeclaration = (EnumDeclaration) e;
      if (!enumDeclaration.enumConstants().isEmpty()) {
        leftBraceTokenIndex = tokenManager.firstIndexBefore((ASTNode) enumDeclaration.enumConstants().get(0), TerminalTokens.TokenNameLBRACE);
      } else if (!enumDeclaration.bodyDeclarations().isEmpty()) {
        leftBraceTokenIndex = tokenManager.firstIndexBefore((ASTNode) e.bodyDeclarations().get(0), TerminalTokens.TokenNameLBRACE);
      } else {
        leftBraceTokenIndex = tokenManager.lastIndexIn(e, TerminalTokens.TokenNameLBRACE);
      }
    } else if (!e.bodyDeclarations().isEmpty()) {
      leftBraceTokenIndex = tokenManager.firstIndexBefore((ASTNode) e.bodyDeclarations().get(0), TerminalTokens.TokenNameLBRACE);
    } else {
      leftBraceTokenIndex = tokenManager.lastIndexIn(e, TerminalTokens.TokenNameLBRACE);
    }
    addEmptyDeclarationsToList(leftBraceTokenIndex, members);
    for (Object o : e.bodyDeclarations()) {
      processBodyDeclaration((BodyDeclaration) o, members);
    }

    Tree.Kind kind;
    switch (e.getNodeType()) {
      default:
        throw new IllegalStateException();
      case ASTNode.ENUM_DECLARATION:
        kind = Tree.Kind.ENUM;
        break;
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
        kind = Tree.Kind.ANNOTATION_TYPE;
        break;
      case ASTNode.TYPE_DECLARATION:
        kind = ((TypeDeclaration) e).isInterface() ? Tree.Kind.INTERFACE : Tree.Kind.CLASS;
        break;
    }
    ClassTreeImpl t = new ClassTreeImpl(
      kind,
      createSyntaxToken(leftBraceTokenIndex),
      members,
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    ).completeModifiers(
      convertModifiers(e.modifiers())
    );
    switch (kind) {
      default:
        break;
      case ENUM:
        t.completeDeclarationKeyword(firstTokenBefore(e.getName(), TerminalTokens.TokenNameenum));
        t.completeIdentifier(convertSimpleName(e.getName()));
        break;
      case CLASS:
        t.completeDeclarationKeyword(firstTokenBefore(e.getName(), TerminalTokens.TokenNameclass));
        t.completeIdentifier(convertSimpleName(e.getName()));
        break;
      case INTERFACE:
        t.completeDeclarationKeyword(firstTokenBefore(e.getName(), TerminalTokens.TokenNameinterface));
        t.completeIdentifier(convertSimpleName(e.getName()));
        break;
      case ANNOTATION_TYPE:
        t.complete(
          firstTokenBefore(e.getName(), TerminalTokens.TokenNameAT),
          firstTokenBefore(e.getName(), TerminalTokens.TokenNameinterface),
          convertSimpleName(e.getName())
        );
        break;
    }

    if (kind == Tree.Kind.CLASS || kind == Tree.Kind.INTERFACE) {
      TypeDeclaration ee = (TypeDeclaration) e;
      t.completeTypeParameters(
        convertTypeParameters(ee.typeParameters())
      );
    }

    switch (kind) {
      default:
        break;
      case CLASS: {
        TypeDeclaration ee = (TypeDeclaration) e;
        if (ee.getSuperclassType() != null) {
          t.completeSuperclass(
            firstTokenBefore(ee.getSuperclassType(), TerminalTokens.TokenNameextends),
            convertType(ee.getSuperclassType())
          );
        }
        // fall through
      }
      case INTERFACE:
      case ENUM: {
        List superInterfaceTypes = kind == Tree.Kind.ENUM ? ((EnumDeclaration) e).superInterfaceTypes() : ((TypeDeclaration) e).superInterfaceTypes();
        if (!superInterfaceTypes.isEmpty()) {
          QualifiedIdentifierListTreeImpl superInterfaces = new QualifiedIdentifierListTreeImpl(
            new ArrayList<>(),
            new ArrayList<>()
          );
          for (int i = 0; i < superInterfaceTypes.size(); i++) {
            Type o = (Type) superInterfaceTypes.get(i);
            if (i > 0) {
              superInterfaces.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
            }
            superInterfaces.add(convertType(o));
          }
          t.completeInterfaces(
            firstTokenBefore((ASTNode) superInterfaceTypes.get(0), kind == Tree.Kind.INTERFACE ? TerminalTokens.TokenNameextends : TerminalTokens.TokenNameimplements),
            superInterfaces
          );
        }
        break;
      }
    }
    return t;
  }

  private EnumConstantTreeImpl processEnumConstantDeclaration(EnumConstantDeclaration e) {
    final int openParTokenIndex = firstTokenIndexAfter(e.getName());
    final InternalSyntaxToken openParToken;
    final InternalSyntaxToken closeParToken;
    if (tokenManager.get(openParTokenIndex).tokenType == TerminalTokens.TokenNameLPAREN) {
      openParToken = createSyntaxToken(openParTokenIndex);
      closeParToken = e.arguments().isEmpty()
        ? firstTokenAfter(e.getName(), TerminalTokens.TokenNameRPAREN)
        : firstTokenAfter((ASTNode) e.arguments().get(e.arguments().size() - 1), TerminalTokens.TokenNameRPAREN);
    } else {
      openParToken = null;
      closeParToken = null;
    }

    final ArgumentListTreeImpl arguments = convertArguments(openParToken, e.arguments(), closeParToken);
    ClassTreeImpl classBody = null;
    if (e.getAnonymousClassDeclaration() != null) {
      List<Tree> members = new ArrayList<>();
      for (Object o : e.getAnonymousClassDeclaration().bodyDeclarations()) {
        processBodyDeclaration((BodyDeclaration) o, members);
      }
      classBody = new ClassTreeImpl(
        Tree.Kind.CLASS,
        firstTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameLBRACE),
        members,
        lastTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameRBRACE)
      );
    }

    final int separatorTokenIndex = firstTokenIndexAfter(e);
    final InternalSyntaxToken separatorToken;
    switch (tokenManager.get(separatorTokenIndex).tokenType) {
      default:
        throw new IllegalStateException();
      case TerminalTokens.TokenNameCOMMA:
      case TerminalTokens.TokenNameSEMICOLON:
        separatorToken = createSyntaxToken(separatorTokenIndex);
        break;
      case TerminalTokens.TokenNameRBRACE:
        separatorToken = null;
        break;
    }

    IdentifierTree identifier = convertSimpleName(e.getName());
    return new EnumConstantTreeImpl(
      convertModifiers(e.modifiers()),
      identifier,
      new NewClassTreeImpl(
        arguments,
        classBody
      ).completeWithIdentifier(
        identifier
      ),
      separatorToken
    );
  }

  private void processBodyDeclaration(ASTNode node, List<Tree> members) {
    final int lastTokenIndex;

    switch (node.getNodeType()) {
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
      case ASTNode.ENUM_DECLARATION:
      case ASTNode.TYPE_DECLARATION: {
        members.add(convertTypeDeclaration((AbstractTypeDeclaration) node));
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameRBRACE);
        break;
      }
      case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
        AnnotationTypeMemberDeclaration e = (AnnotationTypeMemberDeclaration) node;
        members.add(new MethodTreeImpl(
          new FormalParametersListTreeImpl(
            firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
            firstTokenAfter(e.getName(), TerminalTokens.TokenNameRPAREN)
          ),
          e.getDefault() == null ? null : firstTokenBefore(e.getDefault(), TerminalTokens.TokenNamedefault),
          e.getDefault() == null ? null : convertExpression(e.getDefault())
        ).complete(
          convertType(e.getType()),
          convertSimpleName(e.getName()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        ).completeWithModifiers(
          convertModifiers(e.modifiers())
        ));
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameSEMICOLON);
        break;
      }
      case ASTNode.INITIALIZER: {
        Initializer e = (Initializer) node;
        BlockTreeImpl blockTree = convertBlock(e.getBody());
        if (org.eclipse.jdt.core.dom.Modifier.isStatic(e.getModifiers())) {
          members.add(new StaticInitializerTreeImpl(
            firstTokenIn(e, TerminalTokens.TokenNamestatic),
            (InternalSyntaxToken) blockTree.openBraceToken(),
            blockTree.body(),
            (InternalSyntaxToken) blockTree.closeBraceToken()
          ));
        } else {
          members.add(new BlockTreeImpl(
            Tree.Kind.INITIALIZER,
            (InternalSyntaxToken) blockTree.openBraceToken(),
            blockTree.body(),
            (InternalSyntaxToken) blockTree.closeBraceToken()
          ));
        }
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameRBRACE);
        break;
      }
      case ASTNode.METHOD_DECLARATION: {
        MethodDeclaration e = (MethodDeclaration) node;

        FormalParametersListTreeImpl parameters = new FormalParametersListTreeImpl(
          firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
          firstTokenAfter(
            e.parameters().isEmpty() ? e.getName() : (ASTNode) e.parameters().get(e.parameters().size() - 1),
            TerminalTokens.TokenNameRPAREN
          ));
        for (int i = 0; i < e.parameters().size(); i++) {
          SingleVariableDeclaration o = (SingleVariableDeclaration) e.parameters().get(i);
          VariableTreeImpl parameter = createVariable(o);
          if (i < e.parameters().size() - 1) {
            parameter.setEndToken(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
          }
          parameters.add(parameter);
        }

        QualifiedIdentifierListTreeImpl thrownExceptionTypes = new QualifiedIdentifierListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.thrownExceptionTypes().size(); i++) {
          Type o = (Type) e.thrownExceptionTypes().get(i);
          if (i > 0) {
            thrownExceptionTypes.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          thrownExceptionTypes.add(convertType(o));
        }

        members.add(new MethodTreeImpl(
          applyExtraDimensions(convertType(e.getReturnType2()), e.extraDimensions()),
          convertSimpleName(e.getName()),
          parameters,
          e.thrownExceptionTypes().isEmpty() ? null : firstTokenBefore((Type) e.thrownExceptionTypes().get(0), TerminalTokens.TokenNamethrows),
          thrownExceptionTypes,
          convertBlock(e.getBody()),
          e.getBody() == null ? lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON) : null
        ).completeWithModifiers(
          convertModifiers(e.modifiers())
        ).completeWithTypeParameters(
          convertTypeParameters(e.typeParameters())
        ));
        lastTokenIndex = tokenManager.lastIndexIn(node, e.getBody() == null ? TerminalTokens.TokenNameSEMICOLON : TerminalTokens.TokenNameRBRACE);
        break;
      }
      case ASTNode.FIELD_DECLARATION: {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
        ModifiersTreeImpl modifiers = convertModifiers(fieldDeclaration.modifiers());
        TypeTree type = convertType(fieldDeclaration.getType());

        for (int i = 0; i < fieldDeclaration.fragments().size(); i++) {
          VariableDeclarationFragment fragment = (VariableDeclarationFragment) fieldDeclaration.fragments().get(i);
          VariableTreeImpl t = new VariableTreeImpl(
            convertSimpleName(fragment.getName())
          ).completeModifiersAndType(
            modifiers,
            applyExtraDimensions(type, fragment.extraDimensions())
          );
          if (fragment.getInitializer() != null) {
            t.completeTypeAndInitializer(
              t.type(),
              firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL),
              convertExpression(fragment.getInitializer())
            );
          }

          t.setEndToken(
            firstTokenAfter(fragment, i + 1 < fieldDeclaration.fragments().size() ? TerminalTokens.TokenNameCOMMA : TerminalTokens.TokenNameSEMICOLON)
          );

          members.add(t);
        }
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameSEMICOLON);
        break;
      }
    }

    addEmptyDeclarationsToList(lastTokenIndex, members);
  }

  private ArgumentListTreeImpl convertArguments(
    InternalSyntaxToken openParen,
    List list,
    InternalSyntaxToken closeParen
  ) {
    ArgumentListTreeImpl arguments = new ArgumentListTreeImpl(new ArrayList<>(), new ArrayList<>()).complete(openParen, closeParen);
    for (int i = 0; i < list.size(); i++) {
      Expression o = (Expression) list.get(i);
      arguments.add(convertExpression(o));
      if (i < list.size() - 1) {
        arguments.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
      }
    }
    return arguments;
  }

  @Nullable
  private TypeArgumentListTreeImpl convertTypeArguments(List list) {
    if (list.isEmpty()) {
      return null;
    }
    ASTNode last = (ASTNode) list.get(list.size() - 1);
    int tokenIndex = tokenManager.firstIndexAfter(last, ANY_TOKEN);
    while (tokenManager.get(tokenIndex).isComment()) {
      tokenIndex++;
    }
    return convertTypeArguments(
      firstTokenBefore((ASTNode) list.get(0), TerminalTokens.TokenNameLESS),
      list,
      // TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT vs TerminalTokens.TokenNameGREATER
      createSpecialToken(tokenIndex)
    );
  }

  private TypeArgumentListTreeImpl convertTypeArguments(
    InternalSyntaxToken l,
    List list,
    InternalSyntaxToken g
  ) {
    TypeArgumentListTreeImpl typeArguments = new TypeArgumentListTreeImpl(l, new ArrayList<>(), new ArrayList<>(), g);
    for (int i = 0; i < list.size(); i++) {
      Type o = (Type) list.get(i);
      if (i > 0) {
        typeArguments.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      typeArguments.add(convertType(o));
    }
    return typeArguments;
  }

  private TypeParameterListTreeImpl convertTypeParameters(List list) {
    if (list.isEmpty()) {
      return new TypeParameterListTreeImpl();
    }
    ASTNode last = (ASTNode) list.get(list.size() - 1);
    int tokenIndex = tokenManager.firstIndexAfter(last, ANY_TOKEN);
    while (tokenManager.get(tokenIndex).isComment()) {
      tokenIndex++;
    }
    TypeParameterListTreeImpl t = new TypeParameterListTreeImpl(
      firstTokenBefore((ASTNode) list.get(0), TerminalTokens.TokenNameLESS),
      new ArrayList<>(), new ArrayList<>(),
      // TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT vs TerminalTokens.TokenNameGREATER
      createSpecialToken(tokenIndex)
    );
    for (int i = 0; i < list.size(); i++) {
      TypeParameter o = (TypeParameter) list.get(i);
      if (i > 0) {
        t.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      t.add(convertTypeParameter(o));
    }
    return t;
  }

  private TypeParameterTree convertTypeParameter(TypeParameter e) {
    IdentifierTreeImpl t = convertSimpleName(e.getName());
    // TODO why ECJ uses IExtendedModifier here instead of Annotation ?
    t.complete(convertAnnotations(e.modifiers()));
    if (e.typeBounds().isEmpty()) {
      return new TypeParameterTreeImpl(t);
    } else {
      BoundListTreeImpl bounds = new BoundListTreeImpl(new ArrayList<>(), new ArrayList<>());
      for (Object o : e.typeBounds()) {
        bounds.add(convertType((Type) o));
      }
      return new TypeParameterTreeImpl(
        firstTokenAfter(e.getName(), TerminalTokens.TokenNameextends),
        bounds
      ).complete(
        t
      );
    }
  }

  /**
   * @param extraDimensions list of {@link org.eclipse.jdt.core.dom.Dimension}
   */
  private TypeTree applyExtraDimensions(TypeTree type, List extraDimensions) {
    for (Object o : extraDimensions) {
      Dimension e = (Dimension) o;
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.annotations()),
        firstTokenIn(e, TerminalTokens.TokenNameLBRACKET),
        firstTokenIn(e, TerminalTokens.TokenNameRBRACKET)
      );
    }
    return type;
  }

  private VariableTreeImpl createVariable(SingleVariableDeclaration e) {
    // TODO are extraDimensions and varargs mutually exclusive?
    TypeTree type = convertType(e.getType());
    type = applyExtraDimensions(type, e.extraDimensions());
    if (e.isVarargs()) {
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.varargsAnnotations()),
        firstTokenAfter(e.getType(), TerminalTokens.TokenNameELLIPSIS)
      );
    }

    VariableTreeImpl t = new VariableTreeImpl(
      e.isVarargs(),
      convertModifiers(e.modifiers()),
      type,
      convertSimpleName(e.getName())
    );
    if (e.getInitializer() != null) {
      t.completeTypeAndInitializer(
        t.type(),
        firstTokenAfter(e.getName(), TerminalTokens.TokenNameEQUAL),
        convertExpression(e.getInitializer())
      );
    }
    return t;
  }

  private void addVariableToList(VariableDeclarationExpression e2, List list) {
    ModifiersTreeImpl modifiers = convertModifiers(e2.modifiers());
    TypeTree type = convertType(e2.getType());

    for (int i = 0; i < e2.fragments().size(); i++) {
      VariableDeclarationFragment fragment = (VariableDeclarationFragment) e2.fragments().get(i);
      VariableTreeImpl t = new VariableTreeImpl(convertSimpleName(fragment.getName()));
      t.completeModifiers(modifiers);
      if (fragment.getInitializer() == null) {
        t.completeType(type);
      } else {
        t.completeTypeAndInitializer(
          type,
          firstTokenBefore(fragment.getInitializer(), TerminalTokens.TokenNameEQUAL),
          convertExpression(fragment.getInitializer())
        );
      }
      if (i < e2.fragments().size() - 1) {
        t.setEndToken(firstTokenAfter(fragment, TerminalTokens.TokenNameCOMMA));
      }
      list.add(t);
    }
  }

  private IdentifierTreeImpl convertSimpleName(@Nullable SimpleName e) {
    if (e == null) {
      // e.g. break-statement without label
      return null;
    }
    return new IdentifierTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameIdentifier)
    );
  }

  private BlockTreeImpl convertBlock(@Nullable Block e) {
    if (e == null) {
      // e.g. abstract method or finally
      return null;
    }
    List<StatementTree> statements = new ArrayList<>();
    for (Object o : e.statements()) {
      addStatementToList((Statement) o, statements);
    }
    return new BlockTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameLBRACE),
      statements,
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    );
  }

  private void addStatementToList(Statement node, List<StatementTree> statements) {
    if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
      VariableDeclarationStatement e = (VariableDeclarationStatement) node;
      TypeTree tType = convertType(e.getType());
      ModifiersTreeImpl modifiers = convertModifiers(e.modifiers());
      for (int i = 0; i < e.fragments().size(); i++) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) e.fragments().get(i);
        VariableTreeImpl t = new VariableTreeImpl(
          convertSimpleName(fragment.getName())
        ).completeType(
          applyExtraDimensions(tType, fragment.extraDimensions())
        ).completeModifiers(
          modifiers
        );
        if (fragment.getInitializer() != null) {
          t.completeTypeAndInitializer(
            t.type(),
            firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL),
            convertExpression(fragment.getInitializer())
          );
        }
        t.setEndToken(
          firstTokenAfter(fragment, i < e.fragments().size() - 1 ? TerminalTokens.TokenNameCOMMA : TerminalTokens.TokenNameSEMICOLON)
        );
        statements.add(t);
      }
    } else {
      statements.add(convertStatement(node));
    }
  }

  private StatementTree convertStatement(@Nullable Statement node) {
    if (node == null) {
      // e.g. else-statement
      return null;
    }
    switch (node.getNodeType()) {
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
      case ASTNode.BLOCK:
        return convertBlock((Block) node);
      case ASTNode.EMPTY_STATEMENT: {
        EmptyStatement e = (EmptyStatement) node;
        return new EmptyStatementTreeImpl(
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.RETURN_STATEMENT: {
        ReturnStatement e = (ReturnStatement) node;
        return new ReturnStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamereturn),
          convertExpression(e.getExpression()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.FOR_STATEMENT: {
        ForStatement e = (ForStatement) node;

        StatementExpressionListTreeImpl forInitStatement = new StatementExpressionListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.initializers().size(); i++) {
          Expression o = (Expression) e.initializers().get(i);
          if (i > 0) {
            forInitStatement.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
            addVariableToList(
              (VariableDeclarationExpression) o,
              forInitStatement
            );
          } else {
            forInitStatement.add(new ExpressionStatementTreeImpl(
              convertExpression(o),
              null
            ));
          }
        }

        StatementExpressionListTreeImpl forUpdateStatement = new StatementExpressionListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.updaters().size(); i++) {
          Expression o = (Expression) e.updaters().get(i);
          if (i > 0) {
            forUpdateStatement.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          forUpdateStatement.add(new ExpressionStatementTreeImpl(
            convertExpression(o),
            null
          ));
        }

        final int firstSemicolonTokenIndex = e.initializers().isEmpty()
          ? tokenManager.firstIndexIn(e, TerminalTokens.TokenNameSEMICOLON)
          : tokenManager.firstIndexAfter((ASTNode) e.initializers().get(e.initializers().size() - 1), TerminalTokens.TokenNameSEMICOLON);
        final int secondSemicolonTokenIndex = e.getExpression() == null
          ? nextTokenIndex(firstSemicolonTokenIndex, TerminalTokens.TokenNameSEMICOLON)
          : tokenManager.firstIndexAfter(e.getExpression(), TerminalTokens.TokenNameSEMICOLON);

        return new ForStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamefor),
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          forInitStatement,
          createSyntaxToken(firstSemicolonTokenIndex),
          convertExpression(e.getExpression()),
          createSyntaxToken(secondSemicolonTokenIndex),
          forUpdateStatement,
          firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN),
          convertStatement(e.getBody())
        );
      }
      case ASTNode.WHILE_STATEMENT: {
        WhileStatement e = (WhileStatement) node;
        return new WhileStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamewhile),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          convertStatement(e.getBody())
        );
      }
      case ASTNode.IF_STATEMENT: {
        IfStatement e = (IfStatement) node;
        if (e.getElseStatement() != null) {
          return new IfStatementTreeImpl(
            firstTokenAfter(e.getThenStatement(), TerminalTokens.TokenNameelse),
            convertStatement(e.getElseStatement())
          ).complete(
            firstTokenIn(e, TerminalTokens.TokenNameif),
            firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
            convertExpression(e.getExpression()),
            firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
            convertStatement(e.getThenStatement())
          );
        } else {
          return new IfStatementTreeImpl(
            firstTokenIn(e, TerminalTokens.TokenNameif),
            firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
            convertExpression(e.getExpression()),
            firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
            convertStatement(e.getThenStatement())
          );
        }
      }
      case ASTNode.BREAK_STATEMENT: {
        BreakStatement e = (BreakStatement) node;
        if (e.isImplicit()) {
          return new ExpressionStatementTreeImpl(
            convertExpression(e.getExpression()),
            lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
          );
        }
        return new BreakStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamebreak),
          e.getExpression() == null ? convertSimpleName(e.getLabel()) : convertExpression(e.getExpression()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.DO_STATEMENT: {
        DoStatement e = (DoStatement) node;
        return new DoWhileStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamedo),
          convertStatement(e.getBody()),
          firstTokenAfter(e.getBody(), TerminalTokens.TokenNamewhile),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.ASSERT_STATEMENT: {
        AssertStatement e = (AssertStatement) node;
        if (e.getMessage() != null) {
          return new AssertStatementTreeImpl(
            firstTokenBefore(e.getMessage(), TerminalTokens.TokenNameCOLON),
            convertExpression(e.getMessage())
          ).complete(
            firstTokenIn(e, TerminalTokens.TokenNameassert),
            convertExpression(e.getExpression()),
            lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
          );
        } else {
          return new AssertStatementTreeImpl(
            firstTokenIn(e, TerminalTokens.TokenNameassert),
            convertExpression(e.getExpression()),
            lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
          );
        }
      }
      case ASTNode.SWITCH_STATEMENT: {
        SwitchStatement e = (SwitchStatement) node;
        return new SwitchStatementTreeImpl(new SwitchExpressionTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameswitch),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameLBRACE),
          convertSwitchStatements(e.statements()),
          lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
        ));
      }
      case ASTNode.SYNCHRONIZED_STATEMENT: {
        SynchronizedStatement e = (SynchronizedStatement) node;
        return new SynchronizedStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamesynchronized),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          convertBlock(e.getBody())
        );
      }
      case ASTNode.EXPRESSION_STATEMENT: {
        ExpressionStatement e = (ExpressionStatement) node;
        return new ExpressionStatementTreeImpl(
          convertExpression(e.getExpression()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.CONTINUE_STATEMENT: {
        ContinueStatement e = (ContinueStatement) node;
        return new ContinueStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamecontinue),
          convertSimpleName(e.getLabel()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.LABELED_STATEMENT: {
        LabeledStatement e = (LabeledStatement) node;
        return new LabeledStatementTreeImpl(
          convertSimpleName(e.getLabel()),
          firstTokenAfter(e.getLabel(), TerminalTokens.TokenNameCOLON),
          convertStatement(e.getBody())
        );
      }
      case ASTNode.ENHANCED_FOR_STATEMENT: {
        EnhancedForStatement e = (EnhancedForStatement) node;
        return new ForEachStatementImpl(
          firstTokenIn(e, TerminalTokens.TokenNamefor),
          firstTokenBefore(e.getParameter(), TerminalTokens.TokenNameLPAREN),
          createVariable(e.getParameter()),
          firstTokenAfter(e.getParameter(), TerminalTokens.TokenNameCOLON),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          convertStatement(e.getBody())
        );
      }
      case ASTNode.THROW_STATEMENT: {
        ThrowStatement e = (ThrowStatement) node;
        return new ThrowStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamethrow),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.TRY_STATEMENT: {
        TryStatement e = (TryStatement) node;

        List<CatchTree> catches = new ArrayList<>();
        for (Object o : e.catchClauses()) {
          CatchClause e2 = (CatchClause) o;
          catches.add(new CatchTreeImpl(
            firstTokenIn(e2, TerminalTokens.TokenNamecatch),
            firstTokenBefore(e2.getException(), TerminalTokens.TokenNameLPAREN),
            createVariable(e2.getException()),
            firstTokenAfter(e2.getException(), TerminalTokens.TokenNameRPAREN),
            convertBlock(e2.getBody())
          ));
        }

        ResourceListTreeImpl resources = new ResourceListTreeImpl(
          new ArrayList<>(), new ArrayList<>()
        );
        for (int i = 0; i < e.resources().size(); i++) {
          Expression o = (Expression) e.resources().get(i);
          if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
            addVariableToList(
              (VariableDeclarationExpression) o,
              resources
            );
          } else {
            resources.add(convertExpression(o));
          }
          if (i < e.resources().size() - 1) {
            resources.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameSEMICOLON));
          } else {
            int tokenIndex = tokenManager.firstIndexBefore(e.getBody(), TerminalTokens.TokenNameRPAREN);
            while (true) {
              Token token;
              do {
                tokenIndex--;
                token = tokenManager.get(tokenIndex);
              } while (token.isComment());
              if (token.tokenType == TerminalTokens.TokenNameSEMICOLON) {
                resources.separators().add(
                  createSyntaxToken(tokenIndex)
                );
              } else {
                break;
              }
            }
          }
        }

        return new TryStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNametry),
          e.resources().isEmpty() ? null : firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          resources,
          e.resources().isEmpty() ? null : firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN),
          convertBlock(e.getBody()),
          catches,
          e.getFinally() == null ? null : firstTokenBefore(e.getFinally(), TerminalTokens.TokenNamefinally),
          convertBlock(e.getFinally())
        );
      }
      case ASTNode.TYPE_DECLARATION_STATEMENT: {
        TypeDeclarationStatement e = (TypeDeclarationStatement) node;
        return convertTypeDeclaration(e.getDeclaration());
      }
      case ASTNode.CONSTRUCTOR_INVOCATION: {
        ConstructorInvocation e = (ConstructorInvocation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          e.arguments().isEmpty() ? lastTokenIn(e, TerminalTokens.TokenNameLPAREN) : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        return new ExpressionStatementTreeImpl(
          new MethodInvocationTreeImpl(
            new IdentifierTreeImpl(e.arguments().isEmpty()
              ? lastTokenIn(e, TerminalTokens.TokenNamethis)
              : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalTokens.TokenNamethis)),
            convertTypeArguments(e.typeArguments()),
            arguments
          ),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION: {
        SuperConstructorInvocation e = (SuperConstructorInvocation) node;

        ExpressionTree methodSelect = new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNamesuper));
        if (e.getExpression() != null) {
          methodSelect = new MemberSelectExpressionTreeImpl(
            convertExpression(e.getExpression()),
            firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
            (IdentifierTreeImpl) methodSelect
          );
        }

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        return new ExpressionStatementTreeImpl(
          new MethodInvocationTreeImpl(
            methodSelect,
            convertTypeArguments(e.typeArguments()),
            arguments
          ),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
    }
  }

  private List<CaseGroupTreeImpl> convertSwitchStatements(List list) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> labels = null;
    BlockStatementListTreeImpl body = null;
    for (Object o : list) {
      if (o instanceof SwitchCase) {
        if (labels == null) {
          labels = new ArrayList<>();
          body = new BlockStatementListTreeImpl(new ArrayList<>());
        }

        SwitchCase c = (SwitchCase) o;

        List<ExpressionTree> expressions = new ArrayList<>();
        for (Object oo : c.expressions()) {
          expressions.add(
            convertExpression((Expression) oo)
          );
        }

        labels.add(new CaseLabelTreeImpl(
          firstTokenIn(c, c.isDefault() ? TerminalTokens.TokenNamedefault : TerminalTokens.TokenNamecase),
          expressions,
          lastTokenIn(c, /* TerminalTokens.TokenNameCOLON or TerminalTokens.TokenNameARROW */ ANY_TOKEN)
        ));
      } else {
        if (labels != null) {
          groups.add(new CaseGroupTreeImpl(
            labels,
            body
          ));
        }
        labels = null;
        addStatementToList((Statement) o, body);
      }
    }
    if (labels != null) {
      groups.add(new CaseGroupTreeImpl(
        labels,
        body
      ));
    }
    return groups;
  }

  private ExpressionTree convertExpression(@Nullable Expression node) {
    if (node == null) {
      // e.g. condition expression in for-statement
      return null;
    }
    switch (node.getNodeType()) {
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
      case ASTNode.SIMPLE_NAME: {
        return convertSimpleName((SimpleName) node);
      }
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        return new MemberSelectExpressionTreeImpl(
          convertExpression(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          convertSimpleName(e.getName())
        );
      }
      case ASTNode.FIELD_ACCESS: {
        FieldAccess e = (FieldAccess) node;
        return new MemberSelectExpressionTreeImpl(
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
          convertSimpleName(e.getName())
        );
      }
      case ASTNode.SUPER_FIELD_ACCESS: {
        SuperFieldAccess e = (SuperFieldAccess) node;
        if (e.getQualifier() == null) {
          // super.name
          return new MemberSelectExpressionTreeImpl(
            new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNamesuper)),
            firstTokenIn(e, TerminalTokens.TokenNameDOT),
            convertSimpleName(e.getName())
          );
        } else {
          // qualifier.super.name
          return new MemberSelectExpressionTreeImpl(
            new MemberSelectExpressionTreeImpl(
              convertExpression(e.getQualifier()),
              firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
              new IdentifierTreeImpl(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper))
            ),
            firstTokenBefore(e.getName(), TerminalTokens.TokenNameDOT),
            convertSimpleName(e.getName())
          );
        }
      }
      case ASTNode.THIS_EXPRESSION: {
        ThisExpression e = (ThisExpression) node;
        if (e.getQualifier() == null) {
          return new IdentifierTreeImpl(
            firstTokenIn(e, TerminalTokens.TokenNamethis)
          );
        } else {
          return new MemberSelectExpressionTreeImpl(
            convertExpression(e.getQualifier()),
            firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
            new IdentifierTreeImpl(
              firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamethis)
            )
          );
        }
      }
      case ASTNode.TYPE_LITERAL: {
        TypeLiteral e = (TypeLiteral) node;
        return new MemberSelectExpressionTreeImpl(
          (ExpressionTree) convertType(e.getType()),
          lastTokenIn(e, TerminalTokens.TokenNameDOT),
          new IdentifierTreeImpl(
            lastTokenIn(e, TerminalTokens.TokenNameclass)
          )
        );
      }
      case ASTNode.ARRAY_ACCESS: {
        ArrayAccess e = (ArrayAccess) node;
        return new ArrayAccessExpressionTreeImpl(
          new ArrayDimensionTreeImpl(
            firstTokenBefore(e.getIndex(), TerminalTokens.TokenNameLBRACKET),
            convertExpression(e.getIndex()),
            firstTokenAfter(e.getIndex(), TerminalTokens.TokenNameRBRACKET)
          )
        ).complete(
          convertExpression(e.getArray())
        );
      }
      case ASTNode.ARRAY_CREATION: {
        ArrayCreation e = (ArrayCreation) node;

        List<ArrayDimensionTree> dimensions = new ArrayList<>();
        for (Object o : e.dimensions()) {
          dimensions.add(new ArrayDimensionTreeImpl(
            firstTokenBefore((Expression) o, TerminalTokens.TokenNameLBRACKET),
            convertExpression((Expression) o),
            firstTokenAfter((Expression) o, TerminalTokens.TokenNameRBRACKET)
          ));
        }

        InitializerListTreeImpl initializers = new InitializerListTreeImpl(new ArrayList<>(), new ArrayList<>());
        if (e.getInitializer() != null) {
          assert dimensions.isEmpty();

          TypeTree type = convertType(e.getType());
          while (type.is(Tree.Kind.ARRAY_TYPE)) {
            ArrayTypeTree arrayType = (ArrayTypeTree) type;
            dimensions.add(/* TODO suboptimal */ 0, new ArrayDimensionTreeImpl(
              arrayType.openBracketToken(),
              null,
              arrayType.closeBracketToken()
            ));
            type = arrayType.type();
          }

          return ((NewArrayTreeImpl) convertExpression(e.getInitializer()))
            .completeWithNewKeyword(firstTokenIn(e, TerminalTokens.TokenNamenew))
            .complete(type)
            .completeDimensions(dimensions);
        } else {
          TypeTree type = convertType(e.getType());
          while (type.is(Tree.Kind.ARRAY_TYPE)) {
            type = ((ArrayTypeTree) type).type();
          }

          return new NewArrayTreeImpl(
            dimensions,
            initializers
          ).complete(
            type
          ).completeWithNewKeyword(
            firstTokenIn(e, TerminalTokens.TokenNamenew)
          );
        }
      }
      case ASTNode.ARRAY_INITIALIZER: {
        ArrayInitializer e = (ArrayInitializer) node;

        InitializerListTreeImpl initializers = new InitializerListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.expressions().size(); i++) {
          Expression o = (Expression) e.expressions().get(i);
          initializers.add(convertExpression(o));
          final int commaTokenIndex = firstTokenIndexAfter(o);
          if (tokenManager.get(commaTokenIndex).tokenType == TerminalTokens.TokenNameCOMMA) {
            initializers.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
          }
        }
        return new NewArrayTreeImpl(
          Collections.emptyList(),
          initializers
        ).completeWithCurlyBraces(
          firstTokenIn(e, TerminalTokens.TokenNameLBRACE),
          lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
        );
      }
      case ASTNode.ASSIGNMENT: {
        Assignment e = (Assignment) node;
        Op op = operators.get(e.getOperator());
        return new AssignmentExpressionTreeImpl(
          op.kind,
          convertExpression(e.getLeftHandSide()),
          firstTokenAfter(e.getLeftHandSide(), op.tokenType),
          convertExpression(e.getRightHandSide())
        );
      }
      case ASTNode.CAST_EXPRESSION: {
        CastExpression e = (CastExpression) node;
        if (e.getType().getNodeType() == ASTNode.INTERSECTION_TYPE) {
          IntersectionType intersectionType = (IntersectionType) e.getType();
          TypeTree type = convertType((Type) intersectionType.types().get(0));
          BoundListTreeImpl bounds = new BoundListTreeImpl(
            new ArrayList<>(), new ArrayList<>()
          );
          for (int i = 1; i < intersectionType.types().size(); i++) {
            Type o = (Type) intersectionType.types().get(i);
            bounds.add(
              convertType(o)
            );
            if (i < intersectionType.types().size() - 1) {
              bounds.separators().add(
                firstTokenAfter(o, TerminalTokens.TokenNameAND)
              );
            }
          }
          return new TypeCastExpressionTreeImpl(
            type,
            firstTokenAfter((Type) intersectionType.types().get(0), TerminalTokens.TokenNameAND),
            bounds,
            firstTokenAfter(e.getType(), TerminalTokens.TokenNameRPAREN),
            convertExpression(e.getExpression())
          ).complete(
            firstTokenBefore(e.getType(), TerminalTokens.TokenNameLPAREN)
          );
        } else {
          return new TypeCastExpressionTreeImpl(
            convertType(e.getType()),
            firstTokenAfter(e.getType(), TerminalTokens.TokenNameRPAREN),
            convertExpression(e.getExpression())
          ).complete(
            firstTokenIn(e, TerminalTokens.TokenNameLPAREN)
          );
        }
      }
      case ASTNode.CLASS_INSTANCE_CREATION: {
        ClassInstanceCreation e = (ClassInstanceCreation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenAfter(e.getType(), TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          firstTokenAfter(e.arguments().isEmpty() ? e.getType() : (ASTNode) e.arguments().get(e.arguments().size() - 1), TerminalTokens.TokenNameRPAREN)
        );

        ClassTreeImpl classBody = null;
        if (e.getAnonymousClassDeclaration() != null) {
          List<Tree> members = new ArrayList<>();
          for (Object o : e.getAnonymousClassDeclaration().bodyDeclarations()) {
            processBodyDeclaration((BodyDeclaration) o, members);
          }
          classBody = new ClassTreeImpl(
            Tree.Kind.CLASS,
            firstTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameLBRACE),
            members,
            lastTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameRBRACE)
          );
        }

        NewClassTreeImpl t = new NewClassTreeImpl(
          arguments,
          classBody
        ).completeWithNewKeyword(
          e.getExpression() == null ? firstTokenIn(e, TerminalTokens.TokenNamenew) : firstTokenAfter(e.getExpression(), TerminalTokens.TokenNamenew)
        ).completeWithIdentifier(
          convertType(e.getType())
        ).completeWithTypeArguments(
          convertTypeArguments(e.typeArguments())
        );
        if (e.getExpression() != null) {
          t.completeWithEnclosingExpression(convertExpression(e.getExpression()));
          t.completeWithDotToken(firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT));
        }
        return t;
      }
      case ASTNode.CONDITIONAL_EXPRESSION: {
        ConditionalExpression e = (ConditionalExpression) node;
        return new ConditionalExpressionTreeImpl(
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameQUESTION),
          convertExpression(e.getThenExpression()),
          firstTokenAfter(e.getThenExpression(), TerminalTokens.TokenNameCOLON),
          convertExpression(e.getElseExpression())
        ).complete(
          convertExpression(e.getExpression())
        );
      }
      case ASTNode.INFIX_EXPRESSION: {
        InfixExpression e = (InfixExpression) node;
        Op op = operators.get(e.getOperator());
        BinaryExpressionTreeImpl t = new BinaryExpressionTreeImpl(
          op.kind,
          convertExpression(e.getLeftOperand()),
          firstTokenAfter(e.getLeftOperand(), op.tokenType),
          convertExpression(e.getRightOperand())
        );
        for (Object o : e.extendedOperands()) {
          Expression e2 = (Expression) o;
          t = new BinaryExpressionTreeImpl(
            op.kind,
            t,
            firstTokenBefore(e2, op.tokenType),
            convertExpression(e2)
          );
        }
        return t;
      }
      case ASTNode.METHOD_INVOCATION: {
        MethodInvocation e = (MethodInvocation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        ExpressionTree memberSelect;
        if (e.getExpression() == null) {
          memberSelect = convertSimpleName(e.getName());
        } else {
          memberSelect = new MemberSelectExpressionTreeImpl(
            convertExpression(e.getExpression()),
            firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
            convertSimpleName(e.getName())
          );
        }
        return new MethodInvocationTreeImpl(
          memberSelect,
          convertTypeArguments(e.typeArguments()),
          arguments
        );
      }
      case ASTNode.SUPER_METHOD_INVOCATION: {
        SuperMethodInvocation e = (SuperMethodInvocation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        ExpressionTree outermostSelect;
        if (e.getQualifier() == null) {
          outermostSelect = new MemberSelectExpressionTreeImpl(
            new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNamesuper)),
            firstTokenIn(e, TerminalTokens.TokenNameDOT),
            convertSimpleName(e.getName())
          );
        } else {
          final int firstDotTokenIndex = tokenManager.firstIndexAfter(e.getQualifier(), TerminalTokens.TokenNameDOT);
          outermostSelect = new MemberSelectExpressionTreeImpl(
            new MemberSelectExpressionTreeImpl(
              convertExpression(e.getQualifier()),
              createSyntaxToken(firstDotTokenIndex),
              new IdentifierTreeImpl(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper))
            ),
            createSyntaxToken(nextTokenIndex(firstDotTokenIndex, TerminalTokens.TokenNameDOT)),
            convertSimpleName(e.getName())
          );
        }

        return new MethodInvocationTreeImpl(
          outermostSelect,
          null,
          arguments
        );
      }
      case ASTNode.PARENTHESIZED_EXPRESSION: {
        ParenthesizedExpression e = (ParenthesizedExpression) node;
        return new ParenthesizedTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN)
        );
      }
      case ASTNode.POSTFIX_EXPRESSION: {
        PostfixExpression e = (PostfixExpression) node;
        Op op = operators.get(e.getOperator());
        return new InternalPostfixUnaryExpression(
          op.kind,
          convertExpression(e.getOperand()),
          firstTokenAfter(e.getOperand(), op.tokenType)
        );
      }
      case ASTNode.PREFIX_EXPRESSION: {
        PrefixExpression e = (PrefixExpression) node;
        Op op = operators.get(e.getOperator());
        return new InternalPrefixUnaryExpression(
          op.kind,
          firstTokenIn(e, op.tokenType),
          convertExpression(e.getOperand())
        );
      }
      case ASTNode.INSTANCEOF_EXPRESSION: {
        InstanceofExpression e = (InstanceofExpression) node;
        return new InstanceOfTreeImpl(
          firstTokenAfter(e.getLeftOperand(), TerminalTokens.TokenNameinstanceof),
          convertType(e.getRightOperand())
        ).complete(
          convertExpression(e.getLeftOperand())
        );
      }
      case ASTNode.LAMBDA_EXPRESSION: {
        LambdaExpression e = (LambdaExpression) node;
        List<VariableTree> parameters = new ArrayList<>();
        for (int i = 0; i < e.parameters().size(); i++) {
          VariableDeclaration o = (VariableDeclaration) e.parameters().get(i);
          VariableTreeImpl t = o.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT
            ? new VariableTreeImpl(convertSimpleName(o.getName()))
            : createVariable((SingleVariableDeclaration) o);
          parameters.add(t);
          if (i < e.parameters().size() - 1) {
            t.setEndToken(
              firstTokenAfter(o, TerminalTokens.TokenNameCOMMA)
            );
          }
        }
        return new LambdaExpressionTreeImpl(
          e.hasParentheses() ? firstTokenIn(e, TerminalTokens.TokenNameLPAREN) : null,
          parameters,
          e.hasParentheses() ? firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN) : null,
          firstTokenBefore(e.getBody(), TerminalTokens.TokenNameARROW),
          e.getBody().getNodeType() == ASTNode.BLOCK ? convertBlock((Block) e.getBody()) : convertExpression((Expression) e.getBody())
        );
      }
      case ASTNode.CREATION_REFERENCE: {
        CreationReference e = (CreationReference) node;
        MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
          convertType(e.getType()),
          lastTokenIn(e, TerminalTokens.TokenNameCOLON_COLON)
        );
        t.complete(
          convertTypeArguments(e.typeArguments()),
          new IdentifierTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamenew))
        );
        return t;
      }
      case ASTNode.EXPRESSION_METHOD_REFERENCE: {
        ExpressionMethodReference e = (ExpressionMethodReference) node;
        MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameCOLON_COLON)
        );
        t.complete(
          convertTypeArguments(e.typeArguments()),
          convertSimpleName(e.getName())
        );
        return t;
      }
      case ASTNode.TYPE_METHOD_REFERENCE: {
        TypeMethodReference e = (TypeMethodReference) node;
        MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
          convertType(e.getType()),
          firstTokenAfter(e.getType(), TerminalTokens.TokenNameCOLON_COLON)
        );
        t.complete(
          convertTypeArguments(e.typeArguments()),
          convertSimpleName(e.getName())
        );
        return t;
      }
      case ASTNode.SUPER_METHOD_REFERENCE: {
        SuperMethodReference e = (SuperMethodReference) node;
        MethodReferenceTreeImpl t;
        if (e.getQualifier() != null) {
          t = new MethodReferenceTreeImpl(
            new MemberSelectExpressionTreeImpl(
              convertExpression(e.getQualifier()),
              firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
              new IdentifierTreeImpl(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper))
            ),
            firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameCOLON_COLON)
          );
        } else {
          t = new MethodReferenceTreeImpl(
            new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNamesuper)),
            firstTokenIn(e, TerminalTokens.TokenNameCOLON_COLON)
          );
        }
        t.complete(
          convertTypeArguments(e.typeArguments()),
          convertSimpleName(e.getName())
        );
        return t;
      }
      case ASTNode.SWITCH_EXPRESSION: {
        SwitchExpression e = (SwitchExpression) node;
        return new SwitchExpressionTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameswitch),
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameLBRACE),
          convertSwitchStatements(e.statements()),
          lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
        );
      }
      case ASTNode.NULL_LITERAL: {
        NullLiteral e = (NullLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.NULL_LITERAL,
          firstTokenIn(e, TerminalTokens.TokenNamenull)
        );
      }
      case ASTNode.NUMBER_LITERAL: {
        NumberLiteral e = (NumberLiteral) node;
        int tokenIndex = tokenManager.findIndex(e.getStartPosition(), ANY_TOKEN, true);
        int tokenType = tokenManager.get(tokenIndex).tokenType;
        boolean unaryMinus = tokenType == TerminalTokens.TokenNameMINUS;
        if (unaryMinus) {
          tokenIndex++;
          tokenType = tokenManager.get(tokenIndex).tokenType;
        }
        ExpressionTree result;
        switch (tokenType) {
          default:
            throw new IllegalStateException();
          case TerminalTokens.TokenNameIntegerLiteral:
            result = new LiteralTreeImpl(Tree.Kind.INT_LITERAL, createSyntaxToken(tokenIndex));
            break;
          case TerminalTokens.TokenNameLongLiteral:
            result = new LiteralTreeImpl(Tree.Kind.LONG_LITERAL, createSyntaxToken(tokenIndex));
            break;
          case TerminalTokens.TokenNameFloatingPointLiteral:
            result = new LiteralTreeImpl(Tree.Kind.FLOAT_LITERAL, createSyntaxToken(tokenIndex));
            break;
          case TerminalTokens.TokenNameDoubleLiteral:
            result = new LiteralTreeImpl(Tree.Kind.DOUBLE_LITERAL, createSyntaxToken(tokenIndex));
            break;
        }
        if (unaryMinus) {
          result = new InternalPrefixUnaryExpression(Tree.Kind.UNARY_MINUS, createSyntaxToken(tokenIndex - 1), result);
        }
        return result;
      }
      case ASTNode.CHARACTER_LITERAL: {
        CharacterLiteral e = (CharacterLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.CHAR_LITERAL,
          firstTokenIn(e, TerminalTokens.TokenNameCharacterLiteral)
        );
      }
      case ASTNode.BOOLEAN_LITERAL: {
        BooleanLiteral e = (BooleanLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.BOOLEAN_LITERAL,
          firstTokenIn(e, e.booleanValue() ? TerminalTokens.TokenNametrue : TerminalTokens.TokenNamefalse)
        );
      }
      case ASTNode.STRING_LITERAL: {
        StringLiteral e = (StringLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.STRING_LITERAL,
          firstTokenIn(e, TerminalTokens.TokenNameStringLiteral)
        );
      }
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION: {
        Annotation e = (Annotation) node;
        ArgumentListTreeImpl arguments = new ArgumentListTreeImpl(
          new ArrayList<>(), new ArrayList<>()
        );
        if (e.getNodeType() == ASTNode.SINGLE_MEMBER_ANNOTATION) {
          arguments.add(
            convertExpression(((SingleMemberAnnotation) e).getValue())
          );
          arguments.complete(
            firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
            lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
          );
        } else if (e.getNodeType() == ASTNode.NORMAL_ANNOTATION) {
          for (int i = 0; i < ((NormalAnnotation) e).values().size(); i++) {
            MemberValuePair o = (MemberValuePair) ((NormalAnnotation) e).values().get(i);
            arguments.add(new AssignmentExpressionTreeImpl(
              Tree.Kind.ASSIGNMENT,
              convertSimpleName(o.getName()),
              firstTokenAfter(o.getName(), TerminalTokens.TokenNameEQUAL),
              convertExpression(o.getValue())
            ));
            if (i < ((NormalAnnotation) e).values().size() - 1) {
              arguments.separators().add(
                firstTokenAfter(o, TerminalTokens.TokenNameCOMMA)
              );
            }
          }
          arguments.complete(
            firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
            lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
          );
        }
        return new AnnotationTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameAT),
          (TypeTree) convertExpression(e.getTypeName()),
          arguments
        );
      }
    }
  }

  private TypeTree convertType(@Nullable Type node) {
    if (node == null) {
      // e.g. return type of constructor
      return null;
    }
    switch (node.getNodeType()) {
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
      case ASTNode.PRIMITIVE_TYPE: {
        PrimitiveType e = (PrimitiveType) node;
        final JavaTree.PrimitiveTypeTreeImpl t;
        switch (e.getPrimitiveTypeCode().toString()) {
          default:
            throw new IllegalStateException(e.getPrimitiveTypeCode().toString());
          case "byte":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamebyte));
            break;
          case "short":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNameshort));
            break;
          case "char":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamechar));
            break;
          case "int":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNameint));
            break;
          case "long":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamelong));
            break;
          case "float":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamefloat));
            break;
          case "double":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamedouble));
            break;
          case "boolean":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNameboolean));
            break;
          case "void":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamevoid));
            break;
        }
        t.complete(
          convertAnnotations(e.annotations())
        );
        return t;
      }
      case ASTNode.SIMPLE_TYPE: {
        SimpleType e = (SimpleType) node;
        List<AnnotationTree> annotations = new ArrayList<>();
        for (Object o : e.annotations()) {
          annotations.add((AnnotationTree) convertExpression(
            ((Annotation) o)
          ));
        }
        JavaTree.AnnotatedTypeTree t = (JavaTree.AnnotatedTypeTree) convertExpression(e.getName());
        if (t instanceof IdentifierTree && ((IdentifierTree) t).name().equals("var")) {
          // TODO can't be annotated?
          return new VarTypeTreeImpl((InternalSyntaxToken) ((IdentifierTree) t).identifierToken());
        }
        t.complete(annotations);
        return t;
      }
      case ASTNode.UNION_TYPE: {
        UnionType e = (UnionType) node;
        TypeUnionListTreeImpl alternatives = new TypeUnionListTreeImpl(
          new ArrayList<>(), new ArrayList<>()
        );
        for (int i = 0; i < e.types().size(); i++) {
          Type o = (Type) e.types().get(i);
          alternatives.add(convertType(o));
          if (i < e.types().size() - 1) {
            alternatives.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameOR));
          }
        }
        return new JavaTree.UnionTypeTreeImpl(alternatives);
      }
      case ASTNode.ARRAY_TYPE: {
        ArrayType e = (ArrayType) node;
        TypeTree t = convertType(e.getElementType());
        int tokenIndex = tokenManager.firstIndexAfter(e.getElementType(), TerminalTokens.TokenNameLBRACKET);
        for (int i = 0; i < e.dimensions().size(); i++) {
          if (i > 0) {
            tokenIndex = nextTokenIndex(tokenIndex, TerminalTokens.TokenNameLBRACKET);
          }
          t = new JavaTree.ArrayTypeTreeImpl(
            t,
            (List) convertAnnotations(((Dimension) e.dimensions().get(i)).annotations()),
            createSyntaxToken(tokenIndex),
            createSyntaxToken(nextTokenIndex(tokenIndex, TerminalTokens.TokenNameRBRACKET))
          );
        }
        return t;
      }
      case ASTNode.PARAMETERIZED_TYPE: {
        ParameterizedType e = (ParameterizedType) node;
        int pos = e.getStartPosition() + e.getLength() - 1;
        return new JavaTree.ParameterizedTypeTreeImpl(
          convertType(e.getType()),
          convertTypeArguments(
            firstTokenAfter(e.getType(), TerminalTokens.TokenNameLESS),
            e.typeArguments(),
            new InternalSyntaxToken(
              compilationUnit.getLineNumber(pos),
              compilationUnit.getColumnNumber(pos),
              ">",
              /* TODO */ Collections.emptyList(),
              0, 0, false
            )
          )
        );
      }
      case ASTNode.QUALIFIED_TYPE: {
        QualifiedType e = (QualifiedType) node;
        MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
          (ExpressionTree) convertType(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          convertSimpleName(e.getName())
        );
        ((IdentifierTreeImpl) t.identifier()).complete(
          convertAnnotations(e.annotations())
        );
        return t;
      }
      case ASTNode.NAME_QUALIFIED_TYPE: {
        NameQualifiedType e = (NameQualifiedType) node;
        MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
          convertExpression(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          convertSimpleName(e.getName())
        );
        ((IdentifierTreeImpl) t.identifier()).complete(
          convertAnnotations(e.annotations())
        );
        return t;
      }
      case ASTNode.WILDCARD_TYPE: {
        WildcardType e = (WildcardType) node;
        final InternalSyntaxToken questionToken = e.annotations().isEmpty()
          ? firstTokenIn(e, TerminalTokens.TokenNameQUESTION)
          : firstTokenAfter((ASTNode) e.annotations().get(e.annotations().size() - 1), TerminalTokens.TokenNameQUESTION);
        JavaTree.WildcardTreeImpl t;
        if (e.getBound() == null) {
          t = new JavaTree.WildcardTreeImpl(
            questionToken
          );
        } else {
          t = new JavaTree.WildcardTreeImpl(
            e.isUpperBound() ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD,
            e.isUpperBound() ? firstTokenBefore(e.getBound(), TerminalTokens.TokenNameextends) : firstTokenBefore(e.getBound(), TerminalTokens.TokenNamesuper),
            convertType(e.getBound())
          ).complete(
            questionToken
          );
        }
        t.complete(
          convertAnnotations(e.annotations())
        );
        return t;
      }
    }
  }

  private List<AnnotationTree> convertAnnotations(List e) {
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Object o : e) {
      annotations.add((AnnotationTree) convertExpression(
        ((Annotation) o)
      ));
    }
    return annotations;
  }

  private ModifiersTreeImpl convertModifiers(List source) {
    List<ModifierTree> modifiers = new ArrayList<>();
    for (Object o : source) {
      modifiers.add(convertModifier((IExtendedModifier) o));
    }
    return new ModifiersTreeImpl(modifiers);
  }

  private ModifierTree convertModifier(IExtendedModifier node) {
    switch (((ASTNode) node).getNodeType()) {
      default:
        throw new IllegalStateException();
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION:
        return (AnnotationTree) convertExpression((Expression) node);
      case ASTNode.MODIFIER: {
        org.eclipse.jdt.core.dom.Modifier e = (org.eclipse.jdt.core.dom.Modifier) node;
        switch (e.getKeyword().toString()) {
          default:
            throw new IllegalStateException(e.getKeyword().toString());
          case "public":
            return new ModifierKeywordTreeImpl(Modifier.PUBLIC, firstTokenIn(e, TerminalTokens.TokenNamepublic));
          case "protected":
            return new ModifierKeywordTreeImpl(Modifier.PROTECTED, firstTokenIn(e, TerminalTokens.TokenNameprotected));
          case "private":
            return new ModifierKeywordTreeImpl(Modifier.PRIVATE, firstTokenIn(e, TerminalTokens.TokenNameprivate));
          case "static":
            return new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn(e, TerminalTokens.TokenNamestatic));
          case "abstract":
            return new ModifierKeywordTreeImpl(Modifier.ABSTRACT, firstTokenIn(e, TerminalTokens.TokenNameabstract));
          case "final":
            return new ModifierKeywordTreeImpl(Modifier.FINAL, firstTokenIn(e, TerminalTokens.TokenNamefinal));
          case "native":
            return new ModifierKeywordTreeImpl(Modifier.NATIVE, firstTokenIn(e, TerminalTokens.TokenNamenative));
          case "synchronized":
            return new ModifierKeywordTreeImpl(Modifier.SYNCHRONIZED, firstTokenIn(e, TerminalTokens.TokenNamesynchronized));
          case "transient":
            return new ModifierKeywordTreeImpl(Modifier.TRANSIENT, firstTokenIn(e, TerminalTokens.TokenNametransient));
          case "volatile":
            return new ModifierKeywordTreeImpl(Modifier.VOLATILE, firstTokenIn(e, TerminalTokens.TokenNamevolatile));
          case "strictfp":
            return new ModifierKeywordTreeImpl(Modifier.STRICTFP, firstTokenIn(e, TerminalTokens.TokenNamestrictfp));
          case "default":
            return new ModifierKeywordTreeImpl(Modifier.DEFAULT, firstTokenIn(e, TerminalTokens.TokenNamedefault));
        }
      }
    }
  }

  private static final int ANY_TOKEN = -1;

  private static final Map<Object, Op> operators = new HashMap<>();

  private static class Op {
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
    operators.put(PrefixExpression.Operator.INCREMENT, new Op(Tree.Kind.PREFIX_INCREMENT, TerminalTokens.TokenNamePLUS_PLUS));

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
