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
package org.sonar.plugins.java.bridges;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.java.JavaSquid;
import org.sonar.java.ast.visitors.PackageVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourcePackage;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.indexer.SquidIndex;

import java.io.File;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceIndexTest {

  @Test
  public void unresolved_package_should_not_appear_in_index() {
    ResourceIndex resourceIndex = new ResourceIndex(false);

    JavaSquid squid = mock(JavaSquid.class);
    SquidIndex index = mock(SquidIndex.class);
    SensorContext context = mock(SensorContext.class);
    Project project = mock(Project.class);
    ProjectFileSystem moduleFileSystem = mock(ProjectFileSystem.class);

    SourceCode squidFile = new SourceFile("file", "file");
    SourcePackage squidPackage = new SourcePackage(PackageVisitor.UNRESOLVED_PACKAGE);
    squidPackage.addChild(squidFile);
    Collection<SourceCode> squidFiles = Lists.newArrayList(squidFile);

    when(squid.getIndex()).thenReturn(index);
    when(index.search(any(QueryByType.class))).thenReturn(squidFiles);
    when(project.getFileSystem()).thenReturn(moduleFileSystem);
    when(moduleFileSystem.getBasedir()).thenReturn(new File(this.getClass().getResource("/").getFile()));

    resourceIndex.loadSquidResources(squid, context, project);
    assertThat(resourceIndex).hasSize(1);
    assertThat(resourceIndex.containsKey(new SourceFile("file"))).isTrue();

  }
}
