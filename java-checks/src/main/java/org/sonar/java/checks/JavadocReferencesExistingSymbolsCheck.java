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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9360")
public class JavadocReferencesExistingSymbolsCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure this reference is valid.";
  private static final Pattern SEE_TAG_PATTERN = Pattern.compile("@see\\s++(\\S++)");
  private static final Pattern LINK_TAG_PATTERN = Pattern.compile("\\{@link(?:plain)?\\s++([^\\s}]+)");
  private static final String JAVA_LANG_PREFIX = "java.lang.";

  private String currentPackage = "";
  private final Map<String, String> importedSimpleNames = new HashMap<>();
  private final Set<String> declaredTypeSimpleNames = new HashSet<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    currentPackage = "";
    importedSimpleNames.clear();
    declaredTypeSimpleNames.clear();
    Tree tree = context.getTree();
    if (tree != null && tree.is(Tree.Kind.COMPILATION_UNIT)) {
      CompilationUnitTree cut = (CompilationUnitTree) tree;
      PackageDeclarationTree pkg = cut.packageDeclaration();
      if (pkg != null) {
        currentPackage = ExpressionsHelper.concatenate(pkg.packageName());
      }
      collectImports(cut);
      collectDeclaredTypes(cut);
    }
  }

  private void collectImports(CompilationUnitTree cut) {
    cut.imports().stream()
      .filter(importClause -> importClause.is(Tree.Kind.IMPORT))
      .map(ImportTree.class::cast)
      .filter(importTree -> !importTree.isStatic())
      .forEach(importTree -> {
        String fqn = ExpressionsHelper.concatenate((ExpressionTree) importTree.qualifiedIdentifier());
        if (!fqn.endsWith(".*")) {
          int lastDot = fqn.lastIndexOf('.');
          String simpleName = lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;
          importedSimpleNames.put(simpleName, fqn);
        }
      });
  }

  private void collectDeclaredTypes(CompilationUnitTree cut) {
    for (Tree typeTree : cut.types()) {
      collectDeclaredTypesFromTree(typeTree);
    }
  }

  private void collectDeclaredTypesFromTree(Tree tree) {
    if (!tree.is(Tree.Kind.CLASS_KINDS.toArray(new Tree.Kind[0]))) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() != null) {
      declaredTypeSimpleNames.add(classTree.simpleName().name());
    }
    for (Tree member : classTree.members()) {
      collectDeclaredTypesFromTree(member);
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRIVIA);
  }

  @Override
  public void visitNode(Tree tree) {
    // intentionally empty - we only care about trivia
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    if (!syntaxTrivia.isComment(SyntaxTrivia.CommentKind.JAVADOC)) {
      return;
    }

    Object semanticModel = context.getSemanticModel();
    if (semanticModel == null) {
      return;
    }

    Sema sema = (Sema) semanticModel;

    if (hasInvalidReference(sema, syntaxTrivia.comment())) {
      addIssue(syntaxTrivia.range().end().line(), MESSAGE);
    }
  }

  private boolean hasInvalidReference(Sema sema, String javadocText) {
    for (String reference : extractSeeReferences(javadocText)) {
      String typeName = stripMemberReference(reference);
      if (typeName != null && isUnresolvableReference(sema, typeName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Strips method/member references from a Javadoc reference and returns the type name part,
   * or null if the reference is a method-only reference (e.g., "#myMethod").
   */
  static String stripMemberReference(String reference) {
    // Skip method references (starting with #)
    if (reference.startsWith("#")) {
      return null;
    }

    String result = reference;

    // Remove method signature if present (e.g., "MyClass#myMethod()")
    int parenIdx = result.indexOf('(');
    if (parenIdx >= 0) {
      result = result.substring(0, parenIdx);
    }

    // Remove member reference if present (e.g., "MyClass#myField")
    int hashIdx = result.indexOf('#');
    if (hashIdx > 0) {
      result = result.substring(0, hashIdx);
    }

    return result;
  }

  private boolean isUnresolvableReference(Sema sema, String typeName) {
    // If already fully qualified (contains dots), check directly and with inner-class $ notation
    if (typeName.contains(".")) {
      if (!isUnknownType(sema, typeName)) {
        return false;
      }
      // Try interpreting dots as inner class separators (e.g., Outer.Inner -> pkg.Outer$Inner)
      return isUnresolvableInnerClassReference(sema, typeName);
    }

    // Simple name: try imports, then java.lang, then current package, then locally declared types
    String importedFqn = importedSimpleNames.get(typeName);
    if (importedFqn != null) {
      return isUnknownType(sema, importedFqn);
    }

    if (!isUnknownType(sema, JAVA_LANG_PREFIX + typeName)) {
      return false;
    }

    if (!currentPackage.isEmpty() && !isUnknownType(sema, currentPackage + "." + typeName)) {
      return false;
    }

    // Check if it matches a type declared in the current file (handles inner types)
    return !declaredTypeSimpleNames.contains(typeName);
  }

  private boolean isUnresolvableInnerClassReference(Sema sema, String typeName) {
    // For dotted references like "Outer.Inner", try replacing dots with $ for inner class resolution
    // Try with current package prefix and $ notation
    int lastDot = typeName.lastIndexOf('.');
    while (lastDot > 0) {
      String withDollar = typeName.substring(0, lastDot) + "$" + typeName.substring(lastDot + 1);
      if (!isUnknownType(sema, withDollar)) {
        return false;
      }
      if (!currentPackage.isEmpty() && !isUnknownType(sema, currentPackage + "." + withDollar)) {
        return false;
      }
      // Try imported prefix
      String outerName = typeName.substring(0, lastDot);
      int outerLastDot = outerName.lastIndexOf('.');
      String outerSimple = outerLastDot >= 0 ? outerName.substring(outerLastDot + 1) : outerName;
      String importedOuter = importedSimpleNames.get(outerSimple);
      if (importedOuter != null) {
        String resolvedInner = importedOuter + "$" + typeName.substring(lastDot + 1);
        if (!isUnknownType(sema, resolvedInner)) {
          return false;
        }
      }
      lastDot = typeName.lastIndexOf('.', lastDot - 1);
    }
    return true;
  }

  private static boolean isUnknownType(Sema sema, String fullyQualifiedName) {
    Type type = sema.getClassType(fullyQualifiedName);
    return type == null || type.isUnknown();
  }

  @Override
  public void leaveNode(Tree tree) {
    // intentionally empty - no cleanup needed
  }

  static List<String> extractSeeReferences(String javadocText) {
    List<String> references = new ArrayList<>();

    Matcher matcher = SEE_TAG_PATTERN.matcher(javadocText);
    while (matcher.find()) {
      String ref = matcher.group(1);
      if (!ref.startsWith("http://") && !ref.startsWith("https://") && !ref.startsWith("<") && !ref.startsWith("\"")) {
        references.add(ref);
      }
    }

    Matcher anchorMatcher = LINK_TAG_PATTERN.matcher(javadocText);
    while (anchorMatcher.find()) {
      String ref = anchorMatcher.group(1);
      if (!ref.startsWith("http://") && !ref.startsWith("https://")) {
        references.add(ref);
      }
    }

    return references;
  }
}
