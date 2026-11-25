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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.semsitter.ImportScopes.declaredIn;
import static org.sonar.java.semsitter.ImportScopes.denotesType;
import static org.sonar.java.semsitter.ImportScopes.dotted;
import static org.sonar.java.semsitter.ImportScopes.dottedType;
import static org.sonar.java.semsitter.ImportScopes.isInScope;
import static org.sonar.java.semsitter.ImportScopes.isOwnPackage;

/**
 * The compiler diagnostics the checks read from the compilation unit, derived from the tree and its symbols:
 * unused imports and assignments without effect.
 */
final class CompilerWarnings extends BaseTreeVisitor {
  private static final Pattern JAVADOC_REFERENCE = Pattern.compile("\\{@(?:link|linkplain|value)\\s++([^}]*)\\}|@(?:see|throws|exception)\\s++([^\\r\\n]*)");

  private static final Pattern CONTINUATION_STARS = Pattern.compile("(\\R)\\h*+\\*++(?!/)");

  private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]\\r\\n]*)\\]");

  private static final Pattern JAVA_NAME = Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$]*");

  static final Set<ProblemKind> SUPPORTED_KINDS = Collections.unmodifiableSet(EnumSet.of(ProblemKind.UNUSED_IMPORT, ProblemKind.ASSIGNMENT_HAS_NO_EFFECT));

  private final Map<ProblemKind, Set<JWarning>> warnings = new EnumMap<>(ProblemKind.class);

  private final List<IdentifierTree> identifiers = new ArrayList<>();

  private final Set<String> javadocNames = new HashSet<>();

  private final JSema sema;

  private CompilerWarnings(JSema sema) {
    this.sema = sema;
    SUPPORTED_KINDS.forEach(kind -> warnings.put(kind, new LinkedHashSet<>()));
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
    scan(tree.annotations());
    scan(tree.expression());
    scan(tree.identifier().annotations());
  }

  /** The method a method reference names is found through its qualifier, not through an import. */
  @Override
  public void visitMethodReference(MethodReferenceTree tree) {
    scan(tree.expression());
    scan(tree.typeArguments());
    scan(tree.method().annotations());
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (hasNoEffect(tree)) {
      add(ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, "The assignment to variable " + assignedVariable(tree.variable()).name() + " has no effect", tree);
    }
    super.visitAssignmentExpression(tree);
  }

  /** A plain assignment of a variable to itself, unless the variable is volatile: writing it then still publishes the value. */
  static boolean hasNoEffect(AssignmentExpressionTree tree) {
    Symbol target = tree.is(Tree.Kind.ASSIGNMENT) ? assignedVariable(tree.variable()) : null;
    return target != null && !target.isVolatile() && target.equals(assignedVariable(tree.expression()));
  }

  /** The variable a simple name, {@code this.name} or {@code Type.name} designates. */
  private static Symbol assignedVariable(ExpressionTree tree) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(tree);
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

  /**
   * The names a Javadoc reference tag ({@code @link}, {@code @value}, {@code @see}, {@code @throws}) spells, as the compiler
   * reads them: only the comments documenting a declaration are resolved.
   */
  private void collectJavadocNames(Tree tree) {
    SyntaxToken first = isDocumentedDeclaration(tree) ? tree.firstToken() : null;
    if (first != null) {
      for (SyntaxTrivia trivia : first.trivias()) {
        if (trivia.comment().startsWith("/**") || trivia.comment().startsWith("///")) {
          javadocNames.addAll(javadocReferenceNames(trivia.comment()));
        }
      }
    }
    if (tree instanceof JavaTree javaTree && !javaTree.isLeaf()) {
      javaTree.getChildren().forEach(this::collectJavadocNames);
    }
  }

  static boolean isDocumentedDeclaration(Tree tree) {
    return tree instanceof ClassTree || tree instanceof MethodTree || tree instanceof ModuleDeclarationTree
      || (tree instanceof PackageDeclarationTree && tree.parent() instanceof CompilationUnitTree unit && unit.types().isEmpty())
      || (tree instanceof VariableTree && tree.parent() instanceof ClassTree);
  }

  /** The names the references of a Javadoc comment spell, a reference wrapped onto a continuation line included. */
  static List<String> javadocReferenceNames(String comment) {
    List<String> names = new ArrayList<>();
    Matcher matcher = JAVADOC_REFERENCE.matcher(CONTINUATION_STARS.matcher(comment).replaceAll("$1"));
    while (matcher.find()) {
      names.addAll(referenceNames(matcher.group(matcher.group(1) != null ? 1 : 2)));
    }
    if (comment.startsWith("///")) {
      Matcher link = MARKDOWN_LINK.matcher(comment);
      while (link.find()) {
        names.addAll(referenceNames(link.group(1)));
      }
    }
    return names;
  }

  /**
   * The simple names a Javadoc reference spells: its first name and the first name of each parameter type of a method
   * reference, since a qualified type does not go through an import. The reference ends at the first whitespace outside
   * its parameter list, so the description that follows names nothing.
   */
  static List<String> referenceNames(String text) {
    String reference = text.substring(0, referenceEnd(text));
    int open = reference.indexOf('(');
    List<String> names = new ArrayList<>();
    firstName(open < 0 ? reference : reference.substring(0, open)).ifPresent(names::add);
    if (open >= 0) {
      for (String parameter : reference.substring(open + 1).split(",")) {
        firstName(parameter).ifPresent(names::add);
      }
    }
    return names;
  }

  private static Optional<String> firstName(String text) {
    Matcher word = JAVA_NAME.matcher(text);
    return word.find() ? Optional.of(word.group()) : Optional.empty();
  }

  private static int referenceEnd(String text) {
    int depth = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth = Math.max(0, depth - 1);
      } else if (depth == 0 && Character.isWhitespace(c)) {
        return i;
      }
    }
    return text.length();
  }

  private void reportUnusedImports(JavaTree.CompilationUnitTreeImpl unit) {
    if (unit.moduleDeclaration() != null) {
      // The compiler reports no unused import in a module declaration unit.
      return;
    }
    Set<String> seen = new HashSet<>();
    String currentPackage = unit.packageDeclaration() == null ? "" : dotted(unit.packageDeclaration().packageName());
    loadImportedTypes(unit);
    List<IdentifierTree> outOfScope = identifiers.stream().filter(identifier -> !isInFileScope(identifier)).toList();
    for (ImportClauseTree clause : unit.imports()) {
      if (!(clause instanceof ImportTree importTree)) {
        continue;
      }
      String name = dotted(importTree.qualifiedIdentifier());
      String key = (importTree.isStatic() ? "static " : "") + name;
      boolean duplicate = !seen.add(key);
      if (isResolvable(importTree, name, currentPackage) && (duplicate || isOwnPackage(importTree, name, currentPackage) || !isUsed(importTree, name, outOfScope))) {
        add(ProblemKind.UNUSED_IMPORT, "The import " + (name.endsWith(".*") ? name.substring(0, name.length() - 2) : name) + " is never used", importTree);
      }
    }
  }

  /** Loads the imported types first, so that an on-demand import of their package is known to exist. */
  private void loadImportedTypes(JavaTree.CompilationUnitTreeImpl unit) {
    for (ImportClauseTree clause : unit.imports()) {
      if (clause instanceof ImportTree importTree && !importTree.isStatic() && !dotted(importTree.qualifiedIdentifier()).endsWith(".*")) {
        declaresImported(dotted(importTree.qualifiedIdentifier()));
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

  private static boolean isInFileScope(IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    return !symbol.isUnknown() && symbol.declaration() != null && isInScope(symbol, identifier);
  }

  private boolean isUsed(ImportTree importTree, String name, List<IdentifierTree> outOfScope) {
    if (importTree instanceof JavaTree.ImportTreeImpl impl && impl.isModule()) {
      return true;
    }
    boolean onDemand = name.endsWith(".*");
    String qualifier = onDemand ? name.substring(0, name.length() - 2) : name.substring(0, Math.max(0, name.lastIndexOf('.')));
    return usedInJavadoc(importTree, name, qualifier) || outOfScope.stream().anyMatch(identifier -> uses(importTree, name, qualifier, identifier));
  }

  /** A Javadoc reference uses a single-type import of its name, and an on-demand import of a package declaring it. */
  private boolean usedInJavadoc(ImportTree importTree, String name, String qualifier) {
    if (!name.endsWith(".*")) {
      return javadocNames.contains(name.substring(name.lastIndexOf('.') + 1));
    }
    return !importTree.isStatic() && javadocNames.stream().anyMatch(javadocName -> resolvesType(qualifier + "." + javadocName));
  }

  private boolean uses(ImportTree importTree, String name, String qualifier, IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (name.endsWith(".*")) {
      String owner = qualifier.replace('$', '.');
      return declaredIn(symbol, owner, importTree.isStatic())
        || (importTree.isStatic() && !symbol.isUnknown() && !symbol.isTypeSymbol() && isMemberOf(symbol, owner));
    }
    if (!identifier.name().equals(name.substring(name.lastIndexOf('.') + 1))) {
      return false;
    }
    if (symbol.isUnknown()) {
      return true;
    }
    // a class named with a dollar sign is a top-level class, as written in the import, not a nested one
    String dotted = name.replace('$', '.');
    if (!importTree.isStatic()) {
      return denotesType(symbol, dotted, identifier);
    }
    return isMemberOf(symbol, qualifier.replace('$', '.')) || (symbol.isTypeSymbol() && dottedType(symbol).equals(dotted));
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
}
