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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaPsiMetadataTest {
  @TempDir
  Path temporary;

  @Test
  void explicit_package_info_is_visible_without_indexing_its_directory() throws Exception {
    var info = source("package-info.java", "@Deprecated(since=\"source\") package sample;");
    source("Unlisted.java", "package sample; class Unlisted {}");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(info), List.of(), 26)) {
      environment.read((project, files) -> {
        var facade = JavaPsiFacade.getInstance(project);
        var declaration = facade.findPackage("sample");
        assertNotNull(declaration);
        assertNotNull(declaration.getAnnotationList());
        var annotation = declaration.getAnnotationList().findAnnotation("java.lang.Deprecated");
        assertNotNull(annotation);
        assertEquals("\"source\"", annotation.findAttributeValue("since").getText());
        assertNull(facade.findClass("sample.Unlisted", GlobalSearchScope.allScope(project)));
        return null;
      });
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void binary_package_annotations_nested_classes_and_signatures_are_available(boolean archive) throws Exception {
    var library = library(archive);
    var caller = source("Example.java", "class Example { library.Api.Nested<Integer> value; }");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(caller), List.of(library), 26)) {
      environment.read((project, files) -> {
        var facade = JavaPsiFacade.getInstance(project);
        var annotation = facade.findPackage("library").getAnnotationList().findAnnotation("java.lang.Deprecated");
        assertNotNull(annotation);
        assertEquals("\"binary\"", annotation.findAttributeValue("since").getText());
        var api = facade.findClass("library.Api", GlobalSearchScope.allScope(project));
        assertNotNull(api.getModifierList().findAnnotation("java.lang.Deprecated"));
        var moduleFile = api.getContainingFile().getVirtualFile().getParent().getParent().findChild("module-info.class");
        assertNotNull(moduleFile);
        var module = ((PsiJavaFile) PsiManager.getInstance(project).findFile(moduleFile)).getModuleDeclaration();
        assertNotNull(module);
        assertEquals("dependency", module.getName());
        assertNotNull(module.getModifierList().findAnnotation("java.lang.Deprecated"));
        assertEquals("library", module.getExports().iterator().next().getPackageName());
        var type = (PsiClassType) files.getFirst().getClasses()[0].getFields()[0].getType();
        var result = type.resolveGenerics();
        assertTrue(result.isValidResult());
        var nested = result.getElement();
        assertEquals("library.Api.Nested", nested.getQualifiedName());
        assertEquals("java.lang.Number", nested.getTypeParameters()[0].getExtendsListTypes()[0].getCanonicalText());
        assertEquals("java.lang.Integer", result.getSubstitutor().substitute(nested.getTypeParameters()[0]).getCanonicalText());
        var method = api.findMethodsByName("identity", false)[0];
        assertEquals("T", method.getReturnType().getCanonicalText());
        assertEquals("java.lang.CharSequence", method.getTypeParameters()[0].getExtendsListTypes()[0].getCanonicalText());
        assertEquals("java.io.IOException", method.getThrowsList().getReferencedTypes()[0].getCanonicalText());
        return null;
      });
    }
    if (archive) {
      Files.delete(library);
    }
  }

  @Test
  void package_info_snapshots_are_isolated_between_sessions() {
    var path = temporary.resolve("package-info.java");
    var first = new JavaPsiEnvironment.SourceSnapshot(path, "@Deprecated(since=\"first\") package sample;", StandardCharsets.UTF_8);
    var second = new JavaPsiEnvironment.SourceSnapshot(path, "@Deprecated(since=\"second\") package sample;", StandardCharsets.UTF_8);
    try (var left = JavaPsiEnvironment.openSnapshots(jdkHome(), List.of(first), List.of(), 26, false);
      var right = JavaPsiEnvironment.openSnapshots(jdkHome(), List.of(second), List.of(), 26, false)) {
      assertEquals("\"first\"", left.read((project, files) -> JavaPsiFacade.getInstance(project).findPackage("sample")
        .getAnnotationList().findAnnotation("java.lang.Deprecated").findAttributeValue("since").getText()));
      assertEquals("\"second\"", right.read((project, files) -> JavaPsiFacade.getInstance(project).findPackage("sample")
        .getAnnotationList().findAnnotation("java.lang.Deprecated").findAttributeValue("since").getText()));
    }
  }

  @Test
  void source_module_metadata_keeps_annotations_and_directives() throws Exception {
    var descriptor = source("module-info.java", """
      @Deprecated module example {
        requires transitive java.sql;
        requires static java.logging;
        exports api to java.base;
        opens api to java.sql;
        uses api.Service;
        provides api.Service with api.Provider;
      }
      """);
    var service = source("Service.java", "package api; public interface Service {}");
    var provider = source("Provider.java", "package api; public class Provider implements Service {}");
    try (var environment = JavaPsiEnvironment.open(jdkHome(), List.of(descriptor, service, provider), List.of(), 26)) {
      environment.read((project, files) -> {
        var module = files.getFirst().getModuleDeclaration();
        assertNotNull(module);
        assertEquals("example", module.getName());
        assertNotNull(module.getModifierList().findAnnotation("java.lang.Deprecated"));
        var requires = module.getRequires().iterator();
        assertTrue(requires.next().hasModifierProperty(PsiModifier.TRANSITIVE));
        assertTrue(requires.next().hasModifierProperty(PsiModifier.STATIC));
        assertEquals("api", module.getExports().iterator().next().getPackageName());
        assertEquals(List.of("java.base"), new ArrayList<>(module.getExports().iterator().next().getModuleNames()));
        assertEquals("api", module.getOpens().iterator().next().getPackageName());
        assertEquals("api.Service", module.getUses().iterator().next().getClassReference().getQualifiedName());
        assertEquals("api.Service", module.getProvides().iterator().next().getInterfaceReference().getQualifiedName());
        return null;
      });
    }
  }

  private Path library(boolean archive) throws Exception {
    var classes = Files.createDirectories(temporary.resolve("classes"));
    var sources = Map.of(
      "module-info.java", "@Deprecated module dependency { exports library; }",
      "package-info.java", "@Deprecated(since=\"binary\") package library;",
      "Api.java", """
        package library;
        @Deprecated public class Api {
          public static class Nested<T extends Number> {}
          public static <T extends CharSequence> T identity(T value) throws java.io.IOException { return value; }
        }
        """);
    var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin",
      System.getProperty("os.name").startsWith("Windows") ? "javac.exe" : "javac").toString(),
      "--release", "17", "-proc:none", "-d", classes.toString()));
    for (var entry : sources.entrySet()) {
      command.add(source(entry.getKey(), entry.getValue()).toString());
    }
    var process = new ProcessBuilder(command).redirectErrorStream(true).start();
    var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(0, process.waitFor(), output);
    if (!archive) {
      return classes;
    }
    var jar = temporary.resolve("metadata.jar");
    try (var outputStream = new JarOutputStream(Files.newOutputStream(jar)); var paths = Files.walk(classes)) {
      for (var path : paths.filter(Files::isRegularFile).toList()) {
        outputStream.putNextEntry(new JarEntry(classes.relativize(path).toString().replace('\\', '/')));
        Files.copy(path, outputStream);
        outputStream.closeEntry();
      }
    }
    return jar;
  }

  private Path source(String name, String contents) throws Exception {
    return Files.writeString(temporary.resolve(name), contents);
  }

  private static Path jdkHome() {
    return Path.of(System.getProperty("psi.test.jdkHome", System.getProperty("java.home")));
  }
}
