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
package org.sonar.java.model.javac;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.DefaultCaseLabelTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.DirectiveTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.UsesTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.ModuleNameListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JLabelSymbol;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
import org.sonar.java.model.declaration.RequiresDirectiveTreeImpl;
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.OpensDirectiveTreeImpl;
import org.sonar.java.model.declaration.UsesDirectiveTreeImpl;
import org.sonar.java.model.declaration.ProvidesDirectiveTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.java.model.pattern.DefaultPatternTreeImpl;
import org.sonar.java.model.pattern.NullPatternTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

/** Converts javac's public syntax trees directly to SonarJava syntax trees. */
public final class JavacTreeConverter {

  private final CompilationUnitTree unit;
  private final SourcePositions positions;
  private final String source;
  private final SourceTokens tokens;
  private final BiConsumer<com.sun.source.tree.Tree, Tree> registration;
  private final Map<String, JLabelSymbol> activeLabels = new HashMap<>();
  private final Map<String, TypeTree> variableTypes = new HashMap<>();
  private final Map<String, ModifiersTreeImpl> variableModifiers = new HashMap<>();
  private final Map<Integer, SyntaxToken> variableFirstTokens = new HashMap<>();

  private static final class SharedDeclarationVariableTree extends VariableTreeImpl {
    private final SyntaxToken firstToken;

    private SharedDeclarationVariableTree(ModifiersTreeImpl modifiers, TypeTree type, IdentifierTreeImpl simpleName,
      SyntaxToken firstToken) {
      super(modifiers, type, simpleName);
      this.firstToken = firstToken;
    }

    @Override
    public SyntaxToken firstToken() {
      return firstToken;
    }
  }

  private JavacTreeConverter(CompilationUnitTree unit, Trees trees, String source, int sourceVersion,
    BiConsumer<com.sun.source.tree.Tree, Tree> registration) {
    this.unit = unit;
    this.positions = trees.getSourcePositions();
    this.source = source;
    this.tokens = new SourceTokens(source, sourceVersion);
    this.registration = registration;
  }

  public static JavaTree.CompilationUnitTreeImpl convert(CompilationUnitTree unit, Trees trees, String source) {
    return convert(unit, trees, source, Runtime.version().feature(), (javacTree, sonarTree) -> { });
  }

  public static JavaTree.CompilationUnitTreeImpl convert(CompilationUnitTree unit, Trees trees, String source,
    BiConsumer<com.sun.source.tree.Tree, Tree> registration) {
    return convert(unit, trees, source, Runtime.version().feature(), registration);
  }

  public static JavaTree.CompilationUnitTreeImpl convert(CompilationUnitTree unit, Trees trees, String source, int sourceVersion,
    BiConsumer<com.sun.source.tree.Tree, Tree> registration) {
    return new JavacTreeConverter(unit, trees, source, sourceVersion, registration).convert();
  }

  private JavaTree.CompilationUnitTreeImpl convert() {
    JavaTree.PackageDeclarationTreeImpl packageDeclaration = null;
    if (unit.getPackageName() != null) {
      var name = unit.getPackageName();
      int nameStart = start(name);
      int nameEnd = end(name);
      packageDeclaration = new JavaTree.PackageDeclarationTreeImpl(
        unit.getPackageAnnotations().stream().map(annotation -> (org.sonar.plugins.java.api.tree.AnnotationTree) annotation(annotation)).toList(),
        tokenBefore(nameStart, "package"), expression(name), tokenAfter(nameEnd, ";"));
      registration.accept(unit.getPackage(), packageDeclaration);
    }
    List<ImportClauseTree> imports = new ArrayList<>();
    for (ImportTree importTree : unit.getImports()) {
      imports.add(importDeclaration(importTree));
    }
    int importZoneStart = unit.getImports().isEmpty()
      ? unit.getPackage() == null ? 0 : end(unit.getPackage())
      : end(unit.getImports().get(unit.getImports().size() - 1));
    int importZoneEnd = unit.getTypeDecls().isEmpty() ? source.length() : start(unit.getTypeDecls().get(0));
    for (SourceTokens.Token part : tokens.tokensBetween(importZoneStart, importZoneEnd)) {
      if (part.text().equals(";")) {
        imports.add(new EmptyStatementTreeImpl(tokens.syntaxToken(part)));
      }
    }
    List<Tree> types = new ArrayList<>();
    boolean seenType = false;
    for (com.sun.source.tree.Tree declaration : unit.getTypeDecls()) {
      if (declaration instanceof ClassTree classTree) {
        types.add(classDeclaration(classTree));
        seenType = true;
      } else if (declaration.getKind() == com.sun.source.tree.Tree.Kind.EMPTY_STATEMENT) {
        EmptyStatementTreeImpl empty = new EmptyStatementTreeImpl(token(find(start(declaration), end(declaration), ";"), ";"));
        if (seenType) {
          types.add(empty);
        } else {
          imports.add(empty);
        }
      } else {
        throw unsupported(declaration);
      }
    }
    JavaTree.CompilationUnitTreeImpl result = new JavaTree.CompilationUnitTreeImpl(
      packageDeclaration, imports, types, unit.getModule() == null ? null : module(unit.getModule()), token(source.length(), "", true));
    setParents(result);
    registration.accept(unit, result);
    return result;
  }

  private JavaTree.ImportTreeImpl importDeclaration(ImportTree tree) {
    int start = start(tree);
    int end = end(tree);
    InternalSyntaxToken importToken = token(find(start, end, "import"), "import");
    InternalSyntaxToken staticToken = tree.isStatic() ? token(find(start + 6, end, "static"), "static") : null;
    SourceTokens.Token module = tokens.tokensBetween(start + 6, start(tree.getQualifiedIdentifier())).stream()
      .filter(part -> part.text().equals("module")).findFirst().orElse(null);
    InternalSyntaxToken moduleToken = module == null ? null : tokens.syntaxToken(module);
    com.sun.source.tree.Tree imported = tree.getQualifiedIdentifier();
    if (imported instanceof MemberSelectTree select && select.getIdentifier().toString().equals("<error>")) {
      imported = select.getExpression();
    }
    JavaTree.ImportTreeImpl result = new JavaTree.ImportTreeImpl(importToken, staticToken, moduleToken,
      expression((com.sun.source.tree.ExpressionTree) imported), token(findLast(start, end, ";"), ";"));
    registration.accept(tree, result);
    return result;
  }

