/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1191")
public class SunPackagesUsedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private List<Tree> reportedTrees = new ArrayList<>();

  private static final String DEFAULT_EXCLUDE = "";

  @RuleProperty(
      key = "Exclude",
      description = "Comma separated list of Sun packages to be ignored by this rule. Example: sun.misc,sun.security.validator",
      defaultValue = "" + DEFAULT_EXCLUDE)
  public String exclude = DEFAULT_EXCLUDE;
  private String[] excludePackages = null;
  

  @Override
  public void scanFile(JavaFileScannerContext context) {
    reportedTrees.clear();
    excludePackages = exclude.split(",");
    scan(context.getTree());

    if (!reportedTrees.isEmpty()) {
      reportIssueWithSecondaries(context);
    }
  }

  private void reportIssueWithSecondaries(JavaFileScannerContext context) {
    // Sort by line number to ensure consistent primary issue location (first occurrence in source text)
    reportedTrees.sort((t1, t2) -> Integer.compare(
      t1.firstToken().range().start().line(),
      t2.firstToken().range().start().line()
    ));

    List<JavaFileScannerContext.Location> secondaries = reportedTrees.stream()
      .skip(1)
      .map(tree -> new JavaFileScannerContext.Location("Replace also this \"Sun\" reference.", tree))
      .toList();

    int effortToFix = reportedTrees.size();
    context.reportIssue(this, reportedTrees.get(0), "Use classes from the Java API instead of Sun classes.", secondaries, effortToFix);
  }

  @Override
  public void visitImport(ImportTree tree) {
    Tree qualifiedIdentifier = tree.qualifiedIdentifier();
    if (qualifiedIdentifier.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) qualifiedIdentifier;
      String reference = ExpressionsHelper.concatenate(memberSelect);
      if (!isExcluded(reference) && isSunClass(reference)) {
        // For imports, check if we have semantic info by checking if the import's symbol type is resolved
        // In autoscan mode (without bytecode), the symbol's type will be unknown
        var symbol = tree.symbol();
        if (symbol != null && !symbol.isUnknown() && !symbol.type().isUnknown()) {
          // We have full semantic info with bytecode, so this is a real sun.* import
          reportedTrees.add(memberSelect);
        }
        // Without semantic info or bytecode, we can't be sure this is a real sun.* import, so skip it
      }
    }
    super.visitImport(tree);
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    String reference = ExpressionsHelper.concatenate(tree);
    if (!isExcluded(reference) && isSunClass(reference) && isActuallySunPackage(tree)) {
      reportedTrees.add(tree);
    }
  }

  private static boolean isSunClass(String reference) {
    return reference.startsWith("sun.");
  }

  private static boolean isActuallySunPackage(MemberSelectExpressionTree tree) {
    // Check if this is actually a sun.* package reference, not a variable named "sun"
    //
    // Strategy: Find the leftmost identifier in the qualified name chain (e.g., "sun" in "sun.misc.Unsafe")
    // and check if it's a variable/field/parameter. If it is, then this is not a sun.* package reference.

    // Navigate to the leftmost expression in the chain
    ExpressionTree current = tree;
    while (current.is(Tree.Kind.MEMBER_SELECT)) {
      current = ((MemberSelectExpressionTree) current).expression();
    }

    // Now 'current' should be an IdentifierTree representing the leftmost identifier
    if (current.is(Tree.Kind.IDENTIFIER)) {
      var symbol = ((IdentifierTree) current).symbol();
      if (!symbol.isUnknown()) {
        // Check if this symbol is a variable, field, or parameter (not a type or package)
        if (symbol.isVariableSymbol() || symbol.isMethodSymbol()) {
          // This is a variable/method reference like "sun.toString()" where "sun" is a variable
          // The method case handles things like "sun.method().field" where sun is a variable
          return false;
        }

        // If the symbol is a type or package, we need to be more careful.
        // In non-compiling code, variables may be misidentified as packages by the semantic engine.
        // We can detect this by checking if the member select tree's type is resolved.
        if (tree.symbolType().isUnknown()) {
          // Type is not resolved, likely non-compiling code or misidentified variable.
          // Prefer to avoid false positives in these cases.
          return false;
        }

        // If the leftmost symbol is not a variable/method and the type is resolved,
        // then it's likely a package/type reference to sun.*
        return true;
      }
    }

    // If we couldn't determine the leftmost symbol (unknown semantic info),
    // we prefer to avoid false positives.
    // This means we might miss some true sun.* package usages in non-compiling code or AutoScan,
    // but users with proper builds and semantic analysis will still get accurate results.
    return false;
  }

  private boolean isExcluded(String reference) {
    for (String str : excludePackages) {
      if (!str.isEmpty() && reference.startsWith(str)) {
        return true;
      }
    }
    return false;
  }
}
