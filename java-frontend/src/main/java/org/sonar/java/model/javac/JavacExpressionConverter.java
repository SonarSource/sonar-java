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

import org.sonar.java.javac.source.tree.AssignmentTree;
import org.sonar.java.javac.source.tree.ArrayAccessTree;
import org.sonar.java.javac.source.tree.ArrayTypeTree;
import org.sonar.java.javac.source.tree.AnnotatedTypeTree;
import org.sonar.java.javac.source.tree.AnnotationTree;
import org.sonar.java.javac.source.tree.BinaryTree;
import org.sonar.java.javac.source.tree.CompoundAssignmentTree;
import org.sonar.java.javac.source.tree.ConditionalExpressionTree;
import org.sonar.java.javac.source.tree.IdentifierTree;
import org.sonar.java.javac.source.tree.InstanceOfTree;
import org.sonar.java.javac.source.tree.BindingPatternTree;
import org.sonar.java.javac.source.tree.DeconstructionPatternTree;
import org.sonar.java.javac.source.tree.ErroneousTree;
import org.sonar.java.javac.source.tree.LiteralTree;
import org.sonar.java.javac.source.tree.LambdaExpressionTree;
import org.sonar.java.javac.source.tree.MemberSelectTree;
import org.sonar.java.javac.source.tree.MethodInvocationTree;
import org.sonar.java.javac.source.tree.MemberReferenceTree;
import org.sonar.java.javac.source.tree.NewClassTree;
import org.sonar.java.javac.source.tree.NewArrayTree;
import org.sonar.java.javac.source.tree.ParenthesizedTree;
import org.sonar.java.javac.source.tree.ParameterizedTypeTree;
import org.sonar.java.javac.source.tree.TypeCastTree;
import org.sonar.java.javac.source.tree.IntersectionTypeTree;
import org.sonar.java.javac.source.tree.UnaryTree;
import org.sonar.java.javac.source.tree.UnionTypeTree;
import org.sonar.java.javac.source.tree.WildcardTree;
import org.sonar.java.javac.source.util.SourcePositions;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.model.ArrayDimensionTreeImpl;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.KeywordThis;
import org.sonar.java.model.KeywordSuper;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InternalPostfixUnaryExpression;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.java.model.pattern.RecordPatternTreeImpl;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.PatternTree;

/** Converts javac expressions to SonarJava syntax trees. */
public final class JavacExpressionConverter {

  private final org.sonar.java.javac.source.tree.CompilationUnitTree unit;
  private final SourcePositions positions;
  private final SourceTokens tokens;
  private final BiConsumer<org.sonar.java.javac.source.tree.Tree, Tree> registration;
  private final Function<org.sonar.java.javac.source.tree.ClassTree, ClassTreeImpl> anonymousClass;
  private final Function<org.sonar.java.javac.source.tree.BlockTree, BlockTreeImpl> block;
  private final Function<org.sonar.java.javac.source.tree.SwitchExpressionTree, ExpressionTree> switchExpression;

  public JavacExpressionConverter(org.sonar.java.javac.source.tree.CompilationUnitTree unit, SourcePositions positions, SourceTokens tokens) {
    this(unit, positions, tokens, (javacTree, sonarTree) -> { });
  }

  public JavacExpressionConverter(org.sonar.java.javac.source.tree.CompilationUnitTree unit, SourcePositions positions, SourceTokens tokens,
    BiConsumer<org.sonar.java.javac.source.tree.Tree, Tree> registration) {
    this(unit, positions, tokens, registration, tree -> { throw unsupported(tree); }, tree -> { throw unsupported(tree); }, tree -> { throw unsupported(tree); });
  }

  public JavacExpressionConverter(org.sonar.java.javac.source.tree.CompilationUnitTree unit, SourcePositions positions, SourceTokens tokens,
    BiConsumer<org.sonar.java.javac.source.tree.Tree, Tree> registration,
    Function<org.sonar.java.javac.source.tree.ClassTree, ClassTreeImpl> anonymousClass,
    Function<org.sonar.java.javac.source.tree.BlockTree, BlockTreeImpl> block,
    Function<org.sonar.java.javac.source.tree.SwitchExpressionTree, ExpressionTree> switchExpression) {
    this.unit = unit;
    this.positions = positions;
    this.tokens = tokens;
    this.registration = registration;
    this.anonymousClass = anonymousClass;
    this.block = block;
    this.switchExpression = switchExpression;
  }

  public ExpressionTree convert(org.sonar.java.javac.source.tree.ExpressionTree expression) {
    ExpressionTree result = convertExpression(expression);
    registration.accept(expression, result);
    return result;
  }

