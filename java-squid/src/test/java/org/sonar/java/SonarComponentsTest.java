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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Symbolizable;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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

  public void postTestExecutionChecks() {
    // each time a SonarComponent is instantiated the following methods must be called twice
    // once for custom checks, once for custom java checks
    verify(this.checkFactory, times(2)).create(REPOSITORY_NAME);
    verify(this.checks, times(2)).addAnnotatedChecks(anyCollectionOf(Class.class));
    verify(this.checks, times(2)).all();
  }

  @Test
  public void test_sonar_components() {
    DefaultFileSystem fs = new DefaultFileSystem(new File(""));
    JavaTestClasspath javaTestClasspath = mock(JavaTestClasspath.class);
    ImmutableList<File> javaTestClasspathList = ImmutableList.of();
    when(javaTestClasspath.getElements()).thenReturn(javaTestClasspathList);
    File file = new File("");
    Issuable issuable = mock(Issuable.class);
    when(resourcePerspectives.as(eq(Issuable.class), any(InputFile.class))).thenReturn(issuable);
    Highlightable highlightable = mock(Highlightable.class);
    when(resourcePerspectives.as(eq(Highlightable.class), any(InputFile.class))).thenReturn(highlightable);
    Symbolizable symbolizable = mock(Symbolizable.class);
    when(resourcePerspectives.as(eq(Symbolizable.class), any(InputFile.class))).thenReturn(symbolizable);
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    SonarComponents sonarComponents = new SonarComponents(fileLinesContextFactory, resourcePerspectives, fs, null, javaTestClasspath, checkFactory);

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(0);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(0);
    assertThat(sonarComponents.getFileSystem()).isEqualTo(fs);
    assertThat(sonarComponents.getResourcePerspectives()).isEqualTo(resourcePerspectives);
    assertThat(sonarComponents.getJavaClasspath()).isEmpty();
    assertThat(sonarComponents.getJavaTestClasspath()).isEqualTo(javaTestClasspathList);
    assertThat(sonarComponents.issuableFor(file)).isEqualTo(issuable);
    assertThat(sonarComponents.highlightableFor(file)).isEqualTo(highlightable);
    assertThat(sonarComponents.symbolizableFor(file)).isEqualTo(symbolizable);
    assertThat(sonarComponents.fileLinesContextFor(file)).isEqualTo(fileLinesContext);
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

    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(new ArrayList<JavaCheck>());
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isEqualTo(expectedCheck);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(0);

    postTestExecutionChecks();
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

    when(this.checks.all()).thenReturn(new ArrayList<JavaCheck>()).thenReturn(Lists.newArrayList(expectedCheck));
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(0);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator().next()).isEqualTo(expectedCheck);

    postTestExecutionChecks();
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

    when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck)).thenReturn(Lists.newArrayList(expectedTestCheck));
    SonarComponents sonarComponents = new SonarComponents(this.fileLinesContextFactory, this.resourcePerspectives, null, null, null, this.checkFactory, new CheckRegistrar[] {
      expectedRegistrar
    });

    CodeVisitor[] visitors = sonarComponents.checkClasses();
    assertThat(visitors).hasSize(1);
    assertThat(visitors[0]).isEqualTo(expectedCheck);
    Collection<JavaCheck> testChecks = sonarComponents.testCheckClasses();
    assertThat(testChecks).hasSize(1);
    assertThat(testChecks.iterator().next()).isEqualTo(expectedTestCheck);
    assertThat(sonarComponents.checks()).hasSize(2);

    postTestExecutionChecks();
  }

  private static class CustomCheck implements JavaCheck {
  }

  private static class CustomTestCheck implements JavaCheck {
  }
}
