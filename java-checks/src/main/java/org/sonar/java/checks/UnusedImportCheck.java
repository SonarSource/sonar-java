/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Rule(
  key = "UnusedImportCheck",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class UnusedImportCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private final Map<String, Integer> lineByImportReference = Maps.newHashMap();
  private final Set<String> pendingImports = Sets.newHashSet();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.IMPORT_DECLARATION);
    subscribeTo(JavaGrammar.CLASS_TYPE);
    subscribeTo(JavaGrammar.CREATED_NAME);
    subscribeTo(JavaGrammar.ANNOTATION);
  }

  @Override
  public void visitFile(AstNode astNode) {
    lineByImportReference.clear();
    pendingImports.clear();
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.IMPORT_DECLARATION)) {
      if (!isStaticImport(node) && !isImportOnDemand(node)) {
        String reference = merge(node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER));
        lineByImportReference.put(reference, node.getTokenLine());
        pendingImports.add(reference);
      }
    } else {
      String reference = getReference(node);
      updatePendingImports(reference);
    }
  }

  @Override
  public void leaveFile(AstNode node) {
    for (String pendingImport : pendingImports) {
      getContext().createLineViolation(this, "Remove this unused import '" + pendingImport + "'.", lineByImportReference.get(pendingImport));
    }
  }

  private void updatePendingImports(String reference) {
    if (!isFullyQualified(reference)) {
      Iterator<String> it = pendingImports.iterator();
      while (it.hasNext()) {
        String pendingImport = it.next();
        if (pendingImport.endsWith(reference)) {
          it.remove();
        }
      }
    }
  }

  private static boolean isFullyQualified(String reference) {
    return reference.indexOf('.') != -1;
  }

  private static String getReference(AstNode node) {
    if (node.is(JavaGrammar.ANNOTATION)) {
      node = node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    }

    StringBuilder sb = new StringBuilder();
    for (AstNode child : node.getChildren(JavaTokenType.IDENTIFIER)) {
      sb.append(child.getTokenOriginalValue());
      sb.append('.');
    }
    sb.deleteCharAt(sb.length() - 1);

    return sb.toString();
  }

  private static boolean isStaticImport(AstNode node) {
    return node.hasDirectChildren(JavaKeyword.STATIC);
  }

  private static boolean isImportOnDemand(AstNode node) {
    return node.hasDirectChildren(JavaPunctuator.STAR);
  }

  private static String merge(AstNode node) {
    StringBuilder sb = new StringBuilder();

    for (Token token : node.getTokens()) {
      sb.append(token.getOriginalValue());
    }

    return sb.toString();
  }

  @Override
  public void visitToken(Token token) {
    if (token.hasTrivia()) {
      for (Trivia trivia : token.getTrivia()) {
        updatePendingImportsForComments(trivia.getToken().getOriginalValue());
      }
    }
  }

  private void updatePendingImportsForComments(String comment) {
    Iterator<String> it = pendingImports.iterator();
    while (it.hasNext()) {
      String pendingImport = it.next();

      if (comment.contains(extractClassName(pendingImport))) {
        it.remove();
      }
    }
  }

  private static String extractClassName(String reference) {
    int lastIndexOfDot = reference.lastIndexOf('.');
    return lastIndexOfDot == -1 ? reference : reference.substring(lastIndexOfDot + 1);
  }

}
