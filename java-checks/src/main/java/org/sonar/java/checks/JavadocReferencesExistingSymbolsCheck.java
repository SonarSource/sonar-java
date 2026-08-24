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

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9360")
public class JavadocReferencesExistingSymbolsCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure this reference is valid.";
  private static final Pattern SEE_TAG_PATTERN = Pattern.compile("@see\\s++(\\S++)");
  private static final Pattern SEE_ANCHOR_PATTERN = Pattern.compile("\\{@see\\s++([^\\s}]+)");

  private String currentPackage = "";

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    Tree tree = context.getTree();
    if (tree != null && tree.is(Tree.Kind.COMPILATION_UNIT)) {
      Tree pkg = ((CompilationUnitTree) tree).packageDeclaration();
      if (pkg != null) {
        currentPackage = pkg.toString();
      }
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

    for (String reference : extractSeeReferences(syntaxTrivia.comment())) {
      String resolvedRef = resolveReference(reference);
      if (resolvedRef == null) {
        continue;
      }

      Type type = sema.getClassType(resolvedRef);
      if (type != null && !type.isUnknown()) {
        continue;
      }

      addIssue(syntaxTrivia.range().end().line(), MESSAGE);
      return;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    // intentionally empty - no cleanup needed
  }

  static List<String> extractSeeReferences(String javadocText) {
    if (javadocText == null || javadocText.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> references = new java.util.ArrayList<>();
    Matcher matcher = SEE_TAG_PATTERN.matcher(javadocText);
    while (matcher.find()) {
      String ref = matcher.group(1);
      if (!ref.startsWith("http://") && !ref.startsWith("https://")) {
        references.add(ref);
      }
    }

    Matcher anchorMatcher = SEE_ANCHOR_PATTERN.matcher(javadocText);
    while (anchorMatcher.find()) {
      String ref = anchorMatcher.group(1);
      if (!ref.startsWith("http://") && !ref.startsWith("https://")) {
        references.add(ref);
      }
    }

    return references;
  }

  String resolveReference(String reference) {
    if (reference == null || reference.isEmpty()) {
      return null;
    }

    // Skip method references (starting with #)
    if (reference.startsWith("#")) {
      return null;
    }

    // Remove method signature if present (e.g., "MyClass#myMethod()")
    if (reference.contains("(")) {
      int parenIdx = reference.indexOf('(');
      reference = reference.substring(0, parenIdx);
    }

    // Remove member reference if present (e.g., "MyClass#myField")
    int hashIdx = reference.indexOf('#');
    if (hashIdx > 0) {
      reference = reference.substring(0, hashIdx);
    }

    // If already fully qualified, use as-is
    if (reference.contains(".")) {
      return reference;
    }

    // Skip simple method names (e.g., "existingMethod()")
    // These are handled by a different rule
    if (!reference.contains(".")) {
      return null;
    }

    // Resolve relative reference against current package
    if (!currentPackage.isEmpty()) {
      return currentPackage + "." + reference;
    }

    return reference;
  }
}