  private ExpressionTree convertExpression(org.sonar.java.javac.source.tree.ExpressionTree expression) {
    if (expression instanceof ErroneousTree error) {
      List<SourceTokens.Token> parts = tokens.tokensBetween(start(expression), end(expression));
      if (parts.size() == 1 && Character.isDigit(parts.get(0).text().charAt(0))) {
        SourceTokens.Token number = parts.get(0);
        Tree.Kind kind = number.text().endsWith("L") || number.text().endsWith("l") ? Tree.Kind.LONG_LITERAL : Tree.Kind.INT_LITERAL;
        return new LiteralTreeImpl(kind, tokens.syntaxToken(number));
      }
      if (!error.getErrorTrees().isEmpty() && error.getErrorTrees().get(0) instanceof NewArrayTree array
        && array.getType() != null && parts.stream().anyMatch(part -> part.text().equals("["))) {
        SourceTokens.Token newKeyword = tokens.firstToken(start(expression), end(expression), "new");
        SourceTokens.Token open = tokens.firstToken(end(array.getType()), end(expression), "[");
        SourceTokens.Token close = tokens.firstToken(open.end(), end(expression), "]");
        return new NewArrayTreeImpl(List.of(new ArrayDimensionTreeImpl(tokens.syntaxToken(open), null, tokens.syntaxToken(close))),
          ArgumentListTreeImpl.emptyList()).completeWithNewKeyword(tokens.syntaxToken(newKeyword)).complete(convertType(array.getType()));
      }
      if (!error.getErrorTrees().isEmpty() && error.getErrorTrees().get(0) instanceof org.sonar.java.javac.source.tree.ExpressionTree recovered) {
        return convert(recovered);
      }
    }
    if (expression instanceof AnnotationTree annotation) {
      return annotation(annotation);
    }
    if (expression instanceof AnnotatedTypeTree || expression instanceof org.sonar.java.javac.source.tree.PrimitiveTypeTree
      || expression instanceof ArrayTypeTree || expression instanceof ParameterizedTypeTree) {
      return (ExpressionTree) convertType(expression);
    }
    if (expression instanceof IdentifierTree identifier) {
      return identifier(tokens.lastToken(start(expression), end(expression), identifier.getName().toString()));
    }
    if (expression instanceof org.sonar.java.javac.source.tree.SwitchExpressionTree selection) {
      return switchExpression.apply(selection);
    }
    if (expression instanceof MemberSelectTree select) {
      ExpressionTree receiver = convert(select.getExpression());
      String name = select.getIdentifier().toString();
      SourceTokens.Token nameToken = tokens.lastToken(end(select.getExpression()), end(expression), name);
      SourceTokens.Token dot = tokens.firstToken(end(select.getExpression()), nameToken.start(), ".");
      IdentifierTreeImpl nameIdentifier = identifier(nameToken);
      registration.accept(select, nameIdentifier);
      return new MemberSelectExpressionTreeImpl(receiver, tokens.syntaxToken(dot), nameIdentifier);
    }
    if (expression instanceof LiteralTree) {
      List<SourceTokens.Token> literalTokens = tokens.tokensBetween(start(expression), end(expression));
      if (literalTokens.size() > 1 && literalTokens.get(0).text().equals("-")
        && (expression.getKind() == org.sonar.java.javac.source.tree.Tree.Kind.INT_LITERAL
          || expression.getKind() == org.sonar.java.javac.source.tree.Tree.Kind.LONG_LITERAL)) {
        SourceTokens.Token number = literalTokens.get(1);
        LiteralTreeImpl operand = new LiteralTreeImpl(literalKind(expression), tokens.syntaxToken(number));
        return new InternalPrefixUnaryExpression(Tree.Kind.UNARY_MINUS, tokens.syntaxToken(literalTokens.get(0)), operand);
      }
      if (expression.getKind() == org.sonar.java.javac.source.tree.Tree.Kind.STRING_LITERAL) {
        List<SourceTokens.Token> quoted = literalTokens.stream().filter(part -> part.text().startsWith("\"")).toList();
        if (quoted.size() > 1) {
          ExpressionTree result = stringLiteral(quoted.get(0));
          for (int i = 1; i < quoted.size(); i++) {
            SourceTokens.Token plus = tokens.firstToken(quoted.get(i - 1).end(), quoted.get(i).start(), "+");
            result = new BinaryExpressionTreeImpl(Tree.Kind.PLUS, result, tokens.syntaxToken(plus), stringLiteral(quoted.get(i)));
          }
          return result;
        }
      }
      org.sonar.java.model.InternalSyntaxToken literalToken = literalTokens.size() == 1 ? tokens.syntaxToken(literalTokens.get(0))
        : new org.sonar.java.model.InternalSyntaxToken(tokens.line(start(expression)), tokens.column(start(expression)),
          literalTokens.stream().map(SourceTokens.Token::text).collect(java.util.stream.Collectors.joining()),
          tokens.syntaxToken(literalTokens.get(0)).trivias(), false);
      Tree.Kind kind = literalKind(expression);
      if (kind == Tree.Kind.STRING_LITERAL && literalToken.text().startsWith("\"\"\"")) {
        kind = Tree.Kind.TEXT_BLOCK;
      }
      return new LiteralTreeImpl(kind, literalToken);
    }
    if (expression instanceof MethodInvocationTree invocation) {
      ExpressionTree methodSelect = convert(invocation.getMethodSelect());
      int open = end(invocation.getMethodSelect());
      int close = end(expression);
      SourceTokens.Token openParen = tokens.firstToken(open, close, "(");
      SourceTokens.Token closeParen = tokens.lastToken(openParen.end(), close, ")");
      return new MethodInvocationTreeImpl(methodSelect, typeArguments(invocation.getTypeArguments()), arguments(invocation.getArguments(), openParen, closeParen));
    }
    if (expression instanceof MemberReferenceTree reference) {
      int qualifierEnd = end(reference.getQualifierExpression());
      SourceTokens.Token separator = tokens.firstToken(qualifierEnd, end(expression), "::");
      String name = reference.getMode() == MemberReferenceTree.ReferenceMode.NEW ? "new" : reference.getName().toString();
      SourceTokens.Token nameToken = tokens.lastToken(separator.end(), end(expression), name);
      MethodReferenceTreeImpl result = new MethodReferenceTreeImpl(convert(reference.getQualifierExpression()), tokens.syntaxToken(separator));
      IdentifierTreeImpl methodName = new IdentifierTreeImpl(tokens.syntaxToken(nameToken));
      result.complete(typeArguments(reference.getTypeArguments()), methodName);
      registration.accept(reference, methodName);
      return result;
    }
    if (expression instanceof LambdaExpressionTree lambda) {
      List<org.sonar.plugins.java.api.tree.VariableTree> parameters = new java.util.ArrayList<>();
      for (org.sonar.java.javac.source.tree.VariableTree parameter : lambda.getParameters()) {
        String name = parameter.getName().toString();
        boolean unnamed = name.isEmpty();
        if (unnamed) {
          name = "_";
        }
        SourceTokens.Token nameToken = tokens.lastToken(start(parameter), end(parameter), name);
        IdentifierTreeImpl nameTree = new IdentifierTreeImpl(tokens.syntaxToken(nameToken), unnamed);
        VariableTreeImpl variable = parameter.getType() == null ? new VariableTreeImpl(nameTree)
          : new VariableTreeImpl(ModifiersTreeImpl.emptyModifiers(), convertType(parameter.getType()), nameTree);
        parameters.add(variable);
        registration.accept(parameter, variable);
      }
      SourceTokens.Token arrow = tokens.firstToken(start(expression), start(lambda.getBody()), "->");
      SourceTokens.Token first = tokens.tokenAtOrAfter(start(expression));
      org.sonar.java.model.InternalSyntaxToken open = first.text().equals("(") ? tokens.syntaxToken(first) : null;
      org.sonar.java.model.InternalSyntaxToken close = open == null ? null
        : tokens.syntaxToken(tokens.lastToken(start(expression), arrow.start(), ")"));
      Tree body = lambda.getBody() instanceof org.sonar.java.javac.source.tree.ExpressionTree bodyExpression
        ? convert(bodyExpression) : block.apply((org.sonar.java.javac.source.tree.BlockTree) lambda.getBody());
      return new LambdaExpressionTreeImpl(open, parameters, close, tokens.syntaxToken(arrow), body);
    }
    if (expression instanceof NewClassTree creation) {
      org.sonar.java.javac.source.tree.Tree identifier = creation.getIdentifier();
      SourceTokens.Token newToken = tokens.firstToken(start(expression), start(identifier), "new");
      SourceTokens.Token openParen = tokens.firstToken(end(identifier), end(expression), "(");
      int argumentsEnd = creation.getClassBody() == null ? end(expression) : start(creation.getClassBody());
      SourceTokens.Token closeParen = tokens.lastToken(openParen.end(), argumentsEnd, ")");
      ClassTreeImpl classBody = creation.getClassBody() == null ? null : anonymousClass.apply(creation.getClassBody());
      NewClassTreeImpl result = new NewClassTreeImpl(convertType(identifier), arguments(creation.getArguments(), openParen, closeParen), classBody)
        .completeWithNewKeyword(tokens.syntaxToken(newToken)).completeWithTypeArguments(typeArguments(creation.getTypeArguments()));
      if (creation.getEnclosingExpression() != null) {
        result.completeWithEnclosingExpression(convert(creation.getEnclosingExpression()));
        result.completeWithDotToken(tokens.syntaxToken(tokens.firstToken(end(creation.getEnclosingExpression()), newToken.start(), ".")));
      }
      return result;
    }
    if (expression instanceof NewArrayTree creation) {
      List<org.sonar.plugins.java.api.tree.ArrayDimensionTree> dimensions = new java.util.ArrayList<>();
      org.sonar.java.javac.source.tree.Tree baseType = creation.getType();
      while (baseType instanceof ArrayTypeTree
        || baseType instanceof AnnotatedTypeTree annotated && annotated.getUnderlyingType() instanceof ArrayTypeTree) {
        baseType = baseType instanceof ArrayTypeTree arrayType ? arrayType.getType()
          : ((AnnotatedTypeTree) baseType).getUnderlyingType();
      }
      List<AnnotationTree> dimensionAnnotations = new java.util.ArrayList<>();
      org.sonar.java.javac.source.tree.Tree dimensionType = creation.getType();
      while (dimensionType instanceof ArrayTypeTree || dimensionType instanceof AnnotatedTypeTree) {
        if (dimensionType instanceof AnnotatedTypeTree annotated) {
          dimensionAnnotations.addAll(annotated.getAnnotations());
          dimensionType = annotated.getUnderlyingType();
        } else {
          dimensionType = ((ArrayTypeTree) dimensionType).getType();
        }
      }
      creation.getDimAnnotations().forEach(dimensionAnnotations::addAll);
      int cursor = baseType == null ? start(expression) : end(baseType);
      int dimensionIndex = 0;
      int bracketEnd = creation.getInitializers() == null ? end(expression)
        : tokens.firstToken(cursor, end(expression), "{").start();
      while (tokens.tokensBetween(cursor, bracketEnd).stream().anyMatch(t -> t.text().equals("["))) {
        SourceTokens.Token open = tokens.firstToken(cursor, bracketEnd, "[");
        SourceTokens.Token close = tokens.firstToken(open.end(), bracketEnd, "]");
        org.sonar.java.javac.source.tree.ExpressionTree dimension = dimensionIndex < creation.getDimensions().size()
          ? creation.getDimensions().get(dimensionIndex) : null;
        ExpressionTree converted = dimension != null && start(dimension) > open.start() && end(dimension) <= close.start()
          ? convert(dimension) : null;
        if (converted != null) {
          dimensionIndex++;
        }
        ArrayDimensionTreeImpl convertedDimension = new ArrayDimensionTreeImpl(tokens.syntaxToken(open), converted, tokens.syntaxToken(close));
        int dimensionStart = cursor;
        convertedDimension.completeAnnotations(dimensionAnnotations.stream()
          .filter(a -> start(a) >= dimensionStart && end(a) <= open.start())
          .distinct()
          .map(a -> (org.sonar.plugins.java.api.tree.AnnotationTree) annotation(a)).toList());
        dimensions.add(convertedDimension);
        cursor = close.end();
      }
      ArgumentListTreeImpl initializers = ArgumentListTreeImpl.emptyList();
      List<? extends org.sonar.java.javac.source.tree.ExpressionTree> values = creation.getInitializers();
      if (values != null) {
        for (int i = 0; i < values.size(); i++) {
          if (i > 0) {
            initializers.separators().add(tokens.syntaxToken(tokens.firstToken(end(values.get(i - 1)), start(values.get(i)), ",")));
          }
          initializers.add(convert(values.get(i)));
        }
      }
      NewArrayTreeImpl result = new NewArrayTreeImpl(dimensions, initializers);
      if (creation.getType() != null) {
        SourceTokens.Token newKeyword = tokens.firstToken(start(expression), start(creation.getType()), "new");
        result.completeWithNewKeyword(tokens.syntaxToken(newKeyword)).complete(convertType(baseType));
      }
      if (values != null) {
        SourceTokens.Token open = tokens.firstToken(cursor, end(expression), "{");
        SourceTokens.Token close = tokens.lastToken(open.end(), end(expression), "}");
        SourceTokens.Token beforeClose = tokens.tokenAtOrBefore(close.start() - 1);
        if (!values.isEmpty() && beforeClose.text().equals(",")) {
          initializers.separators().add(tokens.syntaxToken(beforeClose));
        }
        result.completeWithCurlyBraces(tokens.syntaxToken(open), tokens.syntaxToken(close));
      }
      return result;
    }
    if (expression instanceof BinaryTree binary) {
      Operator operator = operator(expression.getKind());
      if (operator.kind() == Tree.Kind.PLUS
        && binary.getRightOperand().getKind() == org.sonar.java.javac.source.tree.Tree.Kind.STRING_LITERAL) {
        List<SourceTokens.Token> quoted = tokens.tokensBetween(start(binary.getRightOperand()), end(binary.getRightOperand())).stream()
          .filter(part -> part.text().startsWith("\"")).toList();
        if (quoted.size() > 1) {
          ExpressionTree result = convert(binary.getLeftOperand());
          int cursor = end(binary.getLeftOperand());
          for (SourceTokens.Token part : quoted) {
            SourceTokens.Token plus = tokens.firstToken(cursor, part.start(), "+");
            result = new BinaryExpressionTreeImpl(Tree.Kind.PLUS, result, tokens.syntaxToken(plus), stringLiteral(part));
            cursor = part.end();
          }
          registration.accept(binary.getRightOperand(), result);
          return result;
        }
      }
      return new BinaryExpressionTreeImpl(operator.kind(), convert(binary.getLeftOperand()),
        tokens.syntaxToken(tokens.firstToken(end(binary.getLeftOperand()), start(binary.getRightOperand()), operator.token())),
        convert(binary.getRightOperand()));
    }
    if (expression instanceof UnaryTree unary) {
      Operator operator = operator(expression.getKind());
      ExpressionTree operand = convert(unary.getExpression());
      if (expression.getKind() == org.sonar.java.javac.source.tree.Tree.Kind.POSTFIX_INCREMENT
        || expression.getKind() == org.sonar.java.javac.source.tree.Tree.Kind.POSTFIX_DECREMENT) {
        return new InternalPostfixUnaryExpression(operator.kind(), operand,
          tokens.syntaxToken(tokens.firstToken(end(unary.getExpression()), end(expression), operator.token())));
      }
      return new InternalPrefixUnaryExpression(operator.kind(),
        tokens.syntaxToken(tokens.firstToken(start(expression), start(unary.getExpression()), operator.token())), operand);
    }
    if (expression instanceof AssignmentTree assignment) {
      return assignment(expression, assignment.getVariable(), assignment.getExpression());
    }
    if (expression instanceof CompoundAssignmentTree assignment) {
      return assignment(expression, assignment.getVariable(), assignment.getExpression());
    }
    if (expression instanceof ParenthesizedTree parenthesized) {
      return new ParenthesizedTreeImpl(tokens.syntaxToken(tokens.firstToken(start(expression), start(parenthesized.getExpression()), "(")),
        convert(parenthesized.getExpression()),
        tokens.syntaxToken(tokens.lastToken(end(parenthesized.getExpression()), end(expression), ")")));
    }
    if (expression instanceof ConditionalExpressionTree conditional) {
      return new ConditionalExpressionTreeImpl(convert(conditional.getCondition()),
        tokens.syntaxToken(tokens.firstToken(end(conditional.getCondition()), start(conditional.getTrueExpression()), "?")),
        convert(conditional.getTrueExpression()),
        tokens.syntaxToken(tokens.firstToken(end(conditional.getTrueExpression()), start(conditional.getFalseExpression()), ":")),
        convert(conditional.getFalseExpression()));
    }
    if (expression instanceof ArrayAccessTree access) {
      SourceTokens.Token open = tokens.firstToken(end(access.getExpression()), start(access.getIndex()), "[");
      SourceTokens.Token close = tokens.firstToken(end(access.getIndex()), end(expression), "]");
      return new ArrayAccessExpressionTreeImpl(convert(access.getExpression()),
        new ArrayDimensionTreeImpl(tokens.syntaxToken(open), convert(access.getIndex()), tokens.syntaxToken(close)));
    }
    if (expression instanceof TypeCastTree cast) {
      SourceTokens.Token open = tokens.lastToken(start(cast), start(cast.getType()) + 1, "(");
      SourceTokens.Token close = tokens.firstToken(end(cast.getType()), start(cast.getExpression()), ")");
      if (cast.getType() instanceof IntersectionTypeTree intersection) {
        List<? extends org.sonar.java.javac.source.tree.Tree> bounds = intersection.getBounds();
        QualifiedIdentifierListTreeImpl convertedBounds = QualifiedIdentifierListTreeImpl.emptyList();
        for (int i = 1; i < bounds.size(); i++) {
          if (i > 1) {
            convertedBounds.separators().add(tokens.syntaxToken(tokens.firstToken(end(bounds.get(i - 1)), start(bounds.get(i)), "&")));
          }
          convertedBounds.add(convertType(bounds.get(i)));
        }
        SourceTokens.Token and = tokens.firstToken(end(bounds.get(0)), start(bounds.get(1)), "&");
        return new TypeCastExpressionTreeImpl(tokens.syntaxToken(open), convertType(bounds.get(0)), tokens.syntaxToken(and), convertedBounds,
          tokens.syntaxToken(close), convert(cast.getExpression()));
      }
      return new TypeCastExpressionTreeImpl(tokens.syntaxToken(open), convertType(cast.getType()), tokens.syntaxToken(close),
        convert(cast.getExpression()));
    }
    if (expression instanceof InstanceOfTree instanceOf) {
      org.sonar.java.javac.source.tree.Tree target = instanceOf.getPattern() == null ? instanceOf.getType() : instanceOf.getPattern();
      SourceTokens.Token keyword = tokens.firstToken(end(instanceOf.getExpression()), start(target), "instanceof");
      if (instanceOf.getPattern() != null) {
        return new InstanceOfTreeImpl(convert(instanceOf.getExpression()), tokens.syntaxToken(keyword), convertPattern(instanceOf.getPattern()));
      }
      return new InstanceOfTreeImpl(convert(instanceOf.getExpression()), tokens.syntaxToken(keyword), convertType(instanceOf.getType()));
    }
    throw unsupported(expression);
  }

