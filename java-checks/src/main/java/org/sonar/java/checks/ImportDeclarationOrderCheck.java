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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8445")
public class ImportDeclarationOrderCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) tree;

    List<ImportTree> imports = compilationUnit.imports()
      .stream()
      .filter(importTree -> importTree.is(Tree.Kind.IMPORT))
      .map(ImportTree.class::cast)
      .toList();

    analyzeImportOrder(imports);
  }

  private void analyzeImportOrder(List<ImportTree> imports) {
    // if we don't have any module import declarations, we don't raise the issue
    if (imports.stream().noneMatch(ImportTree::isModule)) {
      return;
    }

    if (hasProblematicImport(imports)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(imports.get(0))
        .withMessage("Import declarations should be organized with module imports first, followed by grouped regular imports and grouped static imports.")
        .withQuickFix(() -> createQuickFix(imports))
        .report();
    }
  }

  private static JavaQuickFix createQuickFix(List<ImportTree> imports) {
    // Separate imports by type and determine order
    List<ImportTree> moduleImports = new ArrayList<>();
    List<ImportTree> regularImports = new ArrayList<>();
    List<ImportTree> staticImports = new ArrayList<>();
    boolean regularFirst = true;
    boolean orderDetermined = false;

    for (ImportTree importTree : imports) {
      ImportType type = ImportType.fromTree(importTree);
      switch (type) {
        case MODULE:
          moduleImports.add(importTree);
          break;
        case REGULAR:
          regularImports.add(importTree);
          orderDetermined = true;
          break;
        case STATIC:
          staticImports.add(importTree);
          if (!orderDetermined) {
            regularFirst = false;
            orderDetermined = true;
          }
          break;
      }
    }

    // Build the reorganized import section
    String reorganized = buildReorganizedImports(moduleImports, regularImports, staticImports, regularFirst);

    // Create text edit to replace the entire import section
    ImportTree firstImport = imports.get(0);
    ImportTree lastImport = imports.get(imports.size() - 1);

    return JavaQuickFix.newQuickFix("Reorganize imports")
      .addTextEdit(JavaTextEdit.replaceBetweenTree(firstImport, lastImport, reorganized))
      .build();
  }

  private static String buildReorganizedImports(List<ImportTree> moduleImports, List<ImportTree> regularImports,
                                                 List<ImportTree> staticImports, boolean regularFirst) {
    StringBuilder reorganized = new StringBuilder();

    // Add module imports first
    for (ImportTree importTree : moduleImports) {
      reorganized.append(getImportText(importTree)).append("\n");
    }
    reorganized.append("\n");
    // Add regular and static imports in the determined order
    if (regularFirst) {
      appendImportGroup(reorganized, regularImports, !staticImports.isEmpty());
      appendImportGroup(reorganized, staticImports, false);
    } else {
      appendImportGroup(reorganized, staticImports, !regularImports.isEmpty());
      appendImportGroup(reorganized, regularImports, false);
    }

    return reorganized.toString().trim();
  }

  private static void appendImportGroup(StringBuilder builder, List<ImportTree> imports, boolean addBlankLineAfter) {
    for (ImportTree importTree : imports) {
      builder.append(getImportText(importTree)).append("\n");
    }
    if (addBlankLineAfter) {
      builder.append("\n");
    }
  }

  private static String getImportText(ImportTree importTree) {
    // Build the import statement from the tree structure
    StringBuilder sb = new StringBuilder("import ");
    if (importTree.isModule()) {
      sb.append("module ");
    } else if (importTree.isStatic()) {
      sb.append("static ");
    }
    sb.append(ExpressionsHelper.concatenate((ExpressionTree) importTree.qualifiedIdentifier())).append(";");
    return sb.toString();
  }

  /**
   * Checks if there is an import that violates the grouping rules.
   * Returns false if all imports are properly organized.
   */
  @VisibleForTesting
  static boolean hasProblematicImport(List<ImportTree> imports) {
    boolean seenRegular = false;
    boolean seenStatic = false;
    boolean seenStaticAfterRegular = false;
    boolean seenRegularAfterStatic = false;

    for (ImportTree importTree : imports) {
      ImportType type = ImportType.fromTree(importTree);
      switch (type) {
        case MODULE:
          if (seenRegular || seenStatic) {
            return true;
          }

          break;

        case REGULAR:
          if (seenStaticAfterRegular) {
            //regular -> static -> regular
            return true;
          }

          seenRegular = true;
          seenRegularAfterStatic |= seenStatic;
          break;

        case STATIC:
          if (seenRegularAfterStatic) {
            //static -> regular -> static
            return true;
          }

          seenStatic = true;
          seenStaticAfterRegular |= seenRegular;
          break;
      }
    }

    return false;
  }

  enum ImportType {
    MODULE, REGULAR, STATIC;

    static ImportType fromTree(ImportTree importTree) {
      if (importTree.isModule()) {
        return MODULE;
      } else if (importTree.isStatic()) {
        return STATIC;
      } else {
        return REGULAR;
      }
    }
  }
}
