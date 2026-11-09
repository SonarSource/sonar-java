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

import com.intellij.psi.JavaModuleGraphHelper;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaPsiModuleGraphTest {
  @TempDir
  Path temporary;

  @ParameterizedTest
  @CsvSource({"java.sql, Connection, java.sql.Connection", "java.sql.rowset, Connection, java.sql.Connection",
    "java.desktop, Node, org.w3c.dom.Node"})
  void resolves_direct_and_transitive_module_imports(String module, String name, String expected) throws IOException {
    var source = source("Example.java", "import module " + module + "; class Example { " + name + " value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 26)) {
      assertEquals(expected, environment.read((project, files) -> {
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        var resolved = type.resolveGenerics();
        assertTrue(resolved.isValidResult());
        return resolved.getElement().getQualifiedName();
      }));
    }
  }

  @ParameterizedTest
  @CsvSource({"Connection, false", "java.sql.Connection, true", "String, true"})
  void preserves_ecj_duplicate_path_ambiguity_without_rejecting_qualified_or_implicit_imports(String typeName, boolean expected) throws IOException {
    var source = source("Example.java", "import module java.se; class Example { " + typeName + " value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 26)) {
      environment.read((project, files) -> {
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        assertEquals(expected, type.resolveGenerics().isValidResult());
        return null;
      });
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"import java.sql.Connection;", "import java.sql.*;"})
  void ordinary_imports_shadow_ecj_module_import_ambiguity(String ordinaryImport) throws IOException {
    var source = source("Example.java", "import module java.se; " + ordinaryImport + " class Example { Connection value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 26)) {
      environment.read((project, files) -> {
        assertTrue(((PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType()).resolveGenerics().isValidResult());
        return null;
      });
    }
  }

  @Test
  void transitive_exports_exclude_non_transitive_requires() {
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(), List.of(), 26)) {
      environment.read((project, files) -> {
        var facade = JavaPsiFacade.getInstance(project);
        var scope = GlobalSearchScope.allScope(project);
        var desktop = facade.findModule("java.desktop", scope);
        var dependencies = JavaModuleGraphHelper.getInstance().getAllTransitiveDependencies(desktop).stream()
          .map(PsiJavaModule::getName).collect(Collectors.toSet());
        assertTrue(dependencies.containsAll(Set.of("java.xml", "java.datatransfer")));
        assertFalse(dependencies.contains("java.prefs"));
        return null;
      });
    }
  }

  @ParameterizedTest
  @CsvSource({"8, true", "26, false"})
  void jdk_internal_packages_respect_the_source_level(int version, boolean expected) throws IOException {
    var source = source("Example.java", "class Example { jdk.internal.misc.Unsafe value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), version)) {
      environment.read((project, files) -> {
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        var target = type.resolve();
        assertNotNull(target);
        assertEquals(expected, JavaPsiFacade.getInstance(project).getResolveHelper()
          .isAccessible(target, files.getFirst().getClasses()[0], null));
        assertEquals(expected, type.resolveGenerics().isValidResult());
        return null;
      });
    }
  }

  @Test
  void qualified_exports_and_non_transitive_readability_are_enforced() {
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(), List.of(), 26)) {
      environment.read((project, files) -> {
        var facade = JavaPsiFacade.getInstance(project);
        var scope = GlobalSearchScope.allScope(project);
        var helper = JavaModuleGraphHelper.getInstance();
        var base = facade.findModule("java.base", scope);
        var desktop = facade.findModule("java.desktop", scope);
        var prefs = facade.findModule("java.prefs", scope);
        var rowset = facade.findModule("java.sql.rowset", scope);
        var internal = facade.findClass("jdk.internal.misc.Unsafe", scope).getContainingFile();
        assertTrue(helper.isAccessible(prefs, desktop));
        assertFalse(helper.isAccessible(prefs, rowset));
        assertTrue(helper.isAccessible(base, rowset));
        assertTrue(helper.isAccessible("jdk.internal.misc", internal, base));
        assertTrue(helper.isAccessible("jdk.internal.misc", internal, desktop));
        assertFalse(helper.isAccessible("jdk.internal.misc", internal, rowset));
        return null;
      });
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void split_packages_match_the_ecj_option_for_directories_and_jars(boolean archive) throws Exception {
    var classes = compile("split", "8", Map.of("Extra.java", "package org.w3c.dom; public class Extra {}"));
    var library = archive ? jar(classes) : classes;
    var source = source("Example.java", "class Example { org.w3c.dom.Node node; org.w3c.dom.Extra extra; }");
    for (int version : List.of(8, 26)) {
      for (boolean ignore : List.of(false, true)) {
        try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(library), version, false, ignore)) {
          environment.read((project, files) -> {
            for (var field : files.getFirst().getClasses()[0].getFields()) {
              var type = (PsiClassType) field.getType();
              assertEquals(version < 9 || ignore, type.resolveGenerics().isValidResult(), field.getName());
            }
            return null;
          });
        }
      }
    }
  }

  @Test
  void modular_classpath_entries_and_explicit_sources_keep_ecj_unnamed_visibility() throws Exception {
    var classes = compile("dependency", "17", Map.of(
      "module-info.java", "module dependency { exports api; }",
      "api/Visible.java", "package api; public class Visible {}",
      "internal/Hidden.java", "package internal; public class Hidden {}"));
    var library = jar(classes);
    var module = source("module-info.java", "module example {}");
    var source = source("Example.java", "class Example { internal.Hidden hidden; java.sql.Connection connection; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source, module), List.of(library), 26)) {
      environment.read((project, files) -> {
        var facade = JavaPsiFacade.getInstance(project);
        var scope = GlobalSearchScope.allScope(project);
        assertNull(facade.findModule("dependency", scope));
        assertNull(facade.findModule("example", scope));
        assertNull(JavaModuleGraphHelper.getInstance().findDescriptorByElement(files.getFirst().getClasses()[0]));
        for (var field : files.getFirst().getClasses()[0].getFields()) {
          assertTrue(((PsiClassType) field.getType()).resolveGenerics().isValidResult());
        }
        return null;
      });
    }
  }

  @Test
  void traversal_terminates_on_cycles_and_missing_modules_and_includes_static_transitive_edges() {
    var snapshots = List.of(
      new JavaPsiEnvironment.SourceSnapshot(temporary.resolve("a/module-info.java"),
        "module a { requires transitive b; requires transitive missing; }", StandardCharsets.UTF_8),
      new JavaPsiEnvironment.SourceSnapshot(temporary.resolve("b/module-info.java"),
        "module b { requires static transitive c; }", StandardCharsets.UTF_8),
      new JavaPsiEnvironment.SourceSnapshot(temporary.resolve("c/module-info.java"),
        "module c { requires transitive a; }", StandardCharsets.UTF_8));
    try (var environment = JavaPsiEnvironment.openSnapshots(jdkHome(), snapshots, List.of(), 26, false)) {
      environment.read((project, files) -> {
        var graph = new JavaPsiModuleGraph(26, false);
        var modules = files.stream().map(file -> file.getModuleDeclaration()).toList();
        for (var module : modules) {
          graph.addRoot(new LightVirtualFile(module.getName()), module);
        }
        assertEquals(Set.of(modules.get(1), modules.get(2)), graph.transitiveDependencies(modules.getFirst()));
        return null;
      });
    }
  }

  @ParameterizedTest
  @CsvSource({"org.w3c.dom, false", "org.w3c.dom.nested, true"})
  void only_the_declared_source_package_participates_in_split_package_detection(String packageName, boolean strictValid) throws IOException {
    var extra = source("Extra.java", "package " + packageName + "; public class Extra {}");
    var caller = source("Example.java", "class Example { org.w3c.dom.Node node; }");
    for (boolean ignore : List.of(false, true)) {
      try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller, extra), List.of(), 26, false, ignore)) {
        environment.read((project, files) -> {
          var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
          assertEquals(ignore || strictValid, type.resolveGenerics().isValidResult());
          return null;
        });
      }
    }
  }

  @Test
  void overlapping_sessions_keep_split_package_policies_independent() throws Exception {
    var library = compile("split", "8", Map.of("Extra.java", "package org.w3c.dom; public class Extra {}"));
    var source = source("Example.java", "class Example { org.w3c.dom.Node node; }");
    try (var strict = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(library), 26, false, false);
         var permissive = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(library), 26, false, true)) {
      assertFalse(strict.<Boolean>read((project, files) ->
        ((PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType()).resolveGenerics().isValidResult()));
      assertTrue(permissive.<Boolean>read((project, files) ->
        ((PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType()).resolveGenerics().isValidResult()));
      assertFalse(strict.<Boolean>read((project, files) ->
        ((PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType()).resolveGenerics().isValidResult()));
    }
  }

  private Path source(String name, String contents) throws IOException {
    return Files.writeString(temporary.resolve(name), contents);
  }

  private Path compile(String name, String release, Map<String, String> sources) throws Exception {
    var directory = Files.createDirectories(temporary.resolve(name));
    var classes = Files.createDirectories(directory.resolve("classes"));
    var command = new java.util.ArrayList<>(List.of(
      Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "javac.exe" : "javac").toString(),
      "--release", release, "-proc:none", "-d", classes.toString()));
    for (var entry : sources.entrySet()) {
      var path = directory.resolve(entry.getKey());
      Files.createDirectories(path.getParent());
      Files.writeString(path, entry.getValue());
      command.add(path.toString());
    }
    var process = new ProcessBuilder(command).redirectErrorStream(true).start();
    var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(0, process.waitFor(), output);
    return classes;
  }

  private Path jar(Path classes) throws IOException {
    var path = classes.getParent().resolve("dependency.jar");
    try (var output = new JarOutputStream(Files.newOutputStream(path)); var files = Files.walk(classes)) {
      for (var file : files.filter(Files::isRegularFile).toList()) {
        output.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
        Files.copy(file, output);
        output.closeEntry();
      }
    }
    return path;
  }

  private static Path jdkHome() {
    return Path.of(System.getProperty("psi.test.jdkHome", System.getProperty("java.home"))).toAbsolutePath().normalize();
  }
}
