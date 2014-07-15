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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Rule(
  key = "UselessImportCheck",
  priority = Priority.MINOR,
  tags={"unused"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class UselessImportCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private final Map<String, Integer> lineByImportReference = Maps.newHashMap();
  private final Set<String> pendingImports = Sets.newHashSet();
  private final Set<String> pendingReferences = Sets.newHashSet();

  private String currentPackage;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.PACKAGE_DECLARATION);
    subscribeTo(JavaGrammar.IMPORT_DECLARATION);
    subscribeTo(JavaGrammar.CLASS_TYPE);
    subscribeTo(JavaGrammar.CREATED_NAME);
    subscribeTo(JavaGrammar.ANNOTATION);
    subscribeTo(JavaKeyword.THROWS);
    subscribeTo(JavaGrammar.QUALIFIED_IDENTIFIER);
  }

  @Override
  public void visitFile(AstNode astNode) {
    pendingReferences.clear();
    lineByImportReference.clear();
    pendingImports.clear();

    currentPackage = "";
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.PACKAGE_DECLARATION)) {
      currentPackage = mergeIdentifiers(node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER));
    } else if (node.is(JavaGrammar.IMPORT_DECLARATION)) {
      if (!isStaticImport(node)) {
        String reference = mergeIdentifiers(node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER));

        if ("java.lang".equals(reference)) {
          getContext().createLineViolation(this, "Remove this unnecessary import: java.lang classes are always implicitly imported.", node);
        } else if (isImportFromSamePackage(reference)) {
          getContext().createLineViolation(this, "Remove this unnecessary import: same package classes are always implicitly imported.", node);
        } else if (!isImportOnDemand(node)) {
          if (isJavaLangImport(reference)) {
            getContext().createLineViolation(this, "Remove this unnecessary import: java.lang classes are always implicitly imported.", node);
          } else if (isDuplicatedImport(reference)) {
            getContext().createLineViolation(this, "Remove this duplicated import.", node);
          } else {
            lineByImportReference.put(reference, node.getTokenLine());
            pendingImports.add(reference);
          }
        }
      }
    } else if (!node.getParent().is(JavaGrammar.IMPORT_DECLARATION)) {
      pendingReferences.addAll(getReferences(node));
    }
  }

  @Override
  public void leaveFile(AstNode node) {
    for (String reference : pendingReferences) {
      updatePendingImports(reference);
    }

    for (String pendingImport : pendingImports) {
      getContext().createLineViolation(this, "Remove this unused import '" + pendingImport + "'.", lineByImportReference.get(pendingImport));
    }
  }

  private static boolean isJavaLangImport(String reference) {
    return reference.startsWith("java.lang.") && reference.indexOf('.', "java.lang.".length()) == -1;
  }

  private boolean isImportFromSamePackage(String reference) {
    return !currentPackage.isEmpty() &&
      reference.startsWith(currentPackage) &&
      (reference.length() == currentPackage.length() || reference.substring(reference.indexOf(currentPackage)).startsWith("."));
  }

  private boolean isDuplicatedImport(String reference) {
    return pendingImports.contains(reference);
  }

  private void updatePendingImports(String reference) {
    String firstClassReference = reference;
    if (isFullyQualified(firstClassReference)) {
      firstClassReference = extractFirstClassName(firstClassReference);
    }
    Iterator<String> it = pendingImports.iterator();
    while (it.hasNext()) {
      String pendingImport = it.next();
      if (pendingImport.endsWith(firstClassReference)) {
        it.remove();
      }
    }
  }

  private static boolean isFullyQualified(String reference) {
    return reference.indexOf('.') != -1;
  }

  private static Collection<String> getReferences(AstNode node) {
    if (node.is(JavaKeyword.THROWS)) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();

      for (AstNode qualifiedIdentifier : node.getNextSibling().getChildren(JavaGrammar.QUALIFIED_IDENTIFIER)) {
        builder.add(mergeIdentifiers(qualifiedIdentifier));
      }

      return builder.build();
    } else {
      AstNode actualNode = node;
      if (node.is(JavaGrammar.ANNOTATION)) {
        actualNode = node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
      }

      return Collections.singleton(mergeIdentifiers(actualNode));
    }
  }

  private static String mergeIdentifiers(AstNode node) {
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

      if (comment.contains(extractLastClassName(pendingImport))) {
        it.remove();
      }
    }
  }

  private static String extractFirstClassName(String reference) {
    int firstIndexOfDot = reference.indexOf('.');
    return firstIndexOfDot == -1 ? reference : reference.substring(0, firstIndexOfDot);
  }

  private static String extractLastClassName(String reference) {
    int lastIndexOfDot = reference.lastIndexOf('.');
    return lastIndexOfDot == -1 ? reference : reference.substring(lastIndexOfDot + 1);
  }

}
