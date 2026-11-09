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
package org.sonar.java.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiAssertStatement;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiBreakStatement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiContinueStatement;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDeconstructionPattern;
import com.intellij.psi.PsiDefaultCaseLabelElement;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEmptyStatement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionListStatement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiImplicitClass;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportModuleStatement;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiInstanceOfExpression;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiLabeledStatement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiPackageAccessibilityStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPattern;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiPostfixExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.PsiProvidesStatement;
import com.intellij.psi.PsiQualifiedExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiRequiresStatement;
import com.intellij.psi.PsiResourceExpression;
import com.intellij.psi.PsiResourceVariable;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiSwitchBlock;
import com.intellij.psi.PsiSwitchExpression;
import com.intellij.psi.PsiSwitchLabelStatementBase;
import com.intellij.psi.PsiSwitchLabeledRuleStatement;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.PsiSynchronizedStatement;
import com.intellij.psi.PsiThisExpression;
import com.intellij.psi.PsiThrowStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiTypeTestPattern;
import com.intellij.psi.PsiUnnamedPattern;
import com.intellij.psi.PsiUsesStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.PsiYieldStatement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.ArrayDimensionTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JLabelSymbol;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.KeywordSuper;
import org.sonar.java.model.KeywordThis;
import org.sonar.java.model.LineColumnConverter;
import org.sonar.java.model.TypeParameterTreeImpl;
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
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/** Builds the existing Sonar syntax and lazy semantic model from PSI. */
final class PsiParser {
  /** Converts a file in an owned PSI environment and attaches lazy semantics and diagnostics. */
  static JavaTree.CompilationUnitTreeImpl convert(JavaPsiEnvironment environment, Project project, PsiJavaFile file,
    JavaPsiConfiguration configuration, String source, Runnable cleaner) {
    var diagnostics = PsiDiagnosticProvider.create(file, configuration, source);
    var converter = new PsiParser(file, source, configuration.sourceVersion());
    var tree = converter.convert();
    var sema = new PsiSema(environment, project, file, cleaner, !configuration.runtimeArchives().isEmpty());
    PsiSemanticAttachment.attach(sema, tree, converter.semanticNodeMap(tree));
    diagnostics.attach(tree);
    return tree;
  }

  private final PsiJavaFile file;
  private final PsiTokenIndex tokens;
  private final String source;
  private final int sourceVersion;
  private final PsiTokenIndex.UnicodeInput input;
  private final LineColumnConverter positions;
  private final Map<PsiElement, List<Tree>> nodes = new IdentityHashMap<>();
  private final Map<Integer, PsiAnnotation> annotations = new LinkedHashMap<>();

  PsiParser(PsiJavaFile file) {
    this(file, file.getText());
  }

  PsiParser(PsiJavaFile file, String source) {
    this(file, source, 26);
  }

  private PsiParser(PsiJavaFile file, String source, int sourceVersion) {
    this.source = source;
    this.sourceVersion = sourceVersion;
    input = PsiTokenIndex.unicodeInput(source);
    positions = new LineColumnConverter(source);
    this.file = input.text().equals(file.getText()) ? file
      : (PsiJavaFile) PsiFileFactory.getInstance(file.getProject()).createFileFromText(file.getName(), file.getFileType(), input.text());
    tokens = new PsiTokenIndex(source, sourceVersion);
    for (var annotation : PsiTreeUtil.findChildrenOfType(this.file, PsiAnnotation.class)) {
      annotations.put(start(annotation), annotation);
    }
  }

  Map<PsiElement, List<Tree>> nodeMap() {
    Map<PsiElement, List<Tree>> result = new IdentityHashMap<>();
    nodes.forEach((element, trees) -> result.put(element, List.copyOf(trees)));
    return Collections.unmodifiableMap(result);
  }

  /** Completes semantic origins for syntax fragments assembled from the token cursor. */
  Map<PsiElement, List<Tree>> semanticNodeMap(JavaTree root) {
    Map<Tree, Boolean> known = new IdentityHashMap<>();
    nodes.values().forEach(trees -> trees.forEach(tree -> known.put(tree, true)));
    semanticOrigins(root, known);
    return nodeMap();
  }

  private void semanticOrigins(JavaTree tree, Map<Tree, Boolean> known) {
    if (!known.containsKey(tree) && tree instanceof AbstractTypedTree && tree.firstToken() != null) {
      int physical = offset(tree.firstToken());
      int translated = Arrays.binarySearch(input.offsets(), physical);
      if (translated < 0) {
        translated = -translated - 1;
      }
      var element = file.findElementAt(translated);
      while (element != null && !(element instanceof com.intellij.psi.PsiExpression) && !(element instanceof PsiJavaCodeReferenceElement)
        && !(element instanceof PsiTypeElement) && !(element instanceof PsiVariable) && !(element instanceof PsiClass)) {
        element = element.getParent();
      }
      if (element != null) {
        remember(element, tree);
      }
    }
    if (!tree.is(Tree.Kind.TOKEN, Tree.Kind.INFERED_TYPE)) {
      tree.getChildren().forEach(child -> semanticOrigins((JavaTree) child, known));
    }
  }

  JavaTree.CompilationUnitTreeImpl convert() {
    PackageDeclarationTree packageTree = null;
    var declaration = file.getPackageStatement();
    if (declaration != null) {
      packageTree = remember(declaration, new JavaTree.PackageDeclarationTreeImpl(
        annotationList(declaration.getAnnotationList()), token(declaration, "package"),
        name(declaration.getPackageReference()), token(declaration, ";")));
    }
    List<ImportClauseTree> imports = new ArrayList<>();
    List<Tree> types = new ArrayList<>();
    ModuleDeclarationTree module = null;
    for (var child : file.getChildren()) {
      if (child instanceof PsiImportList list) {
        for (var entry : list.getAllImportStatements()) {
          var reference = entry.getImportReference();
          boolean moduleImport = entry instanceof PsiImportModuleStatement;
          ExpressionTree imported = moduleImport ? dottedName(((PsiImportModuleStatement) entry).getModuleReference()) : name(reference);
          if (entry.isOnDemand() && !moduleImport) {
            imported = new MemberSelectExpressionTreeImpl(imported, last(entry, "."), new IdentifierTreeImpl(last(entry, "*")));
          }
          imports.add(remember(entry, new JavaTree.ImportTreeImpl(token(entry, "import"),
            entry instanceof PsiImportStaticStatement ? token(entry, "static") : null,
            moduleImport ? token(entry, "module") : null, imported, last(entry, ";"))));
          appendSemicolons(end(entry), end(list), imports);
        }
      } else if (child instanceof PsiClass cls) {
        types.add(classTree(cls));
        appendSemicolons(end(cls), end(file), types);
      } else if (child instanceof PsiJavaModule javaModule) {
        module = module(javaModule);
      } else if (child instanceof PsiErrorElement) {
        throw unsupported(child);
      }
    }
    if (file.getImportList() == null || file.getImportList().getAllImportStatements().length == 0) {
      if (types.isEmpty()) {
        appendSemicolons(0, end(file), imports);
      }
    }
    var result = remember(file, new JavaTree.CompilationUnitTreeImpl(packageTree, imports, types, module, tokens.eof()));
    parents(result);
    return result;
  }

  private static void parents(JavaTree tree) {
    if (tree.is(Tree.Kind.TOKEN, Tree.Kind.INFERED_TYPE)) {
      return;
    }
    for (var child : tree.getChildren()) {
      ((JavaTree) child).setParent(tree);
      parents((JavaTree) child);
    }
  }

  private <T extends Tree> T remember(PsiElement source, T tree) {
    nodes.computeIfAbsent(source, unused -> new ArrayList<>()).add(tree);
    return tree;
  }