  private LiteralTreeImpl stringLiteral(SourceTokens.Token token) {
    return new LiteralTreeImpl(token.text().startsWith("\"\"\"") ? Tree.Kind.TEXT_BLOCK : Tree.Kind.STRING_LITERAL,
      tokens.syntaxToken(token));
  }

  public PatternTree convertPattern(org.sonar.java.javac.source.tree.PatternTree pattern) {
    PatternTree result;
    if (pattern.getKind().name().equals("ANY_PATTERN")) {
      SourceTokens.Token underscore = tokens.tokenAtOrBefore(start(pattern) - 1);
      if (!underscore.text().equals("_")) {
        underscore = tokens.firstToken(start(pattern), end(pattern), "_");
      }
      IdentifierTreeImpl name = new IdentifierTreeImpl(tokens.syntaxToken(underscore), true);
      result = new TypePatternTreeImpl(new VariableTreeImpl(name));
    } else if (pattern instanceof BindingPatternTree binding) {
      org.sonar.java.javac.source.tree.VariableTree variable = binding.getVariable();
      String name = variable.getName().toString();
      boolean unnamed = name.isEmpty();
      if (unnamed) {
        name = "_";
      }
      SourceTokens.Token nameToken = tokens.lastToken(start(variable), end(variable), name);
      TypeTree type = variable.getType() == null
        ? new VarTypeTreeImpl(tokens.syntaxToken(tokens.firstToken(start(variable), nameToken.start(), "var")))
        : convertType(variable.getType());
      VariableTreeImpl converted = new VariableTreeImpl(ModifiersTreeImpl.emptyModifiers(), type,
        new IdentifierTreeImpl(tokens.syntaxToken(nameToken), unnamed));
      registration.accept(variable, converted);
      result = new TypePatternTreeImpl(converted);
    } else if (pattern instanceof DeconstructionPatternTree record) {
      SourceTokens.Token open = tokens.firstToken(end(record.getDeconstructor()), end(pattern), "(");
      SourceTokens.Token close = tokens.lastToken(open.end(), end(pattern), ")");
      List<PatternTree> nested = new java.util.ArrayList<>();
      for (org.sonar.java.javac.source.tree.PatternTree child : record.getNestedPatterns()) {
        nested.add(convertPattern(child));
      }
      result = new RecordPatternTreeImpl(convertType(record.getDeconstructor()), tokens.syntaxToken(open), nested, tokens.syntaxToken(close));
    } else {
      throw unsupported(pattern);
    }
    registration.accept(pattern, result);
    return result;
  }

