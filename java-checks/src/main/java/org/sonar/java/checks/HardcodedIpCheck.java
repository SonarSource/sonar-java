/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.base.Splitter;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(key = "S1313")
public class HardcodedIpCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Matcher IP = Pattern.compile("[^\\d.]*?((?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d|\\.)).*?").matcher("");

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      IP.reset(tree.value());
      if (IP.matches()) {
        String ip = IP.group(1);
        if (areAllBelow256(Splitter.on('.').split(ip))) {
          context.reportIssue(this, tree, "Make this IP \"" + ip + "\" address configurable.");
        }
      }
    }
  }

  private static boolean areAllBelow256(Iterable<String> numbersAsStrings) {
    for (String numberAsString : numbersAsStrings) {
      if (Integer.valueOf(numberAsString) > 255) {
        return false;
      }
    }
    return true;
  }

}
