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
package org.sonar.java.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.search.GlobalSearchScope;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaPsiConfigurationTest {
  @TempDir
  Path temporary;

  @Test
  void implicit_runtime_and_decoded_snapshot_need_neither_launch_flags_nor_a_disk_source() {
    String text = "class Example { String élève; }\r\n";
    var input = new JavaPsiEnvironment.SourceSnapshot(temporary.resolve("Example.java"), text, StandardCharsets.ISO_8859_1);
    var configuration = new JavaPsiConfiguration(null, List.of(temporary.resolve("missing.jar")), 21, false, false);
    String configProperty = System.getProperty("idea.config.path");
    try (var environment = JavaPsiEnvironment.open(configuration, List.of(input))) {
      environment.read((project, files) -> {
        var file = files.getFirst();
        assertEquals(text, file.getText());
        assertEquals(StandardCharsets.ISO_8859_1, file.getVirtualFile().getCharset());
        assertEquals(text.indexOf("élève"), file.getClasses()[0].getFields()[0].getNameIdentifier().getTextOffset());
        assertNotNull(JavaPsiFacade.getInstance(project).findClass("java.lang.String", GlobalSearchScope.allScope(project)));
        assertFalse(ApplicationManager.getApplication().isUnitTestMode());
        return null;
      });
    }
    assertEquals(configProperty, System.getProperty("idea.config.path"));
    assertNull(ApplicationManager.getApplication());
  }

  @ParameterizedTest
  @ValueSource(strings = {"rt.jar", "android.jar"})
  void even_a_missing_explicit_runtime_suppresses_host_lookup(String archive) {
    var configuration = new JavaPsiConfiguration(temporary, List.of(temporary.resolve(archive)), 21, false, false);
    var source = new JavaPsiEnvironment.SourceSnapshot(temporary.resolve("Example.java"), "class Example { String value; }", StandardCharsets.UTF_8);
    try (var environment = JavaPsiEnvironment.open(configuration, List.of(source))) {
      environment.read((project, files) -> {
        assertNull(JavaPsiFacade.getInstance(project).findClass("java.lang.String", GlobalSearchScope.allScope(project)));
        return null;
      });
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {8, 9, 21, 26})
  void module_info_filter_uses_the_effective_source_level(int version) {
    var source = new JavaPsiEnvironment.SourceSnapshot(temporary.resolve("module-info.java"), "module example {}", StandardCharsets.UTF_8);
    var configuration = new JavaPsiConfiguration(null, List.of(), version, false, false);
    try (var environment = JavaPsiEnvironment.open(configuration, List.of(source))) {
      assertEquals(version < 9 ? 0 : 1, environment.read((project, files) -> files.size()).intValue());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Example", "Example.JAVA", "generated.source"})
  void decoded_snapshots_do_not_require_the_experiment_file_extension(String name) {
    var configuration = new JavaPsiConfiguration(null, List.of(), 21, false, false);
    var source = new JavaPsiEnvironment.SourceSnapshot(temporary.resolve(name), "class Example {}", StandardCharsets.UTF_8);
    try (var environment = JavaPsiEnvironment.open(configuration, List.of(source))) {
      assertEquals("Example", environment.read((project, files) -> files.getFirst().getClasses()[0].getName()));
    }
  }

  @Test
  void missing_selected_jrt_provider_preserves_the_failure_contract_without_host_repair() {
    var provider = temporary.resolve("jdk/lib/jrt-fs.jar");
    var configuration = new JavaPsiConfiguration(null, List.of(provider), 21, false, false);
    var failure = assertThrows(IllegalStateException.class, () -> JavaPsiEnvironment.open(configuration, List.of()));
    assertEquals("Failed to init packages for " + provider, failure.getMessage());
  }

  @Test
  void configuration_keeps_an_ordered_copy_of_the_classpath() {
    var paths = new ArrayList<>(List.of(temporary.resolve("first"), temporary.resolve("second")));
    var configuration = new JavaPsiConfiguration(null, paths, 21, false, true);
    paths.clear();
    assertEquals(List.of(temporary.resolve("first"), temporary.resolve("second")), configuration.classpath());
    assertThrows(UnsupportedOperationException.class, () -> configuration.classpath().clear());
  }
}
