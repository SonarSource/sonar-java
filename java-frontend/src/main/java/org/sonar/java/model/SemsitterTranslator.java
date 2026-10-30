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
package org.sonar.java.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sonar.sslr.api.RecognitionException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Builds the SonarJava AST from one SemSitter CST document.
 * Positions come from the CST. Symbols and types come from {@link JSema}.
 */
final class SemsitterTranslator {

  private final JSema sema;
  private final Map<JsonObject, InternalSyntaxToken> tokens = new IdentityHashMap<>();
  private final Deque<JLabelSymbol> labels = new ArrayDeque<>();
  private final Deque<Symbol.TypeSymbol> types = new ArrayDeque<>();
  private InternalSyntaxToken eof = new InternalSyntaxToken(1, 0, "", List.of(), true);
  private boolean switchExpression;
  private String implicitClassName = "";
  private String packageName = "";
  private boolean naming;
  private final Map<String, String> singleImports = new HashMap<>();
  private final List<String> onDemandImports = new ArrayList<>();
  private final Map<String, String> staticImports = new HashMap<>();
  private final List<String> staticOnDemandImports = new ArrayList<>();

  private SemsitterTranslator(JSema sema) {
    this.sema = sema;
  }

  static JavaTree.CompilationUnitTreeImpl translate(JSema sema, JsonObject document) {
    if (document.has("hasError") && document.get("hasError").getAsBoolean()) {
      throw new RecognitionException(1, "SemSitter: syntax error");
    }
    SemsitterTranslator translator = new SemsitterTranslator(sema);
    if (document.has("path") && document.get("path").isJsonPrimitive()) {
      String fileName = Path.of(document.get("path").getAsString()).getFileName().toString();
      translator.implicitClassName = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }
    Node root = new Node(document.getAsJsonObject("tree"));
    translator.indexTokens(root);
    JavaTree.CompilationUnitTreeImpl unit = translator.compilationUnit(root);
    unit.sema = sema;
    setParents(unit);
    return unit;
  }

  private void indexTokens(Node root) {
    List<Node> leaves = new ArrayList<>();
    collectLeaves(root, leaves);
    List<SyntaxTrivia> pending = new ArrayList<>();
    for (Node leaf : leaves) {
      if (isComment(leaf)) {
        pending.add(trivia(leaf));
      } else {
        tokens.put(leaf.json, new InternalSyntaxToken(leaf.line(), leaf.col(), leaf.text(), pending, false));
        pending = new ArrayList<>();
      }
    }
    int line = 1;
    int column = 0;
    if (root.json.has("p")) {
      line = root.json.getAsJsonArray("p").get(2).getAsInt();
      column = root.json.getAsJsonArray("p").get(3).getAsInt();
    } else if (!leaves.isEmpty()) {
      Node last = leaves.get(leaves.size() - 1);
      line = last.json.getAsJsonArray("p").get(2).getAsInt();
      column = last.json.getAsJsonArray("p").get(3).getAsInt();
    }
    eof = new InternalSyntaxToken(line, column, "", pending, true);
  }

  private static void collectLeaves(Node node, List<Node> leaves) {
    if (node.missing()) {
      return;
    }
    if (isComment(node) || node.allChildren().isEmpty() || isLiteral(node.k())) {
      if (!node.text().isEmpty() || isComment(node)) {
        leaves.add(node);
      }
      return;
    }
    for (Node child : node.allChildren()) {
      collectLeaves(child, leaves);
    }
  }

  private JavaTree.CompilationUnitTreeImpl compilationUnit(Node program) {
    JavaTree.PackageDeclarationTreeImpl packageDeclaration = null;
    List<ImportClauseTree> imports = new ArrayList<>();
    List<Tree> types = new ArrayList<>();
    List<Tree> compactMembers = new ArrayList<>();
    ModuleDeclarationTree module = null;
    boolean compact = false;
    for (Node child : program.children()) {
      switch (child.k()) {
        case "package_declaration" -> packageDeclaration = packageDeclaration(child);
        case "module_declaration" -> module = moduleDeclaration(child);
        case "import_declaration" -> imports.add(importDeclaration(child));
        case ";" -> {
          EmptyStatementTreeImpl empty = new EmptyStatementTreeImpl(token(child));
          if (compact) {
            compactMembers.add(empty);
          } else if (types.isEmpty()) {
            imports.add(empty);
          } else {
            types.add(empty);
          }
        }
        case "class_declaration", "interface_declaration", "enum_declaration", "record_declaration", "annotation_type_declaration" ->
          addMember(compact ? compactMembers : types, child);
        case "method_declaration", "constructor_declaration", "field_declaration", "constant_declaration", "local_variable_declaration", "static_initializer", "block" -> {
          if (!compact) {
            compact = true;
            compactMembers.addAll(types);
            types.clear();
          }
          addMember(compactMembers, child);
        }
        default -> {
          // skipped construct
        }
      }
    }
    if (compact) {
      ClassTreeImpl implicitClass = new ClassTreeImpl(Tree.Kind.IMPLICIT_CLASS, null, compactMembers, null);
      bindImplicitClass(implicitClass, compactMembers);
      types.add(implicitClass);
    }
    return new JavaTree.CompilationUnitTreeImpl(packageDeclaration, imports, types, module, eof);
  }

  private ModuleDeclarationTreeImpl moduleDeclaration(Node node) {
    Node body = requiredField(node, "body", childKind(node, "module_body"));
    List<ModuleDirectiveTree> directives = new ArrayList<>();
    for (Node child : body.children()) {
      if (child.k().endsWith("_module_directive")) {
        directives.add(moduleDirective(child));
      }
    }
    return new ModuleDeclarationTreeImpl(
      node.children().stream().filter(SemsitterTranslator::isAnnotation).<AnnotationTree>map(this::annotation).toList(),
      optionalToken(node, "open"),
      token(required(node, "module")),
      moduleName(requiredField(node, "name", firstNamed(node))),
      token(required(body, "{")),
      directives,
      token(required(body, "}")));
  }

  private ModuleDirectiveTree moduleDirective(Node node) {
    InternalSyntaxToken keyword = token(node.children().get(0));
    InternalSyntaxToken semicolon = token(required(node, ";"));
    return switch (node.k()) {
      case "requires_module_directive" -> new RequiresDirectiveTreeImpl(keyword, requiresModifiers(node), moduleName(requiredField(node, "module", lastNamed(node))), semicolon);
      case "exports_module_directive" -> new ExportsDirectiveTreeImpl(keyword, expression(requiredField(node, "package", firstNamed(node))), optionalToken(node, "to"), moduleNames(node), semicolon);
      case "opens_module_directive" -> new OpensDirectiveTreeImpl(keyword, expression(requiredField(node, "package", firstNamed(node))), optionalToken(node, "to"), moduleNames(node), semicolon);
      case "uses_module_directive" -> new UsesDirectiveTreeImpl(keyword, (TypeTree) expression(requiredField(node, "type", firstNamed(node))), semicolon);
      default -> new ProvidesDirectiveTreeImpl(keyword, (TypeTree) expression(requiredField(node, "provided", firstNamed(node))), token(required(node, "with")), providers(node), semicolon);
    };
  }

  private ModifiersTreeImpl requiresModifiers(Node node) {
    List<ModifierTree> modifiers = new ArrayList<>();
    for (Node child : node.children()) {
      if ("requires_modifier".equals(child.k())) {
        Node keyword = child.children().get(0);
        modifiers.add(new ModifierKeywordTreeImpl("static".equals(keyword.text()) ? Modifier.STATIC : Modifier.TRANSITIVE, token(keyword)));
      }
    }
    return new ModifiersTreeImpl(modifiers);
  }

  private ModuleNameListTreeImpl moduleNames(Node node) {
    ModuleNameListTreeImpl names = ModuleNameListTreeImpl.emptyList();
    for (Node child : node.children()) {
      if ("modules".equals(child.f())) {
        names.add(moduleName(child));
      } else if (",".equals(child.text())) {
        names.separators().add(token(child));
      }
    }
    return names;
  }

  private QualifiedIdentifierListTreeImpl providers(Node node) {
    QualifiedIdentifierListTreeImpl providers = QualifiedIdentifierListTreeImpl.emptyList();
    boolean afterWith = false;
    for (Node child : node.children()) {
      if ("with".equals(child.text())) {
        afterWith = true;
      } else if (",".equals(child.text())) {
        providers.separators().add(token(child));
      } else if (afterWith && !";".equals(child.text())) {
        providers.add((TypeTree) expression(child));
      }
    }
    return providers;
  }

  private ModuleNameTreeImpl moduleName(Node node) {
    ModuleNameTreeImpl name = ModuleNameTreeImpl.emptyList();
    addModuleName(name, node);
    return name;
  }

  private void addModuleName(ModuleNameTreeImpl name, Node node) {
    if ("scoped_identifier".equals(node.k())) {
      addModuleName(name, requiredField(node, "scope", firstNamed(node)));
      name.add(new IdentifierTreeImpl(token(requiredField(node, "name", lastNamed(node)))));
    } else {
      name.add(new IdentifierTreeImpl(token(node)));
    }
  }

  private JavaTree.PackageDeclarationTreeImpl packageDeclaration(Node node) {
    naming = true;
    ExpressionTree name = expression(requiredField(node, "name", firstOf(node, "scoped_identifier", "identifier")));
    naming = false;
    packageName = dottedName(name);
    bindPackage(name, packageName);
    return new JavaTree.PackageDeclarationTreeImpl(annotations(node), token(required(node, "package")), name, token(required(node, ";")));
  }

  private JavaTree.ImportTreeImpl importDeclaration(Node node) {
    InternalSyntaxToken staticToken = optionalToken(node, "static");
    naming = true;
    ExpressionTree name = expression(requiredField(node, "name", firstNamed(node)));
    naming = false;
    if (keyword(node, "*") != null || keyword(node, "asterisk") != null) {
      Node star = keyword(node, "*");
      if (star == null) {
        star = keyword(node, "asterisk");
      }
      (staticToken == null ? onDemandImports : staticOnDemandImports).add(dottedName(name));
      name = new MemberSelectExpressionTreeImpl(name, token(lastKeyword(node, ".")), new IdentifierTreeImpl(token(star)));
    } else if (name instanceof MemberSelectExpressionTree select) {
      if (staticToken == null) {
        singleImports.put(select.identifier().name(), dottedName(name));
      } else {
        staticImports.put(select.identifier().name(), dottedName(select.expression()));
      }
    }
    return new JavaTree.ImportTreeImpl(token(required(node, "import")), staticToken, optionalToken(node, "module"), name, token(required(node, ";")));
  }

  private void addMember(List<Tree> members, Node node) {
    switch (node.k()) {
      case "field_declaration", "constant_declaration", "local_variable_declaration" -> members.addAll(variables(node));
      case "method_declaration", "constructor_declaration", "compact_constructor_declaration" -> members.add(method(node));
      case "annotation_type_element_declaration" -> members.add(annotationElement(node));
      case "class_declaration", "interface_declaration", "enum_declaration", "record_declaration", "annotation_type_declaration" -> members.add(typeDeclaration(node));
      case "static_initializer" -> members.add(staticInitializer(node));
      case "block" -> members.add(initializer(node));
      case "enum_constant" -> members.add(enumConstant(node, null));
      case ";" -> members.add(new EmptyStatementTreeImpl(token(node)));
      case "{", "}" -> {
        // punctuation of the body
      }
      default -> {
        // skipped construct
      }
    }
  }