  private int start(PsiElement element) {
    int offset = element.getTextRange().getStartOffset();
    return element.getContainingFile() == file ? input.offsets()[offset] : offset;
  }

  private int end(PsiElement element) {
    int offset = element.getTextRange().getEndOffset();
    return element.getContainingFile() == file ? input.offsets()[offset] : offset;
  }

  private InternalSyntaxToken token(PsiElement element, String text) {
    return tokens.find(start(element), end(element), text, false);
  }

  private InternalSyntaxToken last(PsiElement element, String text) {
    return tokens.find(start(element), end(element), text, true);
  }

  private InternalSyntaxToken exact(PsiElement element) {
    if (element == null) {
      throw new IllegalArgumentException("Missing source PSI element");
    }
    return tokens.slice(start(element), end(element));
  }

  private IdentifierTreeImpl identifier(PsiElement element) {
    return remember(element, new IdentifierTreeImpl(exact(element), sourceVersion >= 22 && element.getText().equals("_")));
  }

  private ExpressionTree name(PsiJavaCodeReferenceElement reference) {
    if (reference == null) {
      throw new IllegalArgumentException("Missing Java reference");
    }
    ExpressionTree result = identifier(reference.getReferenceNameElement());
    if (reference.getQualifier() instanceof PsiJavaCodeReferenceElement qualifier) {
      result = new MemberSelectExpressionTreeImpl(name(qualifier),
        tokens.find(end(qualifier), start(reference.getReferenceNameElement()), ".", false), (IdentifierTreeImpl) result);
    }
    return remember(reference, result);
  }

  private ExpressionTree dottedName(PsiElement element) {
    var cursor = new TypeCursor(start(element), end(element));
    ExpressionTree result = new IdentifierTreeImpl(cursor.take());
    while (cursor.more()) {
      var dot = cursor.take(".");
      result = new MemberSelectExpressionTreeImpl(result, dot, new IdentifierTreeImpl(cursor.take()));
    }
    return remember(element, result);
  }

  private void appendSemicolons(int offset, int limit, List list) {
    for (var lexeme : tokens.lexemes().subList(tokens.index(offset), tokens.lexemes().size())) {
      if (lexeme.start() >= limit || !lexeme.token().text().equals(";")) {
        break;
      }
      list.add(new EmptyStatementTreeImpl(lexeme.token()));
    }
  }

  private ModuleDeclarationTree module(PsiJavaModule module) {
    List<ModuleDirectiveTree> directives = new ArrayList<>();
    for (var child : module.getChildren()) {
      if (child instanceof PsiRequiresStatement requires) {
        var reference = requires.getModuleReference();
        var moduleName = reference == null
          ? singleModuleName(last(requires, "transitive")) : moduleName(reference.getElement());
        directives.add(new RequiresDirectiveTreeImpl(token(requires, "requires"), modifiers(requires.getModifierList()),
          moduleName, last(requires, ";")));
      } else if (child instanceof PsiPackageAccessibilityStatement access) {
        var names = ModuleNameListTreeImpl.emptyList();
        for (var reference : access.getModuleReferences()) {
          if (!names.isEmpty()) {
            names.separators().add(tokens.find(end(access.getPackageReference()), start(reference), ",", true));
          }
          names.add(moduleName(reference));
        }
        var to = names.isEmpty() ? null : token(access, "to");
        if (access.getRole() == PsiPackageAccessibilityStatement.Role.EXPORTS) {
          directives.add(new ExportsDirectiveTreeImpl(token(access, "exports"), name(access.getPackageReference()), to, names, last(access, ";")));
        } else {
          directives.add(new OpensDirectiveTreeImpl(token(access, "opens"), name(access.getPackageReference()), to, names, last(access, ";")));
        }
      } else if (child instanceof PsiUsesStatement uses) {
        directives.add(new UsesDirectiveTreeImpl(token(uses, "uses"), (TypeTree) name(uses.getClassReference()), last(uses, ";")));
      } else if (child instanceof PsiProvidesStatement provides) {
        var implementations = QualifiedIdentifierListTreeImpl.emptyList();
        var list = provides.getImplementationList();
        if (list != null) {
          for (var reference : list.getReferenceElements()) {
            if (!implementations.isEmpty()) {
              implementations.separators().add(tokens.find(start(list), start(reference), ",", true));
            }
            implementations.add((TypeTree) name(reference));
          }
        }
        directives.add(new ProvidesDirectiveTreeImpl(token(provides, "provides"), (TypeTree) name(provides.getInterfaceReference()),
          token(provides, "with"), implementations, last(provides, ";")));
      }
    }
    return remember(module, new ModuleDeclarationTreeImpl(annotationList(module.getModifierList()),
      module.hasModifierProperty("open") ? token(module, "open") : null,
      token(module, "module"), moduleName(module.getNameIdentifier()), token(module, "{"), directives, last(module, "}")));
  }

  private ModuleNameTreeImpl singleModuleName(InternalSyntaxToken name) {
    var moduleName = ModuleNameTreeImpl.emptyList();
    moduleName.add(new IdentifierTreeImpl(name));
    return moduleName;
  }

  private ModuleNameTreeImpl moduleName(PsiElement reference) {
    var result = ModuleNameTreeImpl.emptyList();
    var cursor = new TypeCursor(start(reference), end(reference));
    while (cursor.more()) {
      result.add(new IdentifierTreeImpl(cursor.take()));
      if (cursor.more()) {
        cursor.take(".");
      }
    }
    return result;
  }

  private List<AnnotationTree> annotationList(PsiModifierList list) {
    if (list == null) {
      return List.of();
    }
    return Arrays.stream(list.getAnnotations()).map(a -> (AnnotationTree) annotation(a)).toList();
  }

  private ModifiersTreeImpl modifiers(PsiModifierList list) {
    List<ModifierTree> result = new ArrayList<>();
    if (list != null) {
      for (var child : list.getChildren()) {
        if (child instanceof PsiAnnotation annotation) {
          result.add(annotation(annotation));
        } else if (child instanceof PsiKeyword) {
          String keyword = PsiTokenIndex.decoded(child.getText());
          result.add(new ModifierKeywordTreeImpl(Modifier.valueOf(keyword.toUpperCase(java.util.Locale.ROOT).replace('-', '_')), exact(child)));
        }
      }
    }
    return new ModifiersTreeImpl(result);
  }

  private TypeTree type(PsiTypeElement element) {
    if (element == null) {
      return new InferedTypeTree();
    }
    return remember(element, new TypeCursor(start(element), end(element)).completeType());
  }

  private final class TypeCursor {
    private int offset;
    private int consumedEnd;
    private int physicalEnd;
    private final int limit;
    private boolean includeDimensions = true;

    TypeCursor(int offset, int limit) {
      this.offset = offset;
      this.limit = limit;
    }

    private PsiTokenIndex.Lexeme current() {
      var entries = tokens.lexemes();
      int i = tokens.index(offset);
      if (i > 0 && entries.get(i - 1).end() > offset) {
        return entries.get(i - 1);
      }
      var result = entries.get(i);
      offset = result.start();
      return result;
    }

    boolean more() {
      return current().start() < limit && offset < limit;
    }

    String peek() {
      var entry = current();
      return PsiTokenIndex.decoded(source.substring(offset, entry.end()));
    }

    InternalSyntaxToken take() {
      var entry = current();
      var result = tokens.slice(offset, entry.end());
      offset = entry.end();
      consumedEnd = offset;
      physicalEnd = offset;
      return result;
    }

    InternalSyntaxToken take(String expected) {
      if (expected.equals(">") && peek().startsWith(">")) {
        int end = offset + 1;
        if (source.charAt(offset) == '\\') {
          while (source.charAt(end) == 'u') {
            end++;
          }
          end += 4;
        }
        var result = tokens.slice(offset, end);
        offset = end;
        consumedEnd = offset;
        physicalEnd = offset;
        return result;
      }
      if (!peek().equals(expected)) {
        throw new IllegalArgumentException("Expected '" + expected + "' at " + offset + ", got " + peek());
      }
      return take();
    }

