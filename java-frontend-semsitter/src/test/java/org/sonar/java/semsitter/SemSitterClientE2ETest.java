/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.semsitter.protocol.Document;
import org.sonar.java.semsitter.protocol.TypesResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the real udg-java-server, whose path is given by {@code -Dsemsitter.executable}.
 */
@EnabledIfSystemProperty(named = "semsitter.executable", matches = ".+")
class SemSitterClientE2ETest {

  @TempDir
  Path project;

  @Test
  void serves_files_and_types_of_a_project() throws IOException {
    Path pkg = Files.createDirectories(project.resolve("p"));
    Files.writeString(pkg.resolve("A.java"), "package p;\nimport java.util.List;\npublic class A extends B { List<String> xs; int f() { return g(); } }\n");
    Files.writeString(pkg.resolve("B.java"), "package p;\nclass B { int g() { return 1; } }\n");

    try (SemSitterClient client = SemSitterClient.start(Path.of(System.getProperty("semsitter.executable")), project)) {
      client.setSettings(Map.of("jvm.java_version", List.of("17")));
      assertThat(client.fullGen(List.of()).getFiles()).isEqualTo(2);

      Document document = client.getFile("p/A.java", 0);
      assertThat(document.getPath()).isEqualTo("p/A.java");
      assertThat(document.getSemantic()).isTrue();
      assertThat(document.getHasError()).isFalse();
      assertThat(document.getTree().getKind()).isEqualTo("program");

      TypesResponse types = client.getTypes(List.of("p.B", "java.lang.String", "x.Missing"));
      assertThat(types.getProjectList()).containsExactly("p.B");
      assertThat(types.getMissingList()).containsExactly("x.Missing");
      assertThat(types.getDeclarationsOrThrow("java.lang.String").getKind()).isEqualTo("class");

      assertThat(client.hasPackage(List.of("p", "zz"))).containsEntry("p", true).containsEntry("zz", false);
      assertThatThrownBy(() -> client.getFile("p/Nope.java", 0)).isInstanceOf(IOException.class);
    }
  }
}
