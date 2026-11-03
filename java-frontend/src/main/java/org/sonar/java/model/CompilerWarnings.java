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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * The compiler diagnostics the checks read from the compilation unit, derived from the tree and its symbols:
 * unused imports and assignments without effect.
 */
final class CompilerWarnings extends BaseTreeVisitor {

  private final Map<JProblem.Type, Set<JWarning>> warnings = new EnumMap<>(JProblem.Type.class);
  private final List<IdentifierTree> identifiers = new ArrayList<>();
  private final JSema sema;

  private CompilerWarnings(JSema sema) {
    this.sema = sema;
  }

  static void addTo(JavaTree.CompilationUnitTreeImpl unit) {
    CompilerWarnings visitor = new CompilerWarnings(unit.sema);
    visitor.scan(unit.packageDeclaration());
    visitor.scan(unit.types());
    unit.imports().forEach(visitor::scan);
    visitor.reportUnusedImports(unit);
    unit.addWarnings(visitor.warnings);
  }

  private void add(JProblem.Type type, String message, Tree tree) {
    warnings.computeIfAbsent(type, key -> new LinkedHashSet<>()).add(new JWarning(message, type, tree));
  }

  @Override
  public void visitImport(ImportTree tree) {
    // names inside an import are not uses
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    identifiers.add(tree);
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
      Tree parent = tree.parent();
      boolean identity = cast.erasure().fullyQualifiedName().equals(operand.erasure().fullyQualifiedName());
      boolean discarded = parent != null && parent.is(Tree.Kind.RETURN_STATEMENT, Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT);
      if (identity || discarded) {
        add(JProblem.Type.REDUNDANT_CAST, "Unnecessary cast to " + cast.erasure().name(), tree);
      }
    }
    super.visitTypeCast(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      Symbol target = assignedVariable(tree.variable());
      if (target != null && target.equals(assignedVariable(tree.expression()))) {
        add(JProblem.Type.ASSIGNMENT_HAS_NO_EFFECT, "The assignment to variable " + target.name() + " has no effect", tree);
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

  private void reportUnusedImports(JavaTree.CompilationUnitTreeImpl unit) {
    Set<String> seen = new HashSet<>();
    String currentPackage = unit.packageDeclaration() == null ? "" : dotted(unit.packageDeclaration().packageName());
    for (ImportClauseTree clause : unit.imports()) {
      if (!(clause instanceof ImportTree importTree)) {
        continue;
      }
      String name = dotted(importTree.qualifiedIdentifier());
      String key = (importTree.isStatic() ? "static " : "") + name;
      boolean duplicate = !seen.add(key);
      if (isResolvable(importTree, name, currentPackage) && (duplicate || isOwnPackage(importTree, name, currentPackage) || !isUsed(importTree, name))) {
        add(JProblem.Type.UNUSED_IMPORT, "The import " + (name.endsWith(".*") ? name.substring(0, name.length() - 2) : name) + " is never used", importTree);
      }
    }
  }

  /** Like the compiler, only an import that resolves can be reported as unused. */
  private boolean isResolvable(ImportTree importTree, String name, String currentPackage) {
    if (importTree instanceof JavaTree.ImportTreeImpl impl && impl.isModule()) {
      return true;
    }
    boolean onDemand = name.endsWith(".*");
    String qualifier = onDemand ? name.substring(0, name.length() - 2) : name;
    if (onDemand && !importTree.isStatic() && qualifier.equals(currentPackage)) {
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
    boolean onDemand = name.endsWith(".*");
    String qualifier = onDemand ? name.substring(0, name.length() - 2) : name.substring(0, Math.max(0, name.lastIndexOf('.')));
    for (IdentifierTree identifier : identifiers) {
      Symbol symbol = identifier.symbol();
      // a symbol found in the scope of the file does not consult the import
      boolean inScope = !symbol.isUnknown() && symbol.declaration() != null;
      if (!inScope && uses(importTree, name, qualifier, identifier)) {
        return true;
      }
    }
    return false;
  }

  private boolean uses(ImportTree importTree, String name, String qualifier, IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    if (name.endsWith(".*")) {
      return declaredIn(symbol, qualifier, importTree.isStatic());
    }
    if (!identifier.name().equals(name.substring(name.lastIndexOf('.') + 1))) {
      return false;
    }
    if (symbol.isUnknown()) {
      return true;
    }
    return importTree.isStatic() ? isMemberOf(symbol, qualifier) : denotesType(symbol, name);
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

  private static boolean denotesType(Symbol symbol, String name) {
    if (symbol.isTypeSymbol()) {
      return dottedType(symbol).equals(name);
    }
    return symbol.isMethodSymbol() && "<init>".equals(symbol.name()) && symbol.owner() instanceof Symbol.TypeSymbol owner && dottedType(owner).equals(name);
  }

  private static boolean declaredIn(Symbol symbol, String qualifier, boolean isStatic) {
    if (symbol.isUnknown()) {
      return false;
    }
    if (isStatic) {
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