    List<AnnotationTree> annotations() {
      List<AnnotationTree> result = new ArrayList<>();
      while (more() && peek().equals("@")) {
        var psi = PsiParser.this.annotations.get(current().start());
        if (psi == null) {
          throw new IllegalArgumentException("Missing annotation PSI at " + offset);
        }
        result.add(annotation(psi));
        offset = end(psi);
        consumedEnd = offset;
      }
      return result;
    }

    TypeTree completeType() {
      var result = type();
      if (more()) {
        throw new IllegalArgumentException("Unconverted type syntax at UTF-16 offset " + offset);
      }
      return result;
    }

    TypeTree type() {
      return type(true);
    }

    private TypeTree type(boolean union) {
      var leading = annotations();
      String keyword = peek();
      TypeTree result;
      if (keyword.equals("?")) {
        var question = take();
        JavaTree.WildcardTreeImpl wildcard;
        if (more() && (peek().equals("extends") || peek().equals("super"))) {
          var bound = take();
          wildcard = new JavaTree.WildcardTreeImpl(bound.text().equals("extends") ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD, bound, type()).complete(question);
        } else {
          wildcard = new JavaTree.WildcardTreeImpl(question);
        }
        wildcard.complete(leading);
        return wildcard;
      }
      if (List.of("boolean", "byte", "char", "short", "int", "long", "float", "double", "void").contains(keyword)) {
        var primitive = new JavaTree.PrimitiveTypeTreeImpl(take());
        primitive.complete(leading);
        result = primitive;
      } else {
        JavaTree.AnnotatedTypeTree named = keyword.equals("var") ? new VarTypeTreeImpl(take()) : new IdentifierTreeImpl(take());
        named.complete(leading);
        result = named;
        while (more()) {
          if (peek().equals("<")) {
            result = new JavaTree.ParameterizedTypeTreeImpl(result, arguments());
          } else if (peek().equals(".")) {
            var dot = take();
            var annotated = annotations();
            var identifier = new IdentifierTreeImpl(take());
            identifier.complete(annotated);
            result = new MemberSelectExpressionTreeImpl((ExpressionTree) result, dot, identifier);
          } else {
            break;
          }
        }
      }
      while (includeDimensions && more() && (peek().equals("[") || peek().equals("...") || peek().equals("@"))) {
        var annotated = annotations();
        if (peek().equals("...")) {
          result = new JavaTree.ArrayTypeTreeImpl(result, (List) annotated, take());
        } else {
          var left = take("[");
          result = new JavaTree.ArrayTypeTreeImpl(result, (List) annotated, left, take("]"));
        }
      }
      if (union && more() && peek().equals("|")) {
        var alternatives = QualifiedIdentifierListTreeImpl.emptyList();
        alternatives.add(result);
        while (more() && peek().equals("|")) {
          alternatives.separators().add(take());
          alternatives.add(type(false));
        }
        return new JavaTree.UnionTypeTreeImpl(alternatives);
      }
      return result;
    }

    TypeArgumentListTreeImpl arguments() {
      return arguments(true);
    }

    /** Retains the reference frontend's synthetic bracket positions, including comment-adjacent and nested brackets. */
    TypeArgumentListTreeImpl arguments(boolean typeSyntax) {
      var left = take("<");
      List<TypeTree> arguments = new ArrayList<>();
      List<SyntaxToken> separators = new ArrayList<>();
      while (!peek().startsWith(">")) {
        arguments.add(type());
        if (!peek().equals(",")) {
          break;
        }
        separators.add(take());
      }
      int logicalEnd = consumedEnd + 1;
      boolean inheritedPosition = consumedEnd != physicalEnd;
      var close = take(">");
      if (typeSyntax) {
        logicalEnd = inheritedPosition || !close.trivias().isEmpty() ? logicalEnd : physicalEnd;
        var pos = positions.toPos(logicalEnd - 1);
        close = new InternalSyntaxToken(pos.line(), pos.columnOffset(), ">", List.of(), false);
        consumedEnd = logicalEnd;
      }
      var result = new TypeArgumentListTreeImpl(left, close);
      result.addAll(arguments);
      result.separators().addAll(separators);
      return result;
    }
  }

