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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.JWarning;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "UselessImportCheck", repositoryKey = "squid")
@Rule(key = "S1128")
public class UselessImportCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Pattern COMPILER_WARNING = Pattern.compile("The import ([$\\w]+(\\.[$\\w]+)*+) is never used");
  private static final Pattern NON_WORDS_CHARACTERS = Pattern.compile("\\W+");
  private static final Pattern JAVADOC_REFERENCE = Pattern.compile("\\{@link[^\\}]*\\}|(@see|@throws)[^\n]*\n");

  private String currentPackage;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    CompilationUnitTree cut = context.getTree();
    this.context = context;
    currentPackage = ExpressionsHelper.concatenate(getPackageName(cut));

    List<ImportTree> imports = cut.imports().stream()
      .filter(importClauseTree -> importClauseTree.is(Tree.Kind.IMPORT))
      .map(ImportTree.class::cast)
      .collect(Collectors.toList());

    Set<String> duplicatedImports = new HashSet<>();
    Set<String> allImports = new HashSet<>();
    imports.forEach(importTree -> handleImportTree(importTree, allImports, duplicatedImports));

    CommentVisitor commentVisitor = new CommentVisitor(allImports);
    commentVisitor.checkImportsFromComments(cut);
    cut.warnings().getOrDefault(JWarning.Type.UNUSED_IMPORT, Collections.emptyList())
      .forEach(warn -> checkWarnings(warn, commentVisitor.usedInJavaDoc, duplicatedImports));

  }

  private void checkWarnings(JWarning warning, Set<String> excluded, Set<String> duplicated) {
    Matcher matcher = COMPILER_WARNING.matcher(warning.getMessage());
    Optional<String> fqn = matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    fqn.ifPresent(importName -> {
      if (!excluded.contains(importName) && !importName.startsWith("java.lang")) {
        String message;
        if (duplicated.contains(importName)) {
          message = "Remove this duplicated import.";
        } else if (importName.startsWith(currentPackage)) {
          message = "Remove this unnecessary import: same package classes are always implicitly imported.";
        } else {
          message = "Remove this unused import '" + importName + "'.";
        }
        context.addIssue(warning.getStartLine(), this, message);
      }
    });
  }

  private void handleImportTree(ImportTree importTree, Set<String> allImports, Set<String> duplicatedImports) {
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

  private static ExpressionTree getPackageName(CompilationUnitTree cut) {
    return cut.packageDeclaration() != null ? cut.packageDeclaration().packageName() : null;
  }

  private static boolean isJavaLangImport(String reference) {
    return reference.startsWith("java.lang.") && reference.indexOf('.', "java.lang.".length()) == -1;
  }

  private static class CommentVisitor extends SubscriptionVisitor {
    private Set<String> usedInJavaDoc = new HashSet<>();
    private Set<String> allImports;

    public CommentVisitor(Set<String> allImports) {
      this.allImports = allImports;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    public void checkImportsFromComments(CompilationUnitTree cut) {
      scanTree(cut);
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
}