  private ModuleDeclarationTreeImpl module(ModuleTree tree) {
    int start = start(tree);
    int end = end(tree);
    int nameStart = start(tree.getName());
    InternalSyntaxToken openKeyword = tree.getModuleType() == ModuleTree.ModuleKind.OPEN
      ? token(findLast(0, nameStart, "open"), "open") : null;
    List<ModuleDirectiveTree> directives = new ArrayList<>();
    for (DirectiveTree directive : tree.getDirectives()) {
      directives.add(moduleDirective(directive));
    }
    List<org.sonar.plugins.java.api.tree.AnnotationTree> annotations = new ArrayList<>();
    for (AnnotationTree annotation : tree.getAnnotations()) {
      annotations.add(annotation(annotation));
    }
    ModuleDeclarationTreeImpl result = new ModuleDeclarationTreeImpl(annotations, openKeyword,
      token(find(start, nameStart, "module"), "module"), moduleName(tree.getName()),
      token(find(end(tree.getName()), end, "{"), "{"), directives, token(findLast(start, end, "}"), "}"));
    registration.accept(tree, result);
    return result;
  }

  private ModuleDirectiveTree moduleDirective(DirectiveTree directive) {
    int start = start(directive);
    int end = end(directive);
    InternalSyntaxToken semicolon = token(findLast(start, end, ";"), ";");
    ModuleDirectiveTree result;
    if (directive instanceof RequiresTree requires) {
      int nameStart = start(requires.getModuleName());
      List<ModifierTree> flags = new ArrayList<>();
      if (requires.isStatic()) {
        flags.add(new ModifierKeywordTreeImpl(Modifier.STATIC, token(find(start, nameStart, "static"), "static")));
      }
      if (requires.isTransitive() || requires.getModuleName().toString().equals("transitive")) {
        flags.add(new ModifierKeywordTreeImpl(Modifier.TRANSITIVE, token(find(start, end, "transitive"), "transitive")));
      }
      result = new RequiresDirectiveTreeImpl(token(find(start, nameStart, "requires"), "requires"),
        new ModifiersTreeImpl(flags), moduleName(requires.getModuleName()), semicolon);
    } else if (directive instanceof ExportsTree exports) {
      ModuleNameListTreeImpl targets = moduleNames(exports.getModuleNames());
      InternalSyntaxToken to = targets.isEmpty() ? null : token(find(end(exports.getPackageName()), start(exports.getModuleNames().get(0)), "to"), "to");
      result = new ExportsDirectiveTreeImpl(token(find(start, end, "exports"), "exports"), expression(exports.getPackageName()), to, targets, semicolon);
    } else if (directive instanceof OpensTree opens) {
      ModuleNameListTreeImpl targets = moduleNames(opens.getModuleNames());
      InternalSyntaxToken to = targets.isEmpty() ? null : token(find(end(opens.getPackageName()), start(opens.getModuleNames().get(0)), "to"), "to");
      result = new OpensDirectiveTreeImpl(token(find(start, end, "opens"), "opens"), expression(opens.getPackageName()), to, targets, semicolon);
    } else if (directive instanceof UsesTree uses) {
      result = new UsesDirectiveTreeImpl(token(find(start, end, "uses"), "uses"), type(uses.getServiceName()), semicolon);
    } else if (directive instanceof ProvidesTree provides) {
      QualifiedIdentifierListTreeImpl implementations = QualifiedIdentifierListTreeImpl.emptyList();
      for (com.sun.source.tree.ExpressionTree implementation : provides.getImplementationNames()) {
        if (!implementations.isEmpty()) {
          implementations.separators().add(token(find(end(provides.getImplementationNames().get(implementations.size() - 1)), start(implementation), ","), ","));
        }
        implementations.add(type(implementation));
      }
      result = new ProvidesDirectiveTreeImpl(token(find(start, end, "provides"), "provides"), type(provides.getServiceName()),
        token(find(end(provides.getServiceName()), start(provides.getImplementationNames().get(0)), "with"), "with"), implementations, semicolon);
    } else {
      throw unsupported(directive);
    }
    registration.accept(directive, result);
    return result;
  }

