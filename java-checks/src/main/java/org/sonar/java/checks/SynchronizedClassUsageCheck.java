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
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Map;

@Rule(
  key = "S1149",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SynchronizedClassUsageCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private static final Map<String, String> REPLACEMENTS = ImmutableMap.<String, String> builder()
      .put("Vector", "ArrayList or LinkedList")
      .put("Hashtable", "HashMap")
      .put("StringBuffer", "StringBuilder")
      .build();

  private int lastReportedLine;
  private boolean inImport;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.IMPORT_DECLARATION);
  }

  @Override
  public void visitFile(AstNode astNode) {
    lastReportedLine = -1;
    inImport = false;
  }

  @Override
  public void visitNode(AstNode node) {
    inImport = true;
  }

  @Override
  public void visitToken(Token token) {
    String className = token.getOriginalValue();
    if (!inImport && isSynchronizedClass(className) && lastReportedLine != token.getLine()) {
      getContext().createLineViolation(this, "Replace the synchronized class '" + className + "' by an unsynchronized one such as " + REPLACEMENTS.get(className) + ".", token);
      lastReportedLine = token.getLine();
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    inImport = false;
  }

  private static boolean isSynchronizedClass(String className) {
    return "Vector".equals(className) ||
      "Hashtable".equals(className) ||
      "StringBuffer".equals(className);
  }

}
