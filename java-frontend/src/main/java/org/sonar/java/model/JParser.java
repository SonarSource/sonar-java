/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jdt.internal.formatter.Token;
import org.eclipse.jdt.internal.formatter.TokenManager;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
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
import org.sonar.java.model.pattern.DefaultPatternTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.pattern.NullPatternTreeImpl;
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
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JParser {

  private static final Logger LOG = Loggers.get(JParser.class);

  private static final Predicate<IProblem> IS_SYNTAX_ERROR = error -> (error.getID() & IProblem.Syntax) != 0;
  private static final Predicate<IProblem> IS_UNDEFINED_TYPE_ERROR = error -> (error.getID() & IProblem.UndefinedType) != 0;

  /**
   * @param unitName see {@link ASTParser#setUnitName(String)}
   * @throws RecognitionException in case of syntax errors
   */
  public static JavaTree.CompilationUnitTreeImpl parse(ASTParser astParser, String version, String unitName, String source) {
    astParser.setUnitName(unitName);
    astParser.setSource(source.toCharArray());

    CompilationUnit astNode;
    try {
      astNode = (CompilationUnit) astParser.createAST(null);
    } catch (Exception e) {
      LOG.error("ECJ: Unable to parse file", e);
      throw new RecognitionException(-1, "ECJ: Unable to parse file.", e);
    }

    return convert(version, unitName, source, astNode);
  }

  static JavaTree.CompilationUnitTreeImpl convert(String version, String unitName, String source, CompilationUnit astNode) {
    List<IProblem> errors = Stream.of(astNode.getProblems()).filter(IProblem::isError).collect(Collectors.toList());
    Optional<IProblem> possibleSyntaxError = errors.stream().filter(IS_SYNTAX_ERROR).findFirst();
    if (possibleSyntaxError.isPresent()) {
      IProblem syntaxError = possibleSyntaxError.get();
      int line = syntaxError.getSourceLineNumber();
      int column = astNode.getColumnNumber(syntaxError.getSourceStart());
      String message = String.format("Parse error at line %d column %d: %s", line, column, syntaxError.getMessage());
      // interrupt parsing
      throw new RecognitionException(line, message);
    }

    Set<JProblem> undefinedTypes = errors.stream()
      .filter(IS_UNDEFINED_TYPE_ERROR)
      .map(i -> new JProblem(
        i.getMessage(),
        (i.getID() & IProblem.PreviewFeatureUsed) != 0 ? JProblem.Type.PREVIEW_FEATURE_USED : JProblem.Type.UNDEFINED_TYPE))
      .collect(Collectors.toSet());

    JParser converter = new JParser();
    converter.sema = new JSema(astNode.getAST());
    converter.sema.undefinedTypes.addAll(undefinedTypes);
    converter.compilationUnit = astNode;
    converter.tokenManager = new TokenManager(lex(version, unitName, source.toCharArray()), source, new DefaultCodeFormatterOptions(new HashMap<>()));

    JavaTree.CompilationUnitTreeImpl tree = converter.convertCompilationUnit(astNode);
    tree.sema = converter.sema;
    JWarning.Mapper.warningsFor(astNode).mappedInto(tree);

    ASTUtils.mayTolerateMissingType(astNode.getAST());

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

  private JSema sema;

  private final Deque<JLabelSymbol> labels = new LinkedList<>();

  private void declaration(@Nullable IBinding binding, Tree node) {
    if (binding == null) {
      return;
    }
    sema.declarations.put(binding, node);
  }

  private void usage(@Nullable IBinding binding, IdentifierTree node) {
    if (binding == null) {
      return;
    }
    binding = JSema.declarationBinding(binding);
    sema.usages.computeIfAbsent(binding, k -> new ArrayList<>()).add(node);
  }

  private void usageLabel(@Nullable IdentifierTreeImpl node) {
    if (node == null) {
      return;
    }
    labels.stream()
      .filter(symbol -> symbol.name().equals(node.name()))
      .findFirst()
      .ifPresent(labelSymbol -> {
        labelSymbol.usages.add(node);
        node.labelSymbol = labelSymbol;
      });
  }

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
        return new InternalSyntaxToken(1, 0, "", collectComments(tokenIndex), true);
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
      return new InternalSyntaxToken(line, column, "", collectComments(tokenIndex), true);
    }
    return new InternalSyntaxToken(
      compilationUnit.getLineNumber(t.originalStart),
      compilationUnit.getColumnNumber(t.originalStart),
      t.toString(tokenManager.getSource()),
      collectComments(tokenIndex),
      false
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
      false
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

  private void addEmptyStatementsToList(int tokenIndex, List list) {
    while (true) {
      Token token;
      do {
        tokenIndex++;
        token = tokenManager.get(tokenIndex);
      } while (token.isComment());

      if (token.tokenType != TerminalTokens.TokenNameSEMICOLON) {
        break;
      }
      list.add(new EmptyStatementTreeImpl(createSyntaxToken(tokenIndex)));
    }
  }

  private JavaTree.CompilationUnitTreeImpl convertCompilationUnit(CompilationUnit e) {
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
      ExpressionTree name = convertImportName(e2.getName());
      if (e2.isOnDemand()) {
        name = new MemberSelectExpressionTreeImpl(
          name,
          lastTokenIn(e2, TerminalTokens.TokenNameDOT),
          new IdentifierTreeImpl(lastTokenIn(e2, TerminalTokens.TokenNameMULTIPLY))
        );
      }
      JavaTree.ImportTreeImpl t = new JavaTree.ImportTreeImpl(
        firstTokenIn(e2, TerminalTokens.TokenNameimport),
        e2.isStatic() ? firstTokenIn(e2, TerminalTokens.TokenNamestatic) : null,
        name,
        lastTokenIn(e2, TerminalTokens.TokenNameSEMICOLON)
      );
      t.binding = e2.resolveBinding();
      imports.add(t);

      int tokenIndex = tokenManager.lastIndexIn(e2, TerminalTokens.TokenNameSEMICOLON);
      addEmptyStatementsToList(tokenIndex, imports);
    }

    List<Tree> types = new ArrayList<>();
    for (Object type : e.types()) {
      processBodyDeclaration((AbstractTypeDeclaration) type, types);
    }

    if (e.imports().isEmpty() && e.types().isEmpty()) {
      addEmptyStatementsToList(-1, imports);
    }

    return new JavaTree.CompilationUnitTreeImpl(
      packageDeclaration,
      imports,
      types,
      convertModuleDeclaration(compilationUnit.getModule()),
      firstTokenAfter(e, TerminalTokens.TokenNameEOF)
    );
  }

  private ExpressionTree convertImportName(Name node) {
    switch (node.getNodeType()) {
      case ASTNode.SIMPLE_NAME: {
        return new IdentifierTreeImpl(firstTokenIn(node, TerminalTokens.TokenNameIdentifier));
      }
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        return new MemberSelectExpressionTreeImpl(
          convertImportName(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          (IdentifierTreeImpl) convertImportName(e.getName()));
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
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
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        ModuleNameTreeImpl t = convertModuleName(e.getQualifier());
        t.add(new IdentifierTreeImpl(firstTokenIn(e.getName(), TerminalTokens.TokenNameIdentifier)));
        return t;
      }
      case ASTNode.SIMPLE_NAME: {
        ModuleNameTreeImpl t = ModuleNameTreeImpl.emptyList();
        t.add(new IdentifierTreeImpl(firstTokenIn(node, TerminalTokens.TokenNameIdentifier)));
        return t;
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private ModuleNameListTreeImpl convertModuleNames(List<?> list) {
    ModuleNameListTreeImpl t = ModuleNameListTreeImpl.emptyList();
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
      case ASTNode.REQUIRES_DIRECTIVE: {
        RequiresDirective e = (RequiresDirective) node;
        List<ModifierTree> modifiers = new ArrayList<>();
        for (Object o : e.modifiers()) {
          switch (((ModuleModifier) o).getKeyword().toString()) {
            case "static":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
            case "transitive":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.TRANSITIVE, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
            default:
              throw new IllegalStateException();
          }
        }
        return new RequiresDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamerequires),
          new ModifiersTreeImpl(modifiers),
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
        QualifiedIdentifierListTreeImpl typeNames = QualifiedIdentifierListTreeImpl.emptyList();
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
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private ClassTreeImpl convertTypeDeclaration(AbstractTypeDeclaration e) {
    List<Tree> members = new ArrayList<>();

    int leftBraceTokenIndex = findLeftBraceTokenIndex(e);
    addEmptyStatementsToList(leftBraceTokenIndex, members);

    for (Object o : e.bodyDeclarations()) {
      processBodyDeclaration((BodyDeclaration) o, members);
    }

    ModifiersTreeImpl modifiers = convertModifiers(e.modifiers());
    IdentifierTreeImpl name = createSimpleName(e.getName());

    InternalSyntaxToken openBraceToken = createSyntaxToken(leftBraceTokenIndex);
    InternalSyntaxToken closeBraceToken = lastTokenIn(e, TerminalTokens.TokenNameRBRACE);

    final ClassTreeImpl t;
    switch (e.getNodeType()) {
      case ASTNode.TYPE_DECLARATION:
        t = convertTypeDeclaration((TypeDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      case ASTNode.ENUM_DECLARATION:
        t = convertEnumDeclaration((EnumDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      case ASTNode.RECORD_DECLARATION:
        t = convertRecordDeclaration((RecordDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
        t = convertAnnotationTypeDeclaration((AnnotationTypeDeclaration) e, modifiers, name, openBraceToken, members, closeBraceToken);
        break;
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(e.getNodeType()).toString());
    }

    // no-op for annotation-types
    completeSuperInterfaces(e, t);

    t.typeBinding = e.resolveBinding();
    declaration(t.typeBinding, t);

    return t;
  }

  private ClassTreeImpl convertTypeDeclaration(TypeDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
                                               InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), e.isInterface() ? TerminalTokens.TokenNameinterface : TerminalTokens.TokenNameclass);
    ClassTreeImpl t = new ClassTreeImpl(e.isInterface() ? Tree.Kind.INTERFACE : Tree.Kind.CLASS, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name)
      .completeTypeParameters(convertTypeParameters(e.typeParameters()));

    if (!e.permittedTypes().isEmpty()) {
      List permittedTypes = e.permittedTypes();
      InternalSyntaxToken permitsKeyword = firstTokenBefore((Type) permittedTypes.get(0), TerminalTokens.TokenNameRestrictedIdentifierpermits);
      QualifiedIdentifierListTreeImpl classPermittedTypes = QualifiedIdentifierListTreeImpl.emptyList();

      convertSeparatedTypeList(permittedTypes, classPermittedTypes);
      t.completePermittedTypes(permitsKeyword, classPermittedTypes);
    }

    if (!e.isInterface() && e.getSuperclassType() != null) {
      Type superclassType = e.getSuperclassType();
      t.completeSuperclass(firstTokenBefore(superclassType, TerminalTokens.TokenNameextends), convertType(superclassType));
    }
    return t;
  }

  private ClassTreeImpl convertEnumDeclaration(EnumDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
                                               InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    List<Tree> enumConstants = new ArrayList<>();
    for (Object o : e.enumConstants()) {
      // introduced as first members
      enumConstants.add(processEnumConstantDeclaration((EnumConstantDeclaration) o));
    }
    members.addAll(0, enumConstants);

    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), TerminalTokens.TokenNameenum);
    return new ClassTreeImpl(Tree.Kind.ENUM, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name);
  }

  private ClassTreeImpl convertAnnotationTypeDeclaration(AnnotationTypeDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
    InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), TerminalTokens.TokenNameinterface);
    return new ClassTreeImpl(Tree.Kind.ANNOTATION_TYPE, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name)
      .completeAtToken(firstTokenBefore(e.getName(), TerminalTokens.TokenNameAT));
  }

  private ClassTreeImpl convertRecordDeclaration(RecordDeclaration e, ModifiersTreeImpl modifiers, IdentifierTreeImpl name,
                                                 InternalSyntaxToken openBraceToken, List<Tree> members, InternalSyntaxToken closeBraceToken) {
    InternalSyntaxToken declarationKeyword = firstTokenBefore(e.getName(), TerminalTokens.TokenNameRestrictedIdentifierrecord);
    return new ClassTreeImpl(Tree.Kind.RECORD, openBraceToken, members, closeBraceToken)
      .complete(modifiers, declarationKeyword, name)
      .completeTypeParameters(convertTypeParameters(e.typeParameters()))
      .completeRecordComponents(convertRecordComponents(e));
  }

  private List<VariableTree> convertRecordComponents(RecordDeclaration e) {
    List<VariableTree> recordComponents = new ArrayList<>();

    for (int i = 0; i < e.recordComponents().size(); i++) {
      SingleVariableDeclaration o = (SingleVariableDeclaration) e.recordComponents().get(i);
      VariableTreeImpl recordComponent = convertVariable(o);
      if (i < e.recordComponents().size() - 1) {
        recordComponent.setEndToken(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
      }
      recordComponents.add(recordComponent);
    }
    return recordComponents;
  }

  private int findLeftBraceTokenIndex(AbstractTypeDeclaration e) {
    // TODO try to simplify, note that type annotations can contain LBRACE
    if (e.getNodeType() == ASTNode.ENUM_DECLARATION) {
      EnumDeclaration enumDeclaration = (EnumDeclaration) e;
      if (!enumDeclaration.enumConstants().isEmpty()) {
        return tokenManager.firstIndexBefore((ASTNode) enumDeclaration.enumConstants().get(0), TerminalTokens.TokenNameLBRACE);
      }
      if (!enumDeclaration.bodyDeclarations().isEmpty()) {
        return tokenManager.firstIndexBefore((ASTNode) e.bodyDeclarations().get(0), TerminalTokens.TokenNameLBRACE);
      }
      return tokenManager.lastIndexIn(e, TerminalTokens.TokenNameLBRACE);
    }
    if (!e.bodyDeclarations().isEmpty()) {
      return tokenManager.firstIndexBefore((ASTNode) e.bodyDeclarations().get(0), TerminalTokens.TokenNameLBRACE);
    }
    return tokenManager.lastIndexIn(e, TerminalTokens.TokenNameLBRACE);
  }

  private void completeSuperInterfaces(AbstractTypeDeclaration e, ClassTreeImpl t) {
    List superInterfaces = superInterfaceTypes(e);
    if (!superInterfaces.isEmpty()) {
      QualifiedIdentifierListTreeImpl interfaces = QualifiedIdentifierListTreeImpl.emptyList();
      convertSeparatedTypeList(superInterfaces, interfaces);

      ASTNode firstInterface = (ASTNode) superInterfaces.get(0);
      InternalSyntaxToken keyword = firstTokenBefore(firstInterface, t.is(Tree.Kind.INTERFACE) ? TerminalTokens.TokenNameextends : TerminalTokens.TokenNameimplements);
      t.completeInterfaces(keyword, interfaces);
    }
  }

  private static List<?> superInterfaceTypes(AbstractTypeDeclaration e) {
    switch (e.getNodeType()) {
      case ASTNode.TYPE_DECLARATION:
        return ((TypeDeclaration) e).superInterfaceTypes();
      case ASTNode.ENUM_DECLARATION:
        return ((EnumDeclaration) e).superInterfaceTypes();
      case ASTNode.RECORD_DECLARATION:
        return ((RecordDeclaration) e).superInterfaceTypes();
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
        return Collections.emptyList();
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(e.getNodeType()).toString());
    }
  }

  private <T extends Tree> void convertSeparatedTypeList(List<? extends Type> source, ListTree<T> target) {
    for (int i = 0; i < source.size(); i++) {
      Type o = source.get(i);
      T tree = (T) convertType(o);
      if (i > 0) {
        target.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      target.add(tree);
    }
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
      classBody.typeBinding = e.getAnonymousClassDeclaration().resolveBinding();
      declaration(classBody.typeBinding, classBody);
    }

    final int separatorTokenIndex = firstTokenIndexAfter(e);
    final InternalSyntaxToken separatorToken;
    switch (tokenManager.get(separatorTokenIndex).tokenType) {
      case TerminalTokens.TokenNameCOMMA:
      case TerminalTokens.TokenNameSEMICOLON:
        separatorToken = createSyntaxToken(separatorTokenIndex);
        break;
      case TerminalTokens.TokenNameRBRACE:
        separatorToken = null;
        break;
      default:
        throw new IllegalStateException();
    }

    IdentifierTreeImpl identifier = createSimpleName(e.getName());
    if (e.getAnonymousClassDeclaration() == null) {
      identifier.binding = excludeRecovery(e.resolveConstructorBinding(), arguments.size());
    } else {
      identifier.binding = findConstructorForAnonymousClass(e.getAST(), identifier.typeBinding, e.resolveConstructorBinding());
    }
    usage(identifier.binding, identifier);

    EnumConstantTreeImpl t = new EnumConstantTreeImpl(
      convertModifiers(e.modifiers()),
      identifier,
      new NewClassTreeImpl(identifier, arguments, classBody),
      separatorToken
    );
    t.variableBinding = e.resolveVariable();
    declaration(t.variableBinding, t);
    return t;
  }

  private void processBodyDeclaration(BodyDeclaration node, List<Tree> members) {
    final int lastTokenIndex;

    switch (node.getNodeType()) {
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
      case ASTNode.ENUM_DECLARATION:
      case ASTNode.RECORD_DECLARATION:
      case ASTNode.TYPE_DECLARATION:
        lastTokenIndex = processTypeDeclaration((AbstractTypeDeclaration) node, members);
        break;
      case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
        lastTokenIndex = processAnnotationTypeMemberDeclaration((AnnotationTypeMemberDeclaration) node, members);
        break;
      case ASTNode.INITIALIZER:
        lastTokenIndex = processInitializerDeclaration((Initializer) node, members);
        break;
      case ASTNode.METHOD_DECLARATION:
        lastTokenIndex = processMethodDeclaration((MethodDeclaration) node, members);
        break;
      case ASTNode.FIELD_DECLARATION:
        lastTokenIndex = processFieldDeclaration((FieldDeclaration) node, members);
        break;
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }

    addEmptyStatementsToList(lastTokenIndex, members);
  }

  private int processTypeDeclaration(AbstractTypeDeclaration node, List<Tree> members) {
    members.add(convertTypeDeclaration(node));
    return tokenManager.lastIndexIn(node, TerminalTokens.TokenNameRBRACE);
  }

  private int processAnnotationTypeMemberDeclaration(AnnotationTypeMemberDeclaration e, List<Tree> members) {
    FormalParametersListTreeImpl parameters = new FormalParametersListTreeImpl(
      firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
      firstTokenAfter(e.getName(), TerminalTokens.TokenNameRPAREN));

    Expression defaultExpression = e.getDefault();
    InternalSyntaxToken defaultToken = defaultExpression == null ? null : firstTokenBefore(defaultExpression, TerminalTokens.TokenNamedefault);
    ExpressionTree defaultValue = defaultExpression == null ? null : convertExpression(defaultExpression);

    MethodTreeImpl t = new MethodTreeImpl(parameters, defaultToken, defaultValue)
      .complete(convertType(e.getType()), createSimpleName(e.getName()), lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON))
      .completeWithModifiers(convertModifiers(e.modifiers()));

    t.methodBinding = e.resolveBinding();
    declaration(t.methodBinding, t);

    members.add(t);
    return tokenManager.lastIndexIn(e, TerminalTokens.TokenNameSEMICOLON);
  }

  private int processInitializerDeclaration(Initializer e, List<Tree> members) {
    BlockTreeImpl blockTree = convertBlock(e.getBody());
    if (org.eclipse.jdt.core.dom.Modifier.isStatic(e.getModifiers())) {
      members.add(new StaticInitializerTreeImpl(
        firstTokenIn(e, TerminalTokens.TokenNamestatic),
        (InternalSyntaxToken) blockTree.openBraceToken(),
        blockTree.body(),
        (InternalSyntaxToken) blockTree.closeBraceToken()));
    } else {
      members.add(new BlockTreeImpl(
        Tree.Kind.INITIALIZER,
        (InternalSyntaxToken) blockTree.openBraceToken(),
        blockTree.body(),
        (InternalSyntaxToken) blockTree.closeBraceToken()));
    }
    return tokenManager.lastIndexIn(e, TerminalTokens.TokenNameRBRACE);
  }

  private int processMethodDeclaration(MethodDeclaration e, List<Tree> members) {
    List p = e.parameters();
    final FormalParametersListTreeImpl formalParameters;
    if (e.isCompactConstructor()) {
      // only used for records
      formalParameters = new FormalParametersListTreeImpl(null, null);
    } else {
      InternalSyntaxToken openParen = firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN);
      InternalSyntaxToken closeParen = firstTokenAfter(p.isEmpty() ? e.getName() : (ASTNode) p.get(p.size() - 1), TerminalTokens.TokenNameRPAREN);
      formalParameters = new FormalParametersListTreeImpl(openParen, closeParen);
    }

    for (int i = 0; i < p.size(); i++) {
      SingleVariableDeclaration o = (SingleVariableDeclaration) p.get(i);
      VariableTreeImpl parameter = convertVariable(o);
      if (i < p.size() - 1) {
        parameter.setEndToken(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
      }
      formalParameters.add(parameter);
    }

    QualifiedIdentifierListTreeImpl thrownExceptionTypes = QualifiedIdentifierListTreeImpl.emptyList();
    List tt = e.thrownExceptionTypes();
    convertSeparatedTypeList(tt, thrownExceptionTypes);

    Block body = e.getBody();
    Type returnType = e.getReturnType2();
    InternalSyntaxToken throwsToken = tt.isEmpty() ? null : firstTokenBefore((Type) tt.get(0), TerminalTokens.TokenNamethrows);
    InternalSyntaxToken semcolonToken = body == null ? lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON) : null;
    MethodTreeImpl t = new MethodTreeImpl(
      returnType == null ? null : applyExtraDimensions(convertType(returnType), e.extraDimensions()),
      createSimpleName(e.getName()),
      formalParameters,
      throwsToken,
      thrownExceptionTypes,
      body == null ? null : convertBlock(body),
      semcolonToken
    ).completeWithModifiers(
      convertModifiers(e.modifiers())
    ).completeWithTypeParameters(
      convertTypeParameters(e.typeParameters())
    );
    t.methodBinding = e.resolveBinding();
    declaration(t.methodBinding, t);

    members.add(t);
    return tokenManager.lastIndexIn(e, body == null ? TerminalTokens.TokenNameSEMICOLON : TerminalTokens.TokenNameRBRACE);
  }

  private int processFieldDeclaration(FieldDeclaration fieldDeclaration, List<Tree> members) {
    ModifiersTreeImpl modifiers = convertModifiers(fieldDeclaration.modifiers());
    TypeTree type = convertType(fieldDeclaration.getType());

    for (int i = 0; i < fieldDeclaration.fragments().size(); i++) {
      VariableDeclarationFragment fragment = (VariableDeclarationFragment) fieldDeclaration.fragments().get(i);
      VariableTreeImpl t = new VariableTreeImpl(createSimpleName(fragment.getName()))
        .completeModifiersAndType(modifiers, applyExtraDimensions(type, fragment.extraDimensions()));

      if (fragment.getInitializer() != null) {
        t.completeTypeAndInitializer(t.type(), firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL), convertExpression(fragment.getInitializer()));
      }

      t.setEndToken(firstTokenAfter(fragment, i + 1 < fieldDeclaration.fragments().size() ? TerminalTokens.TokenNameCOMMA : TerminalTokens.TokenNameSEMICOLON));
      t.variableBinding = fragment.resolveBinding();
      declaration(t.variableBinding, t);

      members.add(t);
    }
    return tokenManager.lastIndexIn(fieldDeclaration, TerminalTokens.TokenNameSEMICOLON);
  }

  private ArgumentListTreeImpl convertArguments(@Nullable InternalSyntaxToken openParen, List<?> list, @Nullable InternalSyntaxToken closeParen) {
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList().complete(openParen, closeParen);
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
  private TypeArgumentListTreeImpl convertTypeArguments(List<?> list) {
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

  private TypeArgumentListTreeImpl convertTypeArguments(InternalSyntaxToken l, List<?> list, InternalSyntaxToken g) {
    TypeArgumentListTreeImpl typeArguments = new TypeArgumentListTreeImpl(l, g);
    for (int i = 0; i < list.size(); i++) {
      Type o = (Type) list.get(i);
      if (i > 0) {
        typeArguments.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      typeArguments.add(convertType(o));
    }
    return typeArguments;
  }

  private TypeParameterListTreeImpl convertTypeParameters(List<?> list) {
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
    IdentifierTreeImpl i = createSimpleName(e.getName());
    // TODO why ECJ uses IExtendedModifier here instead of Annotation ?
    i.complete(convertAnnotations(e.modifiers()));
    TypeParameterTreeImpl t;
    List<?> typeBounds = e.typeBounds();
    if (typeBounds.isEmpty()) {
      t = new TypeParameterTreeImpl(i);
    } else {
      QualifiedIdentifierListTreeImpl bounds = QualifiedIdentifierListTreeImpl.emptyList();
      for (int j = 0; j < typeBounds.size(); j++) {
        Object o = typeBounds.get(j);
        bounds.add(convertType((Type) o));
        if (j < typeBounds.size() - 1) {
          bounds.separators().add(firstTokenAfter((ASTNode) o, TerminalTokens.TokenNameAND));
        }
      }
      t = new TypeParameterTreeImpl(
        i,
        firstTokenAfter(e.getName(), TerminalTokens.TokenNameextends),
        bounds
      );
    }
    t.typeBinding = e.resolveBinding();
    return t;
  }

  /**
   * @param extraDimensions list of {@link org.eclipse.jdt.core.dom.Dimension}
   */
  private TypeTree applyExtraDimensions(TypeTree type, List<?> extraDimensions) {
    ITypeBinding typeBinding = ((AbstractTypedTree) type).typeBinding;
    for (int i = 0; i < extraDimensions.size(); i++) {
      Dimension e = (Dimension) extraDimensions.get(i);
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.annotations()),
        firstTokenIn(e, TerminalTokens.TokenNameLBRACKET),
        firstTokenIn(e, TerminalTokens.TokenNameRBRACKET)
      );
      if (typeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) type).typeBinding = typeBinding.createArrayType(i + 1);
      }
    }
    return type;
  }

  private VariableTreeImpl convertVariable(SingleVariableDeclaration e) {
    // TODO are extraDimensions and varargs mutually exclusive?
    TypeTree type = convertType(e.getType());
    type = applyExtraDimensions(type, e.extraDimensions());
    if (e.isVarargs()) {
      ITypeBinding typeBinding = ((AbstractTypedTree) type).typeBinding;
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.varargsAnnotations()),
        firstTokenAfter(e.getType(), TerminalTokens.TokenNameELLIPSIS)
      );
      if (typeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) type).typeBinding = typeBinding.createArrayType(1);
      }
    }

    VariableTreeImpl t = new VariableTreeImpl(
      convertModifiers(e.modifiers()),
      type,
      createSimpleName(e.getName())
    );
    if (e.getInitializer() != null) {
      t.completeTypeAndInitializer(
        t.type(),
        firstTokenAfter(e.getName(), TerminalTokens.TokenNameEQUAL),
        convertExpression(e.getInitializer())
      );
    }
    t.variableBinding = e.resolveBinding();
    declaration(t.variableBinding, t);
    return t;
  }

  private void addVariableToList(VariableDeclarationExpression e2, List list) {
    ModifiersTreeImpl modifiers = convertModifiers(e2.modifiers());
    TypeTree type = convertType(e2.getType());

    for (int i = 0; i < e2.fragments().size(); i++) {
      VariableDeclarationFragment fragment = (VariableDeclarationFragment) e2.fragments().get(i);
      VariableTreeImpl t = new VariableTreeImpl(createSimpleName(fragment.getName()));
      t.completeModifiers(modifiers);
      if (fragment.getInitializer() == null) {
        t.completeType(applyExtraDimensions(type, fragment.extraDimensions()));
      } else {
        t.completeTypeAndInitializer(
          applyExtraDimensions(type, fragment.extraDimensions()),
          firstTokenBefore(fragment.getInitializer(), TerminalTokens.TokenNameEQUAL),
          convertExpression(fragment.getInitializer())
        );
      }
      if (i < e2.fragments().size() - 1) {
        t.setEndToken(firstTokenAfter(fragment, TerminalTokens.TokenNameCOMMA));
      }
      t.variableBinding = fragment.resolveBinding();
      declaration(t.variableBinding, t);
      list.add(t);
    }
  }

  private VarTypeTreeImpl convertVarType(SimpleType simpleType) {
    VarTypeTreeImpl varTree = new VarTypeTreeImpl(firstTokenIn(simpleType.getName(), TerminalTokens.TokenNameIdentifier));
    varTree.typeBinding = simpleType.resolveBinding();
    return varTree;
  }

  private IdentifierTreeImpl createSimpleName(SimpleName e) {
    IdentifierTreeImpl t = new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNameIdentifier));
    t.typeBinding = e.resolveTypeBinding();
    t.binding = e.resolveBinding();
    return t;
  }

  private BlockTreeImpl convertBlock(Block e) {
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
        VariableTreeImpl t = new VariableTreeImpl(createSimpleName(fragment.getName()))
          .completeType(applyExtraDimensions(tType, fragment.extraDimensions()))
          .completeModifiers(modifiers);
        Expression initalizer = fragment.getInitializer();
        if (initalizer != null) {
          InternalSyntaxToken equalToken = firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL);
          t.completeTypeAndInitializer(t.type(), equalToken, convertExpression(initalizer));
        }
        int endTokenType = i < e.fragments().size() - 1 ? TerminalTokens.TokenNameCOMMA : TerminalTokens.TokenNameSEMICOLON;
        t.setEndToken(firstTokenAfter(fragment, endTokenType));

        t.variableBinding = fragment.resolveBinding();
        declaration(t.variableBinding, t);
        statements.add(t);
      }
    } else if (node.getNodeType() == ASTNode.BREAK_STATEMENT && node.getLength() < "break".length()) {
      // skip implicit break-statement
    } else {
      statements.add(createStatement(node));
    }
  }

  private StatementTree createStatement(Statement node) {
    switch (node.getNodeType()) {
      case ASTNode.BLOCK:
        return convertBlock((Block) node);
      case ASTNode.EMPTY_STATEMENT:
        return convertEmptyStatement((EmptyStatement) node);
      case ASTNode.RETURN_STATEMENT:
        return convertReturn((ReturnStatement) node);
      case ASTNode.FOR_STATEMENT:
        return convertFor((ForStatement) node);
      case ASTNode.WHILE_STATEMENT:
        return convertWhile((WhileStatement) node);
      case ASTNode.IF_STATEMENT:
        return convertIf((IfStatement) node);
      case ASTNode.BREAK_STATEMENT:
        return convertBreak((BreakStatement) node);
      case ASTNode.DO_STATEMENT:
        return convertDoWhile((DoStatement) node);
      case ASTNode.ASSERT_STATEMENT:
        return convertAssert((AssertStatement) node);
      case ASTNode.SWITCH_STATEMENT:
        return convertSwitchStatement((SwitchStatement) node);
      case ASTNode.SYNCHRONIZED_STATEMENT:
        return convertSynchronized((SynchronizedStatement) node);
      case ASTNode.EXPRESSION_STATEMENT:
        return convertExpressionStatement((ExpressionStatement) node);
      case ASTNode.CONTINUE_STATEMENT:
        return convertContinue((ContinueStatement) node);
      case ASTNode.LABELED_STATEMENT:
        return convertLabel((LabeledStatement) node);
      case ASTNode.ENHANCED_FOR_STATEMENT:
        return convertForeach((EnhancedForStatement) node);
      case ASTNode.THROW_STATEMENT:
        return convertThrow((ThrowStatement) node);
      case ASTNode.TRY_STATEMENT:
        return convertTry((TryStatement) node);
      case ASTNode.TYPE_DECLARATION_STATEMENT:
        return convertTypeDeclaration(((TypeDeclarationStatement) node).getDeclaration());
      case ASTNode.CONSTRUCTOR_INVOCATION:
        return convertConstructorInvocation((ConstructorInvocation) node);
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
        return convertSuperConstructorInvocation((SuperConstructorInvocation) node);
      case ASTNode.YIELD_STATEMENT:
        return convertYield((YieldStatement) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private EmptyStatementTreeImpl convertEmptyStatement(EmptyStatement e) {
    return new EmptyStatementTreeImpl(lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON));
  }

  private ReturnStatementTreeImpl convertReturn(ReturnStatement e) {
    Expression expression = e.getExpression();
    return new ReturnStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamereturn),
      expression == null ? null : convertExpression(expression),
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private ForStatementTreeImpl convertFor(ForStatement e) {
    StatementListTreeImpl forInitStatement = StatementListTreeImpl.emptyList();
    for (int i = 0; i < e.initializers().size(); i++) {
      Expression o = (Expression) e.initializers().get(i);
      if (i > 0) {
        forInitStatement.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
        addVariableToList((VariableDeclarationExpression) o, forInitStatement);
      } else {
        forInitStatement.add(new ExpressionStatementTreeImpl(convertExpression(o), null));
      }
    }

    StatementListTreeImpl forUpdateStatement = StatementListTreeImpl.emptyList();
    for (int i = 0; i < e.updaters().size(); i++) {
      Expression o = (Expression) e.updaters().get(i);
      if (i > 0) {
        forUpdateStatement.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      forUpdateStatement.add(new ExpressionStatementTreeImpl(convertExpression(o), null));
    }

    final int firstSemicolonTokenIndex = e.initializers().isEmpty()
      ? tokenManager.firstIndexIn(e, TerminalTokens.TokenNameSEMICOLON)
      : tokenManager.firstIndexAfter((ASTNode) e.initializers().get(e.initializers().size() - 1), TerminalTokens.TokenNameSEMICOLON);
    Expression expression = e.getExpression();
    final int secondSemicolonTokenIndex = expression == null
      ? nextTokenIndex(firstSemicolonTokenIndex, TerminalTokens.TokenNameSEMICOLON)
      : tokenManager.firstIndexAfter(expression, TerminalTokens.TokenNameSEMICOLON);

    return new ForStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamefor),
      firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
      forInitStatement,
      createSyntaxToken(firstSemicolonTokenIndex),
      expression == null ? null : convertExpression(expression),
      createSyntaxToken(secondSemicolonTokenIndex),
      forUpdateStatement,
      firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN),
      createStatement(e.getBody())
    );
  }

  private WhileStatementTreeImpl convertWhile(WhileStatement e) {
    Expression expression = e.getExpression();
    return new WhileStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamewhile),
      firstTokenBefore(expression, TerminalTokens.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
      createStatement(e.getBody())
    );
  }

  private IfStatementTreeImpl convertIf(IfStatement e) {
    Expression expression = e.getExpression();
    Statement thenStatement = e.getThenStatement();
    Statement elseStatement = e.getElseStatement();
    return new IfStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameif),
      firstTokenBefore(expression, TerminalTokens.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
      createStatement(thenStatement),
      elseStatement == null ? null : firstTokenAfter(thenStatement, TerminalTokens.TokenNameelse),
      elseStatement == null ? null : createStatement(elseStatement)
    );
  }

  private BreakStatementTreeImpl convertBreak(BreakStatement e) {
    IdentifierTreeImpl identifier = e.getLabel() == null ? null : createSimpleName(e.getLabel());
    usageLabel(identifier);
    return new BreakStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamebreak),
      identifier,
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private DoWhileStatementTreeImpl convertDoWhile(DoStatement e) {
    Statement body = e.getBody();
    Expression expression = e.getExpression();
    return new DoWhileStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamedo),
      createStatement(body),
      firstTokenAfter(body, TerminalTokens.TokenNamewhile),
      firstTokenBefore(expression, TerminalTokens.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private AssertStatementTreeImpl convertAssert(AssertStatement e) {
    Expression message = e.getMessage();
    AssertStatementTreeImpl t = new AssertStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameassert),
      convertExpression(e.getExpression()),
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
    if (message != null) {
      t.complete(firstTokenBefore(message, TerminalTokens.TokenNameCOLON), convertExpression(message));
    }
    return t;
  }

  private SwitchStatementTreeImpl convertSwitchStatement(SwitchStatement e) {
    Expression expression = e.getExpression();
    return new SwitchStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameswitch),
      firstTokenBefore(expression, TerminalTokens.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
      firstTokenAfter(expression, TerminalTokens.TokenNameLBRACE),
      convertSwitchStatements(e.statements()),
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    );
  }

  private List<CaseGroupTreeImpl> convertSwitchStatements(List<?> list) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> caselabels = null;
    StatementListTreeImpl body = null;

    for (Object o : list) {
      if (o instanceof SwitchCase) {
        if (caselabels == null) {
          caselabels = new ArrayList<>();
          body = StatementListTreeImpl.emptyList();
        }

        SwitchCase c = (SwitchCase) o;
        List<ExpressionTree> expressions = new ArrayList<>();
        for (Object oo : c.expressions()) {
          expressions.add(convertExpressionFromCase((Expression) oo));
        }

        caselabels.add(new CaseLabelTreeImpl(
          firstTokenIn(c, c.isDefault() ? TerminalTokens.TokenNamedefault : TerminalTokens.TokenNamecase),
          expressions,
          lastTokenIn(c, /* TerminalTokens.TokenNameCOLON or TerminalTokens.TokenNameARROW */ ANY_TOKEN)
        ));
      } else {
        if (caselabels != null) {
          groups.add(new CaseGroupTreeImpl(caselabels, body));
        }
        caselabels = null;
        addStatementToList((Statement) o, Objects.requireNonNull(body));
      }
    }
    if (caselabels != null) {
      groups.add(new CaseGroupTreeImpl(caselabels, body));
    }
    return groups;
  }

  private ExpressionTree convertExpressionFromCase(Expression e) {
    if (e.getNodeType() == ASTNode.CASE_DEFAULT_EXPRESSION) {
      return new DefaultPatternTreeImpl(firstTokenIn(e, TerminalTokens.TokenNamedefault));
    }
    if (e.getNodeType() == ASTNode.NULL_LITERAL) {
      return new NullPatternTreeImpl((LiteralTreeImpl) convertExpression(e));
    }
    if (e instanceof Pattern) {
      return convertPattern((Pattern) e);
    }
    return convertExpression(e);
  }

  private PatternTree convertPattern(Pattern p) {
    switch (p.getNodeType()) {
      case ASTNode.TYPE_PATTERN:
        return new TypePatternTreeImpl(convertVariable(((TypePattern) p).getPatternVariable()));
      case ASTNode.GUARDED_PATTERN:
        GuardedPattern g = (GuardedPattern) p;
        return new GuardedPatternTreeImpl(
          convertPattern(g.getPattern()),
          firstTokenBefore(g.getExpression(), TerminalTokens.TokenNameIdentifier),
          convertExpression(g.getExpression()));
      case ASTNode.NULL_PATTERN:
        // It is not clear how to reach this one, it seems to be possible only with badly constructed AST
        // fall-through. Do nothing for now.
      default:
        // JEP-405 (not released as part of any JDK yet): ArrayPattern, RecordPattern
        throw new IllegalStateException(ASTNode.nodeClassForType(p.getNodeType()).toString());
    }
  }

  private SynchronizedStatementTreeImpl convertSynchronized(SynchronizedStatement e) {
    Expression expression = e.getExpression();
    return new SynchronizedStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamesynchronized),
      firstTokenBefore(expression, TerminalTokens.TokenNameLPAREN),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
      convertBlock(e.getBody())
    );
  }

  private ExpressionStatementTreeImpl convertExpressionStatement(ExpressionStatement e) {
    return new ExpressionStatementTreeImpl(
      convertExpression(e.getExpression()),
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private ContinueStatementTreeImpl convertContinue(ContinueStatement e) {
    SimpleName label = e.getLabel();
    IdentifierTreeImpl i = label == null ? null : createSimpleName(label);
    usageLabel(i);
    return new ContinueStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamecontinue),
      i,
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private LabeledStatementTreeImpl convertLabel(LabeledStatement e) {
    IdentifierTreeImpl i = createSimpleName(e.getLabel());

    JLabelSymbol symbol = new JLabelSymbol(i.name());
    labels.push(symbol);

    LabeledStatementTreeImpl t = new LabeledStatementTreeImpl(
      i,
      firstTokenAfter(e.getLabel(), TerminalTokens.TokenNameCOLON),
      createStatement(e.getBody())
    );

    labels.pop();
    symbol.declaration = t;
    t.labelSymbol = symbol;
    return t;
  }

  private ForEachStatementImpl convertForeach(EnhancedForStatement e) {
    SingleVariableDeclaration parameter = e.getParameter();
    Expression expression = e.getExpression();
    return new ForEachStatementImpl(
      firstTokenIn(e, TerminalTokens.TokenNamefor),
      firstTokenBefore(parameter, TerminalTokens.TokenNameLPAREN),
      convertVariable(parameter),
      firstTokenAfter(parameter, TerminalTokens.TokenNameCOLON),
      convertExpression(expression),
      firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
      createStatement(e.getBody())
    );
  }

  private ThrowStatementTreeImpl convertThrow(ThrowStatement e) {
    return new ThrowStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNamethrow),
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private TryStatementTreeImpl convertTry(TryStatement e) {
    ResourceListTreeImpl resources = convertResources(e);
    List<CatchTree> catches = convertCatchClauses(e);

    Block f = e.getFinally();
    return new TryStatementTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNametry),
      resources.isEmpty() ? null : firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
      resources,
      resources.isEmpty() ? null : firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN),
      convertBlock(e.getBody()),
      catches,
      f == null ? null : firstTokenBefore(f, TerminalTokens.TokenNamefinally),
      f == null ? null : convertBlock(f)
    );
  }

  private ResourceListTreeImpl convertResources(TryStatement e) {
    List r = e.resources();
    ResourceListTreeImpl resources = ResourceListTreeImpl.emptyList();
    for (int i = 0; i < r.size(); i++) {
      Expression o = (Expression) r.get(i);
      if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
        addVariableToList((VariableDeclarationExpression) o, resources);
      } else {
        resources.add(convertExpression(o));
      }
      addSeparatorToList(e, o, resources.separators(), i < e.resources().size() - 1);
    }
    return resources;
  }

  private void addSeparatorToList(TryStatement tryStatement, Expression resource, List<SyntaxToken> separators, boolean isLast) {
    if (isLast) {
      separators.add(firstTokenAfter(resource, TerminalTokens.TokenNameSEMICOLON));
    } else {
      int tokenIndex = tokenManager.firstIndexBefore(tryStatement.getBody(), TerminalTokens.TokenNameRPAREN);
      while (true) {
        Token token;
        do {
          tokenIndex--;
          token = tokenManager.get(tokenIndex);
        } while (token.isComment());

        if (token.tokenType != TerminalTokens.TokenNameSEMICOLON) {
          break;
        }
        separators.add(createSyntaxToken(tokenIndex));
      }
    }
  }

  private List<CatchTree> convertCatchClauses(TryStatement e) {
    List<CatchTree> catches = new ArrayList<>();
    for (Object o : e.catchClauses()) {
      CatchClause c = (CatchClause) o;
      catches.add(new CatchTreeImpl(
        firstTokenIn(c, TerminalTokens.TokenNamecatch),
        firstTokenBefore(c.getException(), TerminalTokens.TokenNameLPAREN),
        convertVariable(c.getException()),
        firstTokenAfter(c.getException(), TerminalTokens.TokenNameRPAREN),
        convertBlock(c.getBody())
      ));
    }
    return catches;
  }

  private ExpressionStatementTreeImpl convertConstructorInvocation(ConstructorInvocation e) {
    ArgumentListTreeImpl arguments = convertArguments(
      e.arguments().isEmpty() ? lastTokenIn(e, TerminalTokens.TokenNameLPAREN) : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalTokens.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
    );

    IdentifierTreeImpl i = new IdentifierTreeImpl(e.arguments().isEmpty()
      ? lastTokenIn(e, TerminalTokens.TokenNamethis)
      : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalTokens.TokenNamethis));
    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      i,
      convertTypeArguments(e.typeArguments()),
      arguments
    );
    t.methodBinding = e.resolveConstructorBinding();
    if (t.methodBinding != null) {
      t.typeBinding = t.methodBinding.getDeclaringClass();
      t.methodBinding = excludeRecovery(t.methodBinding, arguments.size());
    }
    i.binding = t.methodBinding;
    usage(i.binding, i);
    return new ExpressionStatementTreeImpl(
      t,
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private ExpressionStatementTreeImpl convertSuperConstructorInvocation(SuperConstructorInvocation e) {
    IdentifierTreeImpl i = new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNamesuper));
    ExpressionTree methodSelect = i;
    if (e.getExpression() != null) {
      methodSelect = new MemberSelectExpressionTreeImpl(
        convertExpression(e.getExpression()),
        firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
        i
      );
    }

    ArgumentListTreeImpl arguments = convertArguments(
      firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
    );

    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      methodSelect,
      convertTypeArguments(e.typeArguments()),
      arguments
    );
    t.methodBinding = e.resolveConstructorBinding();
    if (t.methodBinding != null) {
      t.typeBinding = t.methodBinding.getDeclaringClass();
      t.methodBinding = excludeRecovery(t.methodBinding, arguments.size());
    }
    i.binding = t.methodBinding;
    usage(i.binding, i);
    return new ExpressionStatementTreeImpl(
      t,
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private YieldStatementTreeImpl convertYield(YieldStatement e) {
    InternalSyntaxToken yieldKeyword = null;
    if (!e.isImplicit()) {
      try {
        yieldKeyword = firstTokenIn(e, TerminalTokens.TokenNameRestrictedIdentifierYield);
      } catch (AssertionError | IndexOutOfBoundsException error) {
        // TODO ECJ bug? should be "TerminalTokens.TokenNameRestrictedIdentifierYield" in all cases
        yieldKeyword = firstTokenIn(e, TerminalTokens.TokenNameIdentifier);
      }
    }
    return new YieldStatementTreeImpl(
      yieldKeyword,
      convertExpression(e.getExpression()),
      lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
    );
  }

  private ExpressionTree convertExpression(Expression node) {
    ExpressionTree t = createExpression(node);
    ((AbstractTypedTree) t).typeBinding = node.resolveTypeBinding();
    return t;
  }

  private ExpressionTree createExpression(Expression node) {
    switch (node.getNodeType()) {
      case ASTNode.SIMPLE_NAME:
        return convertSimpleName((SimpleName) node);
      case ASTNode.QUALIFIED_NAME:
        return convertQualifiedName((QualifiedName) node);
      case ASTNode.FIELD_ACCESS:
        return convertFieldAccess((FieldAccess) node);
      case ASTNode.SUPER_FIELD_ACCESS:
        return convertFieldAccess((SuperFieldAccess) node);
      case ASTNode.THIS_EXPRESSION:
        return convertThisExpression((ThisExpression) node);
      case ASTNode.ARRAY_ACCESS:
        return convertArrayAccess((ArrayAccess) node);
      case ASTNode.ARRAY_CREATION:
        return convertArrayCreation((ArrayCreation) node);
      case ASTNode.ARRAY_INITIALIZER:
        return convertArrayInitializer((ArrayInitializer) node);
      case ASTNode.ASSIGNMENT:
        return convertAssignment((Assignment) node);
      case ASTNode.CAST_EXPRESSION:
        return convertTypeCastExpression((CastExpression) node);
      case ASTNode.CLASS_INSTANCE_CREATION:
        return convertClassInstanceCreation((ClassInstanceCreation) node);
      case ASTNode.CONDITIONAL_EXPRESSION:
        return convertConditionalExpression((ConditionalExpression) node);
      case ASTNode.INFIX_EXPRESSION:
        return convertInfixExpression((InfixExpression) node);
      case ASTNode.METHOD_INVOCATION:
        return convertMethodInvocation((MethodInvocation) node);
      case ASTNode.SUPER_METHOD_INVOCATION:
        return convertMethodInvocation((SuperMethodInvocation) node);
      case ASTNode.PARENTHESIZED_EXPRESSION:
        return convertParenthesizedExpression((ParenthesizedExpression) node);
      case ASTNode.POSTFIX_EXPRESSION:
        return convertPostfixExpression((PostfixExpression) node);
      case ASTNode.PREFIX_EXPRESSION:
        return convertPrefixExpression((PrefixExpression) node);
      case ASTNode.INSTANCEOF_EXPRESSION:
        return convertInstanceOf((InstanceofExpression) node);
      case ASTNode.PATTERN_INSTANCEOF_EXPRESSION:
        return convertInstanceOf((PatternInstanceofExpression) node);
      case ASTNode.LAMBDA_EXPRESSION:
        return convertLambdaExpression((LambdaExpression) node);
      case ASTNode.CREATION_REFERENCE:
        return convertMethodReference((CreationReference) node);
      case ASTNode.EXPRESSION_METHOD_REFERENCE:
        return convertMethodReference((ExpressionMethodReference) node);
      case ASTNode.TYPE_METHOD_REFERENCE:
        return convertMethodReference((TypeMethodReference) node);
      case ASTNode.SUPER_METHOD_REFERENCE:
        return convertMethodReference((SuperMethodReference) node);
      case ASTNode.SWITCH_EXPRESSION:
        return convertSwitchExpression((SwitchExpression) node);
      case ASTNode.TYPE_LITERAL:
        return convertTypeLiteral((TypeLiteral) node);
      case ASTNode.NULL_LITERAL:
        return convertLiteral((NullLiteral) node);
      case ASTNode.NUMBER_LITERAL:
        return convertLiteral((NumberLiteral) node);
      case ASTNode.CHARACTER_LITERAL:
        return convertLiteral((CharacterLiteral) node);
      case ASTNode.BOOLEAN_LITERAL:
        return convertLiteral((BooleanLiteral) node);
      case ASTNode.STRING_LITERAL:
        return convertLiteral((StringLiteral) node);
      case ASTNode.TEXT_BLOCK:
        return convertTextBlock((TextBlock) node);
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION:
        return convertAnnotation((Annotation) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private IdentifierTreeImpl convertSimpleName(SimpleName e) {
    IdentifierTreeImpl t = createSimpleName(e);
    usage(t.binding, t);
    return t;
  }

  private MemberSelectExpressionTreeImpl convertQualifiedName(QualifiedName e) {
    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    usage(rhs.binding, rhs);
    return new MemberSelectExpressionTreeImpl(
      convertExpression(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
      rhs
    );
  }

  private MemberSelectExpressionTreeImpl convertFieldAccess(FieldAccess e) {
    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    usage(rhs.binding, rhs);
    return new MemberSelectExpressionTreeImpl(
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
      rhs
    );
  }

  private MemberSelectExpressionTreeImpl convertFieldAccess(SuperFieldAccess e) {
    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    usage(rhs.binding, rhs);
    if (e.getQualifier() == null) {
      // super.name
      return new MemberSelectExpressionTreeImpl(
        unqualifiedKeywordSuper(e),
        firstTokenIn(e, TerminalTokens.TokenNameDOT),
        rhs
      );
    }
    // qualifier.super.name
    AbstractTypedTree qualifier = (AbstractTypedTree) convertExpression(e.getQualifier());
    KeywordSuper keywordSuper = new KeywordSuper(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper), null);
    MemberSelectExpressionTreeImpl qualifiedSuper = new MemberSelectExpressionTreeImpl(
      (ExpressionTree) qualifier,
      firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
      keywordSuper
    );
    if (qualifier.typeBinding != null) {
      keywordSuper.typeBinding = qualifier.typeBinding;
      qualifiedSuper.typeBinding = keywordSuper.typeBinding.getSuperclass();
    }
    return new MemberSelectExpressionTreeImpl(
      qualifiedSuper,
      firstTokenBefore(e.getName(), TerminalTokens.TokenNameDOT),
      rhs
    );
  }

  private ExpressionTree convertThisExpression(ThisExpression e) {
    if (e.getQualifier() == null) {
      return new KeywordThis(firstTokenIn(e, TerminalTokens.TokenNamethis), null);
    }
    KeywordThis keywordThis = new KeywordThis(
      firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamethis),
      e.resolveTypeBinding()
    );
    return new MemberSelectExpressionTreeImpl(
      convertExpression(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
      keywordThis
    );
  }

  private MemberSelectExpressionTreeImpl convertTypeLiteral(TypeLiteral e) {
    return new MemberSelectExpressionTreeImpl(
      (ExpressionTree) convertType(e.getType()),
      lastTokenIn(e, TerminalTokens.TokenNameDOT),
      new IdentifierTreeImpl(
        lastTokenIn(e, TerminalTokens.TokenNameclass)
      )
    );
  }

  private ArrayAccessExpressionTreeImpl convertArrayAccess(ArrayAccess e) {
    Expression index = e.getIndex();
    return new ArrayAccessExpressionTreeImpl(
      convertExpression(e.getArray()),
      new ArrayDimensionTreeImpl(
        firstTokenBefore(index, TerminalTokens.TokenNameLBRACKET),
        convertExpression(index),
        firstTokenAfter(index, TerminalTokens.TokenNameRBRACKET)
      )
    );
  }

  private NewArrayTreeImpl convertArrayCreation(ArrayCreation e) {
    List<ArrayDimensionTree> dimensions = new ArrayList<>();
    for (Object o : e.dimensions()) {
      dimensions.add(new ArrayDimensionTreeImpl(
        firstTokenBefore((Expression) o, TerminalTokens.TokenNameLBRACKET),
        convertExpression((Expression) o),
        firstTokenAfter((Expression) o, TerminalTokens.TokenNameRBRACKET)
      ));
    }
    InitializerListTreeImpl initializers = InitializerListTreeImpl.emptyList();
    if (e.getInitializer() != null) {
      assert dimensions.isEmpty();

      TypeTree type = convertType(e.getType());
      while (type.is(Tree.Kind.ARRAY_TYPE)) {
        ArrayTypeTree arrayType = (ArrayTypeTree) type;
        ArrayDimensionTreeImpl dimension = new ArrayDimensionTreeImpl(
          arrayType.openBracketToken(),
          null,
          arrayType.closeBracketToken()
        ).completeAnnotations(arrayType.annotations());
        dimensions.add(/* TODO suboptimal */ 0, dimension);
        type = arrayType.type();
      }

      return ((NewArrayTreeImpl) convertExpression(e.getInitializer()))
        .completeWithNewKeyword(firstTokenIn(e, TerminalTokens.TokenNamenew))
        .complete(type)
        .completeDimensions(dimensions);
    }
    TypeTree type = convertType(e.getType());
    int index = dimensions.size() - 1;
    while (type.is(Tree.Kind.ARRAY_TYPE)) {
      if (!type.annotations().isEmpty()) {
        ((ArrayDimensionTreeImpl) dimensions.get(index))
          .completeAnnotations(type.annotations());
      }
      index--;
      type = ((ArrayTypeTree) type).type();
    }

    return new NewArrayTreeImpl(dimensions, initializers)
      .complete(type)
      .completeWithNewKeyword(firstTokenIn(e, TerminalTokens.TokenNamenew));
  }

  private NewArrayTreeImpl convertArrayInitializer(ArrayInitializer e) {
    InitializerListTreeImpl initializers = InitializerListTreeImpl.emptyList();
    for (int i = 0; i < e.expressions().size(); i++) {
      Expression o = (Expression) e.expressions().get(i);
      initializers.add(convertExpression(o));
      final int commaTokenIndex = firstTokenIndexAfter(o);
      if (tokenManager.get(commaTokenIndex).tokenType == TerminalTokens.TokenNameCOMMA) {
        initializers.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
      }
    }
    return new NewArrayTreeImpl(Collections.emptyList(),initializers)
      .completeWithCurlyBraces(
      firstTokenIn(e, TerminalTokens.TokenNameLBRACE),
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
      );
  }

  private AssignmentExpressionTreeImpl convertAssignment(Assignment e) {
    Op op = operators.get(e.getOperator());
    return new AssignmentExpressionTreeImpl(
      op.kind,
      convertExpression(e.getLeftHandSide()),
      firstTokenAfter(e.getLeftHandSide(), op.tokenType),
      convertExpression(e.getRightHandSide())
    );
  }

  private TypeCastExpressionTreeImpl convertTypeCastExpression(CastExpression e) {
    Type type = e.getType();
    if (type.getNodeType() == ASTNode.INTERSECTION_TYPE) {
      List intersectionTypes = ((IntersectionType) type).types();
      QualifiedIdentifierListTreeImpl bounds = QualifiedIdentifierListTreeImpl.emptyList();
      for (int i = 1; i < intersectionTypes.size(); i++) {
        Type o = (Type) intersectionTypes.get(i);
        bounds.add(convertType(o));
        if (i < intersectionTypes.size() - 1) {
          bounds.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameAND));
        }
      }
      return new TypeCastExpressionTreeImpl(
        firstTokenBefore(type, TerminalTokens.TokenNameLPAREN),
        convertType((Type) intersectionTypes.get(0)),
        firstTokenAfter((Type) intersectionTypes.get(0), TerminalTokens.TokenNameAND),
        bounds,
        firstTokenAfter(type, TerminalTokens.TokenNameRPAREN),
        convertExpression(e.getExpression())
      );
    }
    return new TypeCastExpressionTreeImpl(
      firstTokenBefore(type, TerminalTokens.TokenNameLPAREN),
      convertType(type),
      firstTokenAfter(type, TerminalTokens.TokenNameRPAREN),
      convertExpression(e.getExpression())
    );
  }

  private NewClassTreeImpl convertClassInstanceCreation(ClassInstanceCreation e) {
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
      classBody.typeBinding = e.getAnonymousClassDeclaration().resolveBinding();
      declaration(classBody.typeBinding, classBody);
    }

    NewClassTreeImpl t = new NewClassTreeImpl(
      convertType(e.getType()),
      arguments,
      classBody
    ).completeWithNewKeyword(
      e.getExpression() == null ? firstTokenIn(e, TerminalTokens.TokenNamenew) : firstTokenAfter(e.getExpression(), TerminalTokens.TokenNamenew)
    ).completeWithTypeArguments(
      convertTypeArguments(e.typeArguments())
    );
    if (e.getExpression() != null) {
      t.completeWithEnclosingExpression(convertExpression(e.getExpression()));
      t.completeWithDotToken(firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT));
    }

    IdentifierTreeImpl i = (IdentifierTreeImpl) t.getConstructorIdentifier();
    int nbArguments = arguments.size();
    if (e.getAnonymousClassDeclaration() == null) {
      i.binding = excludeRecovery(e.resolveConstructorBinding(), nbArguments);
    } else {
      i.binding = excludeRecovery(findConstructorForAnonymousClass(e.getAST(), i.typeBinding, e.resolveConstructorBinding()), nbArguments);
    }
    usage(i.binding, i);

    return t;
  }

  private ConditionalExpressionTreeImpl convertConditionalExpression(ConditionalExpression e) {
    return new ConditionalExpressionTreeImpl(
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameQUESTION),
      convertExpression(e.getThenExpression()),
      firstTokenAfter(e.getThenExpression(), TerminalTokens.TokenNameCOLON),
      convertExpression(e.getElseExpression())
    );
  }

  private BinaryExpressionTreeImpl convertInfixExpression(InfixExpression e) {
    Op op = operators.get(e.getOperator());
    BinaryExpressionTreeImpl t = new BinaryExpressionTreeImpl(
      op.kind,
      convertExpression(e.getLeftOperand()),
      firstTokenAfter(e.getLeftOperand(), op.tokenType),
      convertExpression(e.getRightOperand())
    );
    for (Object o : e.extendedOperands()) {
      Expression e2 = (Expression) o;
      t.typeBinding = e.resolveTypeBinding();
      t = new BinaryExpressionTreeImpl(
        op.kind,
        t,
        firstTokenBefore(e2, op.tokenType),
        convertExpression(e2)
      );
    }
    return t;
  }

  private MethodInvocationTreeImpl convertMethodInvocation(MethodInvocation e) {
    ArgumentListTreeImpl arguments = convertArguments(
      firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
    );

    IdentifierTreeImpl rhs = createSimpleName(e.getName());
    ExpressionTree memberSelect;
    if (e.getExpression() == null) {
      memberSelect = rhs;
    } else {
      memberSelect = new MemberSelectExpressionTreeImpl(
        convertExpression(e.getExpression()),
        firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
        rhs
      );
    }
    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      memberSelect,
      convertTypeArguments(e.typeArguments()),
      arguments
    );
    t.methodBinding = excludeRecovery(e.resolveMethodBinding(), arguments.size());
    rhs.binding = t.methodBinding;
    usage(rhs.binding, rhs);
    return t;
  }

  private MethodInvocationTreeImpl convertMethodInvocation(SuperMethodInvocation e) {
    ArgumentListTreeImpl arguments = convertArguments(
      firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
      e.arguments(),
      lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
    );

    IdentifierTreeImpl rhs = createSimpleName(e.getName());

    ExpressionTree outermostSelect;
    if (e.getQualifier() == null) {
      outermostSelect = new MemberSelectExpressionTreeImpl(
        unqualifiedKeywordSuper(e),
        firstTokenIn(e, TerminalTokens.TokenNameDOT),
        rhs
      );
    } else {
      final int firstDotTokenIndex = tokenManager.firstIndexAfter(e.getQualifier(), TerminalTokens.TokenNameDOT);
      AbstractTypedTree qualifier = (AbstractTypedTree) convertExpression(e.getQualifier());
      KeywordSuper keywordSuper = new KeywordSuper(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper), null);
      MemberSelectExpressionTreeImpl qualifiedSuper = new MemberSelectExpressionTreeImpl(
        (ExpressionTree) qualifier,
        createSyntaxToken(firstDotTokenIndex),
        keywordSuper
      );
      if (qualifier.typeBinding != null) {
        keywordSuper.typeBinding = qualifier.typeBinding;
        qualifiedSuper.typeBinding = keywordSuper.typeBinding.getSuperclass();
      }
      outermostSelect = new MemberSelectExpressionTreeImpl(
        qualifiedSuper,
        createSyntaxToken(nextTokenIndex(firstDotTokenIndex, TerminalTokens.TokenNameDOT)),
        rhs
      );
    }

    MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
      outermostSelect,
      null,
      arguments
    );
    t.methodBinding = excludeRecovery(e.resolveMethodBinding(), arguments.size());
    rhs.binding = t.methodBinding;
    usage(rhs.binding, rhs);
    return t;
  }

  private ParenthesizedTreeImpl convertParenthesizedExpression(ParenthesizedExpression e) {
    return new ParenthesizedTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN)
    );
  }

  private InternalPostfixUnaryExpression convertPostfixExpression(PostfixExpression e) {
    Op op = operators.get(e.getOperator());
    return new InternalPostfixUnaryExpression(
      op.kind,
      convertExpression(e.getOperand()),
      firstTokenAfter(e.getOperand(), op.tokenType)
    );
  }

  private InternalPrefixUnaryExpression convertPrefixExpression(PrefixExpression e) {
    Op op = operators.get(e.getOperator());
    return new InternalPrefixUnaryExpression(
      op.kind,
      firstTokenIn(e, op.tokenType),
      convertExpression(e.getOperand())
    );
  }

  private InstanceOfTreeImpl convertInstanceOf(InstanceofExpression e) {
    Expression leftOperand = e.getLeftOperand();
    InternalSyntaxToken instanceofToken = firstTokenAfter(leftOperand, TerminalTokens.TokenNameinstanceof);
    return new InstanceOfTreeImpl(convertExpression(leftOperand), instanceofToken, convertType(e.getRightOperand()));
  }

  private InstanceOfTreeImpl convertInstanceOf(PatternInstanceofExpression e) {
    Expression leftOperand = e.getLeftOperand();
    InternalSyntaxToken instanceofToken = firstTokenAfter(leftOperand, TerminalTokens.TokenNameinstanceof);
    return new InstanceOfTreeImpl(convertExpression(leftOperand), instanceofToken, convertVariable(e.getRightOperand()));
  }

  private LambdaExpressionTreeImpl convertLambdaExpression(LambdaExpression e) {
    List<VariableTree> parameters = new ArrayList<>();
    for (int i = 0; i < e.parameters().size(); i++) {
      VariableDeclaration o = (VariableDeclaration) e.parameters().get(i);
      VariableTreeImpl t;
      if (o.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
        t = new VariableTreeImpl(createSimpleName(o.getName()));
        IVariableBinding variableBinding = o.resolveBinding();
        if (variableBinding != null) {
          t.variableBinding = variableBinding;
          ((InferedTypeTree) t.type()).typeBinding = variableBinding.getType();
          declaration(t.variableBinding, t);
        }
      } else {
        t = convertVariable((SingleVariableDeclaration) o);
      }
      parameters.add(t);
      if (i < e.parameters().size() - 1) {
        t.setEndToken(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
      }
    }
    ASTNode body = e.getBody();
    return new LambdaExpressionTreeImpl(
      e.hasParentheses() ? firstTokenIn(e, TerminalTokens.TokenNameLPAREN) : null,
      parameters,
      e.hasParentheses() ? firstTokenBefore(body, TerminalTokens.TokenNameRPAREN) : null,
      firstTokenBefore(body, TerminalTokens.TokenNameARROW),
      body.getNodeType() == ASTNode.BLOCK ? convertBlock((Block) body) : convertExpression((Expression) body)
    );
  }

  private MethodReferenceTreeImpl convertMethodReference(CreationReference e) {
    MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
      convertType(e.getType()),
      lastTokenIn(e, TerminalTokens.TokenNameCOLON_COLON)
    );
    IdentifierTreeImpl i = new IdentifierTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamenew));
    i.binding = e.resolveMethodBinding();
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private MethodReferenceTreeImpl convertMethodReference(ExpressionMethodReference e) {
    MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
      convertExpression(e.getExpression()),
      firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameCOLON_COLON)
    );
    IdentifierTreeImpl i = createSimpleName(e.getName());
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private MethodReferenceTreeImpl convertMethodReference(TypeMethodReference e) {
    MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
      convertType(e.getType()),
      firstTokenAfter(e.getType(), TerminalTokens.TokenNameCOLON_COLON)
    );
    IdentifierTreeImpl i = createSimpleName(e.getName());
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private MethodReferenceTreeImpl convertMethodReference(SuperMethodReference e) {
    MethodReferenceTreeImpl t;
    if (e.getQualifier() != null) {
      t = new MethodReferenceTreeImpl(
        new MemberSelectExpressionTreeImpl(
          convertExpression(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          unqualifiedKeywordSuper(e)
        ),
        firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameCOLON_COLON)
      );
    } else {
      t = new MethodReferenceTreeImpl(
        unqualifiedKeywordSuper(e),
        firstTokenIn(e, TerminalTokens.TokenNameCOLON_COLON)
      );
    }
    IdentifierTreeImpl i = createSimpleName(e.getName());
    usage(i.binding, i);
    t.complete(convertTypeArguments(e.typeArguments()), i);
    return t;
  }

  private SwitchExpressionTreeImpl convertSwitchExpression(SwitchExpression e) {
    Expression expr = e.getExpression();
    return new SwitchExpressionTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameswitch),
      firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
      convertExpression(expr),
      firstTokenAfter(expr, TerminalTokens.TokenNameRPAREN),
      firstTokenAfter(expr, TerminalTokens.TokenNameLBRACE),
      convertSwitchStatements(e.statements()),
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    );
  }

  private LiteralTreeImpl convertLiteral(NullLiteral e) {
    return new LiteralTreeImpl(Tree.Kind.NULL_LITERAL, firstTokenIn(e, TerminalTokens.TokenNamenull));
  }

  private ExpressionTree convertLiteral(NumberLiteral e) {
    int tokenIndex = tokenManager.findIndex(e.getStartPosition(), ANY_TOKEN, true);
    int tokenType = tokenManager.get(tokenIndex).tokenType;
    boolean unaryMinus = tokenType == TerminalTokens.TokenNameMINUS;
    if (unaryMinus) {
      tokenIndex++;
      tokenType = tokenManager.get(tokenIndex).tokenType;
    }
    ExpressionTree result;
    switch (tokenType) {
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
      default:
        throw new IllegalStateException();
    }
    ((LiteralTreeImpl) result).typeBinding = e.resolveTypeBinding();
    if (unaryMinus) {
      return new InternalPrefixUnaryExpression(Tree.Kind.UNARY_MINUS, createSyntaxToken(tokenIndex - 1), result);
    }
    return result;
  }

  private LiteralTreeImpl convertLiteral(CharacterLiteral e) {
    return new LiteralTreeImpl(Tree.Kind.CHAR_LITERAL, firstTokenIn(e, TerminalTokens.TokenNameCharacterLiteral));
  }

  private LiteralTreeImpl convertLiteral(BooleanLiteral e) {
    InternalSyntaxToken value = firstTokenIn(e, e.booleanValue() ? TerminalTokens.TokenNametrue : TerminalTokens.TokenNamefalse);
    return new LiteralTreeImpl(Tree.Kind.BOOLEAN_LITERAL, value);
  }

  private LiteralTreeImpl convertLiteral(StringLiteral e) {
    return new LiteralTreeImpl(Tree.Kind.STRING_LITERAL, firstTokenIn(e, TerminalTokens.TokenNameStringLiteral));
  }

  private LiteralTreeImpl convertTextBlock(TextBlock e) {
    return new LiteralTreeImpl(Tree.Kind.TEXT_BLOCK, firstTokenIn(e, TerminalTokens.TokenNameTextBlock));
  }

  private AnnotationTreeImpl convertAnnotation(Annotation e) {
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    if (e.getNodeType() == ASTNode.SINGLE_MEMBER_ANNOTATION) {
      arguments.add(convertExpression(((SingleMemberAnnotation) e).getValue()));
      arguments.complete(
        firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
        lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
      );
    } else if (e.getNodeType() == ASTNode.NORMAL_ANNOTATION) {
      for (int i = 0; i < ((NormalAnnotation) e).values().size(); i++) {
        MemberValuePair o = (MemberValuePair) ((NormalAnnotation) e).values().get(i);
        arguments.add(new AssignmentExpressionTreeImpl(
          Tree.Kind.ASSIGNMENT,
          createSimpleName(o.getName()),
          firstTokenAfter(o.getName(), TerminalTokens.TokenNameEQUAL),
          convertExpression(o.getValue())
        ));
        if (i < ((NormalAnnotation) e).values().size() - 1) {
          arguments.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
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

  private KeywordSuper unqualifiedKeywordSuper(ASTNode node) {
    InternalSyntaxToken token = firstTokenIn(node, TerminalTokens.TokenNamesuper);
    do {
      if (node instanceof AbstractTypeDeclaration) {
        return new KeywordSuper(token, ((AbstractTypeDeclaration) node).resolveBinding());
      }
      if (node instanceof AnonymousClassDeclaration) {
        return new KeywordSuper(token, ((AnonymousClassDeclaration) node).resolveBinding());
      }
      node = node.getParent();
    } while (true);
  }

  private TypeTree convertType(Type node) {
    switch (node.getNodeType()) {
      case ASTNode.PRIMITIVE_TYPE:
        return convertPrimitiveType((PrimitiveType) node);
      case ASTNode.SIMPLE_TYPE:
        return convertSimpleType((SimpleType) node);
      case ASTNode.UNION_TYPE:
        return convertUnionType((UnionType) node);
      case ASTNode.ARRAY_TYPE:
        return convertArrayType((ArrayType) node);
      case ASTNode.PARAMETERIZED_TYPE:
        return convertParameterizedType((ParameterizedType) node);
      case ASTNode.QUALIFIED_TYPE:
        return convertQualifiedType((QualifiedType) node);
      case ASTNode.NAME_QUALIFIED_TYPE:
        return convertNamedQualifiedType((NameQualifiedType) node);
      case ASTNode.WILDCARD_TYPE:
        return convertWildcardType((WildcardType) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private JavaTree.PrimitiveTypeTreeImpl convertPrimitiveType(PrimitiveType e) {
    final JavaTree.PrimitiveTypeTreeImpl t;
    switch (e.getPrimitiveTypeCode().toString()) {
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
      default:
        throw new IllegalStateException(e.getPrimitiveTypeCode().toString());
    }
    t.complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private JavaTree.AnnotatedTypeTree convertSimpleType(SimpleType e) {
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Object o : e.annotations()) {
      annotations.add((AnnotationTree) convertExpression(((Annotation) o)));
    }
    JavaTree.AnnotatedTypeTree t = e.isVar() ? convertVarType(e) : (JavaTree.AnnotatedTypeTree) convertExpression(e.getName());
    t.complete(annotations);
    // typeBinding is assigned by convertVarType or convertExpression
    return t;
  }

  private JavaTree.UnionTypeTreeImpl convertUnionType(UnionType e) {
    QualifiedIdentifierListTreeImpl alternatives = QualifiedIdentifierListTreeImpl.emptyList();
    for (int i = 0; i < e.types().size(); i++) {
      Type o = (Type) e.types().get(i);
      alternatives.add(convertType(o));
      if (i < e.types().size() - 1) {
        alternatives.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameOR));
      }
    }
    JavaTree.UnionTypeTreeImpl t = new JavaTree.UnionTypeTreeImpl(alternatives);
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private TypeTree convertArrayType(ArrayType e) {
    @Nullable ITypeBinding elementTypeBinding = e.getElementType().resolveBinding();
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
      if (elementTypeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) t).typeBinding = elementTypeBinding.createArrayType(i + 1);
      }
    }
    return t;
  }

  private JavaTree.ParameterizedTypeTreeImpl convertParameterizedType(ParameterizedType e) {
    int pos = e.getStartPosition() + e.getLength() - 1;
    JavaTree.ParameterizedTypeTreeImpl t = new JavaTree.ParameterizedTypeTreeImpl(
      convertType(e.getType()),
      convertTypeArguments(
        firstTokenAfter(e.getType(), TerminalTokens.TokenNameLESS),
        e.typeArguments(),
        new InternalSyntaxToken(
          compilationUnit.getLineNumber(pos),
          compilationUnit.getColumnNumber(pos),
          ">",
          /* TODO */ Collections.emptyList(),
          false
        )
      )
    );
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private MemberSelectExpressionTreeImpl convertQualifiedType(QualifiedType e) {
    MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
      (ExpressionTree) convertType(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
      createSimpleName(e.getName())
    );
    ((IdentifierTreeImpl) t.identifier()).complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private MemberSelectExpressionTreeImpl convertNamedQualifiedType(NameQualifiedType e) {
    MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
      convertExpression(e.getQualifier()),
      firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
      createSimpleName(e.getName())
    );
    ((IdentifierTreeImpl) t.identifier()).complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  private JavaTree.WildcardTreeImpl convertWildcardType(WildcardType e) {
    final InternalSyntaxToken questionToken = e.annotations().isEmpty()
      ? firstTokenIn(e, TerminalTokens.TokenNameQUESTION)
      : firstTokenAfter((ASTNode) e.annotations().get(e.annotations().size() - 1), TerminalTokens.TokenNameQUESTION);
    JavaTree.WildcardTreeImpl t;
    Type bound = e.getBound();
    if (bound == null) {
      t = new JavaTree.WildcardTreeImpl(questionToken);
    } else {
      t = new JavaTree.WildcardTreeImpl(
        e.isUpperBound() ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD,
        e.isUpperBound() ? firstTokenBefore(bound, TerminalTokens.TokenNameextends) : firstTokenBefore(bound, TerminalTokens.TokenNamesuper),
        convertType(bound)
      ).complete(questionToken);
    }
    t.complete(convertAnnotations(e.annotations()));
    t.typeBinding = e.resolveBinding();
    return t;
  }

  @Nullable
  private static IMethodBinding excludeRecovery(@Nullable IMethodBinding methodBinding, int arguments) {
    if (methodBinding == null) {
      return null;
    }
    if (methodBinding.isVarargs()) {
      if (arguments + 1 < methodBinding.getParameterTypes().length) {
        return null;
      }
    } else {
      if (arguments != methodBinding.getParameterTypes().length) {
        return null;
      }
    }
    return methodBinding;
  }

  @Nullable
  private static IMethodBinding findConstructorForAnonymousClass(AST ast, @Nullable ITypeBinding typeBinding, @Nullable IMethodBinding methodBinding) {
    if (typeBinding == null || methodBinding == null) {
      return null;
    }
    if (typeBinding.isInterface()) {
      typeBinding = ast.resolveWellKnownType("java.lang.Object");
    }
    for (IMethodBinding m : typeBinding.getDeclaredMethods()) {
      if (methodBinding.isSubsignature(m)) {
        return m;
      }
    }
    return null;
  }

  private List<AnnotationTree> convertAnnotations(List<?> e) {
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Object o : e) {
      annotations.add((AnnotationTree) convertExpression(
        ((Annotation) o)
      ));
    }
    return annotations;
  }

  private ModifiersTreeImpl convertModifiers(List<?> source) {
    List<ModifierTree> modifiers = new ArrayList<>();
    for (Object o : source) {
      modifiers.add(convertModifier((IExtendedModifier) o));
    }
    return new ModifiersTreeImpl(modifiers);
  }

  private ModifierTree convertModifier(IExtendedModifier node) {
    switch (((ASTNode) node).getNodeType()) {
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION:
        return (AnnotationTree) convertExpression((Expression) node);
      case ASTNode.MODIFIER:
        return convertModifier((org.eclipse.jdt.core.dom.Modifier) node);
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(((ASTNode) node).getNodeType()).toString());
    }
  }

  private ModifierTree convertModifier(org.eclipse.jdt.core.dom.Modifier node) {
    switch (node.getKeyword().toString()) {
      case "public":
        return new ModifierKeywordTreeImpl(Modifier.PUBLIC, firstTokenIn(node, TerminalTokens.TokenNamepublic));
      case "protected":
        return new ModifierKeywordTreeImpl(Modifier.PROTECTED, firstTokenIn(node, TerminalTokens.TokenNameprotected));
      case "private":
        return new ModifierKeywordTreeImpl(Modifier.PRIVATE, firstTokenIn(node, TerminalTokens.TokenNameprivate));
      case "static":
        return new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn(node, TerminalTokens.TokenNamestatic));
      case "abstract":
        return new ModifierKeywordTreeImpl(Modifier.ABSTRACT, firstTokenIn(node, TerminalTokens.TokenNameabstract));
      case "final":
        return new ModifierKeywordTreeImpl(Modifier.FINAL, firstTokenIn(node, TerminalTokens.TokenNamefinal));
      case "native":
        return new ModifierKeywordTreeImpl(Modifier.NATIVE, firstTokenIn(node, TerminalTokens.TokenNamenative));
      case "synchronized":
        return new ModifierKeywordTreeImpl(Modifier.SYNCHRONIZED, firstTokenIn(node, TerminalTokens.TokenNamesynchronized));
      case "transient":
        return new ModifierKeywordTreeImpl(Modifier.TRANSIENT, firstTokenIn(node, TerminalTokens.TokenNametransient));
      case "volatile":
        return new ModifierKeywordTreeImpl(Modifier.VOLATILE, firstTokenIn(node, TerminalTokens.TokenNamevolatile));
      case "strictfp":
        return new ModifierKeywordTreeImpl(Modifier.STRICTFP, firstTokenIn(node, TerminalTokens.TokenNamestrictfp));
      case "default":
        return new ModifierKeywordTreeImpl(Modifier.DEFAULT, firstTokenIn(node, TerminalTokens.TokenNamedefault));
      case "sealed":
        return new ModifierKeywordTreeImpl(Modifier.SEALED, firstTokenIn(node, TerminalTokens.TokenNameRestrictedIdentifiersealed));
      case "non-sealed": {
        return new ModifierKeywordTreeImpl(Modifier.NON_SEALED, firstTokenIn(node, TerminalTokens.TokenNamenon_sealed));
      }
      default:
        throw new IllegalStateException(node.getKeyword().toString());
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