  private ClassTreeImpl typeDeclaration(Node node) {
    Tree.Kind kind = switch (node.k()) {
      case "interface_declaration" -> Tree.Kind.INTERFACE;
      case "enum_declaration" -> Tree.Kind.ENUM;
      case "record_declaration" -> Tree.Kind.RECORD;
      case "annotation_type_declaration" -> Tree.Kind.ANNOTATION_TYPE;
      default -> Tree.Kind.CLASS;
    };
    Node body = field(node, "body");
    Node declaredName = nameNode(node);
    if (declaredName != null && declaredName.decl() != null && sema.resolve(declaredName.decl()) instanceof Symbol.TypeSymbol typeSymbol) {
      types.push(typeSymbol);
    } else {
      types.push(Symbol.TypeSymbol.UNKNOWN_TYPE);
    }
    List<Tree> members;
    try {
      members = body == null ? new ArrayList<>() : membersOf(body);
    } finally {
      types.pop();
    }
    InternalSyntaxToken open = body == null ? synthetic("{") : token(required(body, "{"));
    InternalSyntaxToken close = body == null ? synthetic("}") : token(required(body, "}"));
    ClassTreeImpl tree = new ClassTreeImpl(kind, open, members, close);
    String keyword = switch (kind) {
      case INTERFACE, ANNOTATION_TYPE -> "interface";
      case ENUM -> "enum";
      case RECORD -> "record";
      default -> "class";
    };
    IdentifierTreeImpl name = identifier(requiredField(node, "name", firstOf(node, "identifier", "type_identifier")));
    tree.complete(modifiers(node), token(required(node, keyword)), name);
    Node typeParameters = field(node, "type_parameters");
    if (typeParameters != null) {
      tree.completeTypeParameters(typeParameters(typeParameters));
    }
    if (kind == Tree.Kind.ANNOTATION_TYPE) {
      Node at = keyword(node, "@");
      if (at != null) {
        tree.completeAtToken(token(at));
      }
    }
    if (kind == Tree.Kind.RECORD) {
      Node parameters = field(node, "parameters");
      tree.completeRecordComponents(
        parameters == null ? synthetic("(") : token(required(parameters, "(")),
        parameters == null ? List.of() : parameters(parameters),
        parameters == null ? synthetic(")") : token(required(parameters, ")")));
    }
    Node superclass = childKind(node, "superclass");
    if (superclass != null) {
      tree.completeSuperclass(token(required(superclass, "extends")), type(firstType(superclass)));
    }
    Node interfaces = childKind(node, "super_interfaces");
    if (interfaces == null) {
      interfaces = childKind(node, "extends_interfaces");
    }
    if (interfaces != null) {
      tree.completeInterfaces(firstKeyword(interfaces), separatedTypes(interfaces));
    }
    Node permits = childKind(node, "permits");
    if (permits != null) {
      tree.completePermittedTypes(token(required(permits, "permits")), separatedTypes(permits));
    }
    bindDeclaration(tree, nameNode(node), name);
    if (tree.superClass() != null && tree.symbol() instanceof JTypeSymbol declared
      && tree.superClass().symbolType().symbol() instanceof JTypeSymbol parent && parent.id != null && !parent.equals(declared)) {
      declared.superName(parent.id);
    }
    return tree;
  }

  private List<Tree> membersOf(Node body) {
    List<Tree> members = new ArrayList<>();
    List<Node> children = body.children();
    for (int i = 0; i < children.size(); i++) {
      Node child = children.get(i);
      if ("enum_constant".equals(child.k())) {
        members.add(enumConstant(child, enumSeparator(children, i)));
      } else if ("enum_body_declarations".equals(child.k())) {
        List<Node> declarations = child.children();
        int j = 0;
        while (childKind(body, "enum_constant") != null && j < declarations.size() && ";".equals(declarations.get(j).text())) {
          j++;
        }
        for (; j < declarations.size(); j++) {
          addMember(members, declarations.get(j));
        }
      } else {
        addMember(members, child);
      }
    }
    return members;
  }

  @Nullable
  private static Node enumSeparator(List<Node> siblings, int index) {
    if (index + 1 >= siblings.size()) {
      return null;
    }
    Node next = siblings.get(index + 1);
    if (",".equals(next.text())) {
      return next;
    }
    if ("enum_body_declarations".equals(next.k())) {
      List<Node> declarations = next.children();
      if (!declarations.isEmpty() && ";".equals(declarations.get(0).text())) {
        return declarations.get(0);
      }
    }
    return null;
  }

  private EnumConstantTreeImpl enumConstant(Node node, @Nullable Node separatorNode) {
    IdentifierTreeImpl name = identifier(requiredField(node, "name", firstOf(node, "identifier")));
    Node arguments = field(node, "arguments");
    ArgumentListTreeImpl args = arguments == null ? ArgumentListTreeImpl.emptyList() : arguments(arguments);
    Node body = field(node, "body");
    ClassTreeImpl classBody = null;
    if (body != null) {
      classBody = new ClassTreeImpl(Tree.Kind.CLASS, token(required(body, "{")), membersOf(body), token(required(body, "}")));
      bindType(classBody, body);
    }
    InternalSyntaxToken separator = separatorNode == null ? null : token(separatorNode);
    EnumConstantTreeImpl tree = new EnumConstantTreeImpl(modifiers(node), name, new NewClassTreeImpl(name, args, classBody), separator);
    bindDeclaration(tree, nameNode(node), name);
    Symbol.TypeSymbol enumType = types.peek();
    if (enumType != null && !enumType.isUnknown()) {
      String owner = enumType.type().fullyQualifiedName();
      Symbol.MethodSymbol constructor = sema.declaredMethod(owner, "<init>", argumentTypes(args));
      name.symbol = constructor != null || !args.isEmpty() ? constructor : sema.resolve(owner + "#<init>()");
      ascribe(name, enumType.type());
      if (tree.symbol() instanceof JSymbol field && field.type().isUnknown()) {
        field.type(enumType.type());
      }
    }
    return tree;
  }

  private MethodTreeImpl annotationElement(Node node) {
    FormalParametersListTreeImpl formals = new FormalParametersListTreeImpl(token(required(node, "(")), token(required(node, ")")));
    Node value = field(node, "value");
    IdentifierTreeImpl name = identifier(requiredField(node, "name", firstOf(node, "identifier")));
    MethodTreeImpl tree = new MethodTreeImpl(formals, value == null ? null : token(required(node, "default")), value == null ? null : expression(value))
      .complete(arrayType(type(requiredField(node, "type", firstType(node))), field(node, "dimensions")), name, token(required(node, ";")))
      .completeWithModifiers(modifiers(node));
    bindDeclaration(tree, nameNode(node), name);
    return tree;
  }

  private MethodTreeImpl method(Node node) {
    Node parameters = field(node, "parameters");
    FormalParametersListTreeImpl formals = new FormalParametersListTreeImpl(
      parameters == null ? null : token(required(parameters, "(")),
      parameters == null ? null : token(required(parameters, ")")));
    if (parameters != null) {
      VariableTree previous = null;
      for (Node child : parameters.children()) {
        if ("formal_parameter".equals(child.k()) || "spread_parameter".equals(child.k()) || "receiver_parameter".equals(child.k())) {
          VariableTreeImpl parameter = variable(child, false);
          formals.add(parameter);
          previous = parameter;
        } else if (",".equals(child.text()) && previous != null) {
          ((VariableTreeImpl) previous).setEndToken(token(child));
        }
      }
    }
    Node body = field(node, "body");
    BlockTreeImpl block = body == null ? null : block(body);
    Node typeNode = field(node, "type");
    TypeTree returnType = typeNode == null || "constructor_declaration".equals(node.k()) ? null : arrayType(type(typeNode), field(node, "dimensions"));
    IdentifierTreeImpl name = identifier(requiredField(node, "name", firstOf(node, "identifier")));
    Node thrown = childKind(node, "throws");
    MethodTreeImpl tree = new MethodTreeImpl(
      returnType,
      name,
      formals,
      thrown == null ? null : token(required(thrown, "throws")),
      thrown == null ? QualifiedIdentifierListTreeImpl.emptyList() : separatedTypes(thrown),
      block,
      body == null ? token(required(node, ";")) : null)
      .completeWithModifiers(modifiers(node));
    Node typeParameters = field(node, "type_parameters");
    if (typeParameters != null) {
      tree.completeWithTypeParameters(typeParameters(typeParameters));
    }
    bindDeclaration(tree, nameNode(node), name);
    return tree;
  }

  private List<VariableTree> parameters(Node parameters) {
    List<VariableTree> trees = new ArrayList<>();
    VariableTreeImpl previous = null;
    for (Node child : parameters.children()) {
      if ("formal_parameter".equals(child.k()) || "spread_parameter".equals(child.k())) {
        previous = variable(child, false);
        trees.add(previous);
      } else if (",".equals(child.text()) && previous != null) {
        previous.setEndToken(token(child));
      }
    }
    return trees;
  }

  private List<VariableTreeImpl> variables(Node declaration) {
    ModifiersTreeImpl modifiers = modifiers(declaration);
    Node typeNode = field(declaration, "type");
    TypeTree type;
    if (typeNode != null) {
      type = type(typeNode);
    } else if ("spread_parameter".equals(declaration.k())) {
      type = spreadType(declaration);
    } else {
      Node catchTypeNode = childKind(declaration, "catch_type");
      type = catchTypeNode == null ? new VarTypeTreeImpl(synthetic("var")) : catchType(catchTypeNode);
    }
    List<VariableTreeImpl> trees = new ArrayList<>();
    VariableTreeImpl previous = null;
    for (Node child : declaration.children()) {
      if ("variable_declarator".equals(child.k()) || "formal_parameter".equals(child.k())) {
        previous = declarator(child, modifiers, type);
        trees.add(previous);
      } else if ((",".equals(child.text()) || ";".equals(child.text())) && previous != null) {
        previous.setEndToken(token(child));
      }
    }
    if (trees.isEmpty() && field(declaration, "name") != null) {
      trees.add(declarator(declaration, modifiers, type));
    }
    return trees;
  }

  private TypeTree spreadType(Node node) {
    TypeTree element = type(firstType(node));
    JavaTree.ArrayTypeTreeImpl array = new JavaTree.ArrayTypeTreeImpl(element, List.of(), token(required(node, "...")));
    ascribe(array, JType.array(sema, element.symbolType() instanceof JType elementType ? elementType : JType.unknown(sema)));
    return array;
  }

  private VariableTreeImpl variable(Node parameter, boolean sharedModifiers) {
    return variables(parameter).get(0);
  }

  private VariableTreeImpl declarator(Node node, ModifiersTreeImpl modifiers, TypeTree type) {
    IdentifierTreeImpl name = identifier(requiredField(node, "name", firstOf(node, "identifier")));
    VariableTreeImpl tree = new VariableTreeImpl(name).completeModifiersAndType(modifiers, applyDimensions(type, node));
    Node value = field(node, "value");
    if (value != null) {
      tree.completeTypeAndInitializer(tree.type(), token(required(node, "=")), expression(value));
    }
    bindDeclaration(tree, nameNode(node) == null ? node : nameNode(node), name);
    if (tree.symbol().type() != null && !tree.symbol().type().isUnknown()) {
      ascribe(tree.type(), tree.symbol().type());
    }
    return tree;
  }

  private TypeTree applyDimensions(TypeTree type, Node node) {
    Node dimensions = field(node, "dimensions");
    if (dimensions == null) {
      return type;
    }
    return arrayType(type, dimensions);
  }

