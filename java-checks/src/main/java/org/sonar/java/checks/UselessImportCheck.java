/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "UselessImportCheck", repositoryKey = "squid")
@Rule(key = "S1128")
public class UselessImportCheck extends IssuableSubscriptionVisitor {

  private static final Pattern COMPILER_WARNING = Pattern.compile("The import ([$\\w]+(\\.[$\\w]+)*+) is never used");
  private static final Pattern NON_WORDS_CHARACTERS = Pattern.compile("\\W+");
  private static final Pattern JAVADOC_REFERENCE = Pattern.compile("\\{@link[^\\}]*\\}|(@see|@throws)[^\n]*\n");

  private String currentPackage = "";

  private final Set<String> allImports = new HashSet<>();
  private final Set<String> duplicatedImports = new HashSet<>();
  private final Set<String> usedInJavaDoc = new HashSet<>();


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.TRIVIA, Tree.Kind.PACKAGE, Tree.Kind.IMPORT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.PACKAGE)) {
      currentPackage = ExpressionsHelper.concatenate(((PackageDeclarationTree) tree).packageName());
    } else {
      handleImportTree((ImportTree) tree);
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    handleWarnings((DefaultJavaFileScannerContext) context);
    allImports.clear();
    duplicatedImports.clear();
    usedInJavaDoc.clear();
    currentPackage = "";
  }

  private void handleWarnings(DefaultJavaFileScannerContext context) {
    List<JWarning> warnings = ((CompilationUnitTreeImpl) context.getTree()).warnings().getOrDefault(JWarning.Type.UNUSED_IMPORT, Collections.emptyList());
    for (JWarning warning : warnings) {
      Matcher matcher = COMPILER_WARNING.matcher(warning.getMessage());
      Optional<String> fqn = matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
      fqn.ifPresent(importName -> {
        if (!usedInJavaDoc.contains(importName) && !importName.startsWith("java.lang")) {
          String message;
          if (duplicatedImports.contains(importName)) {
            message = "Remove this duplicated import.";
          } else if (importName.startsWith(currentPackage)) {
            message = "Remove this unnecessary import: same package classes are always implicitly imported.";
          } else {
            message = "Remove this unused import '" + importName + "'.";
          }
          context.reportIssue(this, warning, message);
        }
      });
    }
  }

  private void handleImportTree(ImportTree importTree) {
    String importName = ExpressionsHelper.concatenate(((ExpressionTree) importTree.qualifiedIdentifier()));
    if (allImports.contains(importName)) {
      duplicatedImports.add(importName);
    } else {
      allImports.add(importName);
    }
    if (isJavaLangImport(importName)) {
      context.reportIssue(this, importTree, "Remove this unnecessary import: java.lang classes are always implicitly imported.");
    }
  }

  private static boolean isJavaLangImport(String reference) {
    return reference.startsWith("java.lang.") && reference.indexOf('.', "java.lang.".length()) == -1;
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    String comment = syntaxTrivia.comment();
    if (!comment.startsWith("/**")) {
      return;
    }
    Matcher matcher = JAVADOC_REFERENCE.matcher(comment);
    while (matcher.find()) {
      String line = matcher.group(0);
      Set<String> words = NON_WORDS_CHARACTERS.splitAsStream(line)
        .filter(w -> !w.isEmpty())
        .collect(Collectors.toSet());

      if (!words.isEmpty()) {
        usedInJavaDoc.addAll(allImports.stream().filter(i -> words.contains(extractLastClassName(i)))
          .collect(Collectors.toSet()));
      }
    }
  }

  private static String extractLastClassName(String reference) {
    int lastIndexOfDot = reference.lastIndexOf('.');
    return lastIndexOfDot == -1 ? reference : reference.substring(lastIndexOfDot + 1);
  }
}
