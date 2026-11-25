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
package org.sonar.java.semsitter;

import com.google.gson.JsonObject;
import com.sonar.sslr.api.RecognitionException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.ModuleNameListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.InternalSyntaxTrivia;
import org.sonar.java.model.JParseResult;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
import org.sonar.java.model.declaration.OpensDirectiveTreeImpl;
import org.sonar.java.model.declaration.ProvidesDirectiveTreeImpl;
import org.sonar.java.model.declaration.RequiresDirectiveTreeImpl;
import org.sonar.java.model.declaration.UsesDirectiveTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonarsource.semsitter.wire.Document;

import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.collectLeaves;
import static org.sonar.java.semsitter.CstNodes.dottedName;
import static org.sonar.java.semsitter.CstNodes.firstNamed;
import static org.sonar.java.semsitter.CstNodes.firstOf;
import static org.sonar.java.semsitter.CstNodes.firstProblem;
import static org.sonar.java.semsitter.CstNodes.isComment;
import static org.sonar.java.semsitter.CstNodes.keyword;
import static org.sonar.java.semsitter.CstNodes.lastKeyword;
import static org.sonar.java.semsitter.CstNodes.lastNamed;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;
import static org.sonar.java.semsitter.CstNodes.setParents;

/**
 * Builds the SonarJava AST from one SemSitter CST document.
 * Positions come from the CST. Symbols and types come from {@link JSema}.
 */
final class SemSitterTranslator extends TranslatorPart {
  private static final String MODULE = "module";
  private static final String PACKAGE = "package";

  private SemSitterTranslator(JSema sema) {
    super(new TranslationState(sema));
  }

  static JParseResult translate(Document document, DocumentTranslator.ServerLoaders loaders, boolean noLibraries, int javaVersion) {
    return translate(document, loaders::types, loaders::hasPackage, noLibraries, javaVersion);
  }

  static JParseResult translate(Document document, Function<Set<String>, JsonObject> types, Function<String, Boolean> packages, boolean noLibraries,
    int javaVersion) {
    JSema sema = new JSema();
    sema.noLibraries(noLibraries);
    sema.ingestDocument(document);
    sema.loadTypes(types);
    sema.loadPackages(packages);
    return JParseResult.success(translate(sema, document, javaVersion));
  }

  private static JavaTree.CompilationUnitTreeImpl translate(JSema sema, Document document, int javaVersion) {
    boolean broken = document.getHasError();
    SemSitterTranslator translator = new SemSitterTranslator(sema);
    translator.state.unnamedVariables = javaVersion >= 22;
    translator.state.records = javaVersion >= 16;
    translator.state.markdownComments = javaVersion >= 23;
    if (!document.getPath().isEmpty()) {
      String fileName = Path.of(document.getPath()).getFileName().toString();
      translator.state.implicitClassName = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }
    CstNode root = new CstNode(document.getTree());
    if (broken) {
      CstNode problem = firstProblem(root);
      int line = problem == null ? 1 : problem.line();
      int column = problem == null ? 1 : (problem.col() + 1);
      throw new RecognitionException(line, String.format("Parse error at line %d column %d: Syntax error", line, column));
    }
    translator.indexTokens(root);
    translator.declareGenericReturns(root);
    JavaTree.CompilationUnitTreeImpl unit = translator.compilationUnit(root);
    translator.state.deferred.forEach(Runnable::run);
    unit.setSemanticModel(sema, sema.undefinedTypes(), sema.getEnvironmentCleaner());
    setParents(unit);
    CompilerWarnings.addTo(unit);
    EffectivelyFinalVariables.mark(unit);
    return unit;
  }

  private void indexTokens(CstNode root) {
    List<CstNode> leaves = new ArrayList<>();
    collectLeaves(root, leaves);
    List<SyntaxTrivia> pending = new ArrayList<>();
    for (CstNode leaf : leaves) {
      if (isComment(leaf)) {
        SyntaxTrivia previous = pending.isEmpty() ? null : pending.get(pending.size() - 1);
        if (state.markdownComments && previous != null && previous.isComment(SyntaxTrivia.CommentKind.MARKDOWN) && leaf.text().startsWith("///")
          && previous.range().end().line() + 1 == leaf.line()) {
          pending.set(pending.size() - 1, new InternalSyntaxTrivia(SyntaxTrivia.CommentKind.MARKDOWN,
            previous.comment() + "\n" + " ".repeat(leaf.col()) + trivia(leaf).comment(), previous.range().start().line(), previous.range().start().columnOffset()));
        } else {
          pending.add(trivia(leaf));
        }
      } else {
        state.tokens.put(leaf.proto, new InternalSyntaxToken(leaf.line(), leaf.col(), leaf.text(), pending, false));
        pending = new ArrayList<>();
      }
    }
    state.eof = new InternalSyntaxToken(root.proto.getEndLine(), root.proto.getEndCol(), "", pending, true);
  }

