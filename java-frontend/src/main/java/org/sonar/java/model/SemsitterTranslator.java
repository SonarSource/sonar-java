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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
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
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
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

  private SemsitterTranslator(JSema sema) {
    this.sema = sema;
  }

  static JavaTree.CompilationUnitTreeImpl translate(JSema sema, JsonObject document) {
    if (document.has("hasError") && document.get("hasError").getAsBoolean()) {
      throw new RecognitionException(1, "SemSitter: syntax error");
    }
    SemsitterTranslator translator = new SemsitterTranslator(sema);
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
    if (!leaves.isEmpty()) {
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
    if (isComment(node) || node.children().isEmpty() || isLiteral(node.k())) {
      if (!node.text().isEmpty() || isComment(node)) {
        leaves.add(node);
      }
      return;
    }
    for (Node child : node.children()) {
      collectLeaves(child, leaves);
    }
  }

  private JavaTree.CompilationUnitTreeImpl compilationUnit(Node program) {
    JavaTree.PackageDeclarationTreeImpl packageDeclaration = null;
    List<ImportClauseTree> imports = new ArrayList<>();
    List<Tree> types = new ArrayList<>();
    for (Node child : program.children()) {
      switch (child.k()) {
        case "package_declaration" -> packageDeclaration = packageDeclaration(child);
        case "import_declaration" -> imports.add(importDeclaration(child));
        case ";" -> imports.add(new EmptyStatementTreeImpl(token(child)));
        default -> addMember(types, child);
      }
    }
    if (types.isEmpty()) {
      List<Tree> members = new ArrayList<>();
      for (Node child : program.children()) {
        if (!"package_declaration".equals(child.k()) && !"import_declaration".equals(child.k())) {
          addMember(members, child);
        }
      }
      if (!members.isEmpty()) {
        types.add(new ClassTreeImpl(Tree.Kind.IMPLICIT_CLASS, null, members, null));
      }
    }
    return new JavaTree.CompilationUnitTreeImpl(packageDeclaration, imports, types, null, eof);
  }

  private JavaTree.PackageDeclarationTreeImpl packageDeclaration(Node node) {
    return new JavaTree.PackageDeclarationTreeImpl(
      annotations(node),
      token(required(node, "package")),
      expression(requiredField(node, "name", firstNamed(node))),
      token(required(node, ";")));
  }

  private JavaTree.ImportTreeImpl importDeclaration(Node node) {
    InternalSyntaxToken staticToken = optionalToken(node, "static");
    ExpressionTree name = expression(requiredField(node, "name", firstNamed(node)));
    if (keyword(node, "*") != null || keyword(node, "asterisk") != null) {
      Node star = keyword(node, "*");
      if (star == null) {
        star = keyword(node, "asterisk");
      }
      name = new MemberSelectExpressionTreeImpl(name, token(required(node, ".")), new IdentifierTreeImpl(token(star)));
    }
    return new JavaTree.ImportTreeImpl(token(required(node, "import")), staticToken, optionalToken(node, "module"), name, token(required(node, ";")));
  }

  private void addMember(List<Tree> members, Node node) {
    switch (node.k()) {
      case "field_declaration", "constant_declaration" -> members.addAll(variables(node));
      case "method_declaration", "constructor_declaration", "compact_constructor_declaration", "annotation_type_element_declaration" -> members.add(method(node));
      case "class_declaration", "interface_declaration", "enum_declaration", "record_declaration", "annotation_type_declaration" -> members.add(typeDeclaration(node));
      case "static_initializer" -> members.add(staticInitializer(node));
      case "block" -> members.add(initializer(node));
      case "enum_constant" -> members.add(enumConstant(node));
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
    return tree;
  }

  private List<Tree> membersOf(Node body) {
    List<Tree> members = new ArrayList<>();
    for (Node child : body.children()) {
      addMember(members, child);
    }
    return members;
  }

  private EnumConstantTreeImpl enumConstant(Node node) {
    IdentifierTreeImpl name = identifier(requiredField(node, "name", firstOf(node, "identifier")));
    Node arguments = field(node, "arguments");
    ArgumentListTreeImpl args = arguments == null ? ArgumentListTreeImpl.emptyList() : arguments(arguments);
    Node body = field(node, "body");
    ClassTreeImpl classBody = null;
    if (body != null) {
      classBody = new ClassTreeImpl(Tree.Kind.CLASS, token(required(body, "{")), membersOf(body), token(required(body, "}")));
      bindType(classBody, body);
    }
    InternalSyntaxToken separator = optionalToken(node, ",");
    if (separator == null) {
      separator = optionalToken(node, ";");
    }
    String constructor = nameNode(node) == null ? null : nameNode(node).valueTarget();
    if (constructor != null) {
      name.symbol = sema.resolve(constructor);
      sema.use(constructor, name);
    }
    EnumConstantTreeImpl tree = new EnumConstantTreeImpl(modifiers(node), name, new NewClassTreeImpl(name, args, classBody), separator);
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
    TypeTree returnType = typeNode == null || "constructor_declaration".equals(node.k()) ? null : type(typeNode);
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
    TypeTree type = typeNode == null ? new VarTypeTreeImpl(synthetic("var")) : type(typeNode);
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
    statements.add(statement(node));
  }

  private StatementTree statement(Node node) {
    return switch (node.k()) {
      case "block" -> block(node);
      case "expression_statement" -> expressionStatement(node);
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
      default -> new EmptyStatementTreeImpl(optionalToken(node, ";") == null ? synthetic(";") : token(required(node, ";")));
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
      token(required(node, ";")));
  }

  private ForStatementTreeImpl forStatement(Node node) {
    StatementListTreeImpl init = StatementListTreeImpl.emptyList();
    StatementListTreeImpl update = StatementListTreeImpl.emptyList();
    InternalSyntaxToken firstSemicolon = null;
    InternalSyntaxToken secondSemicolon = null;
    ExpressionTree condition = null;
    int semicolons = 0;
    for (Node child : node.children()) {
      if ("for".equals(child.text()) || "(".equals(child.text()) || ")".equals(child.text())) {
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
    TypeTree type = type(requiredField(node, "type", firstType(node)));
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
    LabeledStatementTreeImpl tree = new LabeledStatementTreeImpl(name, token(required(node, ":")), statement(lastNamed(node)));
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
        if ("resource".equals(child.k()) || "local_variable_declaration".equals(child.k())) {
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
    StatementListTreeImpl statements = StatementListTreeImpl.emptyList();
    for (Node child : body.children()) {
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
          labels.add(caseLabel(child));
        } else {
          groups.add(switchArm(child, expression));
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

  private CaseGroupTreeImpl switchArm(Node node, boolean expression) {
    List<CaseLabelTreeImpl> labels = new ArrayList<>();
    StatementListTreeImpl body = StatementListTreeImpl.emptyList();
    Node label = childKind(node, "switch_label");
    if (label != null) {
      labels.add(caseLabel(label));
    }
    for (Node child : node.children()) {
      if (same(child, label) || "->".equals(child.text()) || ":".equals(child.text())) {
        continue;
      }
      if (expression && !isStatementKind(child.k())) {
        ExpressionTree value = expression(child);
        body.add(new YieldStatementTreeImpl(null, value, optionalToken(node, ";")));
      } else {
        addStatement(body, child);
      }
    }
    return new CaseGroupTreeImpl(labels, body);
  }

  private CaseLabelTreeImpl caseLabel(Node node) {
    InternalSyntaxToken keyword = keyword(node, "case") != null ? token(keyword(node, "case")) : token(required(node, "default"));
    List<ExpressionTree> expressions = new ArrayList<>();
    InternalSyntaxToken end = null;
    for (Node child : node.children()) {
      if ("case".equals(child.text()) || "default".equals(child.text())) {
        continue;
      }
      if (":".equals(child.text()) || "->".equals(child.text())) {
        end = token(child);
      } else if (",".equals(child.text())) {
        continue;
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
      case "string_literal", "string_fragment" -> literal(Tree.Kind.STRING_LITERAL, token(node), sema.getClassType("java.lang.String"));
      case "character_literal" -> literal(Tree.Kind.CHAR_LITERAL, token(node), sema.getClassType("char"));
      case "text_block" -> literal(Tree.Kind.TEXT_BLOCK, token(node), sema.getClassType("java.lang.String"));
      case "scoped_identifier", "field_access" -> memberSelect(node);
      case "method_invocation" -> invocation(node);
      case "object_creation_expression" -> creation(node);
      case "array_creation_expression" -> arrayCreation(node);
      case "array_initializer" -> arrayInitializer(node);
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
      case "generic_type", "array_type", "integral_type", "floating_point_type", "boolean_type", "void_type", "wildcard" -> typeExpression(node);
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
    return identifier;
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
    ExpressionTree qualifier = object == null ? primary(firstNamed(node)) : expression(object);
    IdentifierTreeImpl identifier = identifier(name == null ? lastNamed(node) : name);
    bindUse(identifier, name == null ? node : name);
    MemberSelectExpressionTreeImpl tree = new MemberSelectExpressionTreeImpl(qualifier, token(required(node, ".")), identifier);
    ascribe(tree, identifier.symbolType());
    return tree;
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
      qualifier = expression(object);
      select = new MemberSelectExpressionTreeImpl(qualifier, token(required(node, ".")), identifier);
      ascribe((AbstractTypedTree) select, identifier.symbolType());
    }
    Node typeArguments = field(node, "type_arguments");
    MethodInvocationTreeImpl tree = new MethodInvocationTreeImpl(select, typeArguments == null ? null : typeArguments(typeArguments), arguments(requiredField(node, "arguments", childKind(node, "argument_list"))));
    if (identifier.symbol instanceof JMethodSymbol method) {
      Type receiver = qualifier == null ? currentType() : qualifier.symbolType();
      Type returned = sema.invocationReturn(method, receiver);
      JMethodSymbol view = method.withReturnType(returned);
      tree.methodSymbol = view;
      identifier.symbol = view;
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
    TypeTree type = type(typeNode == null ? firstType(node) : typeNode);
    Node arguments = field(node, "arguments");
    ArgumentListTreeImpl args = arguments == null ? ArgumentListTreeImpl.emptyList().complete(synthetic("("), synthetic(")")) : arguments(arguments);
    Node body = field(node, "body");
    ClassTreeImpl classBody = null;
    if (body != null) {
      classBody = new ClassTreeImpl(Tree.Kind.CLASS, token(required(body, "{")), membersOf(body), token(required(body, "}")));
      bindType(classBody, body);
    }
    NewClassTreeImpl tree = new NewClassTreeImpl(type, args, classBody).completeWithNewKeyword(token(required(node, "new")));
    Node typeArguments = field(node, "type_arguments");
    if (typeArguments != null) {
      tree.completeWithTypeArguments(typeArguments(typeArguments));
    }
    if (type instanceof IdentifierTreeImpl identifier) {
      String constructor = typeNode == null ? null : typeNode.valueTarget();
      if (constructor != null) {
        identifier.symbol = sema.resolve(constructor);
        sema.use(constructor, identifier);
      }
    }
    ascribe(tree, type.symbolType());
    return tree;
  }

  private NewArrayTreeImpl arrayCreation(Node node) {
    Node dimensions = field(node, "dimensions");
    List<ArrayDimensionTree> dims = dimensions == null ? List.of() : dimensions(dimensions);
    Node initializer = field(node, "value");
    InitializerListTreeImpl initializers = initializer == null ? InitializerListTreeImpl.emptyList() : initializers(initializer);
    NewArrayTreeImpl tree = new NewArrayTreeImpl(dims, initializers)
      .complete(type(requiredField(node, "type", firstType(node))))
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
    InstanceOfTreeImpl tree;
    if (name != null && right != null) {
      VariableTreeImpl variable = new VariableTreeImpl(modifiers(node), type(right), identifier(name));
      bindDeclaration(variable, name, identifier(name));
      tree = new InstanceOfTreeImpl(left, token(required(node, "instanceof")), new TypePatternTreeImpl(variable));
    } else {
      tree = new InstanceOfTreeImpl(left, token(required(node, "instanceof")), type(right == null ? firstType(node) : right));
    }
    ascribe(tree, sema.getClassType("boolean"));
    return tree;
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
        for (Node child : formals.children()) {
          if ("identifier".equals(child.k())) {
            IdentifierTreeImpl name = identifier(child);
            VariableTreeImpl parameter = new VariableTreeImpl(name);
            bindDeclaration(parameter, child, name);
            parameters.add(parameter);
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
    ExpressionTree qualifier = expression(requiredField(node, "type", firstNamed(node)));
    IdentifierTreeImpl name = identifier(requiredField(node, "name", lastOf(node, "identifier")));
    bindUse(name, field(node, "name") == null ? node : field(node, "name"));
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
      case "scoped_identifier", "field_access" -> memberSelect(node);
      case "wildcard" -> wildcard(node);
      default -> {
        ExpressionTree expression = "identifier".equals(node.k()) || "type_identifier".equals(node.k()) ? primary(node) : expression(node);
        yield expression instanceof TypeTree typeTree ? typeTree : new VarTypeTreeImpl(synthetic("var"));
      }
    };
  }

  private JavaTree.PrimitiveTypeTreeImpl primitive(Node node) {
    Node keyword = firstTokenChild(node);
    String text = keyword == null ? node.text() : keyword.text();
    if (text.isEmpty()) {
      text = node.k().replace("_type", "");
    }
    JavaTree.PrimitiveTypeTreeImpl tree = new JavaTree.PrimitiveTypeTreeImpl(keyword == null ? synthetic(text) : token(keyword));
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
    List<Node> children = dimensions.children();
    for (int i = 0; i < children.size(); i++) {
      if ("[".equals(children.get(i).text())) {
        InternalSyntaxToken close = i + 1 < children.size() ? token(children.get(i + 1)) : synthetic("]");
        current = new JavaTree.ArrayTypeTreeImpl(current, List.of(), token(children.get(i)), close);
        ascribe((AbstractTypedTree) current, JType.array(sema, (JType) (element.symbolType() instanceof JType j ? j : JType.unknown(sema))));
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
    IdentifierTreeImpl name = identifier(firstOf(node, "type_identifier", "identifier"));
    bindUse(name, firstOf(node, "type_identifier", "identifier"));
    Node bound = childKind(node, "type_bound");
    TypeParameterTreeImpl tree;
    if (bound == null) {
      tree = new TypeParameterTreeImpl(name);
    } else {
      tree = new TypeParameterTreeImpl(name, token(required(bound, "extends")), separatedTypes(bound));
    }
    if (name.symbol instanceof Symbol.TypeSymbol typeSymbol) {
      tree.typeSymbol = typeSymbol;
    }
    String id = firstOf(node, "type_identifier", "identifier").decl();
    if (id != null) {
      sema.declare(id, tree);
    }
    return tree;
  }

  private QualifiedIdentifierListTreeImpl separatedTypes(Node node) {
    QualifiedIdentifierListTreeImpl list = QualifiedIdentifierListTreeImpl.emptyList();
    for (Node child : node.children()) {
      if (isTypeKind(child.k())) {
        list.add(type(child));
      } else if (",".equals(child.text()) || "&".equals(child.text())) {
        list.separators().add(token(child));
      }
    }
    return list;
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
    ExpressionTree index = null;
    InternalSyntaxToken open = null;
    for (Node child : node.children()) {
      if ("[".equals(child.text())) {
        open = token(child);
        index = null;
      } else if ("]".equals(child.text()) && open != null) {
        dims.add(new ArrayDimensionTreeImpl(open, index, token(child)));
        open = null;
      } else if (open != null) {
        index = expression(child);
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
    if ("marker_annotation".equals(node.k()) || "annotation".equals(node.k())) {
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
    return new AnnotationTreeImpl(token(required(node, "@")), type, arguments);
  }

  private List<AnnotationTree> annotations(Node node) {
    Node modifiers = childKind(node, "modifiers");
    if (modifiers == null) {
      return List.of();
    }
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Node child : modifiers.children()) {
      if ("marker_annotation".equals(child.k()) || "annotation".equals(child.k())) {
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
    identifier.symbol = symbol;
    ascribe(identifier, symbol.type());
    if (tree instanceof ClassTreeImpl classTree && symbol instanceof Symbol.TypeSymbol typeSymbol) {
      classTree.typeSymbol = typeSymbol;
    } else if (tree instanceof MethodTreeImpl method && symbol instanceof Symbol.MethodSymbol methodSymbol) {
      method.methodSymbol = methodSymbol;
    } else if (tree instanceof VariableTreeImpl variable && symbol instanceof Symbol.VariableSymbol variableSymbol) {
      variable.variableSymbol = variableSymbol;
    }
  }

  private void bindType(ClassTreeImpl tree, Node node) {
    String id = node.decl();
    if (id != null && sema.resolve(id) instanceof Symbol.TypeSymbol typeSymbol) {
      tree.typeSymbol = typeSymbol;
      sema.declare(id, tree);
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
    identifier.symbol = symbol;
    ascribe(identifier, symbol.type());
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
      case "type_identifier", "identifier", "scoped_identifier", "generic_type", "array_type", "integral_type", "floating_point_type", "boolean_type", "void_type", "wildcard" -> true;
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
    return new InternalSyntaxToken(node.line(), node.col(), node.text().isEmpty() ? node.k() : node.text(), List.of(), false);
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
