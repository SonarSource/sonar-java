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
package org.sonar.java.filters;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Fail;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FilterVerifier {

  public static void verify(String filename, JavaIssueFilter filter, CodeVisitor... extraCodeVisitors) {
    // set the component to the filter
    filter.setComponentKey(filename);

    IssueCollector issueCollector = new IssueCollector();
    ArrayList<CodeVisitor> codeVisitors = Lists.<CodeVisitor>newArrayList(filter, issueCollector);

    // instantiate the rules filtered by the filter
    codeVisitors.addAll(instantiateRules(filter.filteredRules()));

    for (CodeVisitor codeVisitor : extraCodeVisitors) {
      codeVisitors.add(codeVisitor);
    }

    Collection<File> classpath = FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar", "zip"}, true);
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(codeVisitors, Lists.newArrayList(classpath), null);
    JavaAstScanner.scanSingleFileForTests(new File(filename), visitorsBridge, new JavaConfiguration(Charset.forName("UTF-8")));
    VisitorsBridgeForTests.TestJavaFileScannerContext testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();

    Multimap<Integer, String> issuesByLines = HashMultimap.create();
    for (AnalyzerMessage analyzerMessage : testJavaFileScannerContext.getIssues()) {
      Integer issueLine = analyzerMessage.getLine();
      String ruleKey = AnnotationUtils.getAnnotation(analyzerMessage.getCheck().getClass(), Rule.class).key();
      FilterableIssue issue = mock(FilterableIssue.class);
      when(issue.ruleKey()).thenReturn(RuleKey.of("repo", ruleKey));
      when(issue.componentKey()).thenReturn(filename);
      when(issue.line()).thenReturn(issueLine);

      if (issueCollector.rejectedIssuesLines.contains(issueLine)) {

        assertThat(filter.accept(issue))
          .overridingErrorMessage("Line #" + issueLine + " has been marked with 'NoIssue' but issue of rule '" + ruleKey + "' has been accepted!")
          .isFalse();
      } else if (issueCollector.acceptedIssuesLines.contains(issueLine)) {
        // force check on accepted issues
        assertThat(filter.accept(issue))
          .overridingErrorMessage("Line #" + issueLine + " has been marked with 'WithIssue' but no issue have been raised!")
          .isTrue();
      } else {
        issuesByLines.put(issueLine, ruleKey);
      }
    }

    if (!issuesByLines.isEmpty()) {
      List<Integer> lines = Lists.newArrayList(issuesByLines.keySet());
      Collections.sort(lines);
      StringBuilder builder = new StringBuilder();
      for (Integer line : lines) {
        builder.append("\n#" + line + ": " + issuesByLines.get(line).toString());
      }

      Fail.fail("The following lines have not been marked with 'WithIssue' or 'NoIssue' and raised issues:" + builder.toString());
    }
  }

  private static Set<JavaCheck> instantiateRules(Set<Class<? extends JavaCheck>> filteredRules) {
    Set<JavaCheck> rules = new HashSet<>();
    for (Class<? extends JavaCheck> rule : filteredRules) {
      try {
        rules.add(rule.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        Fail.fail("Unable to instantiate rule " + rule.getCanonicalName());
      }
    }
    return rules;
  }

  private static class IssueCollector extends SubscriptionVisitor {

    private final Set<Integer> rejectedIssuesLines = new HashSet<>();
    private final Set<Integer> acceptedIssuesLines = new HashSet<>();

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      String comment = syntaxTrivia.comment().trim();
      if (comment.endsWith("NoIssue")) {
        rejectedIssuesLines.add(syntaxTrivia.startLine());
      } else if (comment.endsWith("WithIssue")) {
        acceptedIssuesLines.add(syntaxTrivia.startLine());
      }
    }
  }

}
