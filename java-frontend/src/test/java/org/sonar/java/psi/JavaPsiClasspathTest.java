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

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaPsiClasspathTest {

  @TempDir
  Path temporary;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void resolves_a_generic_method_from_a_dependency_jar_or_directory(boolean archive) throws IOException, InterruptedException {
    var library = library("generic", "public static <T> T identity(T value) { return value; }", archive);
    var caller = Files.writeString(temporary.resolve("Example.java"), """
      class Example { String value = library.Dependency.identity("value"); }
      """);
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller), List.of(library), 21)) {
      environment.read((project, files) -> {
        var call = PsiTreeUtil.findChildOfType(files.getFirst(), PsiMethodCallExpression.class);
        assertNotNull(call);
        var result = call.resolveMethodGenerics();
        assertTrue(result.isValidResult());
        var method = (PsiMethod) result.getElement();
        assertNotNull(method);
        assertEquals("library.Dependency", method.getContainingClass().getQualifiedName());
        assertEquals("java.lang.String", result.getSubstitutor().substitute(method.getTypeParameters()[0]).getCanonicalText());
        assertEquals("java.lang.String", call.getType().getCanonicalText());
        assertTrue(method.getContainingFile().getVirtualFile().getPath().startsWith(library.toString()));
        return null;
      });
    }
    if (archive) {
      Files.delete(library);
    }
  }

  @Test
  void preserves_classpath_order_when_dependencies_contain_the_same_class() throws IOException, InterruptedException {
    var first = library("first", "public static String value() { return null; }", true);
    var second = library("second", "public static Integer value() { return null; }", true);
    var caller = Files.writeString(temporary.resolve("Example.java"), """
      class Example { Object value = library.Dependency.value(); }
      """);
    assertEquals("java.lang.String", callType(caller, List.of(first, second)));
    assertEquals("java.lang.Integer", callType(caller, List.of(second, first)));
    Files.delete(first);
    Files.delete(second);
  }

  @Test
  void explicit_source_wins_over_a_binary_with_the_same_name() throws IOException, InterruptedException {
    var binary = library("binary", "public static Integer value() { return null; }", true);
    var declaration = Files.writeString(temporary.resolve("Dependency.java"),
      "package library; public class Dependency { public static String value() { return null; } }");
    var caller = Files.writeString(temporary.resolve("Example.java"), "class Example { Object value = library.Dependency.value(); }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller, declaration), List.of(binary), 21)) {
      environment.read((project, files) -> {
        var call = PsiTreeUtil.findChildOfType(files.getFirst(), PsiMethodCallExpression.class);
        assertNotNull(call);
        assertEquals("java.lang.String", call.getType().getCanonicalText());
        org.junit.jupiter.api.Assertions.assertSame(files.get(1), call.resolveMethod().getContainingFile());
        return null;
      });
    }
    Files.delete(binary);
  }

  @Test
  void replacing_a_dependency_after_disposal_exposes_the_new_binary() throws IOException, InterruptedException {
    var binary = library("replace", "public static String value() { return null; }", true);
    var caller = Files.writeString(temporary.resolve("Example.java"), "class Example { Object value = library.Dependency.value(); }");
    assertEquals("java.lang.String", callType(caller, List.of(binary)));
    Files.delete(binary);
    assertEquals(binary, library("replace", "public static Integer value() { return null; }", true));
    assertEquals("java.lang.Integer", callType(caller, List.of(binary)));
    Files.delete(binary);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void releases_loaded_archives_after_callback_failure_or_cancellation(boolean canceled) throws Exception {
    var binary = library("failure", "public static String value() { return null; }", true);
    var caller = Files.writeString(temporary.resolve("Example.java"), "class Example { Object value = library.Dependency.value(); }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller), List.of(binary), 21)) {
      var indicator = new EmptyProgressIndicator();
      Class<? extends RuntimeException> expected = canceled ? ProcessCanceledException.class : IllegalStateException.class;
      assertThrows(expected,
        () -> ProgressManager.getInstance().runProcess(() -> environment.read((project, files) -> {
          var call = PsiTreeUtil.findChildOfType(files.getFirst(), PsiMethodCallExpression.class);
          assertNotNull(call);
          assertEquals("java.lang.String", call.getType().getCanonicalText());
          if (canceled) {
            indicator.cancel();
            ProgressManager.checkCanceled();
          }
          throw new IllegalStateException("Callback failed after loading a dependency");
        }), indicator));
    }
    Files.delete(binary);
    assertEquals(binary, library("failure", "public static Integer value() { return null; }", true));
    assertEquals("java.lang.Integer", callType(caller, List.of(binary)));
    Files.delete(binary);
  }

  private String callType(Path caller, List<Path> classpath) {
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller), classpath, 21)) {
      return environment.read((project, files) -> {
        var call = PsiTreeUtil.findChildOfType(files.getFirst(), PsiMethodCallExpression.class);
        assertNotNull(call);
        assertTrue(call.resolveMethodGenerics().isValidResult());
        return call.getType().getCanonicalText();
      });
    }
  }

  private Path library(String name, String method, boolean archive) throws IOException, InterruptedException {
    var root = Files.createDirectories(temporary.resolve(name));
    var source = Files.writeString(root.resolve("Dependency.java"), "package library; public class Dependency { " + method + " }");
    var classes = Files.createDirectories(root.resolve("classes"));
    var executable = System.getProperty("os.name").startsWith("Windows") ? "javac.exe" : "javac";
    var compiler = Path.of(System.getProperty("java.home"), "bin", executable);
    var process = new ProcessBuilder(compiler.toString(), "--release", "17", "-d", classes.toString(), source.toString())
      .redirectErrorStream(true).start();
    var compilerOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(0, process.waitFor(), compilerOutput);
    if (!archive) {
      return classes;
    }
    var jar = root.resolve("dependency.jar");
    try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
      output.putNextEntry(new JarEntry("library/Dependency.class"));
      Files.copy(classes.resolve("library/Dependency.class"), output);
      output.closeEntry();
    }
    return jar;
  }

  private static Path jdkHome() {
    return Path.of(System.getProperty("psi.test.jdkHome", System.getProperty("java.home"))).toAbsolutePath().normalize();
  }
}