  public TypeTree convertType(org.sonar.java.javac.source.tree.Tree tree) {
    TypeTree result;
    if (tree instanceof AnnotatedTypeTree annotated) {
      result = convertType(annotated.getUnderlyingType());
      List<org.sonar.plugins.java.api.tree.AnnotationTree> annotations = new java.util.ArrayList<>();
      for (AnnotationTree annotation : annotated.getAnnotations()) {
        annotations.add(annotation(annotation));
      }
      TypeTree target = result;
      while (target instanceof JavaTree.ParameterizedTypeTreeImpl parameterized) {
        target = parameterized.type();
      }
      if (target instanceof MemberSelectExpressionTreeImpl select
        && tokens.tokenAtOrBefore(start(annotated.getAnnotations().get(0)) - 1).text().equals(".")) {
        target = select.identifier();
      }
      if (target instanceof JavaTree.AnnotatedTypeTree annotatedType) {
        annotatedType.complete(annotations);
      } else {
        throw unsupported(tree);
      }
    } else if (tree instanceof org.sonar.java.javac.source.tree.VarTypeTree) {
      result = new org.sonar.java.model.expression.VarTypeTreeImpl(
        tokens.syntaxToken(tokens.firstToken(start(tree), end(tree), "var")));
    } else if (tree instanceof org.sonar.java.javac.source.tree.PrimitiveTypeTree primitive) {
      result = new JavaTree.PrimitiveTypeTreeImpl(syntaxToken(start(tree),
        primitive.getPrimitiveTypeKind().name().toLowerCase(java.util.Locale.ROOT)));
    } else if (tree instanceof ParameterizedTypeTree parameterized) {
      SourceTokens.Token open = tokens.firstToken(end(parameterized.getType()), end(tree), "<");
      TypeArgumentListTreeImpl arguments = new TypeArgumentListTreeImpl(tokens.syntaxToken(open), closingAngle(end(tree) - 1));
      for (int i = 0; i < parameterized.getTypeArguments().size(); i++) {
        org.sonar.java.javac.source.tree.Tree argument = parameterized.getTypeArguments().get(i);
        if (i > 0) {
          arguments.separators().add(tokens.syntaxToken(tokens.firstToken(end(parameterized.getTypeArguments().get(i - 1)), start(argument), ",")));
        }
        arguments.add(convertType(argument));
      }
      result = new JavaTree.ParameterizedTypeTreeImpl(convertType(parameterized.getType()), arguments);
    } else if (tree instanceof ArrayTypeTree array) {
      org.sonar.java.javac.source.tree.Tree element = array.getType();
      int dimension = 0;
      int sharedEndDimensions = 0;
      while (element instanceof ArrayTypeTree || element instanceof AnnotatedTypeTree) {
        if (element instanceof ArrayTypeTree nested) {
          if (end(nested) == end(tree)) {
            sharedEndDimensions++;
          }
          element = nested.getType();
          dimension++;
        } else {
          element = ((AnnotatedTypeTree) element).getUnderlyingType();
        }
      }
      List<SourceTokens.Token> span = tokens.tokensBetween(end(element), end(tree));
      List<SourceTokens.Token> dimensions = span.stream()
        .filter(part -> part.text().equals("[") || part.text().equals("...")).toList();
      int lastComma = -1;
      for (int i = 0; i < span.size(); i++) {
        if (span.get(i).text().equals(",")) {
          lastComma = i;
        }
      }
      int designatorIndex = dimension;
      if (lastComma >= 0) {
        int ownDimensions = (int) span.subList(lastComma + 1, span.size()).stream()
          .filter(part -> part.text().equals("[") || part.text().equals("...")).count();
        if (ownDimensions > 0) {
          designatorIndex = dimensions.size() - ownDimensions + sharedEndDimensions;
        }
      }
      SourceTokens.Token designator = dimensions.get(designatorIndex);
      TypeTree nestedType = convertType(array.getType());
      if (designator.text().equals("...")) {
        result = new JavaTree.ArrayTypeTreeImpl(nestedType, List.of(), tokens.syntaxToken(designator));
      } else {
        SourceTokens.Token close = tokens.firstToken(designator.end(), end(tree), "]");
        result = new JavaTree.ArrayTypeTreeImpl(nestedType, List.of(), tokens.syntaxToken(designator), tokens.syntaxToken(close));
      }
      if (array.getType() instanceof AnnotatedTypeTree annotated && nestedType instanceof JavaTree.ArrayTypeTreeImpl nestedArray
        && tokens.tokensBetween(end(element), start(annotated.getAnnotations().get(0))).stream().anyMatch(part -> part.text().equals("["))) {
        ((JavaTree.ArrayTypeTreeImpl) result).complete(nestedArray.annotations());
        nestedArray.complete(List.of());
      }
    } else if (tree instanceof WildcardTree wildcard) {
      SourceTokens.Token query = tokens.firstToken(start(tree), end(tree), "?");
      if (wildcard.getBound() == null) {
        result = new JavaTree.WildcardTreeImpl(tokens.syntaxToken(query));
      } else {
        String keyword = tree.getKind() == org.sonar.java.javac.source.tree.Tree.Kind.EXTENDS_WILDCARD ? "extends" : "super";
        SourceTokens.Token bound = tokens.firstToken(query.end(), start(wildcard.getBound()), keyword);
        result = new JavaTree.WildcardTreeImpl(keyword.equals("extends") ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD,
          tokens.syntaxToken(bound), convertType(wildcard.getBound())).complete(tokens.syntaxToken(query));
      }
    } else if (tree instanceof UnionTypeTree union) {
      QualifiedIdentifierListTreeImpl alternatives = QualifiedIdentifierListTreeImpl.emptyList();
      for (int i = 0; i < union.getTypeAlternatives().size(); i++) {
        org.sonar.java.javac.source.tree.Tree alternative = union.getTypeAlternatives().get(i);
        if (i > 0) {
          alternatives.separators().add(tokens.syntaxToken(tokens.firstToken(end(union.getTypeAlternatives().get(i - 1)), start(alternative), "|")));
        }
        alternatives.add(convertType(alternative));
      }
      result = new JavaTree.UnionTypeTreeImpl(alternatives);
    } else if (tree instanceof org.sonar.java.javac.source.tree.ExpressionTree expression) {
      result = (TypeTree) convert(expression);
    } else {
      throw unsupported(tree);
    }
    registration.accept(tree, result);
    return result;
  }

