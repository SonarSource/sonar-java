/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Rule(
  key = "UselessImportCheck",
  name = "Useless imports should be removed",
  tags = {"unused"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("10min")
public class UselessImportCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Map<String, Integer> lineByImportReference = Maps.newHashMap();
  private final Set<String> pendingImports = Sets.newHashSet();
  private final Set<String> pendingReferences = Sets.newHashSet();

  private String currentPackage;
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    CompilationUnitTree cut = context.getTree();
    ExpressionTree packageName = getPackageName(cut);

    pendingReferences.clear();
    lineByImportReference.clear();
    pendingImports.clear();

    currentPackage = concatenate(packageName);
    for (ImportClauseTree importClauseTree : cut.imports()) {
      ImportTree importTree = null;

      if (importClauseTree.is(Tree.Kind.IMPORT)) {
        importTree = (ImportTree) importClauseTree;
      }

      if (importTree != null && !importTree.isStatic()) {
        String importName = concatenate((ExpressionTree) importTree.qualifiedIdentifier());
        if ("java.lang.*".equals(importName)) {
          context.addIssue(importTree, this, "Remove this unnecessary import: java.lang classes are always implicitly imported.");
        } else if (isImportFromSamePackage(importName)) {
          context.addIssue(importTree, this, "Remove this unnecessary import: same package classes are always implicitly imported.");
        } else if (!isImportOnDemand(importName)) {
          if (isJavaLangImport(importName)) {
            context.addIssue(importTree, this, "Remove this unnecessary import: java.lang classes are always implicitly imported.");
          } else if (isDuplicatedImport(importName)) {
            context.addIssue(importTree, this, "Remove this duplicated import.");
          } else {
            lineByImportReference.put(importName, FirstSyntaxTokenFinder.firstSyntaxToken(importTree).line());
            pendingImports.add(importName);
          }
        }
      }
    }
    //check references
    scan(cut);
    //check references from comments.
    new CommentVisitor().checkImportsFromComments(cut, pendingImports);
    leaveFile();
  }

  private static ExpressionTree getPackageName(CompilationUnitTree cut) {
    return cut.packageDeclaration() != null ? cut.packageDeclaration().packageName() : null;
  }

  private static boolean isImportOnDemand(String name) {
    return name.endsWith("*");
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    //do not scan imports and package name identifiers.
    if (tree.packageDeclaration() != null) {
      scan(tree.packageDeclaration().annotations());
    }
    scan(tree.types());
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    scan(tree.annotations());
    pendingReferences.add(tree.name());
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    scan(tree.annotations());
    pendingReferences.add(concatenate(tree));
    //Don't visit identifiers of a member select expression.
    if (!tree.expression().is(Tree.Kind.IDENTIFIER)) {
      scan(tree.expression());
    }
  }

  private boolean isImportFromSamePackage(String reference) {
    String importName = reference;
    if (isImportOnDemand(reference)) {
      //strip out .* to compare length with current package.
      importName = reference.substring(0, reference.length() - 2);
    }
    return !currentPackage.isEmpty() &&
        importName.startsWith(currentPackage) &&
        (importName.length() == currentPackage.length() || reference.substring(reference.indexOf(currentPackage)).startsWith("."));
  }

  private boolean isDuplicatedImport(String reference) {
    return pendingImports.contains(reference);
  }

  private static boolean isJavaLangImport(String reference) {
    return reference.startsWith("java.lang.") && reference.indexOf('.', "java.lang.".length()) == -1;
  }

  public void leaveFile() {
    for (String reference : pendingReferences) {
      updatePendingImports(reference);
    }

    for (String pendingImport : pendingImports) {
      context.addIssue(lineByImportReference.get(pendingImport), this, "Remove this unused import '" + pendingImport + "'.");
    }
  }

  private void updatePendingImports(String reference) {
    String firstClassReference = reference;
    if (isFullyQualified(firstClassReference)) {
      firstClassReference = extractFirstClassName(firstClassReference);
    }
    Iterator<String> it = pendingImports.iterator();
    while (it.hasNext()) {
      String pendingImport = it.next();
      if (pendingImport.endsWith("." + firstClassReference)) {
        it.remove();
      }
    }
  }

  private static boolean isFullyQualified(String reference) {
    return reference.indexOf('.') != -1;
  }

  private static String extractFirstClassName(String reference) {
    int firstIndexOfDot = reference.indexOf('.');
    return firstIndexOfDot == -1 ? reference : reference.substring(0, firstIndexOfDot);
  }


  private static String concatenate(@Nullable ExpressionTree tree) {
    if (tree == null) {
      return "";
    }
    Deque<String> pieces = new LinkedList<>();

    ExpressionTree expr = tree;
    while (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
      pieces.push(mse.identifier().name());
      pieces.push(".");
      expr = mse.expression();
    }
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree idt = (IdentifierTree) expr;
      pieces.push(idt.name());
    }

    StringBuilder sb = new StringBuilder();
    for (String piece : pieces) {
      sb.append(piece);
    }
    return sb.toString();
  }

  private static class CommentVisitor extends SubscriptionBaseVisitor {
    private Set<String> pendingImports;

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.TRIVIA);
    }

    public void checkImportsFromComments(CompilationUnitTree cut, Set<String> pendingImports) {
      this.pendingImports = pendingImports;
      scanTree(cut);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      updatePendingImportsForComments(syntaxTrivia.comment());
    }

    private void updatePendingImportsForComments(String comment) {
      Iterator<String> it = pendingImports.iterator();
      while (it.hasNext()) {
        String pendingImport = it.next();
        if (comment.contains(extractLastClassName(pendingImport))) {
          it.remove();
        }
      }
    }

    private static String extractLastClassName(String reference) {
      int lastIndexOfDot = reference.lastIndexOf('.');
      return lastIndexOfDot == -1 ? reference : reference.substring(lastIndexOfDot + 1);
    }

  }
}
