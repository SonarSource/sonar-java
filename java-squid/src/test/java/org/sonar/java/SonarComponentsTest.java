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
package org.sonar.java;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SonarComponentsTest {

  private static final String REPOSITORY_NAME = "custom";

  @Mock
  private FileLinesContextFactory fileLinesContextFactory;

  @Mock
  private ResourcePerspectives resourcePerspectives;

  @Mock
  private CheckFactory checkFactory;

  @Mock
  private Checks<JavaCheck> checks;

  @Before
  public void setUp() {
    // configure mocks that need verification
    when(this.checkFactory.<JavaCheck>create(anyString())).thenReturn(this.checks);
    when(this.checks.addAnnotatedChecks(anyCollectionOf(Class.class))).thenReturn(this.checks);
  }

  @After
  public void postTestExecutionChecks() {
    // each time a SonarComponent is instanciated the following methods must be called twice
    // once for custom checks, once for custom java checks
    verify(this.checkFactory, times(2)).create(REPOSITORY_NAME);
    verify(this.checks, times(2)).addAnnotatedChecks(anyCollectionOf(Class.class));
    verify(this.checks, times(2)).all();
  }

  @Test
  public void creation_of_custom_checks() {
    JavaCheck expectedCheck = new CustomCheck();
    CheckRegistrar expectedRegistrar = new CheckRegistrar() {
      @Override
      public void register(RegistrarContext registrarContext) {
        registrarContext.registerClassesForRepository(
          REPOSITORY_NAME,
          Lists.<Class<? extends JavaCheck>>newArrayList(CustomTestCheck.class),
          null);
      }
    };

    when(this.checks.all()).thenReturn(Lists.<JavaCheck>newArrayList(expectedCheck)).thenReturn(new ArrayList<JavaCheck>());
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isEqualTo(expectedCheck);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(0);
  }

  @Test
  public void creation_of_custom_test_checks() {
    JavaCheck expectedCheck = new CustomTestCheck();
    CheckRegistrar expectedRegistrar = new CheckRegistrar() {
      @Override
      public void register(RegistrarContext registrarContext) {
        registrarContext.registerClassesForRepository(
          REPOSITORY_NAME,
          null,
          Lists.<Class<? extends JavaCheck>>newArrayList(CustomTestCheck.class));
      }
    };

    when(this.checks.all()).thenReturn(new ArrayList<JavaCheck>()).thenReturn(Lists.<JavaCheck>newArrayList(expectedCheck));
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(0);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator().next()).isEqualTo(expectedCheck);
  }

  @Test
  public void creation_of_both_types_test_checks() {
    JavaCheck expectedCheck = new CustomCheck();
    JavaCheck expectedTestCheck = new CustomTestCheck();
    CheckRegistrar expectedRegistrar = new CheckRegistrar() {
      @Override
      public void register(RegistrarContext registrarContext) {
        registrarContext.registerClassesForRepository(
          REPOSITORY_NAME,
          Lists.<Class<? extends JavaCheck>>newArrayList(CustomCheck.class),
          Lists.<Class<? extends JavaCheck>>newArrayList(CustomTestCheck.class));
      }
    };

    when(this.checks.all()).thenReturn(Lists.<JavaCheck>newArrayList(expectedCheck)).thenReturn(Lists.<JavaCheck>newArrayList(expectedTestCheck));
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isEqualTo(expectedCheck);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator().next()).isEqualTo(expectedTestCheck);
  }

  private static class CustomCheck implements JavaCheck {
  }

  private static class CustomTestCheck implements JavaCheck {
  }
}
