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

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Iterator;
import java.util.List;

@Rule(
  key = "SwitchLastCaseIsDefaultCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class SwitchLastCaseIsDefaultCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode defaultLabel = getDefaultLabel(node);
    AstNode lastLabel = getLastLabel(node);

    if (defaultLabel == null) {
      getContext().createLineViolation(this, "Add a default case to this switch.", node);
    } else if (!defaultLabel.equals(lastLabel)) {
      getContext().createLineViolation(this, "Move this default to the end of the switch.", defaultLabel);
    }
  }

  private AstNode getDefaultLabel(AstNode node) {
    Iterator<AstNode> it = node.select()
        .children(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUPS)
        .children(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP)
        .children(JavaGrammar.SWITCH_LABEL)
        .children(JavaKeyword.DEFAULT)
        .iterator();

    return !it.hasNext() ? null : it.next().getParent();
  }

  private AstNode getLastLabel(AstNode node) {
    List<AstNode> labels = ImmutableList.copyOf(node.select()
        .children(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUPS)
        .children(JavaGrammar.SWITCH_BLOCK_STATEMENT_GROUP)
        .children(JavaGrammar.SWITCH_LABEL));

    return labels.isEmpty() ? null : labels.get(labels.size() - 1);
  }

}
