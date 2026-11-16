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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

/**
 * The compiler diagnostics the checks read from the compilation unit, derived from the tree and its symbols:
 * unused imports and assignments without effect.
 */
final class CompilerWarnings extends BaseTreeVisitor {

  private static final Pattern JAVADOC_REFERENCE = Pattern.compile("\\{@(?:link|linkplain|value)\\s+([^}]*)\\}|@(?:see|throws|exception)\\s+([^\\r\\n]*)");
  private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]\\r\\n]*)\\]");
  private static final Pattern JAVA_NAME = Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$]*");

  private static final Map<String, Integer> NUMERIC_RANK = Map.of("byte", 0, "short", 1, "char", 1, "int", 2, "long", 3, "float", 4, "double", 5);

  static final Set<ProblemKind> SUPPORTED_KINDS = Set.of(ProblemKind.UNUSED_IMPORT, ProblemKind.REDUNDANT_CAST, ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, ProblemKind.MASKED_CATCH);

  private final Map<ProblemKind, Set<JWarning>> warnings = new EnumMap<>(ProblemKind.class);
  private final List<IdentifierTree> identifiers = new ArrayList<>();
  private final Set<String> javadocNames = new HashSet<>();
  private final JSema sema;

  private CompilerWarnings(JSema sema) {
    this.sema = sema;
  }

  static void addTo(JavaTree.CompilationUnitTreeImpl unit) {
    CompilerWarnings visitor = new CompilerWarnings((JSema) unit.sema);
    visitor.scan(unit.packageDeclaration());
    visitor.scan(unit.types());
    unit.imports().forEach(visitor::scan);
    visitor.collectJavadocNames(unit);
    visitor.reportUnusedImports(unit);
    unit.addWarnings(visitor.warnings);
  }

  private void add(ProblemKind type, String message, Tree tree) {
    warnings.computeIfAbsent(type, key -> new LinkedHashSet<>()).add(new JWarning(message, type, tree));
  }

  @Override
  public void visitImport(ImportTree tree) {
    // names inside an import are not uses
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    identifiers.add(tree);
    super.visitIdentifier(tree);
  }

  @Override
  public void visitArrayType(org.sonar.plugins.java.api.tree.ArrayTypeTree tree) {
    scan(tree.annotations());
    super.visitArrayType(tree);
  }

  /** Only simple names use an import: the member of a qualified name is not one. */
  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    scan(tree.expression());
    if (tree.identifier() instanceof JavaTree.AnnotatedTypeTree annotated) {
      scan(annotated.annotations());
    }
  }

  @Override
  public void visitTypeCast(org.sonar.plugins.java.api.tree.TypeCastTree tree) {
    Type cast = tree.type().symbolType();
    Type operand = tree.expression().symbolType();
    if (!cast.isUnknown() && !operand.isUnknown() && operand.isSubtypeOf(cast)) {
      if (cast.erasure().fullyQualifiedName().equals(operand.erasure().fullyQualifiedName()) && cast.typeArguments().isEmpty() == operand.typeArguments().isEmpty()) {
        add(ProblemKind.REDUNDANT_CAST, "Unnecessary cast from " + operand.erasure().name() + " to " + cast.erasure().name(), tree);
      }
    }
    if (!cast.isUnknown() && !operand.isUnknown() && operand.isSubtypeOf(cast) && assignedTo(tree).filter(target -> !target.isUnknown() && operand.isSubtypeOf(target) && cast.isSubtypeOf(target)).isPresent()) {
      // the cast widens a value that is assigned to a type accepting it as is
      add(ProblemKind.REDUNDANT_CAST, "Unnecessary cast from " + operand.erasure().name() + " to " + cast.erasure().name(), tree);
    }
    super.visitTypeCast(tree);
    scan(tree.bounds());
  }

  /** The declared type of the variable the cast initializes or is assigned to. */
  private static java.util.Optional<Type> assignedTo(org.sonar.plugins.java.api.tree.TypeCastTree cast) {
    Tree child = cast;
    Tree parent = cast.parent();
    while (parent instanceof org.sonar.plugins.java.api.tree.ParenthesizedTree) {
      child = parent;
      parent = parent.parent();
    }
    if (parent instanceof org.sonar.plugins.java.api.tree.VariableTree variable && variable.initializer() == child && !(variable.type() instanceof org.sonar.plugins.java.api.tree.VarTypeTree)) {
      return java.util.Optional.of(variable.type().symbolType());
    }
    if (parent instanceof AssignmentExpressionTree assignment && assignment.is(Tree.Kind.ASSIGNMENT) && assignment.expression() == child) {
      return java.util.Optional.of(assignment.variable().symbolType());
    }
    return java.util.Optional.empty();
  }

  /**
   * A widening primitive cast on an operand is unnecessary when the operator promotes the operands to the same types without it
   * (all casts of the expression are removed together).
   */
  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    ExpressionTree left = ExpressionUtils.skipParentheses(tree.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(tree.rightOperand());
    boolean leftCast = isWideningPrimitiveCast(left);
    boolean rightCast = isWideningPrimitiveCast(right);
    if (leftCast || rightCast) {
      String leftType = promotionInput(left, leftCast);
      String rightType = promotionInput(right, rightCast);
      if (leftType != null && rightType != null && sameOperandTypes(tree.kind(), leftType, rightType, left, right)) {
        if (leftCast) {
          add(ProblemKind.REDUNDANT_CAST, castMessage(left), left);
        }
        if (rightCast) {
          add(ProblemKind.REDUNDANT_CAST, castMessage(right), right);
        }
      }
    }
    super.visitBinaryExpression(tree);
  }

  private static String castMessage(ExpressionTree cast) {
    return "Unnecessary cast from " + ((org.sonar.plugins.java.api.tree.TypeCastTree) cast).expression().symbolType().name() + " to " + cast.symbolType().name();
  }

  private static boolean isWideningPrimitiveCast(ExpressionTree tree) {
    if (!(tree instanceof org.sonar.plugins.java.api.tree.TypeCastTree cast)) {
      return false;
    }
    String from = cast.expression().symbolType().fullyQualifiedName();
    String to = cast.type().symbolType().fullyQualifiedName();
    return NUMERIC_RANK.containsKey(from) && NUMERIC_RANK.containsKey(to) && (from.equals(to) || isWidening(from, to));
  }

  private static boolean isWidening(String from, String to) {
    if ("char".equals(to)) {
      return false;
    }
    if ("char".equals(from)) {
      return NUMERIC_RANK.get(to) >= NUMERIC_RANK.get("int");
    }
    return NUMERIC_RANK.get(from) < NUMERIC_RANK.get(to);
  }

  /** The type the operand has once its cast is dropped, or null when the operand is not a numeric primitive. */
  private static String promotionInput(ExpressionTree operand, boolean dropCast) {
    Type type = dropCast ? ((org.sonar.plugins.java.api.tree.TypeCastTree) operand).expression().symbolType() : operand.symbolType();
    String name = type.fullyQualifiedName();
    return NUMERIC_RANK.containsKey(name) ? name : null;
  }

  private static boolean sameOperandTypes(Tree.Kind kind, String leftWithout, String rightWithout, ExpressionTree left, ExpressionTree right) {
    String leftWith = left.symbolType().fullyQualifiedName();
    String rightWith = right.symbolType().fullyQualifiedName();
    if (!NUMERIC_RANK.containsKey(leftWith) || !NUMERIC_RANK.containsKey(rightWith)) {
      return false;
    }
    if (kind == Tree.Kind.LEFT_SHIFT || kind == Tree.Kind.RIGHT_SHIFT || kind == Tree.Kind.UNSIGNED_RIGHT_SHIFT) {
      return unaryPromoted(leftWith).equals(unaryPromoted(leftWithout)) && unaryPromoted(rightWith).equals(unaryPromoted(rightWithout));
    }
    return binaryPromoted(leftWith, rightWith).equals(binaryPromoted(leftWithout, rightWithout));
  }

  private static String unaryPromoted(String type) {
    return NUMERIC_RANK.get(type) < NUMERIC_RANK.get("int") ? "int" : type;
  }

  private static String binaryPromoted(String left, String right) {
    String a = unaryPromoted(left);
    String b = unaryPromoted(right);
    return NUMERIC_RANK.get(a) >= NUMERIC_RANK.get(b) ? a : b;
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    List<Type> caught = new ArrayList<>();
    for (CatchTree catchTree : tree.catches()) {
      TypeTree parameterType = catchTree.parameter().type();
      List<TypeTree> alternatives = parameterType instanceof UnionTypeTree union ? union.typeAlternatives() : List.of(parameterType);
      for (TypeTree alternative : alternatives) {
        Type type = alternative.symbolType();
        Type hiding = type.isUnknown() ? null : caught.stream().filter(type::isSubtypeOf).findFirst().orElse(null);
        if (hiding != null) {
          add(ProblemKind.MASKED_CATCH, "Unreachable catch block for " + type.name() + ". It is already handled by the catch block for " + hiding.name(), alternative);
        }
      }
      alternatives.stream().map(TypeTree::symbolType).filter(type -> !type.isUnknown()).forEach(caught::add);
    }
    super.visitTryStatement(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      Symbol target = assignedVariable(tree.variable());
      if (target != null && target.equals(assignedVariable(tree.expression()))) {
        add(ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, "The assignment to variable " + target.name() + " has no effect", tree);
      }
    }
    super.visitAssignmentExpression(tree);
  }

  /** The variable a simple name, {@code this.name} or {@code Type.name} designates. */
  private static Symbol assignedVariable(ExpressionTree tree) {
    ExpressionTree expression = tree;
    while (expression instanceof org.sonar.plugins.java.api.tree.ParenthesizedTree parenthesized) {
      expression = parenthesized.expression();
    }
    IdentifierTree name = null;
    if (expression instanceof IdentifierTree identifier) {
      name = identifier;
    } else if (expression instanceof MemberSelectExpressionTree select) {
      ExpressionTree qualifier = select.expression();
      if (qualifier instanceof IdentifierTree owner && ("this".equals(owner.name()) || owner.symbol().isTypeSymbol())) {
        name = select.identifier();
      }
    }
    if (name == null || !name.symbol().isVariableSymbol() || name.symbol().isUnknown()) {
      return null;
    }
    return name.symbol();
  }

  /** The names a Javadoc reference tag ({@code @link}, {@code @value}, {@code @see}, {@code @throws}) spells, as the compiler reads them. */
  private void collectJavadocNames(Tree tree) {
    if (tree instanceof SyntaxToken token) {
      for (SyntaxTrivia trivia : token.trivias()) {
        if (trivia.comment().startsWith("/**") || trivia.comment().startsWith("///")) {
          addJavadocNames(trivia.comment());
        }
      }
      return;
    }
    if (tree instanceof JavaTree javaTree && !javaTree.isLeaf()) {
      for (Tree child : javaTree.getChildren()) {
        if (child != null) {
          collectJavadocNames(child);
        }
      }
    }
  }

  private void addJavadocNames(String comment) {
    Matcher matcher = JAVADOC_REFERENCE.matcher(comment);
    while (matcher.find()) {
      addReferenceNames(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
    }
    if (comment.startsWith("///")) {
      Matcher link = MARKDOWN_LINK.matcher(comment);
      while (link.find()) {
        addReferenceNames(link.group(1));
      }
    }
  }

  private void addReferenceNames(String reference) {
    Matcher word = JAVA_NAME.matcher(reference);
    boolean first = true;
    while (word.find()) {
      String name = word.group();
      int start = word.start();
      if (first) {
        javadocNames.add(name);
        first = false;
      } else if (reference.substring(0, start).contains("(")) {
        javadocNames.add(name);
      }
    }
  }

  private void reportUnusedImports(JavaTree.CompilationUnitTreeImpl unit) {
    if (unit.moduleDeclaration() != null) {
      // The compiler reports no unused import in a module declaration unit.
      return;
    }
    Set<String> seen = new HashSet<>();
    String currentPackage = unit.packageDeclaration() == null ? "" : dotted(unit.packageDeclaration().packageName());
    // Loads the imported types first, so that an on-demand import of their package is known to exist.
    for (ImportClauseTree clause : unit.imports()) {
      if (clause instanceof ImportTree importTree && !importTree.isStatic() && !dotted(importTree.qualifiedIdentifier()).endsWith(".*")) {
        declaresImported(dotted(importTree.qualifiedIdentifier()));
      }
    }
    for (ImportClauseTree clause : unit.imports()) {
      if (!(clause instanceof ImportTree importTree)) {
        continue;
      }
      String name = dotted(importTree.qualifiedIdentifier());
      String key = (importTree.isStatic() ? "static " : "") + name;
      boolean duplicate = !seen.add(key);
      if (isResolvable(importTree, name, currentPackage) && (duplicate || isOwnPackage(importTree, name, currentPackage) || !isUsed(importTree, name))) {
        add(ProblemKind.UNUSED_IMPORT, "The import " + (name.endsWith(".*") ? name.substring(0, name.length() - 2) : name) + " is never used", importTree);
      }
    }
  }

  private void declaresImported(String name) {
    binaryName(name);
  }

  /** Like the compiler, only an import that resolves can be reported as unused. */
  private boolean isResolvable(ImportTree importTree, String name, String currentPackage) {
    if (importTree instanceof JavaTree.ImportTreeImpl impl && impl.isModule()) {
      return true;
    }
    boolean onDemand = name.endsWith(".*");
    String qualifier = onDemand ? name.substring(0, name.length() - 2) : name;
    if (onDemand && !importTree.isStatic() && (qualifier.equals(currentPackage) || sema.knowsPackage(qualifier))) {
      return true;
    }
    return resolvesType(importTree.isStatic() && !onDemand ? qualifier.substring(0, Math.max(0, qualifier.lastIndexOf('.'))) : qualifier);
  }

  private boolean resolvesType(String dotted) {
    return binaryName(dotted) != null;
  }

  /** The binary name of the type a dotted name denotes, trying each dot as a nesting boundary. */
  private String binaryName(String dotted) {
    String candidate = dotted;
    while (!candidate.isEmpty()) {
      if (sema.declaresType(candidate)) {
        return candidate;
      }
      int dot = candidate.lastIndexOf('.');
      if (dot < 0) {
        return null;
      }
      candidate = candidate.substring(0, dot) + "$" + candidate.substring(dot + 1);
    }
    return null;
  }

  /** The package's own types are imported implicitly, so such an import is never counted as used. */
  private static boolean isOwnPackage(ImportTree importTree, String name, String currentPackage) {
    if (importTree.isStatic() || currentPackage.isEmpty()) {
      return false;
    }
    String qualifier = name.endsWith(".*") ? name.substring(0, name.length() - 2) : name.substring(0, Math.max(0, name.lastIndexOf('.')));
    return (qualifier.equals(currentPackage) && !name.endsWith(".*")) || name.equals(currentPackage + ".*");
  }

  private boolean isUsed(ImportTree importTree, String name) {
    if (importTree instanceof JavaTree.ImportTreeImpl impl && impl.isModule()) {
      return true;
    }
    if (!name.endsWith(".*") && javadocNames.contains(name.substring(name.lastIndexOf('.') + 1))) {
      return true;
    }
    boolean onDemand = name.endsWith(".*");
    String qualifier = onDemand ? name.substring(0, name.length() - 2) : name.substring(0, Math.max(0, name.lastIndexOf('.')));
    for (IdentifierTree identifier : identifiers) {
      Symbol symbol = identifier.symbol();
      // a symbol found in the scope of the file does not consult the import
      boolean inScope = !symbol.isUnknown() && symbol.declaration() != null && isInScope(symbol, identifier);
      if (!inScope && uses(importTree, name, qualifier, identifier)) {
        return true;
      }
    }
    return false;
  }

  /**
   * A member of another type of the same file is in scope only through an import, one of an enclosing or inherited type is not.
   * The annotations of a class are outside its scope, and a method looked up through an incomplete hierarchy may still come from an import.
   */
  private static boolean isInScope(Symbol symbol, IdentifierTree identifier) {
    if (!(symbol.owner() instanceof Symbol.TypeSymbol owner) || owner.isUnknown()) {
      return true;
    }
    boolean incompleteHierarchy = false;
    boolean inScope = false;
    Tree previous = identifier;
    for (Tree tree = identifier.parent(); tree != null; previous = tree, tree = tree.parent()) {
      if (tree instanceof org.sonar.plugins.java.api.tree.ClassTree declared) {
        incompleteHierarchy |= declared.superClass() != null && declared.superClass().symbolType().isUnknown();
        Symbol.TypeSymbol enclosing = declared.symbol();
        if (!isOutsideBody(declared, previous) && (enclosing.equals(owner) || enclosing.type().isSubtypeOf(owner.type().erasure()))) {
          inScope = true;
        }
      }
    }
    return inScope && !(symbol.isMethodSymbol() && incompleteHierarchy);
  }

  /** The annotations, type parameters and supertypes of a class are not part of its body, where its members are in scope. */
  private static boolean isOutsideBody(org.sonar.plugins.java.api.tree.ClassTree declared, Tree child) {
    return child == declared.modifiers() || child == declared.typeParameters() || child == declared.superClass()
      || child == declared.superInterfaces();
  }

  private boolean uses(ImportTree importTree, String name, String qualifier, IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (name.endsWith(".*")) {
      return declaredIn(symbol, qualifier.replace('$', '.'), importTree.isStatic());
    }
    if (!identifier.name().equals(name.substring(name.lastIndexOf('.') + 1))) {
      return false;
    }
    if (symbol.isUnknown()) {
      return true;
    }
    // a class named with a dollar sign is a top-level class, as written in the import, not a nested one
    String dotted = name.replace('$', '.');
    return importTree.isStatic() ? isMemberOf(symbol, qualifier.replace('$', '.')) || (symbol.isTypeSymbol() && dottedType(symbol).equals(dotted)) : denotesType(symbol, dotted, identifier);
  }

  /** A static import also brings in the members the named type inherits. */
  private boolean isMemberOf(Symbol symbol, String qualifier) {
    if (!(symbol.owner() instanceof Symbol.TypeSymbol owner)) {
      return false;
    }
    if (dottedType(owner).equals(qualifier)) {
      return true;
    }
    String binary = binaryName(qualifier);
    return binary != null && sema.getClassType(binary).isSubtypeOf(owner.type());
  }

  private static boolean denotesType(Symbol symbol, String name, IdentifierTree identifier) {
    if (symbol.isTypeSymbol()) {
      return dottedType(symbol).equals(name);
    }
    if (!(symbol.isMethodSymbol() && "<init>".equals(symbol.name()))) {
      return false;
    }
    if (symbol.owner() instanceof Symbol.TypeSymbol owner && dottedType(owner).equals(name)) {
      return true;
    }
    Type written = identifier.symbolType();
    return written != null && !written.isUnknown() && written.erasure().fullyQualifiedName().replace('$', '.').equals(name);
  }

  private static boolean declaredIn(Symbol symbol, String qualifier, boolean isStatic) {
    if (symbol.isUnknown()) {
      return false;
    }
    if (isStatic) {
      if (symbol.isTypeSymbol()) {
        String type = dottedType(symbol);
        return type.startsWith(qualifier + ".") && type.indexOf('.', qualifier.length() + 1) < 0;
      }
      return symbol.owner() instanceof Symbol.TypeSymbol owner && dottedType(owner).equals(qualifier);
    }
    Symbol type = symbol;
    if ("<init>".equals(symbol.name())) {
      type = symbol.owner();
    }
    return type.isTypeSymbol() && !type.isUnknown() && dottedType(type).startsWith(qualifier + ".");
  }

  private static String dottedType(Symbol symbol) {
    return symbol.type().fullyQualifiedName().replace('$', '.');
  }

  private static String dotted(Tree tree) {
    if (tree instanceof MemberSelectExpressionTree select) {
      return dotted(select.expression()) + "." + select.identifier().name();
    }
    return tree instanceof IdentifierTree identifier ? identifier.name() : "";
  }
}
