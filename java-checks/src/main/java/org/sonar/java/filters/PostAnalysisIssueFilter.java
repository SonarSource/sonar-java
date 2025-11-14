/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;

public class PostAnalysisIssueFilter implements JavaFileScanner, SonarJavaIssueFilter {

  private List<JavaIssueFilter> issueFilters;

  @VisibleForTesting
  List<JavaIssueFilter> issueFilters() {
    if (issueFilters == null) {
      issueFilters = Collections.unmodifiableList(Arrays.asList(
        new EclipseI18NFilter(),
        new LombokFilter(),
        new GoogleAutoFilter(),
        new SuppressWarningFilter(),
        new GeneratedCodeFilter(),
        new SpringFilter()));
    }
    return issueFilters;
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    return issueFilters().stream().allMatch(filter -> filter.accept(issue))
      && chain.accept(issue);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    issueFilters().forEach(filter -> filter.scanFile(context));
  }
}
