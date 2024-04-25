/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree.CompilationUnitTreeImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;
import org.sonarsource.analyzer.commons.quickfixes.QuickFix;
import org.sonarsource.analyzer.commons.quickfixes.TextEdit;

@DeprecatedRuleKey(ruleKey = "UselessImportCheck", repositoryKey = "squid")
@Rule(key = "S1128")
public class UselessImportCheck extends IssuableSubscriptionVisitor {

  private static final Pattern COMPILER_WARNING = Pattern.compile("The import ([$\\w]+(\\.[$\\w]+)*+) is never used");
  private static final Pattern NON_WORDS_CHARACTERS = Pattern.compile("\\W+");
  private static final Pattern JAVADOC_REFERENCE = Pattern.compile("\\{@link[^\\}]*\\}|(@see|@throws)[^\n]*\n");

  private String currentPackage = "";
  private final List<ImportTree> imports = new ArrayList<>();
  private final Map<String, String> importsNames = new HashMap<>();
  private final Set<String> duplicatedImports = new HashSet<>();
  private final Set<String> usedInJavaDoc = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.TRIVIA, Tree.Kind.COMPILATION_UNIT, Tree.Kind.PACKAGE, Tree.Kind.IMPORT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      ((CompilationUnitTree) tree).imports()
        .stream()
        .filter(importTree -> importTree.is(Tree.Kind.IMPORT))
        .map(ImportTree.class::cast)
        .forEach(imports::add);
      importsNames.clear();
      duplicatedImports.clear();
      usedInJavaDoc.clear();
      currentPackage = "";
    } else if (tree.is(Tree.Kind.PACKAGE)) {
      currentPackage = ExpressionsHelper.concatenate(((PackageDeclarationTree) tree).packageName());
    } else {
      handleImportTree((ImportTree) tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      handleWarnings(((CompilationUnitTreeImpl) tree).warnings(JProblem.Type.UNUSED_IMPORT));
      imports.clear();
    }
  }

  private void handleWarnings(List<JWarning> warnings) {
    for (JWarning warning : warnings) {
      Matcher matcher = COMPILER_WARNING.matcher(warning.message());
      Optional<String> fqn = matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
      fqn.ifPresent(importName -> {
        if (!usedInJavaDoc.contains(importName) && !importName.startsWith("java.lang")) {
          String message;
          if (duplicatedImports.contains(importName)) {
            message = "Remove this duplicated import.";
          } else if (isImportFromSamePackage(importName, warning.syntaxTree())) {
            message = "Remove this unnecessary import: same package classes are always implicitly imported.";
          } else {
            message = "Remove this unused import '" + importName + "'.";
          }
          ImportTree reportTree = (ImportTree) warning.syntaxTree();
          QuickFixHelper.newIssue(context)
            .forRule(this)
            .onTree(reportTree.qualifiedIdentifier())
            .withMessage(message)
            .withQuickFix(() -> quickFix(reportTree, imports))
            .report();
        }
      });
    }
  }

  private boolean isImportFromSamePackage(String importName, Tree tree) {
    // ECJ warning message does not contain the ".*" in case of star import, we have to find out if we are in this case.
    // Defensive programming, the syntax tree of the warning should always be an ImportTree.
    if (tree.is(Tree.Kind.IMPORT)) {
      Tree qualifiedIdentifier = ((ImportTree) tree).qualifiedIdentifier();
      // Defensive programming, the qualifiedIdentifier should always be a MemberSelectTree.
      if (qualifiedIdentifier.is(Tree.Kind.MEMBER_SELECT) &&
        "*".equals(((MemberSelectExpressionTree) qualifiedIdentifier).identifier().name())) {
        return importName.equals(currentPackage);
      }
    }

    return importName.substring(0, importName.lastIndexOf(".")).equals(currentPackage);
  }

  private void handleImportTree(ImportTree importTree) {
    String importName = ExpressionsHelper.concatenate(((ExpressionTree) importTree.qualifiedIdentifier()));
    if (importsNames.containsKey(importName)) {
      duplicatedImports.add(importName);
    } else {
      importsNames.put(importName, extractLastClassName(importName));
    }
    if (isJavaLangImport(importName)) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(importTree.qualifiedIdentifier())
        .withMessage("Remove this unnecessary import: java.lang classes are always implicitly imported.")
        .withQuickFix(() -> quickFix(importTree, imports))
        .report();
    }
  }

  private static String extractLastClassName(String reference) {
    int lastIndexOfDot = reference.lastIndexOf('.');
    return lastIndexOfDot == -1 ? reference : reference.substring(lastIndexOfDot + 1);
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
        importsNames.forEach((fullyQualifiedName, name) -> {
          if (words.contains(name)) {
            usedInJavaDoc.add(fullyQualifiedName);
          }
        });
      }
    }
  }

  private static QuickFix quickFix(ImportTree importTree, List<ImportTree> imports) {
    int indexOfImport = imports.indexOf(importTree);
    boolean isLastImport = indexOfImport == imports.size() - 1;
    QuickFix.Builder quickFix = QuickFix.newQuickFix("Remove the %simport", importTree.isStatic() ? "static " : "");
    if (imports.size() == 1) {
      // single import not used...
      quickFix.addTextEdit(AnalyzerMessage.removeTree(importTree));
    } else if (!isLastImport) {
      ImportTree nextImport = imports.get(indexOfImport + 1);
      quickFix.addTextEdit(TextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(importTree, true, nextImport, false)));
    } else {
      // last import
      ImportTree previousImport = imports.get(indexOfImport - 1);
      quickFix.addTextEdit(TextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(previousImport, false, importTree, true)));
    }
    return quickFix.build();
  }
}
