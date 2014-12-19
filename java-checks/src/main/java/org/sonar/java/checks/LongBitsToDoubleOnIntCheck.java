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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import java.util.List;

@Rule(key = "S2127",
    priority = Priority.BLOCKER,
    tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class LongBitsToDoubleOnIntCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.<MethodInvocationMatcher>builder()
        .add(MethodInvocationMatcher.create().typeDefinition("java.lang.Double").name("longBitsToDouble").addParameter("long"))
        .build();
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    Type symbolType = ((AbstractTypedTree) mit.arguments().get(0)).getSymbolType();
    if (!(symbolType.is("long") || symbolType.is("java.lang.Long"))) {
      addIssue(mit, "Remove this \"Double.longBitsToDouble\" call.");
    }
  }
}
