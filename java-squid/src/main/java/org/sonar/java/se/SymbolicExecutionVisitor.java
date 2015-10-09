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

import com.google.common.collect.Lists;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.PrintStream;
import java.util.List;

public class SymbolicExecutionVisitor extends SubscriptionVisitor {
  public static final Logger LOG = LoggerFactory.getLogger(SymbolicExecutionVisitor.class);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Lists.newArrayList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    try {
      tree.accept(new ExplodedGraphWalker(new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM), context));
    }catch (ExplodedGraphWalker.MaximumStepsReachedException exception) {
      LOG.error("Could not complete symbolic execution: ", exception);
    }

  }
}
