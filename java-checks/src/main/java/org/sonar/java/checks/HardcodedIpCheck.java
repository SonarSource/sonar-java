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

import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.BaseTreeVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTreeVisitor;
import org.sonar.java.model.JavaTreeVisitorProvider;
import org.sonar.java.model.LiteralTree;
import org.sonar.java.model.Tree;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(
  key = "S1313",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class HardcodedIpCheck extends SquidCheck<LexerlessGrammar> implements JavaTreeVisitorProvider {

  private static final Matcher IP = Pattern.compile("[^\\d.]*?((?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d|\\.)).*?").matcher("");

  @Override
  public JavaTreeVisitor createJavaTreeVisitor() {
    return new BaseTreeVisitor() {

      @Override
      public void visitLiteral(LiteralTree tree) {
        if (tree.is(Tree.Kind.STRING_LITERAL)) {
          IP.reset(tree.value());
          if (IP.matches()) {
            String ip = IP.group(1);
            getContext().createLineViolation(HardcodedIpCheck.this, "Make this IP \"" + ip + "\" address configurable.", ((JavaTree) tree).getLine());
          }
        }
      }

    };
  }

}