  private BlockTreeImpl staticInitializer(Node node) {
    Node body = field(node, "body");
    if (body == null) {
      body = node;
    }
    BlockTreeImpl block = block(body);
    return new StaticInitializerTreeImpl(token(required(node, "static")), (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken());
  }

  private BlockTreeImpl initializer(Node node) {
    BlockTreeImpl block = block(node);
    return new BlockTreeImpl(Tree.Kind.INITIALIZER, (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken());
  }

  private BlockTreeImpl block(Node node) {
    if (!"block".equals(node.k())) {
      Node nested = childKind(node, "block");
      if (nested != null) {
        node = nested;
      }
    }
    InternalSyntaxToken open = token(required(node, "{"));
    InternalSyntaxToken close = token(required(node, "}"));
    List<StatementTree> statements = new ArrayList<>();
    for (Node child : node.children()) {
      if ("{".equals(child.text()) || "}".equals(child.text())) {
        continue;
      }
      addStatement(statements, child);
    }
    return new BlockTreeImpl(open, statements, close);
  }

  private void addStatement(List<StatementTree> statements, Node node) {
    if ("local_variable_declaration".equals(node.k())) {
      statements.addAll(variables(node));
      return;
    }
    if (";".equals(node.text())) {
      statements.add(new EmptyStatementTreeImpl(token(node)));
      return;
    }
    Node switchNode = childKind(node, "switch_expression");
    if ("expression_statement".equals(node.k()) && switchNode != null) {
      statements.add(switchStatement(switchNode));
      statements.add(new EmptyStatementTreeImpl(token(required(node, ";"))));
      return;
    }
    statements.add(statement(node));
  }

  private StatementTree statement(Node node) {
    return switch (node.k()) {
      case "block" -> block(node);
      case "expression_statement" -> expressionStatement(node);
      case "explicit_constructor_invocation" -> constructorInvocation(node);
      case "if_statement" -> ifStatement(node);
      case "while_statement" -> whileStatement(node);
      case "do_statement" -> doStatement(node);
      case "for_statement" -> forStatement(node);
      case "enhanced_for_statement" -> forEach(node);
      case "break_statement" -> breakStatement(node);
      case "continue_statement" -> continueStatement(node);
      case "return_statement" -> returnStatement(node);
      case "yield_statement" -> yieldStatement(node);
      case "throw_statement" -> throwStatement(node);
      case "assert_statement" -> assertStatement(node);
      case "labeled_statement" -> labeled(node);
      case "try_statement", "try_with_resources_statement" -> tryStatement(node);
      case "synchronized_statement" -> synchronizedStatement(node);
      case "switch_statement", "switch_expression" -> switchStatement(node);
      case "local_variable_declaration" -> variables(node).get(0);
      case "class_declaration", "interface_declaration", "enum_declaration", "record_declaration" -> typeDeclaration(node);
      default -> new EmptyStatementTreeImpl(";".equals(node.text()) ? token(node) : token(required(node, ";")));
    };
  }

  private ExpressionStatementTreeImpl expressionStatement(Node node) {
    ExpressionTree expression = null;
    InternalSyntaxToken semicolon = null;
    for (Node child : node.children()) {
      if (";".equals(child.text())) {
        semicolon = token(child);
      } else if (expression == null) {
        expression = expression(child);
      }
    }
    return new ExpressionStatementTreeImpl(expression == null ? literal(Tree.Kind.NULL_LITERAL, synthetic("null"), sema.getClassType("<nulltype>")) : expression, semicolon);
  }

  private ExpressionStatementTreeImpl constructorInvocation(Node node) {
    Node constructor = requiredField(node, "constructor", firstOf(node, "super", "this"));
    IdentifierTreeImpl identifier = identifier(constructor);
    bindUse(identifier, constructor);
    Node object = field(node, "object");
    ExpressionTree select = object == null
      ? identifier
      : new MemberSelectExpressionTreeImpl(expression(object), token(required(node, ".")), identifier);
    Node typeArguments = field(node, "type_arguments");
    MethodInvocationTreeImpl invocation = new MethodInvocationTreeImpl(select, typeArguments == null ? null : typeArguments(typeArguments),
      arguments(requiredField(node, "arguments", childKind(node, "argument_list"))));
    if (identifier.symbol instanceof Symbol.MethodSymbol method) {
      invocation.methodSymbol = method;
      ascribe(invocation, method.owner().type());
      ascribe(identifier, Type.UNKNOWN);
    }
    return new ExpressionStatementTreeImpl(invocation, token(required(node, ";")));
  }

  private IfStatementTreeImpl ifStatement(Node node) {
    Parens condition = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    Node consequence = field(node, "consequence");
    Node alternative = field(node, "alternative");
    return new IfStatementTreeImpl(
      token(required(node, "if")),
      condition.open,
      condition.expr,
      condition.close,
      consequence == null ? new EmptyStatementTreeImpl(synthetic(";")) : statement(consequence),
      alternative == null ? null : token(required(node, "else")),
      alternative == null ? null : statement(alternative));
  }

  private WhileStatementTreeImpl whileStatement(Node node) {
    Parens condition = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    return new WhileStatementTreeImpl(token(required(node, "while")), condition.open, condition.expr, condition.close, statement(requiredField(node, "body", lastNamed(node))));
  }

  private DoWhileStatementTreeImpl doStatement(Node node) {
    Node body = field(node, "body");
    Parens condition = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    return new DoWhileStatementTreeImpl(
      token(required(node, "do")),
      body == null ? new EmptyStatementTreeImpl(synthetic(";")) : statement(body),
      token(required(node, "while")),
      condition.open,
      condition.expr,
      condition.close,
      token(lastKeyword(node, ";")));
  }

  private ForStatementTreeImpl forStatement(Node node) {
    StatementListTreeImpl init = StatementListTreeImpl.emptyList();
    StatementListTreeImpl update = StatementListTreeImpl.emptyList();
    InternalSyntaxToken firstSemicolon = null;
    InternalSyntaxToken secondSemicolon = null;
    ExpressionTree condition = null;
    int semicolons = 0;
    for (Node child : node.children()) {
      if (")".equals(child.text())) {
        break;
      }
      if ("for".equals(child.text()) || "(".equals(child.text())) {
        continue;
      }
      if (";".equals(child.text())) {
        semicolons++;
        if (semicolons == 1) {
          firstSemicolon = token(child);
        } else {
          secondSemicolon = token(child);
        }
        continue;
      }
      if (",".equals(child.text())) {
        (semicolons == 0 ? init : update).separators().add(token(child));
        continue;
      }
      if ("local_variable_declaration".equals(child.k()) && semicolons == 0) {
        init.addAll(variables(child));
        InternalSyntaxToken semi = optionalToken(child, ";");
        if (semi != null) {
          firstSemicolon = semi;
          semicolons = 1;
        }
      } else if (semicolons == 0) {
        init.add(new ExpressionStatementTreeImpl(expression(child), null));
      } else if (semicolons == 1 && condition == null && !"block".equals(child.k()) && !same(field(node, "body"), child)) {
        condition = expression(child);
      } else if (semicolons >= 1 && same(child, field(node, "body"))) {
        break;
      } else if (semicolons >= 2 || same(field(node, "update"), child)) {
        update.add(new ExpressionStatementTreeImpl(expression(child), null));
      }
    }
    Node body = field(node, "body");
    return new ForStatementTreeImpl(
      token(required(node, "for")),
      token(required(node, "(")),
      init,
      firstSemicolon == null ? synthetic(";") : firstSemicolon,
      condition,
      secondSemicolon == null ? synthetic(";") : secondSemicolon,
      update,
      token(required(node, ")")),
      body == null ? new EmptyStatementTreeImpl(synthetic(";")) : statement(body));
  }

  private ForEachStatementImpl forEach(Node node) {
    TypeTree type = arrayType(type(requiredField(node, "type", firstType(node))), field(node, "dimensions"));
    IdentifierTreeImpl name = identifier(requiredField(node, "name", firstOf(node, "identifier")));
    VariableTreeImpl variable = new VariableTreeImpl(modifiers(node), type, name);
    bindDeclaration(variable, requiredField(node, "name", nameNode(node)), name);
    return new ForEachStatementImpl(
      token(required(node, "for")),
      token(required(node, "(")),
      variable,
      token(required(node, ":")),
      expression(requiredField(node, "value", firstNamed(node))),
      token(required(node, ")")),
      statement(requiredField(node, "body", lastNamed(node))));
  }

  private BreakStatementTreeImpl breakStatement(Node node) {
    IdentifierTreeImpl label = labelUse(node);
    return new BreakStatementTreeImpl(token(required(node, "break")), label, token(required(node, ";")));
  }

  private ContinueStatementTreeImpl continueStatement(Node node) {
    return new ContinueStatementTreeImpl(token(required(node, "continue")), labelUse(node), token(required(node, ";")));
  }

  private ReturnStatementTreeImpl returnStatement(Node node) {
    ExpressionTree expression = null;
    for (Node child : node.children()) {
      if (!"return".equals(child.text()) && !";".equals(child.text())) {
        expression = expression(child);
      }
    }
    return new ReturnStatementTreeImpl(token(required(node, "return")), expression, token(required(node, ";")));
  }

  private YieldStatementTreeImpl yieldStatement(Node node) {
    ExpressionTree expression = null;
    for (Node child : node.children()) {
      if (!"yield".equals(child.text()) && !";".equals(child.text())) {
        expression = expression(child);
      }
    }
    Node keyword = keyword(node, "yield");
    return new YieldStatementTreeImpl(keyword == null ? null : token(keyword), expression, token(required(node, ";")));
  }

  private ThrowStatementTreeImpl throwStatement(Node node) {
    return new ThrowStatementTreeImpl(token(required(node, "throw")), expression(firstNamed(node)), token(required(node, ";")));
  }

  private AssertStatementTreeImpl assertStatement(Node node) {
    List<ExpressionTree> expressions = new ArrayList<>();
    InternalSyntaxToken colon = null;
    for (Node child : node.children()) {
      if (":".equals(child.text())) {
        colon = token(child);
      } else if (!"assert".equals(child.text()) && !";".equals(child.text())) {
        expressions.add(expression(child));
      }
    }
    AssertStatementTreeImpl tree = new AssertStatementTreeImpl(token(required(node, "assert")), expressions.get(0), token(required(node, ";")));
    if (expressions.size() > 1) {
      tree.complete(colon == null ? synthetic(":") : colon, expressions.get(1));
    }
    return tree;
  }

  private LabeledStatementTreeImpl labeled(Node node) {
    IdentifierTreeImpl name = identifier(firstOf(node, "identifier"));
    JLabelSymbol symbol = new JLabelSymbol(name.name());
    labels.push(symbol);
    List<Node> children = node.children();
    LabeledStatementTreeImpl tree = new LabeledStatementTreeImpl(name, token(required(node, ":")), statement(children.get(children.size() - 1)));
    labels.pop();
    symbol.declaration = tree;
    tree.labelSymbol = symbol;
    name.labelSymbol = symbol;
    String id = nameNode(node) == null ? null : firstOf(node, "identifier").decl();
    if (id != null) {
      sema.declare(id, tree);
    }
    return tree;
  }

  private TryStatementTreeImpl tryStatement(Node node) {
    ResourceListTreeImpl resources = ResourceListTreeImpl.emptyList();
    Node resourceNode = field(node, "resources");
    if (resourceNode != null) {
      for (Node child : resourceNode.children()) {
        if ("resource".equals(child.k()) && field(child, "name") == null) {
          resources.add(expression(firstNamed(child)));
        } else if ("resource".equals(child.k()) || "local_variable_declaration".equals(child.k())) {
          resources.addAll(variables(child));
        } else if (";".equals(child.text())) {
          resources.separators().add(token(child));
        } else if (!"(".equals(child.text()) && !")".equals(child.text())) {
          resources.add(expression(child));
        }
      }
    }
    List<org.sonar.plugins.java.api.tree.CatchTree> catches = new ArrayList<>();
    for (Node child : node.children()) {
      if ("catch_clause".equals(child.k())) {
        Node parameter = field(child, "parameter");
        if (parameter == null) {
          parameter = childKind(child, "formal_parameter") == null ? childKind(child, "catch_formal_parameter") : childKind(child, "formal_parameter");
        }
        VariableTreeImpl variable = parameter == null ? variables(child).get(0) : variable(parameter, false);
        catches.add(new CatchTreeImpl(token(required(child, "catch")), token(required(child, "(")), variable, token(required(child, ")")), block(requiredField(child, "body", childKind(child, "block")))));
      }
    }
    Node body = field(node, "body");
    Node finallyBlock = childKind(node, "finally_clause");
    if (finallyBlock == null) {
      finallyBlock = field(node, "finally");
    }
    return new TryStatementTreeImpl(
      token(required(node, "try")),
      resources.isEmpty() ? null : token(required(node, "(")),
      resources,
      resources.isEmpty() ? null : token(required(node, ")")),
      body == null ? block(node) : block(body),
      catches,
      finallyBlock == null ? null : token(required(finallyBlock, "finally")),
      finallyBlock == null ? null : block(childKind(finallyBlock, "block") == null ? finallyBlock : childKind(finallyBlock, "block")));
  }

  private SynchronizedStatementTreeImpl synchronizedStatement(Node node) {
    Parens parens = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    return new SynchronizedStatementTreeImpl(token(required(node, "synchronized")), parens.open, parens.expr, parens.close, block(requiredField(node, "body", childKind(node, "block"))));
  }

  private SwitchStatementTreeImpl switchStatement(Node node) {
    Parens parens = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    Node body = field(node, "body");
    return new SwitchStatementTreeImpl(
      token(required(node, "switch")),
      parens.open,
      parens.expr,
      parens.close,
      body == null ? synthetic("{") : token(required(body, "{")),
      body == null ? List.of() : switchGroups(body, false),
      body == null ? synthetic("}") : token(required(body, "}")));
  }

  private SwitchExpressionTreeImpl switchExpression(Node node) {
    boolean previous = switchExpression;
    switchExpression = true;
    Parens parens = parens(requiredField(node, "condition", childKind(node, "parenthesized_expression")));
    Node body = field(node, "body");
    SwitchExpressionTreeImpl tree = new SwitchExpressionTreeImpl(
      token(required(node, "switch")),
      parens.open,
      parens.expr,
      parens.close,
      body == null ? synthetic("{") : token(required(body, "{")),
      body == null ? List.of() : switchGroups(body, true),
      body == null ? synthetic("}") : token(required(body, "}")));
    switchExpression = previous;
    ascribe(tree, Type.UNKNOWN);
    return tree;
  }

  private List<CaseGroupTreeImpl> switchGroups(Node body, boolean expression) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> labels = new ArrayList<>();
    List<CaseLabelTreeImpl> carried = new ArrayList<>();
    StatementListTreeImpl statements = StatementListTreeImpl.emptyList();
    List<Node> children = body.children();
    for (int i = 0; i < children.size(); i++) {
      Node child = children.get(i);
      if ("{".equals(child.text()) || "}".equals(child.text())) {
        continue;
      }
      if ("switch_label".equals(child.k()) || "switch_rule".equals(child.k()) || "switch_block_statement_group".equals(child.k())) {
        if ("switch_label".equals(child.k())) {
          if (!statements.isEmpty()) {
            groups.add(new CaseGroupTreeImpl(labels, statements));
            labels = new ArrayList<>();
            statements = StatementListTreeImpl.emptyList();
          }
          Node next = i + 1 < children.size() ? children.get(i + 1) : null;
          boolean separator = next != null && ":".equals(next.text());
          labels.add(caseLabel(child, separator ? token(next) : null));
          if (separator) {
            i++;
          }
        } else {
          CaseGroupTreeImpl group = switchArm(child, expression, carried);
          if (group.body().isEmpty() && "switch_block_statement_group".equals(child.k()) && nextIsGroup(children, i)) {
            carried = group.labels().stream().map(CaseLabelTreeImpl.class::cast).collect(Collectors.toCollection(ArrayList::new));
          } else {
            groups.add(group);
            carried = new ArrayList<>();
          }
        }
      } else if (!labels.isEmpty()) {
        addStatement(statements, child);
      }
    }
    if (!labels.isEmpty()) {
      groups.add(new CaseGroupTreeImpl(labels, statements));
    }
    return groups;
  }

  private static boolean nextIsGroup(List<Node> siblings, int index) {
    return index + 1 < siblings.size() && "switch_block_statement_group".equals(siblings.get(index + 1).k());
  }

  private CaseGroupTreeImpl switchArm(Node node, boolean expression, List<CaseLabelTreeImpl> carried) {
    List<CaseLabelTreeImpl> labels = new ArrayList<>(carried);
    StatementListTreeImpl body = StatementListTreeImpl.emptyList();
    List<Node> children = node.children();
    for (int i = 0; i < children.size(); i++) {
      Node child = children.get(i);
      if ("switch_label".equals(child.k())) {
        Node next = i + 1 < children.size() ? children.get(i + 1) : null;
        boolean separator = next != null && (":".equals(next.text()) || "->".equals(next.text()));
        labels.add(caseLabel(child, separator ? token(next) : null));
        if (separator) {
          i++;
        }
        continue;
      }
      if ("->".equals(child.text()) || ":".equals(child.text())) {
        continue;
      }
      if ("expression_statement".equals(child.k()) && "switch_rule".equals(node.k())) {
        ExpressionStatementTreeImpl statement = expressionStatement(child);
        body.add(new YieldStatementTreeImpl(null, statement.expression(), (InternalSyntaxToken) statement.semicolonToken()));
      } else if (expression && !isStatementKind(child.k())) {
        ExpressionTree value = expression(child);
        body.add(new YieldStatementTreeImpl(null, value, optionalToken(node, ";")));
      } else {
        addStatement(body, child);
      }
    }
    return new CaseGroupTreeImpl(labels, body);
  }

  private CaseLabelTreeImpl caseLabel(Node node, @Nullable InternalSyntaxToken separator) {
    InternalSyntaxToken keyword = keyword(node, "case") != null ? token(keyword(node, "case")) : token(required(node, "default"));
    List<ExpressionTree> expressions = new ArrayList<>();
    InternalSyntaxToken end = separator;
    boolean caseLabel = keyword(node, "case") != null;
    for (Node child : node.children()) {
      if ("case".equals(child.text())) {
        continue;
      }
      if ("default".equals(child.text())) {
        if (caseLabel) {
          expressions.add(new DefaultPatternTreeImpl(token(child)));
        }
      } else if (":".equals(child.text()) || "->".equals(child.text())) {
        end = token(child);
      } else if (",".equals(child.text())) {
        continue;
      } else if ("pattern".equals(child.k()) || "record_pattern".equals(child.k()) || "type_pattern".equals(child.k())) {
        expressions.add(pattern(child));
      } else if ("guard".equals(child.k()) && !expressions.isEmpty() && expressions.get(expressions.size() - 1) instanceof PatternTree guarded) {
        expressions.set(expressions.size() - 1, new GuardedPatternTreeImpl(guarded, token(required(child, "when")), expression(firstNamed(child))));
      } else if ("null_literal".equals(child.k())) {
        expressions.add(new NullPatternTreeImpl((LiteralTreeImpl) expression(child)));
      } else {
        expressions.add(expression(child));
      }
    }
    return new CaseLabelTreeImpl(keyword, expressions, end == null ? synthetic(":") : end);
  }

  private ExpressionTree expression(Node node) {
    if (node == null) {
      return literal(Tree.Kind.NULL_LITERAL, synthetic("null"), JType.nullType(sema));
    }
    ExpressionTree tree = switch (node.k()) {
      case "identifier", "type_identifier", "this", "super", "true", "false", "null_literal" -> primary(node);
      case "decimal_integer_literal", "hex_integer_literal", "octal_integer_literal", "binary_integer_literal" -> integerLiteral(node);
      case "decimal_floating_point_literal", "hex_floating_point_literal" -> floatingLiteral(node);
      case "string_literal", "string_fragment" -> literal(node.text().startsWith("\"\"\"") ? Tree.Kind.TEXT_BLOCK : Tree.Kind.STRING_LITERAL, token(node), sema.getClassType("java.lang.String"));
      case "character_literal" -> literal(Tree.Kind.CHAR_LITERAL, token(node), sema.getClassType("char"));
      case "text_block" -> literal(Tree.Kind.TEXT_BLOCK, token(node), sema.getClassType("java.lang.String"));
      case "scoped_identifier", "scoped_type_identifier", "field_access" -> memberSelect(node);
      case "method_invocation" -> invocation(node);
      case "object_creation_expression" -> creation(node);
      case "array_creation_expression" -> arrayCreation(node);
      case "array_initializer", "element_value_array_initializer" -> arrayInitializer(node);
      case "element_value_pair" -> elementValuePair(node);
      case "class_literal" -> classLiteral(node);
      case "annotation", "marker_annotation" -> annotation(node);
      case "array_access" -> arrayAccess(node);
      case "assignment_expression" -> assignment(node);
      case "binary_expression" -> binary(node);
      case "unary_expression" -> unary(node, true);
      case "update_expression" -> update(node);
      case "parenthesized_expression" -> parenthesized(node);
      case "ternary_expression" -> ternary(node);
      case "cast_expression" -> cast(node);
      case "instanceof_expression" -> instanceOf(node);
      case "lambda_expression" -> lambda(node);
      case "method_reference" -> methodReference(node);
      case "switch_expression" -> switchExpression(node);
      case "generic_type", "array_type", "integral_type", "floating_point_type", "boolean_type", "void_type", "wildcard", "annotated_type" -> typeExpression(node);
      default -> primary(node);
    };
    return tree;
  }

  private ExpressionTree primary(Node node) {
    if ("this".equals(node.k()) || "this".equals(node.text())) {
      KeywordThis keyword = new KeywordThis(token(node));
      Symbol.TypeSymbol enclosing = types.peek();
      if (enclosing != null) {
        keyword.symbol = sema.receiverSymbol(enclosing, "this", enclosing.type());
        ascribe(keyword, enclosing.type());
      }
      return keyword;
    }
    if ("super".equals(node.k()) || "super".equals(node.text())) {
      KeywordSuper keyword = new KeywordSuper(token(node));
      Symbol.TypeSymbol enclosing = types.peek();
      if (enclosing != null) {
        Type superType = enclosing.superClass() == null ? Type.UNKNOWN : enclosing.superClass();
        keyword.symbol = sema.receiverSymbol(enclosing, "super", superType);
        ascribe(keyword, superType);
      }
      return keyword;
    }
    if ("true".equals(node.k()) || "false".equals(node.k()) || "true".equals(node.text()) || "false".equals(node.text())) {
      return literal(Tree.Kind.BOOLEAN_LITERAL, token(node), sema.getClassType("boolean"));
    }
    if ("null_literal".equals(node.k()) || "null".equals(node.text())) {
      return literal(Tree.Kind.NULL_LITERAL, token(node), JType.nullType(sema));
    }
    IdentifierTreeImpl identifier = identifier(node);
    bindUse(identifier, node);
    if (!naming && identifier.symbol().isUnknown() && node.unresolved()) {
      String type = importedType(identifier.name());
      if (type != null) {
        bindType(identifier, type);
      }
    }
    return identifier;
  }

  /** The type a simple name stands for when SemSitter left it unresolved: imported, in the package, or in java.lang. */
  @Nullable
  private String importedType(String name) {
    List<String> candidates = new ArrayList<>();
    if (singleImports.containsKey(name)) {
      candidates.add(singleImports.get(name));
    }
    candidates.add(packageName.isEmpty() ? name : (packageName + "." + name));
    onDemandImports.forEach(prefix -> candidates.add(prefix + "." + name));
    candidates.add("java.lang." + name);
    return candidates.stream().filter(sema::declaresType).findFirst().orElse(null);
  }

  private void resolveQualifiedType(ExpressionTree qualifier, IdentifierTreeImpl identifier) {
    Symbol owner = qualifier instanceof MemberSelectExpressionTree select ? select.identifier().symbol() : (qualifier instanceof IdentifierTree name ? name.symbol() : Symbol.UNKNOWN_SYMBOL);
    String candidate;
    if (owner.isTypeSymbol() && !owner.isUnknown()) {
      candidate = ((Symbol.TypeSymbol) owner).type().fullyQualifiedName() + "$" + identifier.name();
    } else if (owner.isUnknown() || owner.isPackageSymbol()) {
      String prefix = dottedName(qualifier);
      candidate = prefix.isEmpty() ? null : (prefix + "." + identifier.name());
    } else {
      candidate = null;
    }
    if (candidate != null && sema.declaresType(candidate)) {
      bindType(identifier, candidate);
    }
  }

  /** The method SemSitter left unresolved, looked up in the receiver type, or in the enclosing types and static imports of an unqualified call. */
  @Nullable
  private Symbol.MethodSymbol libraryMethod(@Nullable ExpressionTree qualifier, String name, List<Type> arguments) {
    List<String> owners = new ArrayList<>();
    if (qualifier != null) {
      Type receiver = qualifier.symbolType().erasure();
      if (receiver.isClass()) {
        owners.add(receiver.fullyQualifiedName());
      }
    } else {
      types.stream().filter(type -> !type.isUnknown()).forEach(type -> owners.add(type.type().fullyQualifiedName()));
      if (staticImports.containsKey(name)) {
        owners.add(staticImports.get(name));
      }
      owners.addAll(staticOnDemandImports);
    }
    return owners.stream().map(owner -> sema.declaredMethod(owner, name, arguments)).filter(Objects::nonNull).findFirst().orElse(null);
  }

  private static List<Type> argumentTypes(List<ExpressionTree> arguments) {
    return arguments.stream().map(ExpressionTree::symbolType).toList();
  }

  private void bindType(IdentifierTreeImpl identifier, String fqn) {
    Symbol symbol = sema.resolve(fqn);
    identifier.symbol = symbol;
    ascribe(identifier, symbol.type());
    sema.use(fqn, identifier);
  }

  private void bindQualifierPackages(ExpressionTree qualifier, IdentifierTree identifier) {
    if (identifier.symbol() instanceof Symbol.TypeSymbol type && !type.isUnknown()) {
      Symbol owner = type.owner();
      while (owner != null && !owner.isPackageSymbol()) {
        owner = owner.owner();
      }
      if (owner != null && dottedName(qualifier).equals(owner.name())) {
        bindPackage(qualifier, owner.name());
      }
    }
  }

  private void bindPackage(ExpressionTree tree, String name) {
    ExpressionTree last = tree;
    if (tree instanceof MemberSelectExpressionTree select) {
      int dot = name.lastIndexOf('.');
      if (dot < 0) {
        return;
      }
      bindPackage(select.expression(), name.substring(0, dot));
      last = select.identifier();
    }
    if (last instanceof IdentifierTreeImpl identifier && identifier.symbol().isUnknown()) {
      identifier.symbol = sema.packageSymbol(name);
    }
  }

  private static String dottedName(ExpressionTree tree) {
    if (tree instanceof MemberSelectExpressionTree select) {
      String qualifier = dottedName(select.expression());
      return qualifier.isEmpty() ? "" : (qualifier + "." + select.identifier().name());
    }
    return tree instanceof IdentifierTree identifier ? identifier.name() : "";
  }

  private IdentifierTreeImpl identifier(Node node) {
    String text = node.text().isEmpty() ? node.k() : node.text();
    return new IdentifierTreeImpl(tokens.getOrDefault(node.json, new InternalSyntaxToken(node.line(), node.col(), text, List.of(), false)), "_".equals(text));
  }

  private LiteralTreeImpl integerLiteral(Node node) {
    String text = node.text();
    boolean isLong = text.endsWith("l") || text.endsWith("L");
    return literal(isLong ? Tree.Kind.LONG_LITERAL : Tree.Kind.INT_LITERAL, token(node), sema.getClassType(isLong ? "long" : "int"));
  }

  private LiteralTreeImpl floatingLiteral(Node node) {
    String text = node.text();
    boolean isFloat = text.endsWith("f") || text.endsWith("F");
    return literal(isFloat ? Tree.Kind.FLOAT_LITERAL : Tree.Kind.DOUBLE_LITERAL, token(node), sema.getClassType(isFloat ? "float" : "double"));
  }

  private LiteralTreeImpl literal(Tree.Kind kind, InternalSyntaxToken token, Type type) {
    LiteralTreeImpl tree = new LiteralTreeImpl(kind, token);
    ascribe(tree, type);
    return tree;
  }

  private MemberSelectExpressionTreeImpl memberSelect(Node node) {
    Node object = field(node, "object");
    if (object == null) {
      object = field(node, "scope");
    }
    Node name = field(node, "name");
    if (name == null) {
      name = field(node, "field");
    }
    Node scope = object == null ? firstNamed(node) : object;
    ExpressionTree qualifier = object == null && compoundName(scope) ? typeExpression(scope) : (object == null ? primary(scope) : superQualifier(node, object));
    Node nameNode = name == null ? lastNamed(node) : name;
    IdentifierTreeImpl identifier = identifier(nameNode);
    bindUse(identifier, name != null || nameNode.json.has("refs") ? nameNode : node);
    if (!naming) {
      if (identifier.symbol().isUnknown()) {
        resolveQualifiedType(qualifier, identifier);
      }
      bindQualifierPackages(qualifier, identifier);
    }
    List<AnnotationTree> annotations = node.children().stream().filter(SemsitterTranslator::isAnnotation).<AnnotationTree>map(this::annotation).toList();
    if (!annotations.isEmpty()) {
      identifier.complete(annotations);
    }
    MemberSelectExpressionTreeImpl tree = new MemberSelectExpressionTreeImpl(qualifier, token(lastKeyword(node, ".")), identifier);
    ascribe(tree, identifier.symbolType());
    return tree;
  }

  private ExpressionTree superQualifier(Node node, Node object) {
    ExpressionTree qualifier = expression(object);
    Node superNode = keyword(node, "super");
    if (superNode == null || same(superNode, object)) {
      return qualifier;
    }
    return new MemberSelectExpressionTreeImpl(qualifier, token(required(node, ".")), new IdentifierTreeImpl(token(superNode)));
  }

  private static boolean compoundName(Node node) {
    return switch (node.k()) {
      case "scoped_type_identifier", "scoped_identifier", "generic_type", "field_access" -> true;
      default -> false;
    };
  }

  private MethodInvocationTreeImpl invocation(Node node) {
    Node name = field(node, "name");
    IdentifierTreeImpl identifier = identifier(name == null ? firstOf(node, "identifier") : name);
    bindUse(identifier, name == null ? node : name);
    ExpressionTree select;
    Node object = field(node, "object");
    ExpressionTree qualifier = null;
    if (object == null) {
      select = identifier;
    } else {
      qualifier = superQualifier(node, object);
      select = new MemberSelectExpressionTreeImpl(qualifier, token(lastKeyword(node, ".")), identifier);
    }
    Node typeArguments = field(node, "type_arguments");
    MethodInvocationTreeImpl tree = new MethodInvocationTreeImpl(select, typeArguments == null ? null : typeArguments(typeArguments), arguments(requiredField(node, "arguments", childKind(node, "argument_list"))));
    Type receiver = qualifier == null ? currentType() : qualifier.symbolType();
    if (identifier.symbol().isUnknown()) {
      Symbol.MethodSymbol method = libraryMethod(qualifier, identifier.name(), argumentTypes(tree.arguments()));
      if (method != null) {
        identifier.symbol = method;
      }
    }
    if (identifier.symbol instanceof JMethodSymbol method) {
      Type returned = sema.invocationReturn(method, receiver);
      JMethodSymbol view = method.withReturnType(returned);
      tree.methodSymbol = view;
      identifier.symbol = view;
      ascribe(identifier, returned);
      ascribe(tree, returned);
    }
    return tree;
  }

  private ExpressionTree typeExpression(Node node) {
    TypeTree type = type(node);
    return type instanceof ExpressionTree expression ? expression : literal(Tree.Kind.NULL_LITERAL, synthetic("null"), Type.UNKNOWN);
  }

  private NewClassTreeImpl creation(Node node) {
    Node typeNode = field(node, "type");
    TypeTree type = annotate(type(typeNode == null ? firstType(node) : typeNode), node);
    Node arguments = field(node, "arguments");
    ArgumentListTreeImpl args = arguments == null ? ArgumentListTreeImpl.emptyList().complete(synthetic("("), synthetic(")")) : arguments(arguments);
    Node body = field(node, "body");
    if (body == null) {
      body = childKind(node, "class_body");
    }
    ClassTreeImpl classBody = null;
    if (body != null) {
      classBody = new ClassTreeImpl(Tree.Kind.CLASS, token(required(body, "{")), membersOf(body), token(required(body, "}")));
      bindType(classBody, body);
    }
    NewClassTreeImpl tree = new NewClassTreeImpl(type, args, classBody).completeWithNewKeyword(token(required(node, "new")));
    Node enclosing = node.children().get(0);
    if (!"new".equals(enclosing.text())) {
      tree.completeWithEnclosingExpression(expression(enclosing));
      tree.completeWithDotToken(token(required(node, ".")));
    }
    Node typeArguments = field(node, "type_arguments");
    if (typeArguments != null) {
      tree.completeWithTypeArguments(typeArguments(typeArguments));
    }
    bindConstructor(tree, typeNode);
    ascribe(tree, classBody != null && !classBody.symbol().isUnknown() ? classBody.symbol().type() : type.symbolType());
    return tree;
  }

  private void bindConstructor(NewClassTreeImpl tree, @Nullable Node typeNode) {
    if (!(tree.getConstructorIdentifier() instanceof IdentifierTreeImpl identifier)) {
      return;
    }
    String constructor = typeNode == null ? null : typeNode.valueTarget();
    if (constructor != null) {
      Symbol resolved = sema.resolve(constructor);
      if (resolved instanceof Symbol.MethodSymbol) {
        identifier.symbol = resolved;
        sema.use(constructor, identifier);
        return;
      }
    }
    Type type = tree.identifier().symbolType().erasure();
    String owner = tree.classBody() != null && type.symbol().isInterface() ? "java.lang.Object" : type.fullyQualifiedName();
    Symbol.MethodSymbol declared = type.isClass() ? sema.declaredMethod(owner, "<init>", argumentTypes(tree.arguments())) : null;
    if (declared != null) {
      identifier.symbol = declared;
    } else if (!(identifier.symbol() instanceof Symbol.MethodSymbol)) {
      identifier.symbol = Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
  }

  private NewArrayTreeImpl arrayCreation(Node node) {
    Node initializer = field(node, "value");
    String dimensionsKind = initializer == null ? "dimensions_expr" : "dimensions";
    List<ArrayDimensionTree> dims = new ArrayList<>();
    for (Node child : node.children()) {
      if (dimensionsKind.equals(child.k())) {
        dims.addAll(dimensions(child));
      }
    }
    InitializerListTreeImpl initializers = initializer == null ? InitializerListTreeImpl.emptyList() : initializers(initializer);
    NewArrayTreeImpl tree = new NewArrayTreeImpl(dims, initializers)
      .complete(annotate(type(requiredField(node, "type", firstType(node))), node))
      .completeWithNewKeyword(token(required(node, "new")));
    if (initializer != null) {
      tree.completeWithCurlyBraces(token(required(initializer, "{")), token(required(initializer, "}")));
    }
    ascribe(tree, arrayTypeOf(tree.type(), dims.size() + (initializer == null ? 0 : 1)));
    return tree;
  }

  private NewArrayTreeImpl arrayInitializer(Node node) {
    NewArrayTreeImpl tree = new NewArrayTreeImpl(List.of(), initializers(node)).completeWithCurlyBraces(token(required(node, "{")), token(required(node, "}")));
    return tree;
  }

  private AssignmentExpressionTreeImpl elementValuePair(Node node) {
    Node key = requiredField(node, "key", firstNamed(node));
    IdentifierTreeImpl name = identifier(key);
    bindUse(name, key);
    ExpressionTree value = expression(requiredField(node, "value", lastNamed(node)));
    AssignmentExpressionTreeImpl tree = new AssignmentExpressionTreeImpl(Tree.Kind.ASSIGNMENT, name, token(required(node, "=")), value);
    ascribe(tree, value.symbolType());
    return tree;
  }

  private MemberSelectExpressionTreeImpl classLiteral(Node node) {
    ExpressionTree type = (ExpressionTree) type(firstNamed(node));
    MemberSelectExpressionTreeImpl tree = new MemberSelectExpressionTreeImpl(type, token(required(node, ".")), new IdentifierTreeImpl(token(required(node, "class"))));
    ascribe(tree, sema.getClassType("java.lang.Class"));
    return tree;
  }

  private InitializerListTreeImpl initializers(Node node) {
    InitializerListTreeImpl list = InitializerListTreeImpl.emptyList();
    for (Node child : node.children()) {
      if ("{".equals(child.text()) || "}".equals(child.text())) {
        continue;
      }
      if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(expression(child));
      }
    }
    return list;
  }

  private ArrayAccessExpressionTreeImpl arrayAccess(Node node) {
    Node index = field(node, "index");
    ExpressionTree array = expression(requiredField(node, "array", firstNamed(node)));
    ArrayDimensionTreeImpl dimension = new ArrayDimensionTreeImpl(token(required(node, "[")), index == null ? null : expression(index), token(required(node, "]")));
    ArrayAccessExpressionTreeImpl tree = new ArrayAccessExpressionTreeImpl(array, dimension);
    Type arrayType = array.symbolType();
    ascribe(tree, arrayType.isArray() ? ((Type.ArrayType) arrayType).elementType() : Type.UNKNOWN);
    return tree;
  }

  private AssignmentExpressionTreeImpl assignment(Node node) {
    Node operator = field(node, "operator");
    String text = operator == null ? "=" : operator.text();
    AssignmentExpressionTreeImpl tree = new AssignmentExpressionTreeImpl(
      assignmentKind(text),
      expression(requiredField(node, "left", firstNamed(node))),
      operator == null ? token(required(node, text)) : token(operator),
      expression(requiredField(node, "right", lastNamed(node))));
    ascribe(tree, tree.variable().symbolType());
    return tree;
  }

  private BinaryExpressionTreeImpl binary(Node node) {
    Node operator = field(node, "operator");
    String text = operator == null ? "+" : operator.text();
    ExpressionTree left = expression(requiredField(node, "left", firstNamed(node)));
    ExpressionTree right = expression(requiredField(node, "right", lastNamed(node)));
    BinaryExpressionTreeImpl tree = new BinaryExpressionTreeImpl(binaryKind(text), left, operator == null ? synthetic(text) : token(operator), right);
    ascribe(tree, binaryType(text, left.symbolType(), right.symbolType()));
    return tree;
  }

  private ExpressionTree unary(Node node, boolean prefix) {
    Node operator = field(node, "operator");
    Node operand = field(node, "operand");
    if (operand == null) {
      operand = firstNamed(node);
    }
    String text = operator == null ? "!" : operator.text();
    ExpressionTree value = expression(operand);
    InternalPrefixUnaryExpression tree = new InternalPrefixUnaryExpression(unaryKind(text, true), operator == null ? synthetic(text) : token(operator), value);
    ascribe(tree, "!".equals(text) ? sema.getClassType("boolean") : value.symbolType());
    return tree;
  }

  private ExpressionTree update(Node node) {
    Node operator = field(node, "operator");
    List<Node> children = node.children();
    if (operator == null) {
      operator = children.stream().filter(child -> "++".equals(child.text()) || "--".equals(child.text())).findFirst().orElse(null);
    }
    boolean prefix = !children.isEmpty() && ("++".equals(children.get(0).text()) || "--".equals(children.get(0).text()));
    ExpressionTree operand = expression(firstNamed(node));
    String text = operator == null ? "++" : operator.text();
    InternalSyntaxToken token = operator == null ? synthetic(text) : token(operator);
    ExpressionTree tree = prefix
      ? new InternalPrefixUnaryExpression(unaryKind(text, true), token, operand)
      : new InternalPostfixUnaryExpression(unaryKind(text, false), operand, token);
    ascribe((AbstractTypedTree) tree, operand.symbolType());
    return tree;
  }

  private ParenthesizedTreeImpl parenthesized(Node node) {
    Parens parens = parens(node);
    ParenthesizedTreeImpl tree = new ParenthesizedTreeImpl(parens.open, parens.expr, parens.close);
    ascribe(tree, parens.expr.symbolType());
    return tree;
  }

  private ConditionalExpressionTreeImpl ternary(Node node) {
    ExpressionTree condition = expression(requiredField(node, "condition", firstNamed(node)));
    ExpressionTree then = expression(requiredField(node, "consequence", firstNamed(node)));
    ExpressionTree otherwise = expression(requiredField(node, "alternative", lastNamed(node)));
    ConditionalExpressionTreeImpl tree = new ConditionalExpressionTreeImpl(condition, token(required(node, "?")), then, token(required(node, ":")), otherwise);
    ascribe(tree, then.symbolType().isUnknown() ? otherwise.symbolType() : then.symbolType());
    return tree;
  }

  private TypeCastExpressionTreeImpl cast(Node node) {
    TypeTree type = type(requiredField(node, "type", firstType(node)));
    ExpressionTree value = expression(requiredField(node, "value", lastNamed(node)));
    TypeCastExpressionTreeImpl tree = new TypeCastExpressionTreeImpl(token(required(node, "(")), type, token(required(node, ")")), value);
    ascribe(tree, type.symbolType());
    return tree;
  }

  private InstanceOfTreeImpl instanceOf(Node node) {
    ExpressionTree left = expression(requiredField(node, "left", firstNamed(node)));
    Node name = field(node, "name");
    Node right = field(node, "right");
    Node patternNode = field(node, "pattern");
    InstanceOfTreeImpl tree;
    if (patternNode != null) {
      tree = new InstanceOfTreeImpl(left, token(required(node, "instanceof")), pattern(patternNode));
    } else if (name != null && right != null) {
      VariableTreeImpl variable = new VariableTreeImpl(modifiers(node), type(right), identifier(name));
      bindDeclaration(variable, name, identifier(name));
      tree = new InstanceOfTreeImpl(left, token(required(node, "instanceof")), new TypePatternTreeImpl(variable));
    } else {
      tree = new InstanceOfTreeImpl(left, token(required(node, "instanceof")), type(right == null ? firstType(node) : right));
    }
    ascribe(tree, sema.getClassType("boolean"));
    return tree;
  }

  private PatternTree pattern(Node node) {
    if ("record_pattern".equals(node.k())) {
      return recordPattern(node);
    }
    Node nested = firstOf(node, "record_pattern", "type_pattern", "pattern", "underscore_pattern");
    if (nested != null && !"type_pattern".equals(node.k())) {
      return pattern(nested);
    }
    if ("underscore_pattern".equals(node.k())) {
      return new TypePatternTreeImpl(new VariableTreeImpl(new IdentifierTreeImpl(token(node), true)));
    }
    Node name = lastOf(node, "identifier");
    VariableTreeImpl variable = new VariableTreeImpl(modifiers(node), type(firstType(node)), identifier(name == null ? node : name));
    bindDeclaration(variable, name, (IdentifierTreeImpl) variable.simpleName());
    if (variable.symbol().type() != null && !variable.symbol().type().isUnknown()) {
      ascribe(variable.type(), variable.symbol().type());
    }
    return new TypePatternTreeImpl(variable);
  }

  private RecordPatternTreeImpl recordPattern(Node node) {
    Node body = required(node, "record_pattern_body");
    List<PatternTree> patterns = new ArrayList<>();
    for (Node child : body.children()) {
      if ("record_pattern_component".equals(child.k()) || "record_pattern".equals(child.k())) {
        patterns.add(pattern(child));
      }
    }
    return new RecordPatternTreeImpl(type(firstType(node)), token(required(body, "(")), patterns, token(required(body, ")")));
  }

  private LambdaExpressionTreeImpl lambda(Node node) {
    List<VariableTree> parameters = new ArrayList<>();
    Node formals = field(node, "parameters");
    InternalSyntaxToken open = null;
    InternalSyntaxToken close = null;
    if (formals != null && ("formal_parameters".equals(formals.k()) || "inferred_parameters".equals(formals.k()))) {
      open = optionalToken(formals, "(");
      close = optionalToken(formals, ")");
      if ("formal_parameters".equals(formals.k())) {
        parameters.addAll(parameters(formals));
      } else {
        VariableTreeImpl previous = null;
        for (Node child : formals.children()) {
          if ("identifier".equals(child.k())) {
            IdentifierTreeImpl name = identifier(child);
            previous = new VariableTreeImpl(name);
            bindDeclaration(previous, child, name);
            parameters.add(previous);
          } else if (",".equals(child.text()) && previous != null) {
            previous.setEndToken(token(child));
          }
        }
      }
    } else if (formals != null) {
      IdentifierTreeImpl name = identifier(formals);
      VariableTreeImpl parameter = new VariableTreeImpl(name);
      bindDeclaration(parameter, formals, name);
      parameters.add(parameter);
    }
    Node body = field(node, "body");
    Tree lambdaBody = body == null ? block(node) : ("block".equals(body.k()) ? block(body) : expression(body));
    LambdaExpressionTreeImpl tree = new LambdaExpressionTreeImpl(open, parameters, close, token(required(node, "->")), lambdaBody);
    for (VariableTree parameter : parameters) {
      if (parameter.symbol().owner() instanceof JMethodSymbol method) {
        method.lambda(true);
        tree.methodSymbol = method;
        break;
      }
    }
    return tree;
  }

  private MethodReferenceTreeImpl methodReference(Node node) {
    ExpressionTree qualifier = expression(requiredField(node, "type", node.children().get(0)));
    Node nameNode = field(node, "name");
    if (nameNode == null) {
      List<Node> children = node.children();
      Node last = children.isEmpty() ? null : children.get(children.size() - 1);
      nameNode = last != null && "new".equals(last.text()) ? last : lastOf(node, "identifier");
    }
    IdentifierTreeImpl name = identifier(nameNode == null ? node : nameNode);
    bindUse(name, nameNode == null ? node : nameNode);
    MethodReferenceTreeImpl tree = new MethodReferenceTreeImpl(qualifier, token(required(node, "::")));
    Node typeArguments = field(node, "type_arguments");
    tree.complete(typeArguments == null ? null : typeArguments(typeArguments), name);
    ascribe(tree, Type.UNKNOWN);
    return tree;
  }

  private TypeTree type(Node node) {
    return switch (node.k()) {
      case "integral_type", "floating_point_type", "boolean_type", "void_type" -> primitive(node);
      case "generic_type" -> generic(node);
      case "array_type" -> arrayType(type(requiredField(node, "element", firstType(node))), field(node, "dimensions"));
      case "scoped_identifier", "scoped_type_identifier", "field_access" -> memberSelect(node);
      case "wildcard" -> wildcard(node);
      case "annotated_type" -> annotatedType(node);
      case "type_identifier" -> "var".equals(node.text()) ? new VarTypeTreeImpl(token(node)) : (TypeTree) primary(node);
      default -> {
        ExpressionTree expression = "identifier".equals(node.k()) || "type_identifier".equals(node.k()) ? primary(node) : expression(node);
        yield expression instanceof TypeTree typeTree ? typeTree : new VarTypeTreeImpl(synthetic("var"));
      }
    };
  }

  private TypeTree annotatedType(Node node) {
    return annotate(type(firstType(node)), node);
  }

  private TypeTree annotate(TypeTree type, Node node) {
    List<AnnotationTree> annotations = node.children().stream().filter(SemsitterTranslator::isAnnotation).<AnnotationTree>map(this::annotation).toList();
    if (!annotations.isEmpty() && type instanceof JavaTree.AnnotatedTypeTree annotated) {
      annotated.complete(annotations);
    }
    return type;
  }

  private JavaTree.PrimitiveTypeTreeImpl primitive(Node node) {
    Node keyword = firstTokenChild(node);
    String text = keyword == null ? node.text() : keyword.text();
    if (text.isEmpty()) {
      text = node.k().replace("_type", "");
    }
    JavaTree.PrimitiveTypeTreeImpl tree = new JavaTree.PrimitiveTypeTreeImpl(keyword == null ? token(node) : token(keyword));
    tree.complete(List.of());
    ascribe(tree, sema.getClassType(text));
    return tree;
  }

  private JavaTree.ParameterizedTypeTreeImpl generic(Node node) {
    TypeTree raw = type(firstType(node));
    Node arguments = field(node, "type_arguments");
    if (arguments == null) {
      arguments = childKind(node, "type_arguments");
    }
    JavaTree.ParameterizedTypeTreeImpl tree = new JavaTree.ParameterizedTypeTreeImpl(raw, arguments == null ? new TypeArgumentListTreeImpl(synthetic("<"), synthetic(">")) : typeArguments(arguments));
    List<Type> args = new ArrayList<>();
    for (TypeTree argument : tree.typeArguments()) {
      args.add(argument.symbolType());
    }
    if (raw.symbolType() instanceof JType) {
      List<JType> typeArguments = new ArrayList<>();
      for (Type argument : args) {
        typeArguments.add(argument instanceof JType jType ? jType : JType.unknown(sema));
      }
      ascribe(tree, JType.named(sema, raw.symbolType().erasure().fullyQualifiedName(), typeArguments));
    }
    return tree;
  }

  private TypeTree arrayType(TypeTree element, @Nullable Node dimensions) {
    if (dimensions == null) {
      return element;
    }
    TypeTree current = element;
    List<AnnotationTreeImpl> annotations = new ArrayList<>();
    List<Node> children = dimensions.children();
    for (int i = 0; i < children.size(); i++) {
      if ("[".equals(children.get(i).text())) {
        InternalSyntaxToken close = i + 1 < children.size() ? token(children.get(i + 1)) : synthetic("]");
        current = new JavaTree.ArrayTypeTreeImpl(current, annotations, token(children.get(i)), close);
        annotations = new ArrayList<>();
        ascribe((AbstractTypedTree) current, JType.array(sema, (JType) (element.symbolType() instanceof JType j ? j : JType.unknown(sema))));
      } else if (isAnnotation(children.get(i))) {
        annotations.add(annotation(children.get(i)));
      }
    }
    return current;
  }

  private JavaTree.WildcardTreeImpl wildcard(Node node) {
    Node bound = firstType(node);
    if (bound == null || same(bound, keyword(node, "?"))) {
      JavaTree.WildcardTreeImpl tree = new JavaTree.WildcardTreeImpl(token(required(node, "?")));
      tree.complete(List.of());
      return tree;
    }
    boolean upper = keyword(node, "extends") != null;
    JavaTree.WildcardTreeImpl tree = new JavaTree.WildcardTreeImpl(
      upper ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD,
      token(required(node, upper ? "extends" : "super")),
      type(bound)).complete(token(required(node, "?")));
    tree.complete(List.of());
    return tree;
  }

  private TypeArgumentListTreeImpl typeArguments(Node node) {
    TypeArgumentListTreeImpl list = new TypeArgumentListTreeImpl(token(required(node, "<")), closingAngle(node));
    for (Node child : node.children()) {
      if ("<".equals(child.text()) || ">".equals(child.text()) || ">>".equals(child.text()) || ">>>".equals(child.text())) {
        continue;
      }
      if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(type(child));
      }
    }
    return list;
  }