  private ClassTreeImpl classTree(PsiClass cls) {
    List<Tree> members = new ArrayList<>();
    for (var child : cls.getChildren()) {
      if (child instanceof PsiEnumConstant constant) {
        members.add(enumConstant(constant));
      } else if (child instanceof PsiField field) {
        var variable = variable(field);
        int index = tokens.index(end(field));
        var separator = index > 0 ? tokens.lexemes().get(index - 1).token() : tokens.at(end(field));
        if (!separator.text().equals(",") && !separator.text().equals(";")) {
          separator = tokens.at(end(field));
        }
        if (separator.text().equals(",") || separator.text().equals(";")) {
          variable.setEndToken(separator);
        }
        members.add(variable);
      } else if (child instanceof PsiMethod method) {
        members.add(method(method));
      } else if (child instanceof PsiClass nested) {
        members.add(classTree(nested));
      } else if (child instanceof PsiClassInitializer initializer) {
        var block = block(initializer.getBody());
        members.add(initializer.hasModifierProperty("static")
          ? new StaticInitializerTreeImpl(token(initializer, "static"), (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken())
          : new BlockTreeImpl(Tree.Kind.INITIALIZER, (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken()));
      } else if (child instanceof PsiJavaToken && child.textMatches(";")) {
        var previous = PsiTreeUtil.skipWhitespacesAndCommentsBackward(child);
        boolean enumSeparator = cls.isEnum() && previous != null && (previous.textMatches(",") || previous instanceof PsiEnumConstant);
        if (!enumSeparator && (members.isEmpty() || !(members.getLast() instanceof VariableTree variable) || variable.endToken() != exact(child))) {
          members.add(new EmptyStatementTreeImpl(exact(child)));
        }
      }
    }
    if (cls instanceof PsiImplicitClass) {
      return remember(cls, new ClassTreeImpl(Tree.Kind.IMPLICIT_CLASS, null, members, null));
    }
    var kind = cls.isAnnotationType() ? Tree.Kind.ANNOTATION_TYPE : cls.isEnum() ? Tree.Kind.ENUM
      : cls.isRecord() ? Tree.Kind.RECORD : cls.isInterface() ? Tree.Kind.INTERFACE : Tree.Kind.CLASS;
    var tree = new ClassTreeImpl(kind, exact(cls.getLBrace()), members, exact(cls.getRBrace()));
    if (!(cls instanceof PsiAnonymousClass)) {
      String keyword = cls.isAnnotationType() || cls.isInterface() ? "interface" : cls.isEnum() ? "enum" : cls.isRecord() ? "record" : "class";
      tree.complete(modifiers(cls.getModifierList()), tokens.find(cls.getModifierList() == null ? start(cls) : end(cls.getModifierList()), start(cls.getNameIdentifier()), keyword, false), identifier(cls.getNameIdentifier()));
      if (cls.isAnnotationType()) {
        tree.completeAtToken(tokens.find(cls.getModifierList() == null ? start(cls) : end(cls.getModifierList()), start(cls.getNameIdentifier()), "@", false));
      } else if (!cls.isEnum()) {
        tree.completeTypeParameters(typeParameters(cls.getTypeParameterList()));
      }
      var extendsList = cls.getExtendsList();
      var implementsList = cls.getImplementsList();
      if (extendsList != null && extendsList.getReferenceElements().length > 0) {
        if (cls.isInterface()) {
          tree.completeInterfaces(token(extendsList, "extends"), typeReferences(extendsList));
        } else {
          var reference = extendsList.getReferenceElements()[0];
          tree.completeSuperclass(token(extendsList, "extends"), typeReference(reference));
        }
      }
      if (implementsList != null && implementsList.getReferenceElements().length > 0) {
        tree.completeInterfaces(token(implementsList, "implements"), typeReferences(implementsList));
      }
      var permits = cls.getPermitsList();
      if (permits != null && permits.getReferenceElements().length > 0) {
        tree.completePermittedTypes(token(permits, "permits"), typeReferences(permits));
      }
      var header = cls.getRecordHeader();
      if (header != null) {
        List<VariableTree> components = new ArrayList<>();
        for (var component : header.getRecordComponents()) {
          var variable = variable(component);
          if (tokens.at(end(component)).text().equals(",")) {
            variable.setEndToken(tokens.at(end(component)));
          }
          components.add(variable);
        }
        tree.completeRecordComponents(token(header, "("), components, last(header, ")"));
      }
    }
    return remember(cls, tree);
  }

  private TypeTree typeReference(PsiJavaCodeReferenceElement reference) {
    return remember(reference, new TypeCursor(start(reference), end(reference)).completeType());
  }

  private QualifiedIdentifierListTreeImpl typeReferences(PsiReferenceList source) {
    var result = QualifiedIdentifierListTreeImpl.emptyList();
    if (source != null) {
      for (var reference : source.getReferenceElements()) {
        if (!result.isEmpty()) {
          result.separators().add(tokens.find(start(source), start(reference), ",", true));
        }
        result.add(typeReference(reference));
      }
    }
    return result;
  }

  private TypeParameterListTreeImpl typeParameters(PsiTypeParameterList source) {
    if (source == null || source.getTypeParameters().length == 0) {
      return new TypeParameterListTreeImpl();
    }
    var result = new TypeParameterListTreeImpl(token(source, "<"), tokens.slice(end(source) - 1, end(source)));
    for (var parameter : source.getTypeParameters()) {
      if (!result.isEmpty()) {
        result.separators().add(tokens.find(start(source), start(parameter), ",", true));
      }
      var name = identifier(parameter.getNameIdentifier());
      name.complete(Arrays.stream(parameter.getAnnotations()).map(a -> (AnnotationTree) annotation(a)).toList());
      var references = parameter.getExtendsList().getReferenceElements();
      TypeParameterTreeImpl tree;
      if (references.length == 0) {
        tree = new TypeParameterTreeImpl(name);
      } else {
        var bounds = QualifiedIdentifierListTreeImpl.emptyList();
        for (var reference : references) {
          if (!bounds.isEmpty()) {
            bounds.separators().add(tokens.find(start(parameter), start(reference), "&", true));
          }
          bounds.add(typeReference(reference));
        }
        tree = new TypeParameterTreeImpl(name, token(parameter, "extends"), bounds);
      }
      result.add(remember(parameter, tree));
    }
    return result;
  }

  private final Map<PsiElement, TypeTree> sharedTypes = new IdentityHashMap<>();
  private final Map<PsiModifierList, ModifiersTreeImpl> sharedModifiers = new IdentityHashMap<>();

  private VariableTreeImpl variable(PsiVariable variable) {
    var name = identifier(variable.getNameIdentifier());
    var sourceType = variable.getTypeElement();
    TypeTree type = sourceType == null ? new InferedTypeTree() : sharedTypes.computeIfAbsent(sourceType, this::typeElement);
    int afterName = end(variable.getNameIdentifier());
    int limit = variable.getInitializer() == null ? end(variable) : start(variable.getInitializer());
    type = dimensions(type, afterName, limit);
    var modifiers = sharedModifiers.computeIfAbsent(variable.getModifierList(), this::modifiers);
    var result = new VariableTreeImpl(modifiers, type, name);
    if (variable.getInitializer() != null) {
      result.completeTypeAndInitializer(type, tokens.find(afterName, start(variable.getInitializer()), "=", false), expression(variable.getInitializer()));
    }
    return remember(variable, result);
  }

  private TypeTree typeElement(PsiElement element) {
    return type((PsiTypeElement) element);
  }

  private TypeTree dimensions(TypeTree type, int offset, int limit) {
    var cursor = new TypeCursor(offset, limit);
    while (cursor.more() && (cursor.peek().equals("[") || cursor.peek().equals("@"))) {
      var annotations = cursor.annotations();
      var open = cursor.take("[");
      type = new JavaTree.ArrayTypeTreeImpl(type, (List) annotations, open, cursor.take("]"));
    }
    return type;
  }

  private MethodTreeImpl method(PsiMethod method) {
    var sourceParameters = method.getParameterList();
    boolean hasParentheses = sourceParameters.getText() != null && sourceParameters.getText().startsWith("(");
    var parameters = new FormalParametersListTreeImpl(hasParentheses ? token(sourceParameters, "(") : null, hasParentheses ? last(sourceParameters, ")") : null);
    for (var parameter : hasParentheses ? sourceParameters.getParameters() : PsiParameter.EMPTY_ARRAY) {
      var variable = variable(parameter);
      if (tokens.at(end(parameter)).text().equals(",")) {
        variable.setEndToken(tokens.at(end(parameter)));
      }
      parameters.add(variable);
    }
    var returnType = method.isConstructor() ? null : dimensions(type(method.getReturnTypeElement()), (hasParentheses ? end(sourceParameters) : end(method.getNameIdentifier())),
      method.getBody() == null ? end(method) : start(method.getBody()));
    MethodTreeImpl result;
    if (method instanceof PsiAnnotationMethod annotationMethod) {
      var value = annotationMethod.getDefaultValue();
      result = new MethodTreeImpl(parameters, value == null ? null : token(method, "default"), value == null ? null : expression(value))
        .complete(returnType, identifier(method.getNameIdentifier()), last(method, ";"));
    } else {
      var exceptions = typeReferences(method.getThrowsList());
      result = new MethodTreeImpl(returnType, identifier(method.getNameIdentifier()), parameters,
        exceptions.isEmpty() ? null : token(method.getThrowsList(), "throws"), exceptions,
        method.getBody() == null ? null : block(method.getBody()), method.getBody() == null ? last(method, ";") : null)
        .completeWithTypeParameters(typeParameters(method.getTypeParameterList()));
    }
    return remember(method, result.completeWithModifiers(modifiers(method.getModifierList())));
  }

  private EnumConstantTreeImpl enumConstant(PsiEnumConstant constant) {
    var name = identifier(constant.getNameIdentifier());
    var separator = tokens.at(end(constant));
    return remember(constant, new EnumConstantTreeImpl(modifiers(constant.getModifierList()), name,
      new NewClassTreeImpl(name, arguments(constant.getArgumentList()), constant.getInitializingClass() == null ? null : classTree(constant.getInitializingClass())),
      separator.text().equals(",") || separator.text().equals(";") ? separator : null));
  }

  private AnnotationTreeImpl annotation(PsiAnnotation annotation) {
    var arguments = ArgumentListTreeImpl.emptyList();
    var attributes = annotation.getParameterList().getAttributes();
    for (var attribute : attributes) {
      ExpressionTree value = expression(attribute.getValue());
      if (attribute.getNameIdentifier() != null) {
        value = new AssignmentExpressionTreeImpl(Tree.Kind.ASSIGNMENT, identifier(attribute.getNameIdentifier()), token(attribute, "="), value);
      }
      arguments.add(value);
      if (tokens.at(end(attribute)).text().equals(",")) {
        arguments.separators().add(tokens.at(end(attribute)));
      }
    }
    if (annotation.getParameterList().getText().startsWith("(")) {
      arguments.complete(token(annotation.getParameterList(), "("), last(annotation.getParameterList(), ")"));
    }
    return remember(annotation, new AnnotationTreeImpl(token(annotation, "@"), (TypeTree) name(annotation.getNameReferenceElement()), arguments));
  }

  private ExpressionTree expression(PsiElement element) {
    if (element == null) {
      throw new IllegalArgumentException("Missing expression");
    }
    ExpressionTree result = switch (element) {
      case PsiAnnotation annotation -> annotation(annotation);
      case PsiMethodReferenceExpression reference -> methodReference(reference);
      case PsiReferenceExpression reference -> reference(reference);
      case PsiLiteralExpression literal -> literal(literal);
      case PsiParenthesizedExpression parenthesized -> new ParenthesizedTreeImpl(token(parenthesized, "("), expression(parenthesized.getExpression()), last(parenthesized, ")"));
      case PsiAssignmentExpression assignment -> new AssignmentExpressionTreeImpl(operator(PsiTokenIndex.decoded(assignment.getOperationSign().getText()), false, false),
        expression(assignment.getLExpression()), exact(assignment.getOperationSign()), expression(assignment.getRExpression()));
      case PsiPolyadicExpression polyadic -> binary(polyadic);
      case PsiPrefixExpression prefix -> new InternalPrefixUnaryExpression(operator(PsiTokenIndex.decoded(prefix.getOperationSign().getText()), true, false), exact(prefix.getOperationSign()), expression(prefix.getOperand()));
      case PsiPostfixExpression postfix -> new InternalPostfixUnaryExpression(operator(PsiTokenIndex.decoded(postfix.getOperationSign().getText()), true, true), expression(postfix.getOperand()), exact(postfix.getOperationSign()));
      case PsiConditionalExpression conditional -> new ConditionalExpressionTreeImpl(expression(conditional.getCondition()),
        tokens.find(end(conditional.getCondition()), start(conditional.getThenExpression()), "?", false), expression(conditional.getThenExpression()),
        tokens.find(end(conditional.getThenExpression()), start(conditional.getElseExpression()), ":", false), expression(conditional.getElseExpression()));
      case PsiMethodCallExpression call -> new MethodInvocationTreeImpl(reference(call.getMethodExpression()), typeArguments(call.getTypeArgumentList()), arguments(call.getArgumentList()));
      case PsiNewExpression creation -> newExpression(creation);
      case PsiArrayInitializerExpression initializer -> arrayInitializer(initializer, initializer.getInitializers());
      case PsiArrayInitializerMemberValue initializer -> arrayInitializer(initializer, initializer.getInitializers());
      case PsiArrayAccessExpression access -> new ArrayAccessExpressionTreeImpl(expression(access.getArrayExpression()), new ArrayDimensionTreeImpl(
        tokens.find(end(access.getArrayExpression()), start(access.getIndexExpression()), "[", false), expression(access.getIndexExpression()), last(access, "]")));
      case PsiTypeCastExpression cast -> cast(cast);
      case PsiInstanceOfExpression instanceOf -> instanceOf.getPattern() == null
        ? new InstanceOfTreeImpl(expression(instanceOf.getOperand()), token(instanceOf, "instanceof"), type(instanceOf.getCheckType()))
        : new InstanceOfTreeImpl(expression(instanceOf.getOperand()), token(instanceOf, "instanceof"), pattern(instanceOf.getPattern()));
      case PsiThisExpression thisExpression -> keyword(thisExpression, "this");
      case PsiSuperExpression superExpression -> keyword(superExpression, "super");
      case PsiClassObjectAccessExpression access -> new MemberSelectExpressionTreeImpl((ExpressionTree) type(access.getOperand()), last(access, "."), new IdentifierTreeImpl(last(access, "class")));
      case PsiLambdaExpression lambda -> lambda(lambda);
      case PsiSwitchExpression expression -> switchExpression(expression);
      default -> throw unsupported(element);
    };
    return remember(element, result);
  }

  private IllegalArgumentException unsupported(PsiElement element) {
    return new IllegalArgumentException("Unsupported PSI syntax: " + element.getClass().getName() + " at " + start(element) + ": " + element.getText());
  }

  private ExpressionTree reference(PsiReferenceExpression reference) {
    var name = identifier(reference.getReferenceNameElement());
    var qualifier = reference.getQualifierExpression();
    return qualifier == null ? name : new MemberSelectExpressionTreeImpl(expression(qualifier),
      tokens.find(end(qualifier), start(reference.getReferenceNameElement()), ".", false), name);
  }

  private ExpressionTree keyword(PsiQualifiedExpression element, String keyword) {
    var source = (PsiElement) element;
    IdentifierTreeImpl name = keyword.equals("this") ? new KeywordThis(last(source, keyword), org.sonar.plugins.java.api.semantic.Symbol.UNKNOWN_SYMBOL) : new KeywordSuper(last(source, keyword), org.sonar.plugins.java.api.semantic.Symbol.UNKNOWN_SYMBOL);
    var qualifier = element.getQualifier();
    return qualifier == null ? name : new MemberSelectExpressionTreeImpl(name(qualifier), last(source, "."), name);
  }

  private LiteralTreeImpl literal(PsiLiteralExpression literal) {
    var value = tokens.at(start(literal));
    String kind = literal.getFirstChild().getNode().getElementType().toString();
    Tree.Kind treeKind = switch (kind) {
      case "INTEGER_LITERAL" -> Tree.Kind.INT_LITERAL;
      case "LONG_LITERAL" -> Tree.Kind.LONG_LITERAL;
      case "FLOAT_LITERAL" -> Tree.Kind.FLOAT_LITERAL;
      case "DOUBLE_LITERAL" -> Tree.Kind.DOUBLE_LITERAL;
      case "CHARACTER_LITERAL" -> Tree.Kind.CHAR_LITERAL;
      case "TEXT_BLOCK_LITERAL" -> Tree.Kind.TEXT_BLOCK;
      case "STRING_LITERAL" -> Tree.Kind.STRING_LITERAL;
      case "TRUE_KEYWORD", "FALSE_KEYWORD" -> Tree.Kind.BOOLEAN_LITERAL;
      case "NULL_KEYWORD" -> Tree.Kind.NULL_LITERAL;
      default -> throw unsupported(literal);
    };
    return new LiteralTreeImpl(treeKind, value);
  }

  private ExpressionTree binary(PsiPolyadicExpression polyadic) {
    var operands = polyadic.getOperands();
    ExpressionTree result = expression(operands[0]);
    for (int i = 1; i < operands.length; i++) {
      var sign = polyadic.getTokenBeforeOperand(operands[i]);
      result = new BinaryExpressionTreeImpl(operator(sign.getText(), false, false), result, exact(sign), expression(operands[i]));
      var origin = i == operands.length - 1 ? polyadic : JavaPsiFacade.getElementFactory(file.getProject()).createExpressionFromText(
        polyadic.getText().substring(0, operands[i].getTextRange().getEndOffset() - polyadic.getTextRange().getStartOffset()), polyadic);
      remember(origin, result);
    }
    return result;
  }

  private static Tree.Kind operator(String text, boolean unary, boolean postfix) {
    if (unary) {
      return switch (text) {
        case "+" -> Tree.Kind.UNARY_PLUS;
        case "-" -> Tree.Kind.UNARY_MINUS;
        case "!" -> Tree.Kind.LOGICAL_COMPLEMENT;
        case "~" -> Tree.Kind.BITWISE_COMPLEMENT;
        case "++" -> postfix ? Tree.Kind.POSTFIX_INCREMENT : Tree.Kind.PREFIX_INCREMENT;
        case "--" -> postfix ? Tree.Kind.POSTFIX_DECREMENT : Tree.Kind.PREFIX_DECREMENT;
        default -> throw new IllegalArgumentException("Unknown unary operator " + text);
      };
    }
    return switch (text) {
      case "*" -> Tree.Kind.MULTIPLY;
      case "/" -> Tree.Kind.DIVIDE;
      case "%" -> Tree.Kind.REMAINDER;
      case "+" -> Tree.Kind.PLUS;
      case "-" -> Tree.Kind.MINUS;
      case "<<" -> Tree.Kind.LEFT_SHIFT;
      case ">>" -> Tree.Kind.RIGHT_SHIFT;
      case ">>>" -> Tree.Kind.UNSIGNED_RIGHT_SHIFT;
      case "<" -> Tree.Kind.LESS_THAN;
      case ">" -> Tree.Kind.GREATER_THAN;
      case "<=" -> Tree.Kind.LESS_THAN_OR_EQUAL_TO;
      case ">=" -> Tree.Kind.GREATER_THAN_OR_EQUAL_TO;
      case "==" -> Tree.Kind.EQUAL_TO;
      case "!=" -> Tree.Kind.NOT_EQUAL_TO;
      case "^" -> Tree.Kind.XOR;
      case "|" -> Tree.Kind.OR;
      case "&" -> Tree.Kind.AND;
      case "||" -> Tree.Kind.CONDITIONAL_OR;
      case "&&" -> Tree.Kind.CONDITIONAL_AND;
      case "=" -> Tree.Kind.ASSIGNMENT;
      case "+=" -> Tree.Kind.PLUS_ASSIGNMENT;
      case "-=" -> Tree.Kind.MINUS_ASSIGNMENT;
      case "*=" -> Tree.Kind.MULTIPLY_ASSIGNMENT;
      case "/=" -> Tree.Kind.DIVIDE_ASSIGNMENT;
      case "%=" -> Tree.Kind.REMAINDER_ASSIGNMENT;
      case "&=" -> Tree.Kind.AND_ASSIGNMENT;
      case "|=" -> Tree.Kind.OR_ASSIGNMENT;
      case "^=" -> Tree.Kind.XOR_ASSIGNMENT;
      case "<<=" -> Tree.Kind.LEFT_SHIFT_ASSIGNMENT;
      case ">>=" -> Tree.Kind.RIGHT_SHIFT_ASSIGNMENT;
      case ">>>=" -> Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
      default -> throw new IllegalArgumentException("Unknown binary operator " + text);
    };
  }

  private ArgumentListTreeImpl arguments(PsiExpressionList list) {
    var result = ArgumentListTreeImpl.emptyList();
    if (list != null && !list.getText().isEmpty()) {
      result.complete(token(list, "("), last(list, ")"));
      for (var argument : list.getExpressions()) {
        result.add(expression(argument));
        if (tokens.at(end(argument)).text().equals(",")) {
          result.separators().add(tokens.at(end(argument)));
        }
      }
    }
    return result;
  }

  private TypeArgumentListTreeImpl typeArguments(PsiReferenceParameterList list) {
    return list == null || list.getText().isEmpty() ? null : new TypeCursor(start(list), end(list)).arguments(false);
  }

  private ExpressionTree cast(PsiTypeCastExpression cast) {
    var source = cast.getCastType();
    var cursor = new TypeCursor(start(source), end(source));
    var first = cursor.type();
    if (cursor.more() && cursor.peek().equals("&")) {
      var ampersand = cursor.take();
      var bounds = QualifiedIdentifierListTreeImpl.emptyList();
      while (cursor.more()) {
        bounds.add(cursor.type());
        if (cursor.more()) {
          bounds.separators().add(cursor.take("&"));
        }
      }
      return new TypeCastExpressionTreeImpl(token(cast, "("), first, ampersand, bounds,
        tokens.find(end(source), start(cast.getOperand()), ")", false), expression(cast.getOperand()));
    }
    return new TypeCastExpressionTreeImpl(token(cast, "("), remember(source, first), tokens.find(end(source), start(cast.getOperand()), ")", false), expression(cast.getOperand()));
  }

  private ExpressionTree newExpression(PsiNewExpression creation) {
    if (creation.isArrayCreation() || creation.getClassOrAnonymousClassReference() == null && creation.getType() instanceof com.intellij.psi.PsiArrayType) {
      return newArray(creation);
    }
    var result = new NewClassTreeImpl(typeReference(creation.getClassOrAnonymousClassReference()), arguments(creation.getArgumentList()),
      creation.getAnonymousClass() == null ? null : classTree(creation.getAnonymousClass()))
      .completeWithNewKeyword(tokens.find(creation.getQualifier() == null ? start(creation) : end(creation.getQualifier()),
        start(creation.getClassOrAnonymousClassReference()), "new", false)).completeWithTypeArguments(typeArguments(creation.getTypeArgumentList()));
    if (creation.getQualifier() != null) {
      result.completeWithEnclosingExpression(expression(creation.getQualifier()));
      result.completeWithDotToken(tokens.find(end(creation.getQualifier()), start(creation.getClassOrAnonymousClassReference()), ".", false));
    }
    return result;
  }

  private NewArrayTreeImpl arrayInitializer(PsiElement element, PsiElement[] values) {
    var initializers = InitializerListTreeImpl.emptyList();
    for (var value : values) {
      initializers.add(expression(value));
      if (tokens.at(end(value)).text().equals(",")) {
        initializers.separators().add(tokens.at(end(value)));
      }
    }
    return new NewArrayTreeImpl(List.of(), initializers).completeWithCurlyBraces(token(element, "{"), last(element, "}"));
  }

  private NewArrayTreeImpl newArray(PsiNewExpression creation) {
    var newKeyword = token(creation, "new");
    int offset = offset(newKeyword) + newKeyword.text().length();
    var cursor = new TypeCursor(offset, end(creation));
    cursor.includeDimensions = false;
    var component = cursor.type();
    List<ArrayDimensionTree> dimensions = new ArrayList<>();
    var expressions = Arrays.asList(creation.getArrayDimensions());
    while (cursor.more() && (cursor.peek().equals("[") || cursor.peek().equals("@"))) {
      var annotations = cursor.annotations();
      var left = cursor.take("[");
      ExpressionTree expression = null;
      if (!cursor.peek().equals("]")) {
        int start = cursor.offset;
        var dimension = expressions.stream().filter(e -> start(e) == start).findFirst().orElseThrow();
        expression = expression(dimension);
        cursor.offset = end(dimension);
      }
      var dimension = new ArrayDimensionTreeImpl(left, expression, cursor.take("]")).completeAnnotations(annotations);
      if (expression != null || creation.getArrayInitializer() != null) {
        dimensions.add(dimension);
      }
    }
    var result = creation.getArrayInitializer() == null ? new NewArrayTreeImpl(dimensions, InitializerListTreeImpl.emptyList())
      : arrayInitializer(creation.getArrayInitializer(), creation.getArrayInitializer().getInitializers()).completeDimensions(dimensions);
    return result.complete(component).completeWithNewKeyword(newKeyword);
  }

  private LambdaExpressionTreeImpl lambda(PsiLambdaExpression lambda) {
    List<VariableTree> parameters = new ArrayList<>();
    for (var parameter : lambda.getParameterList().getParameters()) {
      var result = variable(parameter);
      if (tokens.at(end(parameter)).text().equals(",")) {
        result.setEndToken(tokens.at(end(parameter)));
      }
      parameters.add(result);
    }
    var body = lambda.getBody();
    boolean parentheses = lambda.getParameterList().getText().startsWith("(");
    return new LambdaExpressionTreeImpl(parentheses ? token(lambda.getParameterList(), "(") : null, parameters,
      parentheses ? last(lambda.getParameterList(), ")") : null, tokens.find(end(lambda.getParameterList()), start(body), "->", false),
      body instanceof PsiCodeBlock block ? block(block) : expression(body));
  }

  private MethodReferenceTreeImpl methodReference(PsiMethodReferenceExpression reference) {
    var qualifier = reference.getQualifierExpression();
    var type = reference.getQualifierType();
    var result = new MethodReferenceTreeImpl(qualifier == null ? type(type) : expression(qualifier), token(reference, "::"));
    result.complete(typeArguments(reference.getParameterList()), identifier(reference.getReferenceNameElement()));
    return result;
  }

  private PatternTree pattern(PsiPattern pattern) {
    PatternTree result;
    if (pattern instanceof PsiTypeTestPattern type) {
      result = new TypePatternTreeImpl(variable(type.getPatternVariable()), Type.UNKNOWN);
    } else if (pattern instanceof PsiDeconstructionPattern record) {
      var nested = Arrays.stream(record.getDeconstructionList().getDeconstructionComponents()).map(this::pattern).toList();
      result = new RecordPatternTreeImpl(type(record.getTypeElement()), token(record.getDeconstructionList(), "("), nested, last(record.getDeconstructionList(), ")"));
    } else if (pattern instanceof PsiUnnamedPattern unnamed) {
      result = new TypePatternTreeImpl(new VariableTreeImpl(new IdentifierTreeImpl(token(unnamed, "_"), true)), Type.UNKNOWN);
    } else {
      throw unsupported(pattern);
    }
    return remember(pattern, result);
  }

  private BlockTreeImpl block(PsiCodeBlock block) {
    List<StatementTree> statements = new ArrayList<>();
    for (var statement : block.getStatements()) {
      addStatement(statement, statements, true);
    }
    return remember(block, new BlockTreeImpl(exact(block.getLBrace()), statements, exact(block.getRBrace())));
  }

  private void addStatement(PsiStatement statement, List<StatementTree> target, boolean semicolon) {
    if (statement instanceof PsiDeclarationStatement declaration) {
      var declarations = declaration.getDeclaredElements();
      for (int i = 0; i < declarations.length; i++) {
        if (declarations[i] instanceof PsiVariable variable) {
          var tree = variable(variable);
          if (i < declarations.length - 1) {
            tree.setEndToken(tokens.find(end(variable), end(declaration), ",", false));
          } else if (semicolon) {
            tree.setEndToken(last(declaration, ";"));
          }
          target.add(tree);
        } else if (declarations[i] instanceof PsiClass cls) {
          target.add(classTree(cls));
        } else {
          throw unsupported(declarations[i]);
        }
      }
    } else if (!semicolon && statement instanceof PsiEmptyStatement) {
      return;
    } else if (!semicolon && statement instanceof PsiExpressionStatement expression) {
      target.add(new ExpressionStatementTreeImpl(expression(expression.getExpression()), null));
    } else if (statement instanceof PsiExpressionListStatement expressions) {
      for (var expression : expressions.getExpressionList().getExpressions()) {
        target.add(new ExpressionStatementTreeImpl(expression(expression), null));
      }
    } else {
      target.add(statement(statement));
    }
  }

  private StatementTree statement(PsiStatement statement) {
    StatementTree result = switch (statement) {
      case PsiBlockStatement block -> block(block.getCodeBlock());
      case PsiEmptyStatement empty -> new EmptyStatementTreeImpl(token(empty, ";"));
      case PsiExpressionStatement expression -> new ExpressionStatementTreeImpl(expression(expression.getExpression()), last(expression, ";"));
      case PsiReturnStatement returned -> new ReturnStatementTreeImpl(token(returned, "return"), returned.getReturnValue() == null ? null : expression(returned.getReturnValue()), last(returned, ";"));
      case PsiThrowStatement thrown -> new ThrowStatementTreeImpl(token(thrown, "throw"), expression(thrown.getException()), last(thrown, ";"));
      case PsiIfStatement branch -> new IfStatementTreeImpl(token(branch, "if"), token(branch, "("), expression(branch.getCondition()),
        tokens.find(end(branch.getCondition()), start(branch.getThenBranch()), ")", false), statement(branch.getThenBranch()),
        branch.getElseBranch() == null ? null : tokens.find(end(branch.getThenBranch()), start(branch.getElseBranch()), "else", false),
        branch.getElseBranch() == null ? null : statement(branch.getElseBranch()));
      case PsiWhileStatement loop -> new WhileStatementTreeImpl(token(loop, "while"), token(loop, "("), expression(loop.getCondition()),
        tokens.find(end(loop.getCondition()), start(loop.getBody()), ")", false), statement(loop.getBody()));
      case PsiDoWhileStatement loop -> new DoWhileStatementTreeImpl(token(loop, "do"), statement(loop.getBody()),
        tokens.find(end(loop.getBody()), start(loop.getCondition()), "while", false), tokens.find(end(loop.getBody()), start(loop.getCondition()), "(", false),
        expression(loop.getCondition()), last(loop, ")"), last(loop, ";"));
      case PsiForStatement loop -> forStatement(loop);
      case PsiForeachStatement loop -> new ForEachStatementImpl(token(loop, "for"), token(loop, "("), variable(loop.getIterationParameter()),
        tokens.find(end(loop.getIterationParameter()), start(loop.getIteratedValue()), ":", false), expression(loop.getIteratedValue()),
        tokens.find(end(loop.getIteratedValue()), start(loop.getBody()), ")", false), statement(loop.getBody()));
      case PsiBreakStatement broken -> new BreakStatementTreeImpl(token(broken, "break"), labelReference(broken.getLabelIdentifier()), last(broken, ";"));
      case PsiContinueStatement continued -> new ContinueStatementTreeImpl(token(continued, "continue"), labelReference(continued.getLabelIdentifier()), last(continued, ";"));
      case PsiLabeledStatement labeled -> labeled(labeled);
      case PsiSynchronizedStatement synchronizedStatement -> new SynchronizedStatementTreeImpl(token(synchronizedStatement, "synchronized"), token(synchronizedStatement, "("),
        expression(synchronizedStatement.getLockExpression()), tokens.find(end(synchronizedStatement.getLockExpression()), start(synchronizedStatement.getBody()), ")", false), block(synchronizedStatement.getBody()));
      case PsiAssertStatement assertion -> assertion(assertion);
      case PsiTryStatement tried -> tryStatement(tried);
      case PsiSwitchStatement switched -> switchStatement(switched);
      case PsiYieldStatement yielded -> new YieldStatementTreeImpl(token(yielded, "yield"), expression(yielded.getExpression()), last(yielded, ";"));
      case PsiDeclarationStatement declaration -> singleDeclaration(declaration);
      default -> throw unsupported(statement);
    };
    return remember(statement, result);
  }

  private StatementTree singleDeclaration(PsiDeclarationStatement declaration) {
    List<StatementTree> result = new ArrayList<>();
    addStatement(declaration, result, true);
    if (result.size() != 1) {
      throw unsupported(declaration);
    }
    return result.getFirst();
  }

  private ForStatementTreeImpl forStatement(PsiForStatement loop) {
    var initializer = StatementListTreeImpl.emptyList();
    var update = StatementListTreeImpl.emptyList();
    if (loop.getInitialization() != null) {
      addStatement(loop.getInitialization(), initializer, false);
      expressionSeparators(loop.getInitialization(), initializer);
    }
    if (loop.getUpdate() != null) {
      addStatement(loop.getUpdate(), update, false);
      expressionSeparators(loop.getUpdate(), update);
    }
    var first = loop.getInitialization() == null ? token(loop, ";") : last(loop.getInitialization(), ";");
    var second = tokens.find(loop.getCondition() == null ? offset(first) + 1 : end(loop.getCondition()), end(loop), ";", false);
    return new ForStatementTreeImpl(token(loop, "for"), token(loop, "("), initializer, first,
      loop.getCondition() == null ? null : expression(loop.getCondition()), second, update,
      tokens.find(loop.getCondition() == null ? start(loop) : end(loop.getCondition()), start(loop.getBody()), ")", true), statement(loop.getBody()));
  }

  private void expressionSeparators(PsiStatement statement, StatementListTreeImpl list) {
    if (statement instanceof PsiExpressionListStatement expressions) {
      for (var expression : expressions.getExpressionList().getExpressions()) {
        if (tokens.at(end(expression)).text().equals(",")) {
          list.separators().add(tokens.at(end(expression)));
        }
      }
    }
  }

  private int offset(SyntaxToken token) {
    return tokens.lexemes().stream().filter(t -> t.token() == token).findFirst().orElseThrow().start();
  }

  private final java.util.Deque<JLabelSymbol> labels = new java.util.ArrayDeque<>();

  private LabeledStatementTreeImpl labeled(PsiLabeledStatement labeled) {
    var name = identifier(labeled.getLabelIdentifier());
    var symbol = new JLabelSymbol(name.name());
    labels.push(symbol);
    var result = new LabeledStatementTreeImpl(name, token(labeled, ":"), statement(labeled.getStatement()));
    labels.pop();
    symbol.setDeclaration(result);
    result.labelSymbol = symbol;
    return result;
  }

  private IdentifierTreeImpl labelReference(PsiIdentifier label) {
    if (label == null) {
      return null;
    }
    var result = identifier(label);
    for (var symbol : labels) {
      if (symbol.name().equals(result.name())) {
        result.labelSymbol = symbol;
        symbol.addUsage(result);
        break;
      }
    }
    return result;
  }

  private AssertStatementTreeImpl assertion(PsiAssertStatement assertion) {
    var result = new AssertStatementTreeImpl(token(assertion, "assert"), expression(assertion.getAssertCondition()), last(assertion, ";"));
    if (assertion.getAssertDescription() != null) {
      result.complete(tokens.find(end(assertion.getAssertCondition()), start(assertion.getAssertDescription()), ":", false), expression(assertion.getAssertDescription()));
    }
    return result;
  }

  /** Reparse C-style resource dimensions as local syntax because PSI truncates this ECJ-supported resource form. */
  private VariableTreeImpl resourceVariable(PsiTryStatement tried, PsiResourceVariable variable) {
    if (variable.getInitializer() != null || !tokens.at(end(variable.getNameIdentifier())).text().equals("[")) {
      return variable(variable);
    }
    var resourceList = tried.getResourceList();
    var repaired = new StringBuilder(source);
    int open = offset(token(resourceList, "("));
    int close = offset(last(resourceList, ")"));
    for (int i = start(tried); i <= open; i++) {
      repaired.setCharAt(i, ' ');
    }
    repaired.setCharAt(close, ';');
    var reparsed = PsiFileFactory.getInstance(file.getProject()).createFileFromText(file.getName(), file.getFileType(), repaired);
    ((com.intellij.psi.impl.source.PsiFileImpl) reparsed).setOriginalFile(file);
    var declaration = PsiTreeUtil.getParentOfType(reparsed.findElementAt(start(variable)), PsiLocalVariable.class);
    if (declaration == null || declaration.getInitializer() == null) {
      throw unsupported(variable);
    }
    return variable(declaration);
  }

  private TryStatementTreeImpl tryStatement(PsiTryStatement tried) {
    var resources = ResourceListTreeImpl.emptyList();
    var sourceResources = tried.getResourceList();
    if (sourceResources != null) {
      for (var resource : sourceResources) {
        if (resource instanceof PsiResourceVariable variable) {
          resources.add(resourceVariable(tried, variable));
        } else if (resource instanceof PsiResourceExpression expression) {
          resources.add(expression(expression.getExpression()));
        } else {
          throw unsupported(resource);
        }
        if (tokens.at(end(resource)).text().equals(";")) {
          resources.separators().add(tokens.at(end(resource)));
        }
      }
    }
    List<CatchTree> catches = new ArrayList<>();
    for (var caught : tried.getCatchSections()) {
      catches.add(remember(caught, new CatchTreeImpl(token(caught, "catch"), token(caught, "("), variable(caught.getParameter()),
        tokens.find(end(caught.getParameter()), start(caught.getCatchBlock()), ")", false), block(caught.getCatchBlock()))));
    }
    var finallyBlock = tried.getFinallyBlock();
    return new TryStatementTreeImpl(token(tried, "try"), resources.isEmpty() ? null : token(sourceResources, "("), resources,
      resources.isEmpty() ? null : last(sourceResources, ")"), block(tried.getTryBlock()), catches,
      finallyBlock == null ? null : tokens.find(end(tried.getTryBlock()), start(finallyBlock), "finally", true), finallyBlock == null ? null : block(finallyBlock));
  }

  private List<CaseGroupTreeImpl> switchGroups(PsiSwitchBlock switched) {
    List<CaseGroupTreeImpl> result = new ArrayList<>();
    List<CaseLabelTreeImpl> labels = new ArrayList<>();
    var body = StatementListTreeImpl.emptyList();
    for (var statement : switched.getBody().getStatements()) {
      if (statement instanceof PsiSwitchLabelStatementBase label) {
        if (!body.isEmpty()) {
          result.add(new CaseGroupTreeImpl(labels, body));
          labels = new ArrayList<>();
          body = StatementListTreeImpl.emptyList();
        }
        List<ExpressionTree> expressions = new ArrayList<>();
        var list = label.getCaseLabelElementList();
        if (list != null) {
          for (var element : list.getElements()) {
            ExpressionTree expression;
            if (element instanceof PsiPattern pattern) {
              expression = pattern(pattern);
              if (label.getGuardExpression() != null) {
                expression = remember(pattern, new GuardedPatternTreeImpl((PatternTree) expression,
                  tokens.find(end(element), start(label.getGuardExpression()), "when", false), expression(label.getGuardExpression()), Type.UNKNOWN));
              }
            } else if (element instanceof PsiDefaultCaseLabelElement) {
              expression = new DefaultPatternTreeImpl(token(element, "default"), Type.UNKNOWN);
            } else if (element instanceof PsiLiteralExpression literal && literal.textMatches("null")) {
              expression = new NullPatternTreeImpl(literal(literal));
            } else {
              expression = expression(element);
            }
            expressions.add(expression);
          }
        }
        var separator = label instanceof PsiSwitchLabeledRuleStatement rule ? tokens.find(start(label), start(rule.getBody()), "->", true) : last(label, ":");
        labels.add(new CaseLabelTreeImpl(token(label, label.isDefaultCase() ? "default" : "case"), expressions, separator));
        if (label instanceof PsiSwitchLabeledRuleStatement rule) {
          var ruleBody = rule.getBody();
          if (ruleBody instanceof PsiExpressionStatement expression) {
            body.add(new YieldStatementTreeImpl(null, expression(expression.getExpression()), last(expression, ";")));
          } else {
            addStatement(ruleBody, body, true);
          }
        }
      } else {
        addStatement(statement, body, true);
      }
    }
    if (!labels.isEmpty()) {
      result.add(new CaseGroupTreeImpl(labels, body));
    }
    return result;
  }

  private SwitchStatementTreeImpl switchStatement(PsiSwitchStatement statement) {
    return new SwitchStatementTreeImpl(token(statement, "switch"), token(statement, "("), expression(statement.getExpression()),
      tokens.find(end(statement.getExpression()), start(statement.getBody()), ")", false), exact(statement.getBody().getLBrace()), switchGroups(statement), exact(statement.getBody().getRBrace()));
  }

  private SwitchExpressionTreeImpl switchExpression(PsiSwitchExpression expression) {
    return new SwitchExpressionTreeImpl(token(expression, "switch"), token(expression, "("), expression(expression.getExpression()),
      tokens.find(end(expression.getExpression()), start(expression.getBody()), ")", false), exact(expression.getBody().getLBrace()), switchGroups(expression), exact(expression.getBody().getRBrace()));
  }

}
