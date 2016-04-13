/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.filters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.commons.io.FileUtils;
import org.fest.assertions.Fail;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class EclipseI18NFilterTest {

  @Test
  public void test() {
    verifyFilter("src/test/files/filters/EclipseI18NFilter.java", new EclipseI18NFilter());
  }

  private static void verifyFilter(String filename, JavaIssueFilter filter) {
    // set the component to the filter
    filter.setComponentKey(filename);

    NoIssueCollector noIssueCollector = new NoIssueCollector();
    ArrayList<CodeVisitor> codeVisitors = Lists.<CodeVisitor>newArrayList(filter, noIssueCollector);

    // instantiate the rules filtered by the filter
    codeVisitors.addAll(instantiateRules(filter.filteredRules()));

    Collection<File> classpath = FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar", "zip"}, true);
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(codeVisitors, Lists.newArrayList(classpath), null);
    JavaAstScanner.scanSingleFileForTests(new File(filename), visitorsBridge, new JavaConfiguration(Charset.forName("UTF-8")));
    VisitorsBridgeForTests.TestJavaFileScannerContext testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();

    for (AnalyzerMessage analyzerMessage : testJavaFileScannerContext.getIssues()) {
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(analyzerMessage.getCheck().getClass(), Rule.class);
      Issue issue = new DefaultIssue()
        .setRuleKey(RuleKey.of("repo", ruleAnnotation.key()))
        .setComponentKey(filename)
        .setLine(analyzerMessage.getLine());

      if (noIssueCollector.lines.contains(analyzerMessage.getLine())) {
        assertThat(filter.accept(issue)).isFalse();
      } else {
        assertThat(filter.accept(issue)).isTrue();
      }
    }
  }

  private static Set<JavaFileScanner> instantiateRules(Set<Class<? extends JavaFileScanner>> filteredRules) {
    Set<JavaFileScanner> rules = new HashSet<>();
    for (Class<? extends JavaFileScanner> rule : filteredRules) {
      try {
        rules.add(rule.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        Fail.fail("Unable to instantiate rule " + rule.getCanonicalName());
      }
    }
    return rules;
  }

  private static class NoIssueCollector extends SubscriptionVisitor {

    private final Set<Integer> lines = new HashSet<>();

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      if (syntaxTrivia.comment().trim().endsWith("NoIssue")) {
        lines.add(syntaxTrivia.startLine());
      }
    }
  }
}
