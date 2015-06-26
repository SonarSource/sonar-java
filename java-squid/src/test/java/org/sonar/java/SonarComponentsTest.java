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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Collection;
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
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class SonarComponentsTest {

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
    when(this.checkFactory.<JavaCheck>create(anyString())).thenReturn(this.checks);
    when(this.checks.addAnnotatedChecks(anyCollectionOf(Class.class))).thenReturn(this.checks);
  }

  @Test
  public void creation_of_custom_checks() {
    final JavaCheck expectedCheck = new CustomCheck();
    final CheckRegistrar expectedRegistrar = new MyCheckRegistrer();

    when(this.checks.all()).thenReturn(Lists.<JavaCheck>newArrayList(expectedCheck));
    final SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    final CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isSameAs(expectedCheck);
    final Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(0);
    verify(this.checkFactory, times(1)).create("myRepo");
    verify(this.checks, times(1)).addAnnotatedChecks(anyCollectionOf(Class.class));
    verify(this.checks, times(1)).all();
  }

  @Test
  public void creation_of_custom_test_checks() {
    final JavaCheck expectedCheck = new CustomTestCheck();
    final CheckRegistrar expectedRegistrar = new MyTestCheckRegistrer();

    when(this.checks.all()).thenReturn(Lists.<JavaCheck>newArrayList(expectedCheck));
    final SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    final CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(0);
    final Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator()
      .next()).isSameAs(expectedCheck);
    verify(this.checkFactory, times(1)).create("myTestRepo");
    verify(this.checks, times(1)).addAnnotatedChecks(anyCollectionOf(Class.class));
    verify(this.checks, times(1)).all();
  }

  @Test
  public void creation_of_both_types_test_checks() {
    final JavaCheck expectedCheck = new CustomCheck();
    final CheckRegistrar expectedRegistrar = new MyCheckRegistrer();
    final JavaCheck expectedTestCheck = new CustomTestCheck();
    final CheckRegistrar expectedTestRegistrar = new MyTestCheckRegistrer();

    when(this.checks.all()).thenReturn(Lists.<JavaCheck>newArrayList(expectedCheck))
      .thenReturn(Lists.<JavaCheck>newArrayList(expectedTestCheck));
    final SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar,
      expectedTestRegistrar
    });

    final CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isSameAs(expectedCheck);
    verify(this.checkFactory, times(1)).create("myRepo");

    final Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator()
      .next()).isSameAs(expectedTestCheck);
    verify(this.checkFactory, times(1)).create("myTestRepo");

    verify(this.checks, times(2)).addAnnotatedChecks(anyCollectionOf(Class.class));
    verify(this.checks, times(2)).all();
  }

  private static class MyCheckRegistrer implements CheckRegistrar {

    @Override
    public void register(final RegistrarContext registrarContext) {
      registrarContext.registerClassesForRepository("myRepo", Lists.<Class<? extends JavaCheck>>newArrayList(CustomCheck.class));
    }

    @Override
    public Type type() {
      return Type.SOURCE_CHECKS;
    }

  }

  private static class MyTestCheckRegistrer implements CheckRegistrar {

    @Override
    public void register(final RegistrarContext registrarContext) {
      registrarContext.registerClassesForRepository("myTestRepo", Lists.<Class<? extends JavaCheck>>newArrayList(CustomTestCheck.class));
    }

    @Override
    public Type type() {
      return Type.TEST_CHECKS;
    }

  }

  private static class CustomCheck implements JavaCheck {

  }

  private static class CustomTestCheck implements JavaCheck {

  }
}