  private TypeParameterListTreeImpl typeParameters(Node node) {
    TypeParameterListTreeImpl list = new TypeParameterListTreeImpl(token(required(node, "<")), closingAngle(node));
    for (Node child : node.children()) {
      if ("<".equals(child.text()) || child.text().startsWith(">")) {
        continue;
      }
      if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else if ("type_parameter".equals(child.k())) {
        list.add(typeParameter(child));
      }
    }
    return list;
  }

  private TypeParameterTree typeParameter(Node node) {
    Node nameNode = firstOf(node, "type_identifier", "identifier");
    IdentifierTreeImpl name = identifier(nameNode);
    bindUse(name, nameNode);
    Node bound = childKind(node, "type_bound");
    TypeParameterTreeImpl tree;
    if (bound == null) {
      tree = new TypeParameterTreeImpl(name);
    } else {
      tree = new TypeParameterTreeImpl(name, token(required(bound, "extends")), separatedTypes(bound));
    }
    if (name.symbol().isUnknown()) {
      bindDeclaration(tree, nameNode, name);
    }
    if (name.symbol instanceof Symbol.TypeSymbol typeSymbol) {
      tree.typeSymbol = typeSymbol;
    }
    return tree;
  }

  private TypeTree catchType(Node node) {
    int alternatives = 0;
    for (Node child : node.children()) {
      if (isTypeKind(child.k())) {
        alternatives++;
      }
    }
    if (alternatives <= 1) {
      return type(firstType(node));
    }
    QualifiedIdentifierListTreeImpl list = QualifiedIdentifierListTreeImpl.emptyList();
    for (Node child : node.children()) {
      if (isTypeKind(child.k())) {
        list.add(type(child));
      } else if ("|".equals(child.text())) {
        list.separators().add(token(child));
      }
    }
    return new JavaTree.UnionTypeTreeImpl(list);
  }

