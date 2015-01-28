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
package org.sonar.java.filters;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.api.rule.RuleKey;

import java.util.Collection;
import java.util.Map;

/**
 * Issue filter used to ignore issues in the block that follow the <code>@SuppressWarnings</code> annotation.
 * <p/>
 * Plugins, via {@link org.sonar.api.BatchExtension}s, must feed this filter by registering the
 * lines in which are covered by suppress warnings. Note that filters are disabled for the issues reported by
 * end-users from UI or web services.
 *
 * @since 3.6
 */
public class SuppressWarningsFilter implements IssueFilter {

  private final Map<String, Multimap<Integer, String>> suppressWarningsLinesByResource = Maps.newHashMap();

  public void addComponent(String componentKey, Multimap<Integer, String> warningLines) {
    suppressWarningsLinesByResource.put(componentKey, warningLines);
  }

  @Override
  public boolean accept(Issue issue, IssueFilterChain chain) {
    Integer line = issue.line();
    RuleKey ruleKey = issue.ruleKey();
    if (line != null) {
      Multimap<Integer, String> suppressWarningsLines = suppressWarningsLinesByResource.get(issue.componentKey());
      if (suppressWarningsLines != null && suppressWarningsLines.containsKey(line)) {
        Collection<String> lineWarnings = suppressWarningsLines.get(line);
        for (String warning : lineWarnings) {
          if ((warningIsTheRuleName(warning, ruleKey) || "all".equals(warning)) && !"S1309".equals(ruleKey.rule())) {
            return false;
          }
        }
      }
    }
    return chain.accept(issue);
  }

  private boolean warningIsTheRuleName(String warning, RuleKey ruleKey) {
    String[] parts = warning.split(":");
    if (parts.length != 2) {
      return false;
    } else {
      return parts[0].equals(ruleKey.repository()) && parts[1].equals(ruleKey.rule());
    }
  }
}
