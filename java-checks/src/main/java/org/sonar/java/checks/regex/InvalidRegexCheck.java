/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.SyntaxError;
import org.sonar.java.regex.ast.BackReferenceTree;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5856")
public class InvalidRegexCheck extends AbstractRegexCheck {

  private static final String ERROR_MESSAGE = "Fix the %s error%s inside this regex.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    List<SyntaxError> syntaxErrors = regexForLiterals.getSyntaxErrors();
    new GroupVisitor().visit(regexForLiterals);
    if (!syntaxErrors.isEmpty()) {
      reportSyntaxErrors(syntaxErrors);
    }
  }

  private void reportSyntaxErrors(List<SyntaxError> syntaxErrors) {
    // report on the first issue
    RegexSyntaxElement tree = syntaxErrors.get(0).getOffendingSyntaxElement();
    List<RegexIssueLocation> secondaries = syntaxErrors.stream()
      .map(error -> new RegexCheck.RegexIssueLocation(error.getOffendingSyntaxElement(), error.getMessage()))
      .collect(Collectors.toList());

    reportIssue(tree, secondaries, "syntax");
  }

  private void reportIssue(RegexSyntaxElement tree, List<RegexIssueLocation> secondaries, String errorKind) {
    String msg = String.format(ERROR_MESSAGE, errorKind, secondaries.size() > 1 ? "s" : "");
    reportIssue(tree, msg, null, secondaries);
  }

  private final class GroupVisitor extends RegexBaseVisitor {

    final Map<String, CapturingGroupTree> groupNames = new HashMap<>();
    final Map<String, BackReferenceTree> backReferenceNames = new HashMap<>();

    @Override
    public void visitCapturingGroup(CapturingGroupTree tree) {
      super.visitCapturingGroup(tree);
      tree.getName().ifPresent(groupName -> groupNames.put(groupName, tree));
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      super.visitBackReference(tree);
      if (tree.isNamedGroup()) {
        backReferenceNames.put(tree.groupName(), tree);
      }
    }

    @Override
    protected void after(RegexParseResult regexParseResult) {
      BackReferenceTree firstWrongBackReference = null;
      List<RegexIssueLocation> secondaries = new ArrayList<>();

      for (Map.Entry<String, BackReferenceTree> backReference : backReferenceNames.entrySet()) {
        String key = backReference.getKey();
        BackReferenceTree backReferenceTree = backReference.getValue();

        CapturingGroupTree capturingGroupTree = groupNames.get(key);
        String groupName = backReferenceTree.groupName();
        boolean reported = false;
        if (capturingGroupTree == null) {
          secondaries.add(new RegexIssueLocation(backReferenceTree, String.format("There is no group named '%s'.", groupName)));
          reported = true;
        } else if (isBefore(backReferenceTree, capturingGroupTree)) {
          secondaries.add(new RegexIssueLocation(backReferenceTree, String.format("The group named '%s' is not yet declared at this position.", groupName)));
          reported = true;
        }
        if (reported && firstWrongBackReference == null) {
          firstWrongBackReference = backReferenceTree;
        }
      }

      if (firstWrongBackReference != null) {
        reportIssue(firstWrongBackReference, secondaries, "back reference");
      }
    }

    private boolean isBefore(RegexTree t1, RegexTree t2) {
      return t1.getRange().getBeginningOffset() < t2.getRange().getBeginningOffset();
    }

  }

}
