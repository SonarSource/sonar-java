/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.ast.visitors;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IssueBuilderSubscriptionVisitorTest {

  private static final IssueBuilderSubscriptionVisitor VISITOR = new IssueBuilderSubscriptionVisitor() {
    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.VARIABLE);
    }
  };
  static {
    DefaultJavaFileScannerContext context = mock(DefaultJavaFileScannerContext.class);
    when(context.newIssue()).thenReturn(new InternalJavaIssueBuilder(null, null));
    VISITOR.setContext(context);
  }

  @Test
  void can_call_newIssue() {
    assertThat(VISITOR.newIssue()).isInstanceOf(FluentReporting.JavaIssueBuilder.class);
  }

  @Test
  void can_not_call_reportIssue_tree_message() {
    assertThatThrownBy(() -> VISITOR.reportIssue(null, ""))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("IssueBuilderSubsciptionVisitor should only use newIssue().");
  }

  @Test
  void can_not_call_reportIssue_startTree_endTree_message() {
    assertThatThrownBy(() -> VISITOR.reportIssue(null, null, ""))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("IssueBuilderSubsciptionVisitor should only use newIssue().");
  }

  @Test
  void can_not_call_reportIssue_tree_message_flow_cost() {
    List<JavaFileScannerContext.Location> flow = Collections.emptyList();
    assertThatThrownBy(() -> VISITOR.reportIssue(null, "", flow, 42))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("IssueBuilderSubsciptionVisitor should only use newIssue().");
  }

  @Test
  void can_not_call_addIssue() {
    assertThatThrownBy(() -> VISITOR.addIssue(42, ""))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("IssueBuilderSubsciptionVisitor should only use newIssue().");
  }

  @Test
  void can_not_call_scanTree() {
    assertThatThrownBy(() -> VISITOR.scanTree(null))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("IssueBuilderSubsciptionVisitor should not drive visit of AST.");
  }

}
