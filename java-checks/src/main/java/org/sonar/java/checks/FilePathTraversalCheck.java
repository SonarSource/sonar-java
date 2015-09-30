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
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S2083",
  name = "Values used in path traversal should be neutralized",
  priority = Priority.CRITICAL,
  tags = {"cwe", "owasp-a4", "sans-top25-risky", "security"})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INPUT_VALIDATION_AND_REPRESENTATION)
@SqaleConstantRemediation("15min")
public class FilePathTraversalCheck extends IssuableSubscriptionVisitor {

  private Set<IdentifierTree> identifiersUsedInFileConstructors;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    identifiersUsedInFileConstructors = Sets.newHashSet();
    super.scanFile(context);
    identifiersUsedInFileConstructors.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    if (newClassTree.symbolType().is("java.io.File")) {
      checkArguments(newClassTree);
    }
  }

  private void checkArguments(NewClassTree constructor) {
    for (ExpressionTree argument : constructor.arguments()) {
      if (argument.symbolType().is("java.lang.String") && argument.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) argument;
        Symbol symbol = identifier.symbol();
        if (symbol.owner().isMethodSymbol() && (singleUse(symbol) || !usedBefore(symbol, constructor))) {
          identifiersUsedInFileConstructors.add(identifier);
          reportIssue(identifier, "\"" + identifier.name() + "\" is provided externally to the method and not sanitized before use.");
        }
      }
    }
  }

  private static boolean singleUse(Symbol symbol) {
    return symbol.usages().size() == 1;
  }

  private boolean usedBefore(Symbol symbol, Tree tree) {
    SyntaxToken targetToken = FirstSyntaxTokenFinder.firstSyntaxToken(tree);
    for (IdentifierTree identifier : symbol.usages()) {
      if (isBeforeToken(identifier.identifierToken(), targetToken) && !identifiersUsedInFileConstructors.contains(identifier)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isBeforeToken(SyntaxToken token, SyntaxToken lastToken) {
    int lastTokenLine = lastToken.line();
    int tokenLine = token.line();
    return tokenLine < lastTokenLine || (tokenLine == lastTokenLine && lastToken.column() > token.column());
  }

}
