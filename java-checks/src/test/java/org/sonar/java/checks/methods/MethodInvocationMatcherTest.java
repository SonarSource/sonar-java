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
package org.sonar.java.checks.methods;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MethodInvocationMatcherTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_fail_if_addParameter_is_called_after_withNoParameterConstraint() throws Exception {
    MethodInvocationMatcher matcher = MethodInvocationMatcher.create().name("name")
      .withNoParameterConstraint()
      .withNoParameterConstraint();
    exception.expect(IllegalStateException.class);
    matcher.addParameter("int");
  }

  @Test
  public void should_fail_if_withNoParameterConstraint_is_called_after_addParameter() throws Exception {
    MethodInvocationMatcher matcher = MethodInvocationMatcher.create().name("name").addParameter("int");
    exception.expect(IllegalStateException.class);
    matcher.withNoParameterConstraint();
  }

}
