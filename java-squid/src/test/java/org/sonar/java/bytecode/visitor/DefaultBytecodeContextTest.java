/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.bytecode.visitor;

import org.junit.Test;
import org.sonar.api.resources.Resource;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultBytecodeContextTest {
  @Test
  public void test_without_sonar_components() throws Exception {
    JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
    DefaultBytecodeContext context = new DefaultBytecodeContext(javaResourceLocator);
    assertThat(context.getJavaResourceLocator()).isEqualTo(javaResourceLocator);
    JavaResourceLocator otherJavaResourceLocator = mock(JavaResourceLocator.class);
    context.setJavaResourceLocator(otherJavaResourceLocator);
    assertThat(context.getJavaResourceLocator()).isEqualTo(otherJavaResourceLocator);
    context.reportIssue(null, null, null, 0);
  }

  @Test
  public void test_with_sonar_components() throws Exception {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    JavaResourceLocator javaResourceLocator = mock(JavaResourceLocator.class);
    DefaultBytecodeContext context = new DefaultBytecodeContext(sonarComponents, javaResourceLocator);
    assertThat(context.getJavaResourceLocator()).isEqualTo(javaResourceLocator);

    String message = "msg";
    int line = 42;
    JavaCheck javaCheck = mock(JavaCheck.class);
    Resource resource = mock(Resource.class);
    String name = "name";
    when(resource.getPath()).thenReturn(name);
    context.reportIssue(javaCheck, resource, message, line);
    verify(sonarComponents).addIssue(new File(name), javaCheck, line, message, null);
  }
}
