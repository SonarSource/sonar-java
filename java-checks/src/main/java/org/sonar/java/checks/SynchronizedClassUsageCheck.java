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

import com.google.common.collect.ImmutableMap;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;
import java.util.Map;

@Rule(
  key = "S1149",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SynchronizedClassUsageCheck extends SquidCheck<LexerlessGrammar> {

  private static final Map<String, String> REPLACEMENTS = ImmutableMap.<String, String> builder()
    .put("Vector", "\"ArrayList\" or \"LinkedList\"")
    .put("Hashtable", "\"HashMap\"")
    .put("StringBuffer", "\"StringBuilder\"")
    .put("Stack", "\"Deque\"")
    .build();

  private int lastReportedLine;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_TYPE);
    subscribeTo(JavaGrammar.CREATED_NAME);
    subscribeTo(JavaGrammar.QUALIFIED_IDENTIFIER);
  }

  @Override
  public void visitFile(AstNode astNode) {
    lastReportedLine = -1;
  }

  @Override
  public void visitNode(AstNode node) {
    String className = getClassName(node);

    if (lastReportedLine != node.getTokenLine() && isSynchronizedClass(className) && !isExcluded(node)) {
      getContext().createLineViolation(this, "Replace the synchronized class \"" + className + "\" by an unsynchronized one such as " + REPLACEMENTS.get(className) + ".", node);
      lastReportedLine = node.getTokenLine();
    }
  }

  private static String getClassName(AstNode node) {
    String className;

    if (!node.hasDirectChildren(JavaPunctuator.DOT)) {
      className = node.getTokenOriginalValue();
    } else {
      List<AstNode> identifiers = node.getChildren(JavaTokenType.IDENTIFIER);
      className = identifiers.get(identifiers.size() - 1).getTokenOriginalValue();
    }

    return className;
  }

  private static boolean isSynchronizedClass(String className) {
    return REPLACEMENTS.containsKey(className);
  }

  private static boolean isExcluded(AstNode node) {
    return node.hasAncestor(JavaGrammar.IMPORT_DECLARATION) ||
      isInOverridenMethodSignature(node);
  }

  private static boolean isInOverridenMethodSignature(AstNode node) {
    return isInMethodSignature(node) && isOverridenInClass(node);
  }

  private static boolean isInMethodSignature(AstNode node) {
    return node.is(JavaGrammar.CLASS_TYPE) &&
      node.getParent().is(JavaGrammar.TYPE) &&
      node.getParent().getParent().is(JavaGrammar.MEMBER_DECL, JavaGrammar.FORMAL_PARAMETER_DECLS);
  }

  private static boolean isOverridenInClass(AstNode node) {
    AstNode enclosingClassOrInterface = getFirstAncestor(node, JavaGrammar.MEMBER_DECL, JavaGrammar.INTERFACE_MEMBER_DECL);
    if (!enclosingClassOrInterface.is(JavaGrammar.MEMBER_DECL)) {
      return false;
    }

    for (AstNode modifier : enclosingClassOrInterface.getParent().getChildren(JavaGrammar.MODIFIER)) {
      if (AstNodeTokensMatcher.matches(modifier, "@Override")) {
        return true;
      }
    }

    return false;
  }

  private static AstNode getFirstAncestor(AstNode node, AstNodeType t1, AstNodeType t2) {
    AstNode result = node.getParent();
    while (result != null && !result.is(t1, t2)) {
      result = result.getParent();
    }

    return result;
  }

}
