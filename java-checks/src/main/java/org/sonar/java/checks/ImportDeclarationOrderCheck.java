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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8445")
public class ImportDeclarationOrderCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) tree;

    List<ImportTree> imports = new ArrayList<>();
    // Collect all import statements
    compilationUnit.imports()
      .stream()
      .filter(importTree -> importTree.is(Tree.Kind.IMPORT))
      .map(ImportTree.class::cast)
      .forEach(imports::add);

    analyzeImportOrder(imports);
  }

  private void analyzeImportOrder(List<ImportTree> imports) {
    if (imports.size() <= 1) {
      return;
    }

    ImportType previousType = ImportType.SENTINEL_IMPORT;
    for (ImportTree importTree : imports) {
      ImportType currentType = classifyImport(importTree);

      if (currentType.ordinal() < previousType.ordinal()) {
        String message = buildMessage(currentType, previousType);
        reportIssue(importTree.importKeyword(), message);
      }

      previousType = currentType;
    }
  }

  private static String buildMessage(ImportType currentType, ImportType previousType) {
    return String.format("Reorder this %s import to come before %s imports.",
      currentType.getDescription(),
      previousType.getDescription());
  }

  private static ImportType classifyImport(ImportTree importTree) {
    boolean isWildcard = isWildcardImport(importTree);

    if (importTree.isModule()) {
      return ImportType.MODULE_IMPORT;
    }

    if (importTree.isStatic()) {
      return isWildcard ? ImportType.STATIC_PACKAGE_IMPORT : ImportType.STATIC_SINGLE_TYPE_IMPORT;
    }

    return isWildcard ? ImportType.PACKAGE_IMPORT : ImportType.SINGLE_TYPE_IMPORT;
  }

  private static boolean isWildcardImport(ImportTree importTree) {
    return "*".equals(importTree.qualifiedIdentifier().lastToken().text());
  }

  enum ImportType {
    SENTINEL_IMPORT("sentinel"),
    MODULE_IMPORT("module"),
    PACKAGE_IMPORT("on-demand package"),
    SINGLE_TYPE_IMPORT("single-type"),
    STATIC_PACKAGE_IMPORT("static on-demand package"),
    STATIC_SINGLE_TYPE_IMPORT("static single-type");

    private final String description;

    ImportType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }
}