  private JavaTree.CompilationUnitTreeImpl compilationUnit(CstNode program) {
    JavaTree.PackageDeclarationTreeImpl packageDeclaration = null;
    List<ImportClauseTree> imports = new ArrayList<>();
    List<Tree> types = new ArrayList<>();
    List<Tree> compactMembers = new ArrayList<>();
    ModuleDeclarationTree module = null;
    boolean[] compact = {false};
    for (CstNode child : program.children()) {
      TopLevel collected = addTopLevel(child, imports, types, compactMembers, compact[0]);
      packageDeclaration = collected.packageDeclaration == null ? packageDeclaration : collected.packageDeclaration;
      module = collected.module == null ? module : collected.module;
      compact[0] = collected.compact;
    }
    if (compact[0]) {
      ClassTreeImpl implicitClass = new ClassTreeImpl(Tree.Kind.IMPLICIT_CLASS, null, compactMembers, null);
      declarations().bindImplicitClass(implicitClass, compactMembers);
      types.add(implicitClass);
    }
    return new JavaTree.CompilationUnitTreeImpl(packageDeclaration, imports, types, module, state.eof);
  }

  /** One top-level construct. */
  private TopLevel addTopLevel(CstNode child, List<ImportClauseTree> imports, List<Tree> types, List<Tree> compactMembers, boolean compact) {
    return switch (child.k()) {
      case "package_declaration" -> new TopLevel(packageDeclaration(child), null, compact);
      case "module_declaration" -> new TopLevel(null, moduleDeclaration(child), compact);
      case "import_declaration" -> {
        imports.add(importDeclaration(child));
        yield new TopLevel(null, null, compact);
      }
      case ";" -> {
        EmptyStatementTreeImpl empty = new EmptyStatementTreeImpl(token(child));
        if (compact) {
          compactMembers.add(empty);
        } else if (types.isEmpty()) {
          imports.add(empty);
        } else {
          types.add(empty);
        }
        yield new TopLevel(null, null, compact);
      }
      case "class_declaration", "interface_declaration", "enum_declaration", "record_declaration", "annotation_type_declaration" -> {
        declarations().addMember(compact ? compactMembers : types, child);
        yield new TopLevel(null, null, compact);
      }
      case "method_declaration", "constructor_declaration", "field_declaration", "constant_declaration", "local_variable_declaration", "static_initializer", "block" -> {
        if (!compact) {
          compact = true;
          compactMembers.addAll(types);
          types.clear();
        }
        declarations().addMember(compactMembers, child);
        yield new TopLevel(null, null, compact);
      }
      default -> new TopLevel(null, null, compact);
    };
  }

  private record TopLevel(@Nullable JavaTree.PackageDeclarationTreeImpl packageDeclaration, @Nullable ModuleDeclarationTree module, boolean compact) {
  }

  private ModuleDeclarationTreeImpl moduleDeclaration(CstNode node) {
    CstNode body = requiredField(node, "body", childKind(node, "module_body"));
    List<ModuleDirectiveTree> directives = new ArrayList<>();
    for (CstNode child : body.children()) {
      if (child.k().endsWith("_module_directive")) {
        directives.add(moduleDirective(child));
      }
    }
    return new ModuleDeclarationTreeImpl(
      node.children().stream().filter(CstNodes::isAnnotation).<AnnotationTree>map(members()::annotation).toList(),
      optionalToken(node, "open"),
      token(required(node, MODULE)),
      moduleName(requiredField(node, "name", firstNamed(node))),
      token(required(body, "{")),
      directives,
      token(required(body, "}")));
  }

  private ModuleDirectiveTree moduleDirective(CstNode node) {
    InternalSyntaxToken keyword = token(node.children().get(0));
    InternalSyntaxToken semicolon = token(required(node, ";"));
    return switch (node.k()) {
      case "requires_module_directive" ->
        new RequiresDirectiveTreeImpl(keyword, requiresModifiers(node), moduleName(requiredField(node, MODULE, lastNamed(node))), semicolon);
      case "exports_module_directive" -> new ExportsDirectiveTreeImpl(keyword, directivePackage(node), optionalToken(node, "to"), moduleNames(node), semicolon);
      case "opens_module_directive" -> new OpensDirectiveTreeImpl(keyword, directivePackage(node), optionalToken(node, "to"), moduleNames(node), semicolon);
      case "uses_module_directive" -> new UsesDirectiveTreeImpl(keyword, (TypeTree) expressions().expression(requiredField(node, "type", firstNamed(node))), semicolon);
      default -> new ProvidesDirectiveTreeImpl(keyword, (TypeTree) expressions().expression(requiredField(node, "provided", firstNamed(node))),
        token(required(node, "with")), providers(node), semicolon);
    };
  }

  private ExpressionTree directivePackage(CstNode node) {
    return expressions().expression(requiredField(node, PACKAGE, firstNamed(node)));
  }

