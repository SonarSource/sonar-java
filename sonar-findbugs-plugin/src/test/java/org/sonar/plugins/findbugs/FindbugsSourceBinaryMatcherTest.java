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
package org.sonar.plugins.findbugs;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class FindbugsSourceBinaryMatcherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File src1;
  private File target1;

  private File sourceFile1;
  private File sourceFile1WithoutExtension;
  private File classFile1;
  private File innerClassFile1;

  private File classWithNoCorrespondingSource;
  private File notClassFile;

  @Before
  public void setup() throws Exception {
    src1 = temp.newFolder("src1");
    target1 = temp.newFolder("target1");

    temp.newFolder("src1/example");
    temp.newFolder("target1/example");

    sourceFile1 = temp.newFile("src1/example/File1.java");
    sourceFile1WithoutExtension = temp.newFile("src1/example/File1");
    classFile1 = temp.newFile("target1/example/File1.class");
    innerClassFile1 = temp.newFile("target1/example/File1$1.class");

    classWithNoCorrespondingSource = temp.newFile("target1/ClassWithNoCorrespondingSource.class");
    notClassFile = temp.newFile("target1/NotClassFile.claSs");
  }

  @Test
  public void should_analyze_class_and_inner_class_of_modified_source() {
    List<File> classes = new FindbugsSourceBinaryMatcher(ImmutableList.of(src1), ImmutableList.of(target1)).classesToAnalyze(ImmutableList.of(sourceFile1));
    assertThat(classes).contains(classFile1, innerClassFile1);
  }

  @Test
  public void should_not_analyze_class_and_inner_class_of_non_modified_source() {
    List<File> classes = new FindbugsSourceBinaryMatcher(ImmutableList.of(src1), ImmutableList.of(target1)).classesToAnalyze(Collections.EMPTY_LIST);
    for (File clazz : classes) {
      assertThat(clazz).isNotEqualTo(classFile1);
      assertThat(clazz).isNotEqualTo(innerClassFile1);
    }
  }

  @Test
  public void should_always_analyze_classes_with_no_corresponding_source() {
    List<File> classes = new FindbugsSourceBinaryMatcher(ImmutableList.of(src1), ImmutableList.of(target1)).classesToAnalyze(Collections.EMPTY_LIST);
    assertThat(classes).contains(classWithNoCorrespondingSource);
  }

  @Test
  public void should_not_analyze_non_class_files() {
    List<File> classes = new FindbugsSourceBinaryMatcher(ImmutableList.of(src1), ImmutableList.of(target1)).classesToAnalyze(Collections.EMPTY_LIST);
    for (File clazz : classes) {
      assertThat(clazz).isNotEqualTo(notClassFile);
    }
  }

  @Test
  public void should_not_fail_with_file_as_folder() {
    new FindbugsSourceBinaryMatcher(ImmutableList.of(sourceFile1), ImmutableList.of(sourceFile1)).classesToAnalyze(Collections.EMPTY_LIST);
  }

  @Test
  public void should_discard_the_source_file_extension() {
    List<File> classes = new FindbugsSourceBinaryMatcher(ImmutableList.of(src1), ImmutableList.of(target1)).classesToAnalyze(ImmutableList.of(sourceFile1WithoutExtension));
    assertThat(classes).contains(classFile1, innerClassFile1);
  }

}
