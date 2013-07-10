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
package org.sonar.plugins.java.decorators;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChidamberKemererDistributionBuilderTest {

  private ChidamberKemererDistributionBuilder decorator;

  @Before
  public void setUp() {
    decorator = new ChidamberKemererDistributionBuilder();
  }

  @Test
  public void should_execute_on_java_project() {
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("java");
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
    when(project.getLanguageKey()).thenReturn("py");
    assertThat(decorator.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void test_dependencies() {
    assertThat(decorator.generatesLcom4Distribution()).isSameAs(CoreMetrics.LCOM4_DISTRIBUTION);
    assertThat(decorator.dependsInLcom4()).isSameAs(CoreMetrics.LCOM4);
    assertThat(decorator.generatesRfcDistribution()).isSameAs(CoreMetrics.RFC_DISTRIBUTION);
    assertThat(decorator.dependsInRfc()).isSameAs(CoreMetrics.RFC);
  }

}
