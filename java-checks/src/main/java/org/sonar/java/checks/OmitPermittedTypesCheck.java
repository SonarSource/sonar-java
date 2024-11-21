/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S6217")
public class OmitPermittedTypesCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this redundant permitted list.";

  private final List<ClassTree> sealedClassesInFile = new ArrayList<>();
  private final Set<Type> typesDeclaredInFile = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.permitsKeyword() != null) {
      sealedClassesInFile.add(classTree);
    }
    typesDeclaredInFile.add(classTree.symbol().type());
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    for (ClassTree sealedClass : sealedClassesInFile) {
      ListTree<TypeTree> permittedTypes = sealedClass.permittedTypes();
      if (permittedTypes.stream().map(TypeTree::symbolType).allMatch(typesDeclaredInFile::contains)) {
        SyntaxToken permitsKeyword = sealedClass.permitsKeyword();
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(permitsKeyword)
          .withMessage(MESSAGE)
          .withSecondaries(permittedTypes.stream().map(t ->
            new JavaFileScannerContext.Location("Permitted type", t))
            .toList())
          .withQuickFix(() -> getQuickFix(permitsKeyword, permittedTypes))
          .report();
      }
    }
    sealedClassesInFile.clear();
    typesDeclaredInFile.clear();
    super.leaveFile(context);
  }

  private static JavaQuickFix getQuickFix(SyntaxToken permitsKeyword, ListTree<TypeTree> typeTrees) {
    return JavaQuickFix.newQuickFix("Remove permitted list")
      .addTextEdit(JavaTextEdit.removeTextSpan(textSpanBetween(
        permitsKeyword, true, QuickFixHelper.nextToken(typeTrees), false)))
      .build();
  }

}
