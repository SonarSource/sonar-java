/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.java;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class UCFGCollectorTest {

  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void illegalStateException_when_projectbasedir_not_defined() {
    MapSettings settings = new MapSettings();
    try {
      UCFGCollector.projectWorkDirFromConfig(settings.asConfig());
      fail("should have failed");
    }catch (IllegalStateException e) {
      assertThat(e).hasMessage("sonar.projectBaseDir is not defined");
    }
  }

  @Test
  public void create_workdir_path() throws IOException {
    File baseDir = tmp.newFolder("baseDir");
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.projectBaseDir ", baseDir.getAbsolutePath());
    // default workdir
    File workDir = UCFGCollector.projectWorkDirFromConfig(settings.asConfig());
    assertThat(workDir.getAbsolutePath()).isEqualTo(baseDir.getAbsolutePath() + File.separator + ".sonar");

    //relative workdir
    settings.setProperty(CoreProperties.WORKING_DIRECTORY, "customWorkDir");
    workDir = UCFGCollector.projectWorkDirFromConfig(settings.asConfig());
    assertThat(workDir.getAbsolutePath()).isEqualTo(baseDir.getAbsolutePath() + File.separator + "customWorkDir");

    //absolute workdir
    File absoluteCustomWorkDir = new File(baseDir, "absoluteCustomWorkDir");
    settings.setProperty(CoreProperties.WORKING_DIRECTORY, absoluteCustomWorkDir.getAbsolutePath());
    workDir = UCFGCollector.projectWorkDirFromConfig(settings.asConfig());
    assertThat(workDir.getAbsolutePath()).isEqualTo(absoluteCustomWorkDir.getAbsolutePath());
  }


  @Test
  public void generate_protobuf_file_in_workdir() throws IOException {
    File baseDir = tmp.newFolder("baseDirProtobuf");
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.projectBaseDir ", baseDir.getAbsolutePath());
    CompilationUnitTree cut = getCompilationUnitTreeWithSemantics("class A { String fun(String a) {return a;} String fun2(String a) {return a;}}");
    UCFGJavaVisitor visitor = new UCFGCollector(settings.asConfig()).getVisitor();
    visitor.fileKey = "randomFileKey.java";
    visitor.visitCompilationUnit(cut);

    String[] list = new File(baseDir, ".sonar/ucfg/java").list();
    assertThat(list).hasSize(2).containsExactly("ucfg_0.proto", "ucfg_1.proto");
  }

  private CompilationUnitTree getCompilationUnitTreeWithSemantics(String source) {
    File testJarsDir = new File("target/test-jars/");
    SquidClassLoader squidClassLoader = new SquidClassLoader(Arrays.asList(testJarsDir.listFiles()));
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse(source);
    SemanticModel.createFor(cut, squidClassLoader);
    return cut;
  }
}
