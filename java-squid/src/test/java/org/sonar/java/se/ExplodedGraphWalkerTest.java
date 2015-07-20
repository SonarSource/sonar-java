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
package org.sonar.java.se;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.se.checks.ConditionAlwaysTrueOrFalseCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Map;

public class ExplodedGraphWalkerTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/se/SeEngineTest.java", new IssueVisitor());
  }

  class IssueVisitor implements JavaFileScanner {

    @Override
    public void scanFile(JavaFileScannerContext context) {
      reportIssuesFor(context, new NullDereferenceCheck());
      reportIssuesFor(context, new ConditionAlwaysTrueOrFalseCheck());
    }

    private void reportIssuesFor(JavaFileScannerContext context, JavaCheck check) {
      Multimap<Tree, String> issues = ((DefaultJavaFileScannerContext) context).getSEIssues((Class<? extends SECheck>) check.getClass());
      for (Map.Entry<Tree, String> issue : issues.entries()) {
        context.reportIssue(check, issue.getKey(), issue.getValue());
      }
    }
  }

}
