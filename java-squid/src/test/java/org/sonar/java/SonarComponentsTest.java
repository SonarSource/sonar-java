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
package org.sonar.java;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarComponentsTest {


  @Test
  public void creation_of_custom_checks() throws Exception {

    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
    CheckFactory checkFactory = mock(CheckFactory.class);

    Checks<JavaCheck> checks = mock(Checks.class);
    when(checkFactory.<JavaCheck>create(anyString())).thenReturn(checks);
    when(checks.addAnnotatedChecks(anyCollectionOf(Class.class))).thenReturn(checks);
    when(checks.all()).thenReturn(Lists.<JavaCheck>newArrayList(new CustomCheck()));
    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, resourcePerspectives, null, null, null,
        checkFactory, new CheckRegistrar[]{new MyCheckRegistrer()});

    assertThat(sonarComponents.checkClasses()).hasSize(1);
    verify(checkFactory, times(1)).create("myRepo");
    verify(checks, times(1)).addAnnotatedChecks(anyCollectionOf(Class.class));
    verify(checks, times(1)).all();


  }

  private static class MyCheckRegistrer implements CheckRegistrar {
    @Override
    public void register(RegistrarContext registrarContext) {
      registrarContext.registerClassesForRepository("myRepo", Lists.<Class<? extends JavaCheck>>newArrayList(CustomCheck.class));
    }
  }

  private static class CustomCheck implements JavaCheck {

  }
}