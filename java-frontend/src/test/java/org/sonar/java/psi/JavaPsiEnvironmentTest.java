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
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaPsiEnvironmentTest {

  @TempDir
  Path temporary;

  @Test
  void cancellable_sessions_retain_the_callers_progress_scope() {
    var previousApplication = ApplicationManager.getApplication();
    var canceled = new AtomicBoolean();
    var configuration = new JavaPsiConfiguration(null, List.of(), 21, false, false);
    try (var environment = JavaPsiEnvironment.open(configuration, List.of(), canceled::get)) {
      var indicator = new EmptyProgressIndicator();
      assertThrows(ProcessCanceledException.class, () -> ProgressManager.getInstance().runProcess(() -> environment.read((project, files) -> {
        indicator.cancel();
        ProgressManager.checkCanceled();
        throw new AssertionError("Cancellation must propagate");
      }), indicator));
      assertThrows(ProcessCanceledException.class, () -> environment.read((project, files) -> {
        canceled.set(true);
        return environment.read((unused, inputs) -> inputs.size());
      }));
    }
    assertSame(previousApplication, ApplicationManager.getApplication());
  }

  @Test
  void parses_a_source_file_and_resolves_a_jdk_type() throws IOException {
    var source = Files.writeString(temporary.resolve("Example.java"), "package sample; class Example { String value; }");
    var release = new Properties();
    try (var input = Files.newInputStream(jdkHome().resolve("release"))) {
      release.load(input);
    }
    var selectedVersion = Runtime.Version.parse(release.getProperty("JAVA_VERSION").replace("\"", "")).feature();
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 21)) {
      environment.read((project, files) -> {
        assertEquals(LanguageLevel.JDK_21, LanguageLevelProjectExtension.getInstance(project).getLanguageLevel());
        assertEquals(1, files.size());
        var file = files.getFirst();
        assertEquals("sample", file.getPackageName());
        assertNull(PsiTreeUtil.findChildOfType(file, PsiErrorElement.class));
        var declaration = file.getClasses()[0];
        assertEquals("sample.Example", declaration.getQualifiedName());
        var stringType = (PsiClassType) declaration.getFields()[0].getType();
        var resolved = stringType.resolve();
        assertNotNull(resolved);
        assertEquals("java.lang.String", resolved.getQualifiedName());
        assertTrue(resolved.getContainingFile().getVirtualFile().getPath().contains("java.base/java/lang/String.class"));
        assertTrue(resolved.getContainingFile().getVirtualFile().getPath().startsWith(jdkHome().toString()));
        try (var bytes = new DataInputStream(new ByteArrayInputStream(resolved.getContainingFile().getVirtualFile().contentsToByteArray()))) {
          assertEquals(0xCAFEBABE, bytes.readInt());
          bytes.readUnsignedShort();
          assertEquals(selectedVersion + 44, bytes.readUnsignedShort());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
        return null;
      });
    }
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment"));
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"));
  }

  @ParameterizedTest
  @ValueSource(ints = {26, 27})
  void parses_modern_java_and_resolves_types_at_the_requested_language_level(int version) throws IOException {
    var input = source("Example.java", """
      class Parent { Parent(String value) {} }
      class Example extends Parent {
        Example(String value) {
          var trimmed = value.trim();
          super(trimmed);
        }
      }
      """);
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(input), List.of(), version)) {
      environment.read((project, files) -> {
        var level = LanguageLevelProjectExtension.getInstance(project).getLanguageLevel();
        assertEquals(version, level.feature());
        assertFalse(level.isUnsupported());
        assertFalse(level.isPreview());
        assertNull(PsiTreeUtil.findChildOfType(files.getFirst(), PsiErrorElement.class));
        var call = PsiTreeUtil.findChildOfType(files.getFirst(), PsiMethodCallExpression.class);
        assertNotNull(call);
        assertNotNull(call.resolveMethod());
        assertEquals("java.lang.String", call.getType().getCanonicalText());
        return null;
      });
    }
  }

  @Test
  void resolves_secondary_classes_nested_classes_and_generic_calls_across_explicit_files() throws IOException {
    var caller = source("Example.java", """
      package sample;
      class Example {
        String value = Secondary.identity("value");
        Secondary.Nested nested;
      }
      """);
    var declarations = source("Declarations.java", """
      package sample;
      class Declarations {}
      class Secondary {
        static <T> T identity(T value) { return value; }
        static class Nested {}
      }
      """);
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller, declarations), List.of(), 21)) {
      environment.read((project, files) -> {
        var call = PsiTreeUtil.findChildOfType(files.getFirst(), PsiMethodCallExpression.class);
        assertNotNull(call);
        var result = call.resolveMethodGenerics();
        assertTrue(result.isValidResult());
        var method = (PsiMethod) result.getElement();
        assertNotNull(method);
        assertEquals("sample.Secondary", method.getContainingClass().getQualifiedName());
        assertSame(files.get(1), method.getContainingFile());
        assertEquals("java.lang.String", result.getSubstitutor().substitute(method.getTypeParameters()[0]).getCanonicalText());
        assertEquals("java.lang.String", call.getType().getCanonicalText());
        var nestedType = (PsiClassType) files.getFirst().getClasses()[0].getFields()[1].getType();
        assertEquals("sample.Secondary.Nested", nestedType.resolve().getQualifiedName());
        return null;
      });
    }
  }

  @Test
  void does_not_resolve_unsupplied_sources_in_the_same_directory() throws IOException {
    var caller = source("Example.java", "package sample; class Example { Hidden value; }");
    source("Hidden.java", "package sample; class Hidden {}");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller), List.of(), 21)) {
      environment.read((project, files) -> {
        assertNull(PsiTreeUtil.findChildOfType(files.getFirst(), PsiErrorElement.class));
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        assertNull(type.resolve());
        assertNull(JavaPsiFacade.getInstance(project).findClass("sample.Hidden", GlobalSearchScope.allScope(project)));
        assertNull(JavaPsiFacade.getInstance(project).findClass("org.junit.jupiter.api.Test", GlobalSearchScope.allScope(project)));
        return null;
      });
    }
  }

  @Test
  void disposing_one_project_preserves_another_live_project() throws IOException {
    var first = source("First.java", "package first; class First { String value; }");
    var second = source("Second.java", "package second; class Second { Integer value; }");
    try (var firstEnvironment = JavaPsiEnvironment.open(jdkHome(), List.of(first), List.of(), 21)) {
      try (var secondEnvironment = JavaPsiEnvironment.open(jdkHome(), List.of(second), List.of(), 21)) {
        secondEnvironment.read((project, files) -> {
          assertEquals("second.Second", files.getFirst().getClasses()[0].getQualifiedName());
          assertNull(JavaPsiFacade.getInstance(project).findClass("first.First", GlobalSearchScope.allScope(project)));
          return null;
        });
      }
      firstEnvironment.read((project, files) -> {
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        assertEquals("java.lang.String", type.resolve().getQualifiedName());
        assertNull(JavaPsiFacade.getInstance(project).findClass("second.Second", GlobalSearchScope.allScope(project)));
        return null;
      });
    }
  }

  @Test
  void close_is_idempotent_and_prevents_further_queries() throws IOException {
    var input = source("Example.java", "class Example {}");
    var environment = JavaPsiEnvironment.open(jdkHome(), List.of(input), List.of(), 21);
    environment.close();
    environment.close();
    assertThrows(IllegalStateException.class, () -> environment.read((project, files) -> files.size()).intValue());
  }

  @Test
  void repeated_sessions_load_updated_source_text() throws IOException {
    var input = source("Example.java", "class Example { String value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(input), List.of(), 21)) {
      assertEquals("String", environment.read((project, files) -> files.getFirst().getClasses()[0].getFields()[0].getType().getPresentableText()));
    }
    Files.writeString(input, "class Example { Integer value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(input), List.of(), 21)) {
      assertEquals("Integer", environment.read((project, files) -> files.getFirst().getClasses()[0].getFields()[0].getType().getPresentableText()));
    }
    Files.delete(input);
    assertFalse(Files.exists(input));
  }

  @Test
  void rejects_unsupported_java_versions_instead_of_silently_using_another_level() {
    assertThrows(IllegalArgumentException.class, () -> JavaPsiEnvironment.open(jdkHome(), List.of(), List.of(), 99));
  }

  @Test
  void rejects_a_missing_selected_jdk_instead_of_using_the_running_jvm() {
    assertThrows(IllegalArgumentException.class, () -> JavaPsiEnvironment.open(temporary, List.of(), List.of(), 21));
  }

  @Test
  void rejects_missing_source_files_but_tolerates_missing_classpath_entries() throws IOException {
    var input = source("Example.java", "class Example {}");
    var missing = temporary.resolve("Missing.java");
    assertThrows(IllegalArgumentException.class, () -> JavaPsiEnvironment.open(jdkHome(), List.of(missing), List.of(), 21));
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(input), List.of(missing), 21)) {
      assertEquals(1, environment.read((project, files) -> files.size()).intValue());
    }
  }

  @Test
  void rejects_a_missing_or_corrupt_jdk_provider_and_releases_the_failed_environment() throws IOException {
    var previousApplication = ApplicationManager.getApplication();
    var library = Files.createDirectories(temporary.resolve("jdk/lib"));
    Files.createFile(library.resolve("modules"));
    var selectedJdk = library.getParent();
    assertThrows(UncheckedIOException.class, () -> JavaPsiEnvironment.open(selectedJdk, List.of(), List.of(), 27));
    assertSame(previousApplication, ApplicationManager.getApplication());
    Files.writeString(library.resolve("jrt-fs.jar"), "corrupt archive");
    assertThrows(IllegalStateException.class, () -> JavaPsiEnvironment.open(selectedJdk, List.of(), List.of(), 27));
    assertSame(previousApplication, ApplicationManager.getApplication());
    var source = source("Example.java", "class Example { String value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(source), List.of(), 27)) {
      assertEquals("java.lang.String", environment.read((project, files) -> files.getFirst().getClasses()[0].getFields()[0].getType().getCanonicalText()));
    }
  }

  @Test
  void refuses_to_replace_an_existing_intellij_application() throws Exception {
    PsiRuntimeTestSupport.isolated(JavaPsiEnvironmentTest.class, "foreignApplication", temporary);
  }

  private static void foreignApplication(Path temporary) {
    var previous = ApplicationManager.getApplication();
    var owner = Disposer.newDisposable();
    try {
      var host = new MockApplication(owner);
      ApplicationManager.setApplication(host);
      assertThrows(IllegalStateException.class, () -> JavaPsiEnvironment.open(jdkHome(), List.of(), List.of(), 21));
      assertSame(host, ApplicationManager.getApplication());
    } finally {
      try {
        Disposer.dispose(owner);
      } finally {
        ApplicationManager.setApplication(previous);
      }
    }
  }

  private Path source(String filename, String contents) throws IOException {
    return Files.writeString(temporary.resolve(filename), contents);
  }

  private static Path jdkHome() {
    return Path.of(System.getProperty("psi.test.jdkHome", System.getProperty("java.home"))).toAbsolutePath().normalize();
  }
}
