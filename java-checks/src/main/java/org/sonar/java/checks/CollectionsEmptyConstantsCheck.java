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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Map;

@Rule(
  key = "S1596",
  name = "Collections.emptyList(), emptyMap() and emptySet() should be used instead of Collections.EMPTY_LIST, EMPTY_MAP and EMPTY_SET",
  tags = {"obsolete", "pitfall"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class CollectionsEmptyConstantsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Map<String, String> IDENTIFIER_REPLACEMENT = new ImmutableMap.Builder<String, String>()
    .put("EMPTY_LIST", "emptyList()")
    .put("EMPTY_MAP", "emptyMap()")
    .put("EMPTY_SET", "emptySet()")
    .build();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    super.visitMemberSelectExpression(tree);
    String identifier = tree.identifier().name();
    boolean isCollectionsCall = tree.expression().is(Kind.IDENTIFIER) && "Collections".equals(((IdentifierTree) tree.expression()).name());
    boolean callEmptyConstant = identifier.startsWith("EMPTY_");
    if (isCollectionsCall && callEmptyConstant) {
      context.addIssue(tree, this, "Replace \"Collections."+identifier+"\" by \"Collections."+ IDENTIFIER_REPLACEMENT.get(identifier)+"\".");
    }
  }

}
