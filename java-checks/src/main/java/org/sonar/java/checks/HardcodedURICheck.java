/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S1075")
public class HardcodedURICheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final MethodMatchers MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.net.URI")
      .constructor()
      .addParametersMatcher(JAVA_LANG_STRING).build(),
    MethodMatchers.create()
      .ofTypes("java.io.File")
      .constructor()
      .addParametersMatcher(JAVA_LANG_STRING)
      .addParametersMatcher(ANY, JAVA_LANG_STRING)
      .build());

  private static final String SCHEME = "[a-zA-Z][a-zA-Z\\+\\.\\-]+";
  private static final String FOLDER_NAME = "[^/?%*:\\\\|\"<>]+";
  private static final String URI_REGEX = String.format("^%s://.+", SCHEME);
  private static final String LOCAL_URI = String.format("^(~/|/|//[\\w-]+/|%s:/)(%s/)*%s/?",
    SCHEME, FOLDER_NAME, FOLDER_NAME);
  private static final String BACKSLASH_LOCAL_URI = String.format("^(~\\\\\\\\|\\\\\\\\\\\\\\\\[\\w-]+\\\\\\\\|%s:\\\\\\\\)(%s\\\\\\\\)*%s(\\\\\\\\)?",
    SCHEME, FOLDER_NAME, FOLDER_NAME);
  private static final String DISK_URI = "^[A-Za-z]:(/|\\\\)";

  private static final Pattern URI_PATTERN = Pattern.compile(URI_REGEX + "|" + LOCAL_URI + "|" + DISK_URI + "|" + BACKSLASH_LOCAL_URI);
  private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("filename|path", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATH_DELIMETERS_PATTERN = Pattern.compile("\"/\"|\"//\"|\"\\\\\\\\\"|\"\\\\\\\\\\\\\\\\\"");
  private static final Pattern RELATIVE_URI_PATTERN = Pattern.compile("^(/[\\w-+!*.]+){1,2}");


  // we use these variables to track when we are visiting an annotation
  private final Deque<AnnotationTree> annotationsStack = new ArrayDeque<>();

  private record IdentifierData(Symbol symbol, String identifier) {
  }

  private final List<IdentifierData> identifiersUsedInAnnotations = new ArrayList<>();

  private record VariableData(Symbol symbol, String identifier, ExpressionTree initializer) {
  }

  private final List<VariableData> hardCodedUri = new ArrayList<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    annotationsStack.clear();
    identifiersUsedInAnnotations.clear();
    hardCodedUri.clear();
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    // now, we know all variable that are used in annotation so we can report issues
    Set<Symbol> idSymbols = new HashSet<>();
    Set<String> idNamesWithSemantic = new HashSet<>();
    Set<String> idNamesWithoutSemantic = new HashSet<>();

    for (IdentifierData i : identifiersUsedInAnnotations) {
      if (i.symbol().isUnknown()) {
        idNamesWithoutSemantic.add(i.identifier());
      } else {
        idSymbols.add(i.symbol());
        idNamesWithSemantic.add(i.identifier());
      }
    }

    for(VariableData v : hardCodedUri) {
      // equals to an identifier with unknown semantic, we cannot compare their symbols
      if (idNamesWithoutSemantic.contains(v.identifier())) {
        continue;
      }

      // idNamesWithSemantic is used to only compare the symbols when their string identifier are the same
      // as comparing symbols is costly
      if (idNamesWithSemantic.contains(v.identifier()) && idSymbols.contains(v.symbol())) {
        continue;
      }
      reportHardcodedURI(v.initializer());
    }
  }


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.NEW_CLASS, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.ANNOTATION, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof NewClassTree classTree) {
      checkNewClassTree(classTree);
    } else if (tree instanceof VariableTree variableTree) {
      checkVariable(variableTree);
    } else if (tree instanceof AnnotationTree annotationTree) {
      annotationsStack.add(annotationTree);
    } else if (tree instanceof IdentifierTree identifier && !annotationsStack.isEmpty()) {
      identifiersUsedInAnnotations.add(new IdentifierData(identifier.symbol(), identifier.name()));
    } else if (tree instanceof AssignmentExpressionTree assignment) {
      checkAssignment(assignment);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree instanceof AnnotationTree) {
      annotationsStack.pop();
    }
  }

  private void checkNewClassTree(NewClassTree nct) {
    if (MATCHERS.matches(nct)) {
      nct.arguments().forEach(this::checkExpression);
    }
  }

  private void checkVariable(VariableTree tree) {
    ExpressionTree initializer = tree.initializer();

    if (!isFileNameVariable(tree.simpleName())
      || initializer == null
      // we don't raise issues when the variable is annotated
      || !tree.modifiers().annotations().isEmpty()
    ) {
      return;
    }

    String stringLiteral = stringLiteral(initializer);
    if (stringLiteral == null) {
      // The check below applies only to binary expressions, so we apply it only if the initializer is not a literal.
      reportStringConcatenationWithPathDelimiter(initializer);
      return;
    }

    // small relative Uri that are static and final are allowed
    if (ModifiersUtils.hasAll(tree.modifiers(), Modifier.STATIC, Modifier.FINAL)
      && RELATIVE_URI_PATTERN.matcher(stringLiteral).matches()) {
      return;
    }

    if (isHardcodedURI(initializer)) {
      hardCodedUri.add(new VariableData(tree.symbol(),
        tree.simpleName().name(),
        initializer));
    }
  }

  private void checkAssignment(AssignmentExpressionTree tree) {
    if (isFileNameVariable(getVariableIdentifier(tree)) && !isPartOfAnnotation(tree)) {
      checkExpression(tree.expression());
    }
  }

  private static boolean isPartOfAnnotation(AssignmentExpressionTree tree) {
    Tree parent = tree.parent();
    while (parent != null) {
      if (parent.is(Tree.Kind.ANNOTATION)) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  private static boolean isFileNameVariable(@Nullable IdentifierTree variable) {
    return variable != null && VARIABLE_NAME_PATTERN.matcher(variable.name()).find();
  }

  private void checkExpression(ExpressionTree expr) {
    if (isHardcodedURI(expr)) {
      reportHardcodedURI(expr);
    } else {
      reportStringConcatenationWithPathDelimiter(expr);
    }
  }

  private static boolean isHardcodedURI(ExpressionTree expr) {
    String stringLiteral = stringLiteral(expr);
    return stringLiteral != null
      && !stringLiteral.contains("*")
      && !stringLiteral.contains("$")
      && URI_PATTERN.matcher(stringLiteral).find();
  }

  @Nullable
  private static String stringLiteral(ExpressionTree expr) {
    ExpressionTree unquoted = ExpressionUtils.skipParentheses(expr);

    if (unquoted instanceof LiteralTree literalTree && literalTree.is(Tree.Kind.STRING_LITERAL)) {
      return LiteralUtils.trimQuotes(literalTree.value());
    }
    return null;
  }

  private void reportHardcodedURI(ExpressionTree hardcodedURI) {
    reportIssue(hardcodedURI, "Refactor your code to get this URI from a customizable parameter.");
  }

  private void reportStringConcatenationWithPathDelimiter(ExpressionTree expr) {
    expr.accept(new StringConcatenationVisitor());
  }

  private class StringConcatenationVisitor extends BaseTreeVisitor {
    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.PLUS)) {
        checkPathDelimiter(tree.leftOperand());
        checkPathDelimiter(tree.rightOperand());
      }
      super.visitBinaryExpression(tree);
    }

    private void checkPathDelimiter(ExpressionTree expr) {
      ExpressionTree newExpr = ExpressionUtils.skipParentheses(expr);
      if (newExpr.is(Tree.Kind.STRING_LITERAL) && PATH_DELIMETERS_PATTERN.matcher(((LiteralTree) newExpr).value()).find()) {
        reportIssue(newExpr, "Remove this hard-coded path-delimiter.");
      }
    }
  }

  @CheckForNull
  private static IdentifierTree getVariableIdentifier(AssignmentExpressionTree tree) {
    ExpressionTree variable = ExpressionUtils.skipParentheses(tree.variable());
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) variable;
    } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) variable).identifier();
    }
    // ignore assignments in arrays
    return null;
  }

}