  private ModifiersTreeImpl requiresModifiers(CstNode node) {
    List<ModifierTree> modifiers = new ArrayList<>();
    for (CstNode child : node.children()) {
      if ("requires_modifier".equals(child.k())) {
        CstNode keyword = child.children().get(0);
        modifiers.add(new ModifierKeywordTreeImpl("static".equals(keyword.text()) ? Modifier.STATIC : Modifier.TRANSITIVE, token(keyword)));
      }
    }
    return new ModifiersTreeImpl(modifiers);
  }

  private ModuleNameListTreeImpl moduleNames(CstNode node) {
    ModuleNameListTreeImpl names = ModuleNameListTreeImpl.emptyList();
    for (CstNode child : node.children()) {
      if ("modules".equals(child.f())) {
        names.add(moduleName(child));
      } else if (",".equals(child.text())) {
        names.separators().add(token(child));
      }
    }
    return names;
  }

  private QualifiedIdentifierListTreeImpl providers(CstNode node) {
    QualifiedIdentifierListTreeImpl providers = QualifiedIdentifierListTreeImpl.emptyList();
    boolean afterWith = false;
    for (CstNode child : node.children()) {
      if ("with".equals(child.text())) {
        afterWith = true;
      } else if (",".equals(child.text())) {
        providers.separators().add(token(child));
      } else if (afterWith && !";".equals(child.text())) {
        providers.add((TypeTree) expressions().expression(child));
      }
    }
    return providers;
  }

  private ModuleNameTreeImpl moduleName(CstNode node) {
    ModuleNameTreeImpl name = ModuleNameTreeImpl.emptyList();
    addModuleName(name, node);
    return name;
  }

  private void addModuleName(ModuleNameTreeImpl name, CstNode node) {
    if ("scoped_identifier".equals(node.k())) {
      addModuleName(name, requiredField(node, "scope", firstNamed(node)));
      name.add(new IdentifierTreeImpl(token(requiredField(node, "name", lastNamed(node)))));
    } else {
      name.add(new IdentifierTreeImpl(token(node)));
    }
  }

  private JavaTree.PackageDeclarationTreeImpl packageDeclaration(CstNode node) {
    state.naming = true;
    ExpressionTree name = expressions().expression(requiredField(node, "name", firstOf(node, "scoped_identifier", "identifier")));
    state.naming = false;
    state.packageName = dottedName(name);
    names().bindPackage(name, state.packageName);
    return new JavaTree.PackageDeclarationTreeImpl(members().annotations(node), token(required(node, PACKAGE)), name, token(required(node, ";")));
  }

  private JavaTree.ImportTreeImpl importDeclaration(CstNode node) {
    InternalSyntaxToken staticToken = optionalToken(node, "static");
    state.naming = true;
    ExpressionTree name = expressions().expression(requiredField(node, "name", firstNamed(node)));
    state.naming = false;
    CstNode star = keyword(node, "asterisk");
    if (star != null) {
      (staticToken == null ? state.imports.onDemandImports : state.imports.staticOnDemandImports).add(dottedName(name));
      name = new MemberSelectExpressionTreeImpl(name, token(lastKeyword(node, ".")), new IdentifierTreeImpl(token(star)));
    } else if (name instanceof MemberSelectExpressionTree select) {
      if (staticToken == null) {
        state.imports.singleImports.putIfAbsent(select.identifier().name(), dottedName(name));
      } else {
        state.imports.staticImports.putIfAbsent(select.identifier().name(), dottedName(select.expression()));
      }
    }
    JavaTree.ImportTreeImpl imported = new JavaTree.ImportTreeImpl(token(required(node, "import")), staticToken, optionalToken(node, MODULE), name, token(required(node, ";")));
    if (staticToken == null && name instanceof MemberSelectExpressionTree select && !"*".equals(select.identifier().name())) {
      String fqn = dottedName(name);
      if (sema.declaresType(fqn) && sema.resolve(fqn) instanceof Symbol.TypeSymbol type) {
        imported.setSymbol(type);
      } else if (!imported.isModule()) {
        imported.setSymbol(Symbol.TypeSymbol.UNKNOWN_TYPE);
      }
    }
    return imported;
  }

  private SyntaxTrivia trivia(CstNode node) {
    String text = node.text();
    if (text.startsWith("//") && text.endsWith("\r")) {
      text = text.substring(0, text.length() - 1);
    }
    SyntaxTrivia.CommentKind kind;
    if (state.markdownComments && text.startsWith("///")) {
      kind = SyntaxTrivia.CommentKind.MARKDOWN;
    } else if (text.startsWith("/**") && !"/**/".equals(text)) {
      kind = SyntaxTrivia.CommentKind.JAVADOC;
    } else if (text.startsWith("//")) {
      kind = SyntaxTrivia.CommentKind.LINE;
    } else {
      kind = SyntaxTrivia.CommentKind.BLOCK;
    }
    return new InternalSyntaxTrivia(kind, text, node.line(), node.col());
  }
}
