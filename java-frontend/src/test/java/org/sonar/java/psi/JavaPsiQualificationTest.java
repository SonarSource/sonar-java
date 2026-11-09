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

import com.intellij.mock.MockApplication;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.Disposer;
import com.intellij.pom.java.JavaFeature;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JavaPsiQualificationTest {

  @TempDir
  Path temporary;

  @ParameterizedTest
  @ValueSource(ints = {26, 27})
  void selects_preview_levels_and_parses_primitive_patterns(int version) throws IOException {
    var source = source("Example.java", "class Example { boolean test(Object value) { return value instanceof int; } }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), version, true)) {
      environment.read((project, files) -> {
        var level = LanguageLevelProjectExtension.getInstance(project).getLanguageLevel();
        assertEquals(version, level.feature());
        assertTrue(level.isPreview());
        assertTrue(JavaFeature.PRIMITIVE_TYPES_IN_PATTERNS.isSufficient(level));
        assertNull(PsiTreeUtil.findChildOfType(files.getFirst(), PsiErrorElement.class));
        return null;
      });
    }
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), version)) {
      environment.read((project, files) -> {
        var level = LanguageLevelProjectExtension.getInstance(project).getLanguageLevel();
        assertFalse(level.isPreview());
        assertFalse(JavaFeature.PRIMITIVE_TYPES_IN_PATTERNS.isSufficient(level));
        return null;
      });
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"rt.jar", "android.jar"})
  void explicit_incomplete_runtime_suppresses_the_host_jdk(String name) throws IOException {
    var archive = temporary.resolve(name);
    try (var output = new JarOutputStream(Files.newOutputStream(archive))) {
      output.putNextEntry(new JarEntry("META-INF/"));
      output.closeEntry();
    }
    var source = source("Example.java", "class Example { String value; }");
    try (var environment = JavaPsiEnvironment.open(temporary.resolve("absent-jdk"), List.of(source), List.of(archive), 21)) {
      environment.read((project, files) -> {
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        assertNull(type.resolve());
        assertNull(JavaPsiFacade.getInstance(project).findClass("java.lang.String", GlobalSearchScope.allScope(project)));
        return null;
      });
    }
    Files.delete(archive);
  }

  @Test
  void supplied_android_runtime_resolves_only_its_own_classes() throws IOException {
    var archive = Path.of(System.getProperty("psi.test.repositoryRoot", "../../sonar-java"), "java-frontend/src/test/resources/android.jar").toRealPath();
    var source = source("Example.java", "class Example { String missing; Integer available; }");
    try (var environment = JavaPsiEnvironment.open(temporary, List.of(source), List.of(archive), 17)) {
      environment.read((project, files) -> {
        var fields = files.getFirst().getClasses()[0].getFields();
        assertNull(((PsiClassType) fields[0].getType()).resolve());
        var integer = ((PsiClassType) fields[1].getType()).resolve();
        assertNotNull(integer);
        assertTrue(integer.getContainingFile().getVirtualFile().getPath().startsWith(archive.toString()));
        return null;
      });
    }
  }

  @Test
  void malformed_selected_provider_preserves_the_ecj_class_format_failure() throws IOException {
    var previousApplication = ApplicationManager.getApplication();
    var lib = Files.createDirectories(temporary.resolve("jdk/lib"));
    var provider = lib.resolve("jrt-fs.jar");
    try (var output = new JarOutputStream(Files.newOutputStream(provider))) {
      output.putNextEntry(new JarEntry("jdk/internal/jrtfs/JrtFileSystemProvider.class"));
      output.closeEntry();
    }
    var failure = assertThrows(ClassFormatError.class,
      () -> JavaPsiEnvironment.open(jdkHome(), List.of(), List.of(provider), 21));
    assertEquals("Truncated class file", failure.getMessage());
    assertSame(previousApplication, ApplicationManager.getApplication());
    Files.delete(provider);
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(), List.of(), 21)) {
      assertEquals(Integer.valueOf(0), environment.read((project, files) -> files.size()));
    }
  }

  @Test
  void cancellation_and_callback_failure_release_read_access_and_allow_recovery() throws IOException {
    var previousApplication = ApplicationManager.getApplication();
    var source = source("Example.java", "class Example { String value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 21)) {
      var failure = new IllegalStateException("callback failure");
      assertSame(failure, assertThrows(IllegalStateException.class,
        () -> environment.read((project, files) -> { throw failure; })));
      var indicator = new EmptyProgressIndicator();
      assertThrows(ProcessCanceledException.class, () -> ProgressManager.getInstance().runProcess(() -> {
        indicator.cancel();
        environment.read((project, files) -> { fail("Canceled action must not run"); return null; });
      }, indicator));
      assertEquals("Example", environment.read((project, files) -> files.getFirst().getClasses()[0].getName()));
    }
    assertSame(previousApplication, ApplicationManager.getApplication());
  }

  @Test
  void independent_sessions_resolve_concurrently_and_dispose_cleanly() throws Exception {
    var previousApplication = ApplicationManager.getApplication();
    var first = source("First.java", "package first; class First { String value; }");
    var second = source("Second.java", "package second; class Second { Integer value; }");
    var ready = new CountDownLatch(2);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var jobs = List.of(first, second).stream().map(source -> workers.submit(() -> {
        try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 21)) {
          ready.countDown();
          assertTrue(ready.await(15, TimeUnit.SECONDS));
          return environment.read((project, files) -> {
            var own = files.getFirst().getClasses()[0];
            var other = source.equals(first) ? "second.Second" : "first.First";
            assertNull(JavaPsiFacade.getInstance(project).findClass(other, GlobalSearchScope.allScope(project)));
            return ((PsiClassType) own.getFields()[0].getType()).resolve().getQualifiedName();
          });
        }
      })).toList();
      assertEquals("java.lang.String", jobs.get(0).get(30, TimeUnit.SECONDS));
      assertEquals("java.lang.Integer", jobs.get(1).get(30, TimeUnit.SECONDS));
    }
    assertSame(previousApplication, ApplicationManager.getApplication());
  }

  @Test
  void isolated_classloader_embeds_psi_without_replacing_host_application() throws Exception {
    PsiRuntimeTestSupport.isolated(JavaPsiQualificationTest.class, "hostApplication", temporary);
  }

  private static void hostApplication(Path temporary) throws Exception {
    var previous = ApplicationManager.getApplication();
    var owner = Disposer.newDisposable();
    var source = Files.writeString(temporary.resolve("Example.java"), "class Example { String value; }");
    var classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    var urls = Arrays.stream(classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))).map(path -> {
      try {
        return Path.of(path).toUri().toURL();
      } catch (java.net.MalformedURLException e) {
        throw new IllegalArgumentException(e);
      }
    }).toArray(URL[]::new);
    try (var loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
      var host = new MockApplication(owner);
      ApplicationManager.setApplication(host);
      var type = loader.loadClass(JavaPsiEnvironment.class.getName());
      assertNotSame(JavaPsiEnvironment.class, type);
      try (var environment = (AutoCloseable) type.getMethod("open", Path.class, List.class, List.class, int.class)
        .invoke(null, jdkHome(), List.of(source), List.of(), 21)) {
        var query = (BiFunction<Object, List<?>, Integer>) (project, files) -> files.size();
        assertEquals(1, type.getMethod("read", BiFunction.class).invoke(environment, query));
        assertSame(host, ApplicationManager.getApplication());
      }
      assertSame(host, ApplicationManager.getApplication());
    } finally {
      try {
        Disposer.dispose(owner);
      } finally {
        ApplicationManager.setApplication(previous);
      }
    }
  }

  @Test
  void resolves_a_jdk_module_import() throws IOException {
    var source = source("Example.java", "import module java.base; class Example { List<String> values; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 26)) {
      environment.read((project, files) -> {
        assertNull(PsiTreeUtil.findChildOfType(files.getFirst(), PsiErrorElement.class));
        var facade = JavaPsiFacade.getInstance(project);
        assertNotNull(facade.findModule("java.base", GlobalSearchScope.allScope(project)));
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        assertNotNull(type.resolve());
        assertEquals("java.util.List", type.resolve().getQualifiedName());
        return null;
      });
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26})
  void maps_each_supported_sonar_source_level(int version) throws IOException {
    var input = source("Example.java", "class Example { String value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(input), List.of(), version)) {
      environment.read((project, files) -> {
        assertEquals(version, LanguageLevelProjectExtension.getInstance(project).getLanguageLevel().feature());
        assertNull(PsiTreeUtil.findChildOfType(files.getFirst(), PsiErrorElement.class));
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        assertEquals("java.lang.String", type.resolve().getQualifiedName());
        return null;
      });
    }
  }

  @Test
  void decoded_snapshots_preserve_paths_encodings_line_endings_and_offsets() {
    var declarations = temporary.resolve("Declarations.java");
    var caller = temporary.resolve("Caller.java");
    var text = "package sample;\r\nclass Helper { String café; }\r\n";
    var snapshots = List.of(
      new JavaPsiEnvironment.SourceSnapshot(declarations, text, StandardCharsets.ISO_8859_1),
      new JavaPsiEnvironment.SourceSnapshot(caller, "package sample; class Caller { Helper value; }", StandardCharsets.UTF_8));
    try (var environment = JavaPsiEnvironment.openSnapshots(jdkHome(), snapshots, List.of(), 26, false)) {
      environment.read((project, files) -> {
        var file = files.getFirst();
        assertEquals(text, file.getText());
        assertEquals(declarations.toString(), file.getVirtualFile().getPath());
        assertEquals(StandardCharsets.ISO_8859_1, file.getVirtualFile().getCharset());
        assertFalse(file.getVirtualFile().isWritable());
        var field = file.getClasses()[0].getFields()[0];
        assertEquals(text.indexOf("café"), field.getNameIdentifier().getTextOffset());
        var reference = (PsiClassType) files.get(1).getClasses()[0].getFields()[0].getType();
        assertSame(file.getClasses()[0], reference.resolve());
        return null;
      });
    }
  }

  @Test
  void opened_sources_are_snapshots_even_before_the_first_read() throws IOException {
    var original = "class Example { String original; }\r\n";
    var input = source("Example.java", original);
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(input), List.of(), 21)) {
      Files.writeString(input, "class Changed {}\n");
      Files.delete(input);
      assertEquals(original, environment.read((project, files) -> files.getFirst().getText()));
    }
  }

  @Test
  void duplicate_source_declarations_follow_explicit_input_order() throws IOException {
    var first = source("First.java", "package sample; class Duplicate { String first; }");
    var second = source("Second.java", "package sample; class Duplicate { Integer second; }");
    for (var inputs : List.of(List.of(first, second), List.of(second, first))) {
      try (var environment = JavaPsiEnvironment.open(jdkHome(), inputs, List.of(), 21)) {
        environment.read((project, files) -> {
          var facade = JavaPsiFacade.getInstance(project);
          var scope = GlobalSearchScope.allScope(project);
          assertSame(files.getFirst().getClasses()[0], facade.findClass("sample.Duplicate", scope));
          assertEquals(2, facade.findClasses("sample.Duplicate", scope).length);
          return null;
        });
      }
    }
  }

  private Path source(String name, String text) throws IOException {
    return Files.writeString(temporary.resolve(name), text);
  }

  private static Path jdkHome() {
    return Path.of(System.getProperty("psi.test.jdkHome", System.getProperty("java.home"))).toAbsolutePath().normalize();
  }
}