  private AnnotationTreeImpl annotation(AnnotationTree annotation) {
    int start = start(annotation);
    int end = end(annotation);
    TypeTree annotationType = convertType(annotation.getAnnotationType());
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    List<? extends org.sonar.java.javac.source.tree.ExpressionTree> values = annotation.getArguments();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        arguments.separators().add(tokens.syntaxToken(tokens.firstToken(end(values.get(i - 1)), start(values.get(i)), ",")));
      }
      arguments.add(convert(values.get(i)));
    }
    List<SourceTokens.Token> trailing = tokens.tokensBetween(end(annotation.getAnnotationType()), end);
    if (!trailing.isEmpty() && trailing.get(0).text().equals("(")) {
      arguments.complete(tokens.syntaxToken(trailing.get(0)), tokens.syntaxToken(tokens.lastToken(start, end, ")")));
    }
    AnnotationTreeImpl result = new AnnotationTreeImpl(tokens.syntaxToken(tokens.firstToken(start, end, "@")), annotationType, arguments);
    registration.accept(annotation, result);
    return result;
  }

  private ArgumentListTreeImpl arguments(List<? extends org.sonar.java.javac.source.tree.ExpressionTree> javacArguments,
    SourceTokens.Token openParen, SourceTokens.Token closeParen) {
    ArgumentListTreeImpl arguments = ArgumentListTreeImpl.emptyList();
    for (int i = 0; i < javacArguments.size(); i++) {
      if (i > 0) {
        arguments.separators().add(tokens.syntaxToken(tokens.firstToken(end(javacArguments.get(i - 1)), start(javacArguments.get(i)), ",")));
      }
      arguments.add(convert(javacArguments.get(i)));
    }
    return arguments.complete(tokens.syntaxToken(openParen), tokens.syntaxToken(closeParen));
  }

  private TypeArgumentListTreeImpl typeArguments(List<? extends org.sonar.java.javac.source.tree.Tree> javacArguments) {
    if (javacArguments == null || javacArguments.isEmpty()) {
      return null;
    }
    SourceTokens.Token open = tokens.lastToken(0, start(javacArguments.get(0)), "<");
    TypeArgumentListTreeImpl result = new TypeArgumentListTreeImpl(tokens.syntaxToken(open), closingAngleAfter(end(javacArguments.get(javacArguments.size() - 1))));
    for (int i = 0; i < javacArguments.size(); i++) {
      if (i > 0) {
        result.separators().add(tokens.syntaxToken(tokens.firstToken(end(javacArguments.get(i - 1)), start(javacArguments.get(i)), ",")));
      }
      result.add(convertType(javacArguments.get(i)));
    }
    return result;
  }

  private org.sonar.java.model.InternalSyntaxToken closingAngleAfter(int offset) {
    SourceTokens.Token token = tokens.tokenAtOrAfter(offset);
    int index = token.text().indexOf('>', Math.max(0, offset - token.start()));
    if (index < 0) {
      throw new IllegalArgumentException("Expected closing angle after offset " + offset);
    }
    return closingAngle(token.start() + index);
  }

  private ExpressionTree assignment(org.sonar.java.javac.source.tree.ExpressionTree expression,
    org.sonar.java.javac.source.tree.ExpressionTree variable, org.sonar.java.javac.source.tree.ExpressionTree value) {
    Operator operator = operator(expression.getKind());
    return new AssignmentExpressionTreeImpl(operator.kind(), convert(variable),
      tokens.syntaxToken(tokens.firstToken(end(variable), start(value), operator.token())), convert(value));
  }

  private org.sonar.java.model.InternalSyntaxToken syntaxToken(int offset, String expected) {
    SourceTokens.Token token = tokens.tokenAtOrAfter(offset);
    if (token.start() != offset || !token.text().equals(expected)) {
      throw new IllegalArgumentException("Expected token '" + expected + "' at offset " + offset);
    }
    return tokens.syntaxToken(token);
  }

  private IdentifierTreeImpl identifier(SourceTokens.Token token) {
    return switch (token.text()) {
      case "this" -> new KeywordThis(tokens.syntaxToken(token));
      case "super" -> new KeywordSuper(tokens.syntaxToken(token));
      default -> new IdentifierTreeImpl(tokens.syntaxToken(token));
    };
  }

  private org.sonar.java.model.InternalSyntaxToken syntaxToken(int offset) {
    SourceTokens.Token token = tokens.tokenAtOrAfter(offset);
    if (token.start() != offset) {
      throw new IllegalArgumentException("Expected token at offset " + offset);
    }
    return tokens.syntaxToken(token);
  }

  private org.sonar.java.model.InternalSyntaxToken closingAngle(int offset) {
    SourceTokens.Token token = tokens.tokenAtOrBefore(offset);
    if (offset < token.start() || offset >= token.end() || token.text().charAt(offset - token.start()) != '>') {
      throw new IllegalArgumentException("Expected closing angle at offset " + offset);
    }
    return new org.sonar.java.model.InternalSyntaxToken(tokens.line(offset), tokens.column(offset), ">", List.of(), false);
  }

  private int start(org.sonar.java.javac.source.tree.Tree tree) {
    return Math.toIntExact(positions.getStartPosition(unit, tree));
  }

  private int end(org.sonar.java.javac.source.tree.Tree tree) {
    return Math.toIntExact(positions.getEndPosition(unit, tree));
  }

  private static Tree.Kind literalKind(org.sonar.java.javac.source.tree.Tree tree) {
    return switch (tree.getKind()) {
      case INT_LITERAL -> Tree.Kind.INT_LITERAL;
      case LONG_LITERAL -> Tree.Kind.LONG_LITERAL;
      case FLOAT_LITERAL -> Tree.Kind.FLOAT_LITERAL;
      case DOUBLE_LITERAL -> Tree.Kind.DOUBLE_LITERAL;
      case BOOLEAN_LITERAL -> Tree.Kind.BOOLEAN_LITERAL;
      case CHAR_LITERAL -> Tree.Kind.CHAR_LITERAL;
      case STRING_LITERAL -> Tree.Kind.STRING_LITERAL;
      case NULL_LITERAL -> Tree.Kind.NULL_LITERAL;
      default -> throw unsupported(tree);
    };
  }

  private static Operator operator(org.sonar.java.javac.source.tree.Tree.Kind kind) {
    return switch (kind) {
      case MULTIPLY -> new Operator(Tree.Kind.MULTIPLY, "*");
      case DIVIDE -> new Operator(Tree.Kind.DIVIDE, "/");
      case REMAINDER -> new Operator(Tree.Kind.REMAINDER, "%");
      case PLUS -> new Operator(Tree.Kind.PLUS, "+");
      case MINUS -> new Operator(Tree.Kind.MINUS, "-");
      case LEFT_SHIFT -> new Operator(Tree.Kind.LEFT_SHIFT, "<<");
      case RIGHT_SHIFT -> new Operator(Tree.Kind.RIGHT_SHIFT, ">>");
      case UNSIGNED_RIGHT_SHIFT -> new Operator(Tree.Kind.UNSIGNED_RIGHT_SHIFT, ">>>");
      case LESS_THAN -> new Operator(Tree.Kind.LESS_THAN, "<");
      case GREATER_THAN -> new Operator(Tree.Kind.GREATER_THAN, ">");
      case LESS_THAN_EQUAL -> new Operator(Tree.Kind.LESS_THAN_OR_EQUAL_TO, "<=");
      case GREATER_THAN_EQUAL -> new Operator(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, ">=");
      case EQUAL_TO -> new Operator(Tree.Kind.EQUAL_TO, "==");
      case NOT_EQUAL_TO -> new Operator(Tree.Kind.NOT_EQUAL_TO, "!=");
      case AND -> new Operator(Tree.Kind.AND, "&");
      case XOR -> new Operator(Tree.Kind.XOR, "^");
      case OR -> new Operator(Tree.Kind.OR, "|");
      case CONDITIONAL_AND -> new Operator(Tree.Kind.CONDITIONAL_AND, "&&");
      case CONDITIONAL_OR -> new Operator(Tree.Kind.CONDITIONAL_OR, "||");
      case UNARY_PLUS -> new Operator(Tree.Kind.UNARY_PLUS, "+");
      case UNARY_MINUS -> new Operator(Tree.Kind.UNARY_MINUS, "-");
      case BITWISE_COMPLEMENT -> new Operator(Tree.Kind.BITWISE_COMPLEMENT, "~");
      case LOGICAL_COMPLEMENT -> new Operator(Tree.Kind.LOGICAL_COMPLEMENT, "!");
      case PREFIX_INCREMENT -> new Operator(Tree.Kind.PREFIX_INCREMENT, "++");
      case PREFIX_DECREMENT -> new Operator(Tree.Kind.PREFIX_DECREMENT, "--");
      case POSTFIX_INCREMENT -> new Operator(Tree.Kind.POSTFIX_INCREMENT, "++");
      case POSTFIX_DECREMENT -> new Operator(Tree.Kind.POSTFIX_DECREMENT, "--");
      case ASSIGNMENT -> new Operator(Tree.Kind.ASSIGNMENT, "=");
      case MULTIPLY_ASSIGNMENT -> new Operator(Tree.Kind.MULTIPLY_ASSIGNMENT, "*=");
      case DIVIDE_ASSIGNMENT -> new Operator(Tree.Kind.DIVIDE_ASSIGNMENT, "/=");
      case REMAINDER_ASSIGNMENT -> new Operator(Tree.Kind.REMAINDER_ASSIGNMENT, "%=");
      case PLUS_ASSIGNMENT -> new Operator(Tree.Kind.PLUS_ASSIGNMENT, "+=");
      case MINUS_ASSIGNMENT -> new Operator(Tree.Kind.MINUS_ASSIGNMENT, "-=");
      case LEFT_SHIFT_ASSIGNMENT -> new Operator(Tree.Kind.LEFT_SHIFT_ASSIGNMENT, "<<=");
      case RIGHT_SHIFT_ASSIGNMENT -> new Operator(Tree.Kind.RIGHT_SHIFT_ASSIGNMENT, ">>=");
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT -> new Operator(Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT, ">>>=");
      case AND_ASSIGNMENT -> new Operator(Tree.Kind.AND_ASSIGNMENT, "&=");
      case XOR_ASSIGNMENT -> new Operator(Tree.Kind.XOR_ASSIGNMENT, "^=");
      case OR_ASSIGNMENT -> new Operator(Tree.Kind.OR_ASSIGNMENT, "|=");
      default -> throw new UnsupportedOperationException("Unsupported javac operator: " + kind);
    };
  }

  private static UnsupportedOperationException unsupported(org.sonar.java.javac.source.tree.Tree tree) {
    return new UnsupportedOperationException("Unsupported javac expression: " + tree.getKind());
  }

  private record Operator(Tree.Kind kind, String token) {
  }
}
