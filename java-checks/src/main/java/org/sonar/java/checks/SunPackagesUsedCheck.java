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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1191",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SunPackagesUsedCheck extends SquidCheck<LexerlessGrammar> {

  private int lastReportedLine;

  private static final String DEFAULT_EXCLUDE = "";

  @RuleProperty(
      key = "exclude",
      defaultValue = "" + DEFAULT_EXCLUDE)
  public String exclude = DEFAULT_EXCLUDE;
  private String[] excludePackages = null;



  @Override
  public void init() {
    subscribeTo(JavaGrammar.QUALIFIED_IDENTIFIER);
    subscribeTo(JavaGrammar.CLASS_TYPE);
    subscribeTo(JavaGrammar.CREATED_NAME);
  }

  @Override
  public void visitFile(AstNode node) {
    lastReportedLine = -1;
    excludePackages = exclude.split(",");
  }

  @Override
  public void visitNode(AstNode node) {
    String reference = merge(node);
    if (lastReportedLine != node.getTokenLine() && isSunClass(reference) && !isExcluded(reference)) {
      getContext().createLineViolation(this, "Replace this usage of Sun classes by ones from the Java API.", node);
      lastReportedLine = node.getTokenLine();
    }
  }

  private boolean isSunClass(String reference) {
    return reference.startsWith("com.sun.") || reference.startsWith("sun.");
  }

  private String merge(AstNode node) {
    StringBuilder sb = new StringBuilder();
    for (Token token : node.getTokens()) {
      sb.append(token.getOriginalValue());
    }
    return sb.toString();
  }

  private boolean isExcluded(String reference) {
    for(String str: excludePackages) {
      if(!str.isEmpty() && reference.startsWith(str)) {
        return true;
      }
    }
    return false;
  }

}