  private QualifiedIdentifierListTreeImpl separatedTypes(Node node) {
    QualifiedIdentifierListTreeImpl list = QualifiedIdentifierListTreeImpl.emptyList();
    addSeparatedTypes(list, node);
    return list;
  }

  private void addSeparatedTypes(QualifiedIdentifierListTreeImpl list, Node node) {
    for (Node child : node.children()) {
      if ("type_list".equals(child.k())) {
        addSeparatedTypes(list, child);
      } else if (isTypeKind(child.k())) {
        list.add(type(child));
      } else if (",".equals(child.text()) || "&".equals(child.text())) {
        list.separators().add(token(child));
      }
    }
  }

  private ArgumentListTreeImpl arguments(Node node) {
    if (node == null) {
      return ArgumentListTreeImpl.emptyList();
    }
    ArgumentListTreeImpl list = ArgumentListTreeImpl.emptyList();
    InternalSyntaxToken open = null;
    InternalSyntaxToken close = null;
    for (Node child : node.children()) {
      if ("(".equals(child.text())) {
        open = token(child);
      } else if (")".equals(child.text())) {
        close = token(child);
      } else if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(expression(child));
      }
    }
    return list.complete(open, close);
  }

  private List<ArrayDimensionTree> dimensions(Node node) {
    List<ArrayDimensionTree> dims = new ArrayList<>();
    List<AnnotationTree> annotations = new ArrayList<>();
    ExpressionTree index = null;
    InternalSyntaxToken open = null;
    for (Node child : node.children()) {
      if ("[".equals(child.text())) {
        open = token(child);
        index = null;
      } else if ("]".equals(child.text()) && open != null) {
        dims.add(new ArrayDimensionTreeImpl(open, index, token(child)).completeAnnotations(annotations));
        annotations = new ArrayList<>();
        open = null;
      } else if (open != null) {
        index = expression(child);
      } else if (isAnnotation(child)) {
        annotations.add(annotation(child));
      }
    }
    return dims;
  }

  private ModifiersTreeImpl modifiers(Node node) {
    Node modifiers = childKind(node, "modifiers");
    List<ModifierTree> trees = new ArrayList<>();
    if (modifiers != null) {
      for (Node child : modifiers.children()) {
        ModifierTree modifier = modifier(child);
        if (modifier != null) {
          trees.add(modifier);
        }
      }
    }
    return new ModifiersTreeImpl(trees);
  }

  @Nullable
  private ModifierTree modifier(Node node) {
    if (isAnnotation(node)) {
      return annotation(node);
    }
    Modifier modifier = modifierKeyword(node.text());
    return modifier == null ? null : new ModifierKeywordTreeImpl(modifier, token(node));
  }

  private AnnotationTreeImpl annotation(Node node) {
    Node name = field(node, "name");
    TypeTree type = name == null ? (TypeTree) primary(firstOf(node, "identifier", "scoped_identifier", "type_identifier")) : (TypeTree) expression(name);
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    Node args = field(node, "arguments");
    if (args != null) {
      arguments = arguments(args);
    }
    AnnotationTreeImpl tree = new AnnotationTreeImpl(token(required(node, "@")), type, arguments);
    ascribe(tree, type.symbolType());
    for (ExpressionTree argument : arguments) {
      if (argument instanceof AssignmentExpressionTree assignment && assignment.variable() instanceof IdentifierTreeImpl element && element.symbol().isUnknown()
        && type.symbolType().isClass()) {
        Symbol.MethodSymbol method = sema.declaredMethod(type.symbolType().fullyQualifiedName(), element.name(), List.of());
        if (method != null) {
          element.symbol = method;
          ascribe(element, method.returnType().type());
        }
      }
    }
    return tree;
  }

  private List<AnnotationTree> annotations(Node node) {
    Node modifiers = childKind(node, "modifiers");
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Node child : (modifiers == null ? node : modifiers).children()) {
      if (isAnnotation(child)) {
        annotations.add(annotation(child));
      }
    }
    return annotations;
  }

  private void bindDeclaration(Tree tree, @Nullable Node name, IdentifierTreeImpl identifier) {
    if (name == null) {
      return;
    }
    String id = name.decl();
    if (id == null) {
      return;
    }
    Symbol symbol = sema.resolve(id);
    sema.declare(id, tree);
    for (String extra : name.decls()) {
      if (!extra.equals(id)) {
        sema.declare(extra, tree);
      }
    }
    if (tree instanceof VariableTreeImpl variable && symbol instanceof JSymbol variableSymbol && symbol.isVariableSymbol() && symbol.type().isUnknown()
      && variable.type() != null && !variable.type().symbolType().isUnknown()) {
      variableSymbol.type(variable.type().symbolType());
    }
    identifier.symbol = symbol;
    ascribe(identifier, identifierType(symbol));
    if (tree instanceof ClassTreeImpl classTree && symbol instanceof Symbol.TypeSymbol typeSymbol) {
      classTree.typeSymbol = typeSymbol;
    } else if (tree instanceof MethodTreeImpl method && symbol instanceof Symbol.MethodSymbol methodSymbol) {
      method.methodSymbol = methodSymbol;
    } else if (tree instanceof VariableTreeImpl variable && symbol instanceof Symbol.VariableSymbol variableSymbol) {
      variable.variableSymbol = variableSymbol;
    }
  }

  private void bindImplicitClass(ClassTreeImpl tree, List<Tree> members) {
    JTypeSymbol implicitClass = null;
    for (Tree member : members) {
      if (member instanceof MethodTree method && method.symbol().owner() instanceof JTypeSymbol owner && !owner.isUnknown()) {
        implicitClass = owner;
        break;
      }
    }
    if (implicitClass == null && !implicitClassName.isEmpty() && sema.resolve(implicitClassName) instanceof JTypeSymbol named) {
      implicitClass = named;
    }
    if (implicitClass == null) {
      return;
    }
    tree.typeSymbol = implicitClass;
    sema.declare(implicitClass.id, tree);
    for (Tree member : members) {
      if (member instanceof MethodTree method && method.symbol() instanceof JMethodSymbol symbol && (symbol.owner() == null || symbol.owner().isUnknown())) {
        symbol.owner(implicitClass);
      }
    }
  }

  private void bindType(ClassTreeImpl tree, Node node) {
    String id = node.decl();
    if (id != null && sema.resolve(id) instanceof Symbol.TypeSymbol typeSymbol) {
      tree.typeSymbol = typeSymbol;
      sema.declare(id, tree);
      return;
    }
    for (Tree member : tree.members()) {
      Symbol symbol = member instanceof MethodTree method ? method.symbol() : (member instanceof VariableTree variable ? variable.symbol() : null);
      if (symbol != null && !symbol.isUnknown() && symbol.owner() instanceof Symbol.TypeSymbol owner) {
        tree.typeSymbol = owner;
        return;
      }
    }
  }

  private void bindUse(IdentifierTreeImpl identifier, Node node) {
    String target = node.valueTarget();
    if (target == null) {
      target = node.typeTarget();
    }
    if (target == null) {
      return;
    }
    Symbol symbol = sema.resolve(target);
    String typeTarget = node.typeTarget();
    if (symbol.isMethodSymbol() && "<init>".equals(symbol.name()) && typeTarget != null && !typeTarget.equals(target)) {
      target = typeTarget;
      symbol = sema.resolve(target);
    }
    if (symbol.isMethodSymbol() && symbol.isUnknown()) {
      symbol = Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
    identifier.symbol = symbol;
    ascribe(identifier, identifierType(symbol));
    sema.use(target, identifier);
  }

  @Nullable
  private IdentifierTreeImpl labelUse(Node node) {
    Node name = firstOf(node, "identifier");
    if (name == null || "break".equals(name.text()) || "continue".equals(name.text())) {
      return null;
    }
    IdentifierTreeImpl identifier = identifier(name);
    for (JLabelSymbol label : labels) {
      if (label.name().equals(identifier.name())) {
        label.usages.add(identifier);
        identifier.labelSymbol = label;
      }
    }
    return identifier;
  }

  private Parens parens(@Nullable Node node) {
    if (node == null) {
      return new Parens(synthetic("("), literal(Tree.Kind.BOOLEAN_LITERAL, synthetic("true"), sema.getClassType("boolean")), synthetic(")"));
    }
    if (!"parenthesized_expression".equals(node.k())) {
      return new Parens(synthetic("("), expression(node), synthetic(")"));
    }
    InternalSyntaxToken open = token(required(node, "("));
    InternalSyntaxToken close = token(required(node, ")"));
    ExpressionTree expression = null;
    for (Node child : node.children()) {
      if (!"(".equals(child.text()) && !")".equals(child.text())) {
        expression = expression(child);
      }
    }
    return new Parens(open, expression == null ? literal(Tree.Kind.BOOLEAN_LITERAL, synthetic("true"), sema.getClassType("boolean")) : expression, close);
  }

  @Nullable
  private Type currentType() {
    Symbol.TypeSymbol enclosing = types.isEmpty() ? null : types.peek();
    return enclosing == null ? Type.UNKNOWN : enclosing.type();
  }

  private Type arrayTypeOf(TypeTree element, int dimensions) {
    Type type = element.symbolType();
    if (!(type instanceof JType jType)) {
      return Type.UNKNOWN;
    }
    for (int i = 0; i < Math.max(dimensions, 1); i++) {
      jType = JType.array(sema, jType);
    }
    return jType;
  }

  private static Type binaryType(String operator, Type left, Type right) {
    if (operator.equals("&&") || operator.equals("||") || operator.equals("==") || operator.equals("!=")
      || operator.equals("<") || operator.equals(">") || operator.equals("<=") || operator.equals(">=") || operator.equals("instanceof")) {
      return left instanceof JType j ? j.sema.getClassType("boolean") : Type.UNKNOWN;
    }
    if ("+".equals(operator) && (left.is("java.lang.String") || right.is("java.lang.String"))) {
      return left instanceof JType j ? j.sema.getClassType("java.lang.String") : Type.UNKNOWN;
    }
    if (left.isUnknown()) {
      return right;
    }
    return left;
  }

  private static Tree.Kind binaryKind(String operator) {
    return switch (operator) {
      case "+" -> Tree.Kind.PLUS;
      case "-" -> Tree.Kind.MINUS;
      case "*" -> Tree.Kind.MULTIPLY;
      case "/" -> Tree.Kind.DIVIDE;
      case "%" -> Tree.Kind.REMAINDER;
      case "<<" -> Tree.Kind.LEFT_SHIFT;
      case ">>" -> Tree.Kind.RIGHT_SHIFT;
      case ">>>" -> Tree.Kind.UNSIGNED_RIGHT_SHIFT;
      case "<" -> Tree.Kind.LESS_THAN;
      case ">" -> Tree.Kind.GREATER_THAN;
      case "<=" -> Tree.Kind.LESS_THAN_OR_EQUAL_TO;
      case ">=" -> Tree.Kind.GREATER_THAN_OR_EQUAL_TO;
      case "==" -> Tree.Kind.EQUAL_TO;
      case "!=" -> Tree.Kind.NOT_EQUAL_TO;
      case "&" -> Tree.Kind.AND;
      case "|" -> Tree.Kind.OR;
      case "^" -> Tree.Kind.XOR;
      case "&&" -> Tree.Kind.CONDITIONAL_AND;
      case "||" -> Tree.Kind.CONDITIONAL_OR;
      default -> Tree.Kind.PLUS;
    };
  }

  private static Tree.Kind assignmentKind(String operator) {
    return switch (operator) {
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
      default -> Tree.Kind.ASSIGNMENT;
    };
  }

  private static Tree.Kind unaryKind(String operator, boolean prefix) {
    return switch (operator) {
      case "++" -> prefix ? Tree.Kind.PREFIX_INCREMENT : Tree.Kind.POSTFIX_INCREMENT;
      case "--" -> prefix ? Tree.Kind.PREFIX_DECREMENT : Tree.Kind.POSTFIX_DECREMENT;
      case "-" -> Tree.Kind.UNARY_MINUS;
      case "+" -> Tree.Kind.UNARY_PLUS;
      case "~" -> Tree.Kind.BITWISE_COMPLEMENT;
      default -> Tree.Kind.LOGICAL_COMPLEMENT;
    };
  }

  @Nullable
  private static Modifier modifierKeyword(String text) {
    return switch (text) {
      case "public" -> Modifier.PUBLIC;
      case "protected" -> Modifier.PROTECTED;
      case "private" -> Modifier.PRIVATE;
      case "static" -> Modifier.STATIC;
      case "abstract" -> Modifier.ABSTRACT;
      case "final" -> Modifier.FINAL;
      case "native" -> Modifier.NATIVE;
      case "synchronized" -> Modifier.SYNCHRONIZED;
      case "transient" -> Modifier.TRANSIENT;
      case "volatile" -> Modifier.VOLATILE;
      case "strictfp" -> Modifier.STRICTFP;
      case "default" -> Modifier.DEFAULT;
      case "sealed" -> Modifier.SEALED;
      case "non-sealed" -> Modifier.NON_SEALED;
      case "transitive" -> Modifier.TRANSITIVE;
      default -> null;
    };
  }

  private static boolean isTypeKind(String kind) {
    return switch (kind) {
      case "type_identifier", "identifier", "scoped_identifier", "scoped_type_identifier", "generic_type", "array_type", "integral_type", "floating_point_type", "boolean_type", "void_type", "wildcard", "annotated_type" -> true;
      default -> false;
    };
  }

  private static boolean isStatementKind(String kind) {
    return kind.endsWith("_statement") || "block".equals(kind) || "local_variable_declaration".equals(kind) || "switch_expression".equals(kind);
  }

  private static boolean isLiteral(String kind) {
    return switch (kind) {
      case "string_literal", "character_literal", "text_block", "decimal_integer_literal", "hex_integer_literal", "octal_integer_literal", "binary_integer_literal", "decimal_floating_point_literal", "hex_floating_point_literal" -> true;
      default -> false;
    };
  }

  private static boolean isComment(Node node) {
    return "line_comment".equals(node.k()) || "block_comment".equals(node.k());
  }

  private static SyntaxTrivia trivia(Node node) {
    String text = node.text();
    if (text.startsWith("//") && text.endsWith("\r")) {
      text = text.substring(0, text.length() - 1);
    }
    SyntaxTrivia.CommentKind kind;
    if (text.startsWith("///")) {
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

  private InternalSyntaxToken token(Node node) {
    InternalSyntaxToken found = tokens.get(node.json);
    if (found != null) {
      return found;
    }
    if ("@interface".equals(node.text())) {
      return new InternalSyntaxToken(node.line(), node.col(), "@", List.of(), false);
    }
    if (!node.children().isEmpty()) {
      for (Node child : node.children()) {
        if (child.children().isEmpty() && !child.text().isEmpty() && !isComment(child)) {
          return token(child);
        }
      }
    }
    String text = node.text().isEmpty() ? node.k() : node.text();
    if (text.indexOf('_') >= 0) {
      text = "";
    }
    return new InternalSyntaxToken(node.line(), node.col(), text, List.of(), false);
  }

  private InternalSyntaxToken closingAngle(Node node) {
    for (int i = node.children().size() - 1; i >= 0; i--) {
      Node child = node.children().get(i);
      if (">".equals(child.text()) || ">>".equals(child.text()) || ">>>".equals(child.text())) {
        return new InternalSyntaxToken(child.line(), child.col() + child.text().length() - 1, ">", List.of(), false);
      }
    }
    return synthetic(">");
  }

  private InternalSyntaxToken synthetic(String text) {
    return new InternalSyntaxToken(1, 0, text, List.of(), false);
  }

  @Nullable
  private InternalSyntaxToken optionalToken(Node node, String text) {
    Node found = keyword(node, text);
    return found == null ? null : token(found);
  }

  private static Type identifierType(Symbol symbol) {
    return symbol instanceof Symbol.MethodSymbol method && !method.isUnknown() ? method.returnType().type() : symbol.type();
  }

  private static void ascribe(Tree tree, Type type) {
    if (tree instanceof AbstractTypedTree typed && type != null) {
      typed.type = type;
    }
  }

  @Nullable
  private static Node keyword(Node node, String text) {
    for (Node child : node.children()) {
      if (text.equals(child.text()) || text.equals(child.k())) {
        return child;
      }
    }
    return null;
  }

  private static Node lastKeyword(Node node, String text) {
    List<Node> children = node.children();
    for (int i = children.size() - 1; i >= 0; i--) {
      if (text.equals(children.get(i).text())) {
        return children.get(i);
      }
    }
    return node;
  }

  private static boolean same(@Nullable Node left, @Nullable Node right) {
    return left != null && right != null && left.json == right.json;
  }

  private static Node required(Node node, String text) {
    Node found = keyword(node, text);
    return found == null ? node : found;
  }

  @Nullable
  private static Node field(Node node, String name) {
    for (Node child : node.children()) {
      if (name.equals(child.f())) {
        return child;
      }
    }
    return null;
  }

  private static Node requiredField(Node node, String name, @Nullable Node fallback) {
    Node found = field(node, name);
    return found != null ? found : (fallback == null ? node : fallback);
  }

  @Nullable
  private static Node childKind(Node node, String kind) {
    for (Node child : node.children()) {
      if (kind.equals(child.k())) {
        return child;
      }
    }
    return null;
  }

  @Nullable
  private static Node firstOf(Node node, String... kinds) {
    for (Node child : node.children()) {
      for (String kind : kinds) {
        if (kind.equals(child.k())) {
          return child;
        }
      }
    }
    return null;
  }

  @Nullable
  private static Node lastOf(Node node, String kind) {
    Node found = null;
    for (Node child : node.children()) {
      if (kind.equals(child.k())) {
        found = child;
      }
    }
    return found;
  }

  @Nullable
  private static Node nameNode(Node node) {
    Node named = field(node, "name");
    return named != null ? named : firstOf(node, "identifier", "type_identifier");
  }

  @Nullable
  private static Node firstNamed(Node node) {
    for (Node child : node.children()) {
      if (!child.text().equals(child.k()) || child.k().contains("_")) {
        if (!child.children().isEmpty() || child.k().contains("_") || "identifier".equals(child.k()) || "type_identifier".equals(child.k())) {
          return child;
        }
      }
    }
    return node.children().isEmpty() ? node : node.children().get(0);
  }

  @Nullable
  private static boolean isAnnotation(Node node) {
    return "annotation".equals(node.k()) || "marker_annotation".equals(node.k());
  }

  private static Node lastNamed(Node node) {
    List<Node> children = node.children();
    for (int i = children.size() - 1; i >= 0; i--) {
      Node child = children.get(i);
      if (child.k().contains("_") || "identifier".equals(child.k())) {
        return child;
      }
    }
    return node;
  }

  @Nullable
  private static Node firstType(Node node) {
    for (Node child : node.children()) {
      if (isTypeKind(child.k())) {
        return child;
      }
    }
    return null;
  }

  @Nullable
  private static Node firstTokenChild(Node node) {
    for (Node child : node.children()) {
      if (child.children().isEmpty() && !child.text().isEmpty()) {
        return child;
      }
    }
    return null;
  }

  private static InternalSyntaxToken firstKeyword(Node node) {
    for (Node child : node.children()) {
      if (child.children().isEmpty() && !child.text().isEmpty() && !",".equals(child.text())) {
        return new InternalSyntaxToken(child.line(), child.col(), child.text(), List.of(), false);
      }
    }
    return new InternalSyntaxToken(node.line(), node.col(), "implements", List.of(), false);
  }

  private static void setParents(Tree node) {
    if (node instanceof JavaTree javaTree && node.kind() != Tree.Kind.INFERED_TYPE && node.kind() != Tree.Kind.TOKEN) {
      for (Tree child : javaTree.getChildren()) {
        if (child instanceof JavaTree javaChild) {
          javaChild.setParent(node);
          setParents(child);
        }
      }
    }
  }

  private record Parens(InternalSyntaxToken open, ExpressionTree expr, InternalSyntaxToken close) {
  }

  private static final class Node {
    private final JsonObject json;

    private Node(JsonObject json) {
      this.json = json;
    }

    private String k() {
      return json.get("k").getAsString();
    }

    private String f() {
      return json.has("f") ? json.get("f").getAsString() : "";
    }

    private String text() {
      return json.has("t") ? json.get("t").getAsString() : "";
    }

    private boolean missing() {
      return json.has("m") && json.get("m").getAsBoolean();
    }

    private int line() {
      return json.has("p") ? json.getAsJsonArray("p").get(0).getAsInt() : 1;
    }

    private int col() {
      return json.has("p") ? json.getAsJsonArray("p").get(1).getAsInt() : 0;
    }

    private List<Node> children() {
      List<Node> children = new ArrayList<>();
      for (Node child : allChildren()) {
        if (!isComment(child)) {
          children.add(child);
        }
      }
      return children;
    }

    private List<Node> allChildren() {
      if (!json.has("c")) {
        return List.of();
      }
      List<Node> children = new ArrayList<>();
      for (JsonElement element : json.getAsJsonArray("c")) {
        Node child = new Node(element.getAsJsonObject());
        if (!child.missing()) {
          children.add(child);
        }
      }
      return children;
    }

    @Nullable
    private String decl() {
      List<String> decls = decls();
      return decls.isEmpty() ? null : decls.get(0);
    }

    private List<String> decls() {
      if (!json.has("decls")) {
        return List.of();
      }
      List<String> ids = new ArrayList<>();
      for (JsonElement element : json.getAsJsonArray("decls")) {
        ids.add(element.getAsString());
      }
      return ids;
    }

    private boolean unresolved() {
      if (!json.has("refs")) {
        return "type_identifier".equals(k());
      }
      for (JsonElement element : json.getAsJsonArray("refs")) {
        if ("bound".equals(element.getAsJsonObject().get("outcome").getAsString())) {
          return false;
        }
      }
      return true;
    }

    @Nullable
    private String valueTarget() {
      return target(true);
    }

    @Nullable
    private String typeTarget() {
      return target(false);
    }

    @Nullable
    private String target(boolean value) {
      if (!json.has("refs")) {
        return null;
      }
      String fallback = null;
      for (JsonElement element : json.getAsJsonArray("refs")) {
        JsonObject ref = element.getAsJsonObject();
        if (!ref.has("target") || ref.get("target").isJsonNull() || !"bound".equals(ref.get("outcome").getAsString())) {
          continue;
        }
        String access = ref.has("access") ? ref.get("access").getAsString() : "";
        boolean isValue = "r".equals(access) || "w".equals(access) || "rw".equals(access);
        if (isValue == value) {
          return ref.get("target").getAsString();
        }
        fallback = ref.get("target").getAsString();
      }
      return value ? null : fallback;
    }
  }
}