  private ModuleNameListTreeImpl moduleNames(List<? extends com.sun.source.tree.ExpressionTree> names) {
    ModuleNameListTreeImpl result = ModuleNameListTreeImpl.emptyList();
    if (names == null) {
      return result;
    }
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        result.separators().add(token(find(end(names.get(i - 1)), start(names.get(i)), ","), ","));
      }
      result.add(moduleName(names.get(i)));
    }
    return result;
  }

  private ModuleNameTree moduleName(com.sun.source.tree.ExpressionTree tree) {
    ModuleNameTreeImpl result = ModuleNameTreeImpl.emptyList();
    int cursor = start(tree);
    for (String part : tree.toString().split("\\.")) {
      int offset = find(cursor, end(tree), part);
      result.add(identifier(offset, part));
      cursor = offset + part.length();
    }
    registration.accept(tree, result);
    return result;
  }

  private ClassTreeImpl classDeclaration(ClassTree tree) {
    int start = start(tree);
    int end = end(tree);
    Tree.Kind kind = switch (tree.getKind()) {
      case CLASS -> Tree.Kind.CLASS;
      case INTERFACE -> Tree.Kind.INTERFACE;
      case ENUM -> Tree.Kind.ENUM;
      case ANNOTATION_TYPE -> Tree.Kind.ANNOTATION_TYPE;
      case RECORD -> Tree.Kind.RECORD;
      default -> throw unsupported(tree);
    };
    int headEnd = tree.getMembers().isEmpty() ? end : start(tree.getMembers().get(0));
    if (kind == Tree.Kind.CLASS && tokens.tokensBetween(start, headEnd).stream().noneMatch(part -> part.text().equals("class"))) {
      List<Tree> implicitMembers = new ArrayList<>();
      for (com.sun.source.tree.Tree member : tree.getMembers()) {
        if (member instanceof MethodTree method) {
          implicitMembers.add(methodDeclaration(method, tree.getSimpleName().toString()));
        } else if (member instanceof VariableTree variable) {
          implicitMembers.add(variableDeclaration(variable));
        } else if (member instanceof ClassTree nested) {
          implicitMembers.add(classDeclaration(nested));
        } else if (member instanceof BlockTree initializer) {
          implicitMembers.add(initializer.isStatic() ? staticInitializer(initializer) : block(initializer, Tree.Kind.INITIALIZER));
        }
      }
      ClassTreeImpl result = new ClassTreeImpl(Tree.Kind.IMPLICIT_CLASS, null, implicitMembers, null)
        .complete(ModifiersTreeImpl.emptyModifiers(), null, null);
      registration.accept(tree, result);
      return result;
    }
    String keyword = switch (kind) {
      case CLASS -> "class";
      case INTERFACE -> "interface";
      case ENUM -> "enum";
      case ANNOTATION_TYPE -> "interface";
      case RECORD -> "record";
      default -> throw unsupported(tree);
    };
    int keywordPos = findLast(start, headEnd, keyword);
    int namePos = find(keywordPos + keyword.length(), headEnd, tree.getSimpleName().toString());
    int openBrace = find(namePos, end, "{");
    int closeBrace = findLast(openBrace, end, "}");
    List<Tree> members = new ArrayList<>();
    int memberCursor = openBrace + 1;
    boolean afterEnumConstant = false;
    List<org.sonar.plugins.java.api.tree.VariableTree> recordComponents = new ArrayList<>();
    for (com.sun.source.tree.Tree member : tree.getMembers()) {
      if (kind == Tree.Kind.RECORD && start(member) < openBrace && member instanceof VariableTree component) {
        String componentName = component.getName().toString();
        VariableTreeImpl converted = new VariableTreeImpl(ModifiersTreeImpl.emptyModifiers(), type(component.getType()),
          identifier(find(end(component.getType()), end(component), componentName), componentName));
        SourceTokens.Token following = tokens.tokenAtOrAfter(end(component));
        if (following.text().equals(",")) {
          converted.setEndToken(tokens.syntaxToken(following));
        }
        recordComponents.add(converted);
        registration.accept(component, converted);
        continue;
      }
      addEmptyClassMembers(members, memberCursor, start(member), afterEnumConstant);
      if (member.getKind() == com.sun.source.tree.Tree.Kind.ERRONEOUS) {
        SourceTokens.Token following = tokens.tokenAtOrAfter(end(member));
        memberCursor = following.text().equals(";") ? following.end() : end(member);
        continue;
      }
      if (member instanceof VariableTree variable) {
        members.add(kind == Tree.Kind.ENUM && variable.getInitializer() instanceof com.sun.source.tree.NewClassTree
          ? enumConstant(variable) : variableDeclaration(variable));
      } else if (member instanceof MethodTree method) {
        members.add(methodDeclaration(method, tree.getSimpleName().toString()));
      } else if (member instanceof ClassTree nestedClass) {
        members.add(classDeclaration(nestedClass));
      } else if (member instanceof BlockTree initializer) {
        members.add(initializer.isStatic() ? staticInitializer(initializer) : block(initializer, Tree.Kind.INITIALIZER));
      } else {
        throw unsupported(member);
      }
      memberCursor = end(member);
      afterEnumConstant = kind == Tree.Kind.ENUM && member instanceof VariableTree variable
        && variable.getInitializer() instanceof com.sun.source.tree.NewClassTree;
    }
    addEmptyClassMembers(members, memberCursor, closeBrace, afterEnumConstant);
    IdentifierTreeImpl simpleName = identifier(namePos, tree.getSimpleName().toString());
    ClassTreeImpl result = new ClassTreeImpl(kind, token(openBrace, "{"), members, token(closeBrace, "}"))
      .complete(modifiers(tree.getModifiers()), token(keywordPos, keyword), simpleName);
    if (kind == Tree.Kind.RECORD) {
      int openParen = find(namePos + tree.getSimpleName().length(), openBrace, "(");
      int closeParen = findLast(openParen, openBrace, ")");
      result.completeRecordComponents(token(openParen, "("), recordComponents, token(closeParen, ")"));
    }
    if (kind == Tree.Kind.ANNOTATION_TYPE) {
      result.completeAtToken(token(findLast(start, keywordPos, "@"), "@"));
    }
    result.completeTypeParameters(typeParameters(tree.getTypeParameters(), namePos + tree.getSimpleName().length()));
    if (tree.getExtendsClause() != null) {
      result.completeSuperclass(tokenBefore(start(tree.getExtendsClause()), "extends"), type(tree.getExtendsClause()));
    }
    if (!tree.getImplementsClause().isEmpty()) {
      QualifiedIdentifierListTreeImpl interfaces = QualifiedIdentifierListTreeImpl.emptyList();
      for (com.sun.source.tree.Tree implemented : tree.getImplementsClause()) {
        if (!interfaces.isEmpty()) {
          interfaces.separators().add(token(find(end(tree.getImplementsClause().get(interfaces.size() - 1)), start(implemented), ","), ","));
        }
        interfaces.add(type(implemented));
      }
      String interfaceKeyword = kind == Tree.Kind.INTERFACE ? "extends" : "implements";
      result.completeInterfaces(tokenBefore(start(tree.getImplementsClause().get(0)), interfaceKeyword), interfaces);
    }
    if (!tree.getPermitsClause().isEmpty()) {
      QualifiedIdentifierListTreeImpl permitted = QualifiedIdentifierListTreeImpl.emptyList();
      for (com.sun.source.tree.Tree permit : tree.getPermitsClause()) {
        if (!permitted.isEmpty()) {
          permitted.separators().add(token(find(end(tree.getPermitsClause().get(permitted.size() - 1)), start(permit), ","), ","));
        }
        permitted.add(type(permit));
      }
      result.completePermittedTypes(tokenBefore(start(tree.getPermitsClause().get(0)), "permits"), permitted);
    }
    registration.accept(tree, result);
    registration.accept(tree, simpleName);
    return result;
  }

  private void addEmptyClassMembers(List<Tree> members, int start, int end, boolean afterEnumConstant) {
    boolean separator = afterEnumConstant;
    for (SourceTokens.Token part : tokens.tokensBetween(start, end)) {
      if (part.text().equals(";")) {
        if (!separator) {
          members.add(new EmptyStatementTreeImpl(tokens.syntaxToken(part)));
        }
        separator = false;
      }
    }
  }

  private MethodTreeImpl methodDeclaration(MethodTree tree, String className) {
    if (tree.getReceiverParameter() != null) {
      throw unsupported(tree.getReceiverParameter());
    }
    int start = start(tree);
    int end = end(tree);
    String name = tree.getName().toString();
    if (name.equals("<init>")) {
      name = className;
    }
    int namePos = methodNamePosition(tree, name);
    if (tree.getReturnType() == null) {
      name = tokens.tokenAtOrAfter(namePos).text();
    }
    int parameterEnd = tree.getBody() == null ? end : start(tree.getBody());
    boolean compactConstructor = tree.getReturnType() == null && tokens.tokensBetween(namePos + name.length(), parameterEnd).stream()
      .noneMatch(part -> part.text().equals("("));
    int openParen = compactConstructor ? -1 : find(namePos + name.length(), end, "(");
    int closeParen = compactConstructor ? -1 : findLast(openParen, parameterEnd, ")");
    FormalParametersListTreeImpl parameters = new FormalParametersListTreeImpl(
      compactConstructor ? null : token(openParen, "("), compactConstructor ? null : token(closeParen, ")"));
    if (!compactConstructor) {
      for (VariableTree parameter : tree.getParameters()) {
        parameters.add(variableDeclaration(parameter));
      }
    }
    QualifiedIdentifierListTreeImpl throwsClauses = QualifiedIdentifierListTreeImpl.emptyList();
    for (com.sun.source.tree.ExpressionTree exception : tree.getThrows()) {
      if (!throwsClauses.isEmpty()) {
        throwsClauses.separators().add(token(find(end(tree.getThrows().get(throwsClauses.size() - 1)), start(exception), ","), ","));
      }
      throwsClauses.add(type(exception));
    }
    InternalSyntaxToken throwsToken = throwsClauses.isEmpty() ? null : tokenBefore(start(tree.getThrows().get(0)), "throws");
    BlockTreeImpl body = tree.getBody() == null ? null : block(tree.getBody());
    TypeTree returnType = tree.getReturnType() == null ? null : type(tree.getReturnType());
    InternalSyntaxToken semicolon = body == null ? token(findLast(start, end, ";"), ";") : null;
    IdentifierTreeImpl simpleName = identifier(namePos, name);
    MethodTreeImpl result = tree.getDefaultValue() == null
      ? new MethodTreeImpl(returnType, simpleName, parameters, throwsToken, throwsClauses, body, semicolon)
      : new MethodTreeImpl(parameters, token(find(closeParen + 1, start(tree.getDefaultValue()), "default"), "default"),
        valueExpression((com.sun.source.tree.ExpressionTree) tree.getDefaultValue())).complete(returnType, simpleName, semicolon);
    result.completeWithModifiers(modifiers(tree.getModifiers()));
    result.completeWithTypeParameters(typeParameters(tree.getTypeParameters(), start));
    registration.accept(tree, result);
    registration.accept(tree, simpleName);
    return result;
  }

  private TypeParameterListTreeImpl typeParameters(List<? extends TypeParameterTree> parameters, int from) {
    if (parameters.isEmpty()) {
      return new TypeParameterListTreeImpl();
    }
    int open = find(from, start(parameters.get(0)), "<");
    int close = source.indexOf('>', end(parameters.get(parameters.size() - 1)));
    if (close < 0) {
      throw unsupported(parameters.get(parameters.size() - 1));
    }
    TypeParameterListTreeImpl result = new TypeParameterListTreeImpl(token(open, "<"), closingAngle(close));
    for (int i = 0; i < parameters.size(); i++) {
      TypeParameterTree parameter = parameters.get(i);
      if (i > 0) {
        result.separators().add(token(find(end(parameters.get(i - 1)), start(parameter), ","), ","));
      }
      int namePos = find(start(parameter), end(parameter), parameter.getName().toString());
      IdentifierTreeImpl name = identifier(namePos, parameter.getName().toString());
      QualifiedIdentifierListTreeImpl bounds = QualifiedIdentifierListTreeImpl.emptyList();
      for (com.sun.source.tree.Tree bound : parameter.getBounds()) {
        if (!bounds.isEmpty()) {
          bounds.separators().add(token(find(end(parameter.getBounds().get(bounds.size() - 1)), start(bound), "&"), "&"));
        }
        bounds.add(type(bound));
      }
      TypeParameterTreeImpl converted = bounds.isEmpty() ? new TypeParameterTreeImpl(name)
        : new TypeParameterTreeImpl(name, token(find(namePos + parameter.getName().length(), start(parameter.getBounds().get(0)), "extends"), "extends"), bounds);
      result.add(converted);
      registration.accept(parameter, converted);
    }
    return result;
  }

  private VariableTreeImpl variableDeclaration(VariableTree tree) {
    int start = start(tree);
    int end = end(tree);
    String name = tree.getName().toString();
    boolean unnamed = name.isEmpty();
    if (unnamed) {
      name = "_";
    }
    int namePos = findLast(start, tree.getInitializer() == null ? end : start(tree.getInitializer()), name);
    TypeTree declaredType;
    if (tree.getType() == null) {
      declaredType = new VarTypeTreeImpl(token(find(start, namePos, "var"), "var"));
    } else {
      com.sun.source.tree.Tree javacType = tree.getType();
      String key = javacType.getKind() + ":" + start(javacType) + ":" + end(javacType);
      declaredType = variableTypes.computeIfAbsent(key, unused -> type(javacType));
    }
    IdentifierTreeImpl simpleName = unnamed ? new IdentifierTreeImpl(token(namePos, name), true) : identifier(namePos, name);
    ModifiersTree javaModifiers = tree.getModifiers();
    String modifierKey = start(javaModifiers) + ":" + end(javaModifiers) + ":" + javaModifiers.getFlags();
    ModifiersTreeImpl declaredModifiers = start(javaModifiers) < 0 ? ModifiersTreeImpl.emptyModifiers()
      : variableModifiers.computeIfAbsent(modifierKey, unused -> modifiers(javaModifiers));
    SyntaxToken firstToken = variableFirstTokens.get(start);
    VariableTreeImpl result = firstToken == null ? new VariableTreeImpl(declaredModifiers, declaredType, simpleName)
      : new SharedDeclarationVariableTree(declaredModifiers, declaredType, simpleName, firstToken);
    if (tree.getInitializer() != null) {
      int equals = find(namePos + name.length(), start(tree.getInitializer()), "=");
      result.completeTypeAndInitializer(declaredType, token(equals, "="), valueExpression(tree.getInitializer()));
    }
    SourceTokens.Token last = tokens.tokenAtOrBefore(end - 1);
    if ((last.text().equals(";") || last.text().equals(",")) && last.end() == end) {
      result.setEndToken(tokens.syntaxToken(last));
    } else {
      SourceTokens.Token following = tokens.tokenAtOrAfter(end);
      if (following.text().equals(",") || following.text().equals(";")) {
        result.setEndToken(tokens.syntaxToken(following));
      }
    }
    SyntaxToken declarationFirstToken = declaredModifiers.firstToken() == null ? declaredType.firstToken() : declaredModifiers.firstToken();
    variableFirstTokens.putIfAbsent(start, declarationFirstToken);
    registration.accept(tree, result);
    registration.accept(tree, simpleName);
    return result;
  }

  private EnumConstantTreeImpl enumConstant(VariableTree tree) {
    int start = start(tree);
    int end = end(tree);
    String name = tree.getName().toString();
    int namePosition = find(start, end, name);
    IdentifierTreeImpl simpleName = identifier(namePosition, name);
    com.sun.source.tree.NewClassTree creation = (com.sun.source.tree.NewClassTree) tree.getInitializer();
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    List<? extends com.sun.source.tree.ExpressionTree> values = creation.getArguments();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        arguments.separators().add(token(find(end(values.get(i - 1)), start(values.get(i)), ","), ","));
      }
      arguments.add(valueExpression(values.get(i)));
    }
    SourceTokens.Token afterName = tokens.tokenAtOrAfter(namePosition + name.length());
    if (afterName.text().equals("(")) {
      int close = matchingCloseParen(afterName.start(), end);
      arguments.complete(tokens.syntaxToken(afterName), token(close, ")"));
    }
    NewClassTreeImpl initializer = new NewClassTreeImpl(simpleName, arguments,
      creation.getClassBody() == null ? null : anonymousClass(creation.getClassBody()));
    List<ModifierTree> annotations = new ArrayList<>();
    for (AnnotationTree annotation : tree.getModifiers().getAnnotations()) {
      annotations.add(annotation(annotation));
    }
    SourceTokens.Token separator = tokens.tokenAtOrAfter(end);
    InternalSyntaxToken separatorToken = separator.text().equals(",") || separator.text().equals(";") ? tokens.syntaxToken(separator) : null;
    EnumConstantTreeImpl result = new EnumConstantTreeImpl(new ModifiersTreeImpl(annotations), simpleName, initializer, separatorToken);
    registration.accept(tree, result);
    registration.accept(tree, simpleName);
    registration.accept(creation, initializer);
    return result;
  }

  private BlockTreeImpl block(BlockTree tree) {
    return block(tree, Tree.Kind.BLOCK);
  }

  private BlockTreeImpl block(BlockTree tree, Tree.Kind kind) {
    int start = start(tree);
    int end = end(tree);
    List<StatementTree> statements = new ArrayList<>();
    for (com.sun.source.tree.StatementTree statement : tree.getStatements()) {
      statements.add(statement(statement));
    }
    BlockTreeImpl result = new BlockTreeImpl(kind, token(find(start, end, "{"), "{"), statements, token(findLast(start, end, "}"), "}"));
    registration.accept(tree, result);
    return result;
  }

  private StaticInitializerTreeImpl staticInitializer(BlockTree tree) {
    int start = start(tree);
    int end = end(tree);
    List<StatementTree> statements = new ArrayList<>();
    for (com.sun.source.tree.StatementTree statement : tree.getStatements()) {
      statements.add(statement(statement));
    }
    int openBrace = find(start, end, "{");
    StaticInitializerTreeImpl result = new StaticInitializerTreeImpl(token(findLast(0, openBrace, "static"), "static"), token(openBrace, "{"),
      statements, token(findLast(start, end, "}"), "}"));
    registration.accept(tree, result);
    return result;
  }

  private StatementTree statement(com.sun.source.tree.StatementTree tree) {
    StatementTree result = statementValue(tree);
    registration.accept(tree, result);
    return result;
  }

  private StatementTree statementValue(com.sun.source.tree.StatementTree tree) {
    int start = start(tree);
    int end = end(tree);
    if (tree instanceof BreakTree branch) {
      String label = branch.getLabel() == null ? null : branch.getLabel().toString();
      return new BreakStatementTreeImpl(token(find(start, end, "break"), "break"),
        label == null ? null : labelUsage(find(start + 5, end, label), label), token(findLast(start, end, ";"), ";"));
    }
    if (tree instanceof ContinueTree branch) {
      String label = branch.getLabel() == null ? null : branch.getLabel().toString();
      return new ContinueStatementTreeImpl(token(find(start, end, "continue"), "continue"),
        label == null ? null : labelUsage(find(start + 8, end, label), label), token(findLast(start, end, ";"), ";"));
    }
    if (tree instanceof LabeledStatementTree labeled) {
      String label = labeled.getLabel().toString();
      int labelStart = find(start, start(labeled.getStatement()), label);
      JLabelSymbol symbol = new JLabelSymbol(label);
      JLabelSymbol previous = activeLabels.put(label, symbol);
      IdentifierTreeImpl declarationName = identifier(labelStart, label);
      declarationName.labelSymbol = symbol;
      StatementTree body = statement(labeled.getStatement());
      if (previous == null) {
        activeLabels.remove(label);
      } else {
        activeLabels.put(label, previous);
      }
      LabeledStatementTreeImpl result = new LabeledStatementTreeImpl(declarationName,
        token(find(labelStart + label.length(), start(labeled.getStatement()), ":"), ":"), body);
      result.labelSymbol = symbol;
      symbol.setDeclaration(result);
      return result;
    }
    if (tree instanceof DoWhileLoopTree loop) {
      var condition = ((com.sun.source.tree.ParenthesizedTree) loop.getCondition()).getExpression();
      int whileStart = find(end(loop.getStatement()), start(condition), "while");
      int open = find(whileStart + 5, start(condition), "(");
      int close = find(end(condition), end, ")");
      return new DoWhileStatementTreeImpl(token(find(start, whileStart, "do"), "do"), statement(loop.getStatement()),
        token(whileStart, "while"), token(open, "("), valueExpression(condition), token(close, ")"), token(findLast(close, end, ";"), ";"));
    }
    if (tree instanceof AssertTree assertion) {
      AssertStatementTreeImpl result = new AssertStatementTreeImpl(token(find(start, start(assertion.getCondition()), "assert"), "assert"),
        valueExpression(assertion.getCondition()), token(findLast(start, end, ";"), ";"));
      if (assertion.getDetail() != null) {
        result.complete(token(find(end(assertion.getCondition()), start(assertion.getDetail()), ":"), ":"), valueExpression(assertion.getDetail()));
      }
      return result;
    }
    if (tree instanceof SynchronizedTree synchronizedTree) {
      var expression = ((com.sun.source.tree.ParenthesizedTree) synchronizedTree.getExpression()).getExpression();
      return new SynchronizedStatementTreeImpl(token(find(start, start(expression), "synchronized"), "synchronized"),
        token(find(start, start(expression), "("), "("), valueExpression(expression),
        token(find(end(expression), start(synchronizedTree.getBlock()), ")"), ")"), block(synchronizedTree.getBlock()));
    }
    if (tree instanceof TryTree tryTree) {
      ResourceListTreeImpl resources = ResourceListTreeImpl.emptyList();
      for (com.sun.source.tree.Tree resource : tryTree.getResources()) {
        if (!resources.isEmpty()) {
          resources.separators().add(token(findLast(start(tryTree.getResources().get(resources.size() - 1)), start(resource), ";"), ";"));
        }
        resources.add(resource instanceof VariableTree variable ? variableDeclaration(variable) : valueExpression((com.sun.source.tree.ExpressionTree) resource));
      }
      int tryKeyword = find(start, start(tryTree.getBlock()), "try");
      InternalSyntaxToken openParen = resources.isEmpty() ? null : token(find(tryKeyword + 3, start(tryTree.getResources().get(0)), "("), "(");
      InternalSyntaxToken closeParen = resources.isEmpty() ? null : token(find(end(tryTree.getResources().get(tryTree.getResources().size() - 1)), start(tryTree.getBlock()), ")"), ")");
      if (closeParen != null) {
        SourceTokens.Token trailing = tokens.tokenAtOrBefore(find(end(tryTree.getResources().get(tryTree.getResources().size() - 1)), start(tryTree.getBlock()), ")") - 1);
        if (trailing.text().equals(";")) {
          resources.separators().add(tokens.syntaxToken(trailing));
        }
      }
      List<org.sonar.plugins.java.api.tree.CatchTree> catches = new ArrayList<>();
      for (com.sun.source.tree.CatchTree catcher : tryTree.getCatches()) {
        int catchStart = start(catcher);
        int parameterStart = start(catcher.getParameter());
        int parameterEnd = end(catcher.getParameter());
        CatchTreeImpl converted = new CatchTreeImpl(token(find(catchStart, parameterStart, "catch"), "catch"),
          token(find(catchStart, parameterStart, "("), "("), variableDeclaration(catcher.getParameter()),
          token(find(parameterEnd, start(catcher.getBlock()), ")"), ")"), block(catcher.getBlock()));
        catches.add(converted);
        registration.accept(catcher, converted);
      }
      BlockTreeImpl finallyBlock = tryTree.getFinallyBlock() == null ? null : block(tryTree.getFinallyBlock());
      InternalSyntaxToken finallyKeyword = finallyBlock == null ? null
        : token(find(end(tryTree.getCatches().isEmpty() ? tryTree.getBlock() : tryTree.getCatches().get(tryTree.getCatches().size() - 1)),
          start(tryTree.getFinallyBlock()), "finally"), "finally");
      return new TryStatementTreeImpl(token(tryKeyword, "try"), openParen, resources, closeParen,
        block(tryTree.getBlock()), catches, finallyKeyword, finallyBlock);
    }
    if (tree instanceof SwitchTree selection) {
      var expression = ((com.sun.source.tree.ParenthesizedTree) selection.getExpression()).getExpression();
      int firstCase = selection.getCases().isEmpty() ? end : start(selection.getCases().get(0));
      int open = find(start, start(expression), "(");
      int close = find(end(expression), firstCase, ")");
      int openBrace = find(close + 1, firstCase, "{");
      int closeBrace = findLast(start, end, "}");
      return new SwitchStatementTreeImpl(token(find(start, open, "switch"), "switch"), token(open, "("), valueExpression(expression),
        token(close, ")"), token(openBrace, "{"), switchGroups(selection.getCases(), false), token(closeBrace, "}"));
    }
    if (tree instanceof YieldTree yield) {
      return new YieldStatementTreeImpl(token(find(start, start(yield.getValue()), "yield"), "yield"),
        valueExpression(yield.getValue()), token(findLast(start, end, ";"), ";"));
    }
    if (tree instanceof IfTree conditional) {
      var condition = ((com.sun.source.tree.ParenthesizedTree) conditional.getCondition()).getExpression();
      int open = find(start, start(condition), "(");
      int close = find(end(condition), start(conditional.getThenStatement()), ")");
      StatementTree thenStatement = statement(conditional.getThenStatement());
      StatementTree elseStatement = conditional.getElseStatement() == null ? null : statement(conditional.getElseStatement());
      InternalSyntaxToken elseToken = elseStatement == null ? null
        : token(find(end(conditional.getThenStatement()), start(conditional.getElseStatement()), "else"), "else");
      return new IfStatementTreeImpl(token(find(start, open, "if"), "if"), token(open, "("),
        valueExpression(condition), token(close, ")"), thenStatement, elseToken, elseStatement);
    }
    if (tree instanceof WhileLoopTree loop) {
      var condition = ((com.sun.source.tree.ParenthesizedTree) loop.getCondition()).getExpression();
      int open = find(start, start(condition), "(");
      int close = find(end(condition), start(loop.getStatement()), ")");
      return new WhileStatementTreeImpl(token(find(start, open, "while"), "while"), token(open, "("),
        valueExpression(condition), token(close, ")"), statement(loop.getStatement()));
    }
    if (tree instanceof ForLoopTree loop) {
      int open = find(start, end, "(");
      int firstSemicolon = find(open + 1, end, ";");
      int secondSemicolon = find(firstSemicolon + 1, end, ";");
      int close = findLast(secondSemicolon + 1, start(loop.getStatement()), ")");
      StatementListTreeImpl initializer = StatementListTreeImpl.emptyList();
      for (com.sun.source.tree.StatementTree part : loop.getInitializer()) {
        initializer.add(forPart(part));
      }
      StatementListTreeImpl update = StatementListTreeImpl.emptyList();
      for (com.sun.source.tree.ExpressionStatementTree part : loop.getUpdate()) {
        update.add(new ExpressionStatementTreeImpl(valueExpression(part.getExpression()), null));
      }
      if (loop.getInitializer().stream().anyMatch(part -> !(part instanceof VariableTree))) {
        addForSeparators(initializer, loop.getInitializer());
      }
      addForSeparators(update, loop.getUpdate());
      return new ForStatementTreeImpl(token(find(start, open, "for"), "for"), token(open, "("),
        initializer, token(firstSemicolon, ";"), loop.getCondition() == null ? null : valueExpression(loop.getCondition()),
        token(secondSemicolon, ";"), update, token(close, ")"), statement(loop.getStatement()));
    }
    if (tree instanceof EnhancedForLoopTree loop) {
      int open = find(start, start(loop.getVariable()), "(");
      int colon = find(end(loop.getVariable()), start(loop.getExpression()), ":");
      int close = find(end(loop.getExpression()), start(loop.getStatement()), ")");
      return new ForEachStatementImpl(token(find(start, open, "for"), "for"), token(open, "("),
        variableDeclaration(loop.getVariable()), token(colon, ":"), valueExpression(loop.getExpression()), token(close, ")"), statement(loop.getStatement()));
    }
    if (tree instanceof ReturnTree returnTree) {
      return new ReturnStatementTreeImpl(token(find(start, end, "return"), "return"),
        returnTree.getExpression() == null ? null : valueExpression(returnTree.getExpression()),
        token(findLast(start, end, ";"), ";"));
    }
    if (tree instanceof ThrowTree throwTree) {
      return new ThrowStatementTreeImpl(token(find(start, end, "throw"), "throw"),
        valueExpression(throwTree.getExpression()), token(findLast(start, end, ";"), ";"));
    }
    if (tree instanceof com.sun.source.tree.ExpressionStatementTree expressionStatement) {
      return new ExpressionStatementTreeImpl(valueExpression(expressionStatement.getExpression()),
        token(findLast(start, end, ";"), ";"));
    }
    if (tree instanceof VariableTree variableTree) {
      return variableDeclaration(variableTree);
    }
    if (tree instanceof ClassTree localClass) {
      return classDeclaration(localClass);
    }
    if (tree instanceof BlockTree blockTree) {
      return block(blockTree);
    }
    if (tree.getKind() == com.sun.source.tree.Tree.Kind.EMPTY_STATEMENT) {
      return new EmptyStatementTreeImpl(token(find(start, end, ";"), ";"));
    }
    throw unsupported(tree);
  }

  private List<CaseGroupTreeImpl> switchGroups(List<? extends CaseTree> cases, boolean expressionContext) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> pendingLabels = new ArrayList<>();
    List<CaseTree> pendingCases = new ArrayList<>();
    for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
      CaseTree caseTree = cases.get(caseIndex);
      int start = start(caseTree);
      int end = end(caseTree);
      boolean defaultCase = caseTree.getLabels().size() == 1 && caseTree.getLabels().get(0) instanceof DefaultCaseLabelTree
        && tokens.tokenAtOrAfter(start).text().equals("default");
      String keyword = defaultCase ? "default" : "case";
      String separator = caseTree.getCaseKind() == CaseTree.CaseKind.RULE ? "->" : ":";
      List<ExpressionTree> expressions = new ArrayList<>();
      for (com.sun.source.tree.CaseLabelTree caseLabel : caseTree.getLabels()) {
        if (caseLabel instanceof ConstantCaseLabelTree constant) {
          ExpressionTree converted = valueExpression(constant.getConstantExpression());
          expressions.add(constant.getConstantExpression().getKind() == com.sun.source.tree.Tree.Kind.NULL_LITERAL
            ? new NullPatternTreeImpl((org.sonar.plugins.java.api.tree.LiteralTree) converted) : converted);
        } else if (caseLabel instanceof PatternCaseLabelTree patternLabel) {
          org.sonar.plugins.java.api.tree.PatternTree converted = new JavacExpressionConverter(unit, positions, tokens, registration,
            this::anonymousClass, this::block, this::switchExpression).convertPattern(patternLabel.getPattern());
          if (caseTree.getGuard() != null) {
            converted = new GuardedPatternTreeImpl(converted,
              token(find(end(patternLabel.getPattern()), start(caseTree.getGuard()), "when"), "when"), valueExpression(caseTree.getGuard()));
            registration.accept(patternLabel.getPattern(), converted);
          }
          expressions.add(converted);
        } else if (caseLabel instanceof DefaultCaseLabelTree && !defaultCase) {
          expressions.add(new DefaultPatternTreeImpl(token(start(caseLabel), "default")));
        }
      }
      int separatorPos = find(start, end, separator);
      CaseLabelTreeImpl label = new CaseLabelTreeImpl(token(find(start, separatorPos, keyword), keyword), expressions, token(separatorPos, separator));
      pendingLabels.add(label);
      pendingCases.add(caseTree);
      StatementListTreeImpl body = StatementListTreeImpl.emptyList();
      if (caseTree.getStatements() != null) {
        for (com.sun.source.tree.StatementTree caseStatement : caseTree.getStatements()) {
          body.add(statement(caseStatement));
        }
      } else if (caseTree.getBody() instanceof com.sun.source.tree.StatementTree caseStatement) {
        body.add(statement(caseStatement));
      } else if (caseTree.getBody() instanceof com.sun.source.tree.ExpressionTree caseExpression) {
        body.add(expressionContext ? new YieldStatementTreeImpl(null, valueExpression(caseExpression),
          token(find(end(caseExpression), end, ";"), ";")) : new ExpressionStatementTreeImpl(valueExpression(caseExpression), null));
      }
      if (body.isEmpty() && separator.equals(":") && caseIndex + 1 < cases.size()) {
        continue;
      }
      CaseGroupTreeImpl group = new CaseGroupTreeImpl(List.copyOf(pendingLabels), body);
      groups.add(group);
      for (CaseTree pendingCase : pendingCases) {
        registration.accept(pendingCase, group);
      }
      pendingLabels.clear();
      pendingCases.clear();
    }
    return groups;
  }

  private StatementTree forPart(com.sun.source.tree.StatementTree tree) {
    if (tree instanceof com.sun.source.tree.ExpressionStatementTree expressionStatement) {
      return new ExpressionStatementTreeImpl(valueExpression(expressionStatement.getExpression()), null);
    }
    if (tree instanceof VariableTree variable) {
      VariableTreeImpl result = variableDeclaration(variable);
      if (result.endToken() != null && result.endToken().text().equals(";")) {
        result.setEndToken(null);
      }
      return result;
    }
    throw unsupported(tree);
  }

  private void addForSeparators(StatementListTreeImpl result, List<? extends com.sun.source.tree.StatementTree> parts) {
    for (int i = 1; i < parts.size(); i++) {
      result.separators().add(token(findLast(start(parts.get(i - 1)), end(parts.get(i)), ","), ","));
    }
  }

  private ExpressionTree valueExpression(com.sun.source.tree.ExpressionTree tree) {
    return new JavacExpressionConverter(unit, positions, tokens, registration, this::anonymousClass, this::block, this::switchExpression).convert(tree);
  }

  private ExpressionTree switchExpression(com.sun.source.tree.SwitchExpressionTree selection) {
    int start = start(selection);
    int end = end(selection);
    var expression = ((com.sun.source.tree.ParenthesizedTree) selection.getExpression()).getExpression();
    int firstCase = selection.getCases().isEmpty() ? end : start(selection.getCases().get(0));
    int open = find(start, start(expression), "(");
    int close = find(end(expression), firstCase, ")");
    int openBrace = find(close + 1, firstCase, "{");
    int closeBrace = findLast(start, end, "}");
    SwitchExpressionTreeImpl result = new SwitchExpressionTreeImpl(token(find(start, open, "switch"), "switch"),
      token(open, "("), valueExpression(expression), token(close, ")"), token(openBrace, "{"),
      switchGroups(selection.getCases(), true), token(closeBrace, "}"));
    registration.accept(selection, result);
    return result;
  }

  private ClassTreeImpl anonymousClass(ClassTree tree) {
    int start = start(tree);
    int end = end(tree);
    int openBrace = find(start, end, "{");
    int closeBrace = findLast(openBrace, end, "}");
    List<Tree> members = new ArrayList<>();
    int memberCursor = openBrace + 1;
    for (com.sun.source.tree.Tree member : tree.getMembers()) {
      addEmptyClassMembers(members, memberCursor, start(member), false);
      if (member.getKind() == com.sun.source.tree.Tree.Kind.ERRONEOUS) {
        SourceTokens.Token following = tokens.tokenAtOrAfter(end(member));
        memberCursor = following.text().equals(";") ? following.end() : end(member);
        continue;
      }
      if (member instanceof VariableTree variable) {
        members.add(variableDeclaration(variable));
      } else if (member instanceof MethodTree method) {
        members.add(methodDeclaration(method, ""));
      } else if (member instanceof ClassTree nested) {
        members.add(classDeclaration(nested));
      } else if (member instanceof BlockTree initializer) {
        members.add(initializer.isStatic() ? staticInitializer(initializer) : block(initializer, Tree.Kind.INITIALIZER));
      } else {
        throw unsupported(member);
      }
      memberCursor = end(member);
    }
    addEmptyClassMembers(members, memberCursor, closeBrace, false);
    ClassTreeImpl result = new ClassTreeImpl(Tree.Kind.CLASS, token(openBrace, "{"), members, token(closeBrace, "}"));
    registration.accept(tree, result);
    return result;
  }

  private ModifiersTreeImpl modifiers(ModifiersTree tree) {
    List<ModifierTree> result = new ArrayList<>();
    int start = start(tree);
    int end = end(tree);
    if (start < 0 || end < start) {
      return ModifiersTreeImpl.emptyModifiers();
    }
    for (javax.lang.model.element.Modifier flag : tree.getFlags()) {
      String keyword = flag.toString();
      if (flag == javax.lang.model.element.Modifier.NON_SEALED) {
        SourceTokens.Token first = tokens.firstToken(start, end, "non");
        result.add(new ModifierKeywordTreeImpl(Modifier.NON_SEALED,
          new InternalSyntaxToken(tokens.line(first.start()), tokens.column(first.start()), "non-sealed",
            tokens.syntaxToken(first).trivias(), false)));
      } else {
        int offset = find(start, end, keyword);
        result.add(new ModifierKeywordTreeImpl(Modifier.valueOf(flag.name()), token(offset, keyword)));
      }
    }
    for (AnnotationTree annotation : tree.getAnnotations()) {
      result.add(annotation(annotation));
    }
    result.sort(Comparator.comparingInt((ModifierTree modifier) -> modifier.firstToken().range().start().line())
      .thenComparingInt(modifier -> modifier.firstToken().range().start().column()));
    return new ModifiersTreeImpl(result);
  }

  private AnnotationTreeImpl annotation(AnnotationTree annotation) {
    int start = start(annotation);
    int end = end(annotation);
    TypeTree annotationType = type(annotation.getAnnotationType());
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    List<? extends com.sun.source.tree.ExpressionTree> values = annotation.getArguments();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        arguments.separators().add(token(find(end(values.get(i - 1)), start(values.get(i)), ","), ","));
      }
      arguments.add(valueExpression(values.get(i)));
    }
    List<SourceTokens.Token> trailing = tokens.tokensBetween(end(annotation.getAnnotationType()), end);
    if (!trailing.isEmpty() && trailing.get(0).text().equals("(")) {
      arguments.complete(tokens.syntaxToken(trailing.get(0)), token(findLast(start, end, ")"), ")"));
    }
    AnnotationTreeImpl result = new AnnotationTreeImpl(token(start, "@"), annotationType, arguments);
    registration.accept(annotation, result);
    return result;
  }

  private TypeTree type(com.sun.source.tree.Tree tree) {
    return new JavacExpressionConverter(unit, positions, tokens, registration, this::anonymousClass, this::block, this::switchExpression).convertType(tree);
  }

  private ExpressionTree expression(com.sun.source.tree.Tree tree) {
    if (tree instanceof com.sun.source.tree.ExpressionTree expressionTree) {
      return valueExpression(expressionTree);
    }
    throw unsupported(tree);
  }

  private IdentifierTreeImpl identifier(int offset, String name) {
    return new IdentifierTreeImpl(token(offset, name));
  }

  private IdentifierTreeImpl labelUsage(int offset, String name) {
    IdentifierTreeImpl usage = identifier(offset, name);
    JLabelSymbol symbol = activeLabels.get(name);
    if (symbol != null) {
      usage.labelSymbol = symbol;
      symbol.addUsage(usage);
    }
    return usage;
  }

  private InternalSyntaxToken tokenBefore(int end, String text) {
    return token(findLast(0, end, text), text);
  }

  private int methodNamePosition(MethodTree tree, String name) {
    int searchEnd = tree.getBody() == null ? end(tree) : start(tree.getBody());
    for (SourceTokens.Token candidate : tokens.tokensBetween(start(tree), searchEnd)) {
      if (candidate.text().equals(name) && tokens.tokenAtOrAfter(candidate.end()).text().equals("(")) {
        return candidate.start();
      }
    }
    if (tree.getReturnType() == null) {
      List<SourceTokens.Token> openings = tokens.tokensBetween(start(tree), searchEnd).stream()
        .filter(candidate -> candidate.text().equals("(")).toList();
      if (!openings.isEmpty()) {
        return tokens.tokenAtOrBefore(openings.get(openings.size() - 1).start() - 1).start();
      }
      return findLast(start(tree), searchEnd, name);
    }
    throw new IllegalArgumentException("Missing method name '" + name + "'");
  }

  private int matchingCloseParen(int open, int limit) {
    int depth = 0;
    for (SourceTokens.Token part : tokens.tokensBetween(open, limit)) {
      if (part.text().equals("(")) {
        depth++;
      } else if (part.text().equals(")") && --depth == 0) {
        return part.start();
      }
    }
    throw new IllegalArgumentException("Missing ')' for '(' at offset " + open);
  }

  private InternalSyntaxToken tokenAfter(int start, String text) {
    return token(find(start, source.length(), text), text);
  }

  private InternalSyntaxToken token(int offset, String text) {
    return token(offset, text, false);
  }

  private InternalSyntaxToken token(int offset, String text, boolean eof) {
    SourceTokens.Token sourceToken = tokens.tokenAtOrAfter(offset);
    if (sourceToken.start() != offset || !sourceToken.text().equals(text) || sourceToken.isEof() != eof) {
      throw new IllegalArgumentException("Expected token '" + text + "' at offset " + offset);
    }
    return tokens.syntaxToken(sourceToken);
  }

  private InternalSyntaxToken closingAngle(int offset) {
    return new InternalSyntaxToken(tokens.line(offset), tokens.column(offset), ">", List.of(), false);
  }

  private int find(int start, int end, String text) {
    return tokens.firstToken(start, end, text).start();
  }

  private int findLast(int start, int end, String text) {
    return tokens.lastToken(start, end, text).start();
  }

  private int start(com.sun.source.tree.Tree tree) {
    return Math.toIntExact(positions.getStartPosition(unit, tree));
  }

  private int end(com.sun.source.tree.Tree tree) {
    return Math.toIntExact(positions.getEndPosition(unit, tree));
  }

  private static IllegalArgumentException unsupported(com.sun.source.tree.Tree tree) {
    return new IllegalArgumentException("Unsupported javac tree: " + tree.getKind());
  }

  private static void setParents(JavaTree tree) {
    if (tree.isLeaf()) {
      return;
    }
    for (Tree child : tree.getChildren()) {
      if (child instanceof JavaTree javaChild) {
        javaChild.setParent(tree);
        setParents(javaChild);
      }
    }
  }
}
