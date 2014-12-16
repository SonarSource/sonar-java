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
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import java.util.List;

@Rule(
  key = "S2204",
  priority = Priority.BLOCKER,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class EqualsOnAtomicClassCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      equalsInvocationMatcher("java.util.concurrent.atomic.AtomicBoolean"),
      equalsInvocationMatcher("java.util.concurrent.atomic.AtomicInteger"),
      equalsInvocationMatcher("java.util.concurrent.atomic.AtomicLong"));
  }

  private MethodInvocationMatcher equalsInvocationMatcher(String fullyQualifiedName) {
    return MethodInvocationMatcher.create()
      .callSite(TypeCriteria.is(fullyQualifiedName))
      .name("equals")
      .addParameter("java.lang.Object");
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    addIssue(mit, "Use \".get()\" to retrieve the value and compare it instead.");
    super.onMethodFound(mit);
  }

}
