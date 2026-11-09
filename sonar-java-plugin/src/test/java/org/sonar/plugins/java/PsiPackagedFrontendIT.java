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
package org.sonar.plugins.java;

import com.intellij.mock.MockApplication;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;

class PsiPackagedFrontendIT {
  @TempDir
  Path temporary;

  @Test
  void packaged_parser_allows_normal_process_exit() throws Exception {
    var output = temporary.resolve("parser.log");
    var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
      "-Dpsi.plugin.artifact=" + System.getProperty("psi.plugin.artifact"),
      "-cp", System.getProperty("java.class.path"), getClass().getName())
      .redirectErrorStream(true).redirectOutput(output.toFile()).start();
    try {
      assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("packaged parser process exited normally: %s", Files.readString(output)).isTrue();
      assertThat(process.exitValue()).as("packaged parser output: %s", Files.readString(output)).isZero();
    } finally {
      process.destroyForcibly();
    }
  }

  public static void main(String[] args) throws Exception {
    new PsiPackagedFrontendIT().packaged_parser_resolves_jdk_types_without_changing_the_host_application();
  }

  @Test
  void packaged_parser_resolves_jdk_types_without_changing_the_host_application() throws Exception {
    var artifact = Path.of(System.getProperty("psi.plugin.artifact"));
    try (var jar = new JarFile(artifact.toFile())) {
      assertThat(jar.getEntry("org/sonar/java/psi/internal/intellij/core/CoreApplicationEnvironment.class")).isNotNull();
      assertThat(jar.stream().map(java.util.jar.JarEntry::getName))
        .noneMatch(name -> name.startsWith("com/intellij/") || name.startsWith("kotlin/")
          || name.startsWith("org/sonar/java/jdt/") || name.startsWith("org/eclipse/jdt/") || name.startsWith("org/eclipse/core/")
          || name.startsWith("org/apache/jasper/compiler/JDTCompiler"));
      assertThat(jar.getEntry("org/sonar/java/model/frontend.properties")).isNull();
    }
    URL[] urls = {artifact.toUri().toURL(), InputFile.class.getProtectionDomain().getCodeSource().getLocation(),
      Logger.class.getProtectionDomain().getCodeSource().getLocation()};
    var previousLoader = Thread.currentThread().getContextClassLoader();
    var previousApplication = ApplicationManager.getApplication();
    var owner = Disposer.newDisposable();
    var host = new MockApplication(owner);
    ApplicationManager.setApplication(host);
    try (var loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      var modeType = loader.loadClass("org.sonar.java.model.JParserConfig$Mode");
      var mode = modeType.getEnumConstants()[1];
      var versionType = loader.loadClass("org.sonar.plugins.java.api.JavaVersion");
      var version = loader.loadClass("org.sonar.java.model.JavaVersionImpl").getConstructor(int.class).newInstance(21);
      var selected = modeType.getMethod("create", versionType, List.class).invoke(mode, version, List.of());
      assertThat(selected.getClass().getName()).isEqualTo("org.sonar.java.psi.PsiParserConfig");
      var tree = loader.loadClass("org.sonar.java.model.JParserConfig").getMethod("parse", String.class, String.class)
        .invoke(selected, "Sample.java", "class Sample { java.util.List<String> values; String trim(String text) { return text.trim(); } }");
      try {
        var classes = (List<?>) call(loader, "org.sonar.plugins.java.api.tree.CompilationUnitTree", tree, "types");
        var members = (List<?>) call(loader, "org.sonar.plugins.java.api.tree.ClassTree", classes.getFirst(), "members");
        var typeTree = call(loader, "org.sonar.plugins.java.api.tree.VariableTree", members.getFirst(), "type");
        var type = call(loader, "org.sonar.plugins.java.api.tree.TypeTree", typeTree, "symbolType");
        assertThat(call(loader, "org.sonar.plugins.java.api.semantic.Type", type, "fullyQualifiedName")).isEqualTo("java.util.List");
        var arguments = (List<?>) call(loader, "org.sonar.plugins.java.api.semantic.Type", type, "typeArguments");
        assertThat(call(loader, "org.sonar.plugins.java.api.semantic.Type", arguments.getFirst(), "fullyQualifiedName")).isEqualTo("java.lang.String");
        var sema = tree.getClass().getField("sema").get(tree);
        var string = loader.loadClass("org.sonar.plugins.java.api.semantic.Sema").getMethod("getClassType", String.class)
          .invoke(sema, "java.lang.String");
        assertThat(call(loader, "org.sonar.plugins.java.api.semantic.Type", string, "isUnknown")).isEqualTo(false);
      } finally {
        ((Runnable) tree.getClass().getMethod("getEnvironmentCleaner").invoke(tree)).run();
      }
      assertThat(ApplicationManager.getApplication()).isSameAs(host);
    } finally {
      Thread.currentThread().setContextClassLoader(previousLoader);
      try {
        Disposer.dispose(owner);
      } finally {
        ApplicationManager.setApplication(previousApplication);
      }
    }
  }

  @Test
  void packaged_jasper_translates_custom_tag_files() throws Exception {
    Path webapp = Files.createDirectories(temporary.resolve("webapp"));
    Path tags = Files.createDirectories(webapp.resolve("WEB-INF/tags"));
    Path output = Files.createDirectories(temporary.resolve("generated"));
    Files.writeString(tags.resolve("hello.tag"),
      "<%@ attribute name=\"value\" type=\"java.lang.String\" required=\"true\" %><%= value %>");
    Files.writeString(webapp.resolve("index.jsp"),
      "<%@ taglib prefix=\"t\" tagdir=\"/WEB-INF/tags\" %><t:hello value=\"hello\" />");
    URL[] urls = {Path.of(System.getProperty("psi.plugin.artifact")).toUri().toURL(),
      InputFile.class.getProtectionDomain().getCodeSource().getLocation(), Logger.class.getProtectionDomain().getCodeSource().getLocation()};
    var previousLoader = Thread.currentThread().getContextClassLoader();
    try (var loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(loader);
      var factoryType = loader.loadClass("javax.servlet.jsp.JspFactory");
      factoryType.getMethod("setDefaultFactory", factoryType).invoke(null,
        loader.loadClass("org.apache.jasper.runtime.JspFactoryImpl").getConstructor().newInstance());
      var contextType = loader.loadClass("org.apache.jasper.servlet.JspCServletContext");
      var context = contextType.getConstructor(java.io.PrintWriter.class, URL.class, ClassLoader.class, boolean.class, boolean.class)
        .newInstance(new java.io.PrintWriter(new java.io.StringWriter()), webapp.toUri().toURL(), loader, false, true);
      var optionsType = loader.loadClass("org.apache.jasper.Options");
      var constructor = loader.loadClass("org.sonar.java.jsp.JasperOptions").getDeclaredConstructor(contextType, Path.class);
      constructor.setAccessible(true);
      var options = constructor.newInstance(context, output);
      var servletContextType = loader.loadClass("javax.servlet.ServletContext");
      var runtimeType = loader.loadClass("org.apache.jasper.compiler.JspRuntimeContext");
      var runtime = runtimeType.getConstructor(servletContextType, optionsType).newInstance(context, options);
      var compilationType = loader.loadClass("org.apache.jasper.JspCompilationContext");
      var compilation = compilationType.getConstructor(String.class, optionsType, servletContextType,
        loader.loadClass("org.apache.jasper.servlet.JspServletWrapper"), runtimeType)
        .newInstance("/index.jsp", options, context, null, runtime);
      compilationType.getMethod("setClassLoader", ClassLoader.class).invoke(compilation, loader);
      var compiler = compilationType.getMethod("createCompiler").invoke(compilation);
      loader.loadClass("org.apache.jasper.compiler.Compiler").getMethod("compile", boolean.class, boolean.class)
        .invoke(compiler, false, true);
      Path generated = Path.of((String) compilationType.getMethod("getServletJavaFileName").invoke(compilation));
      assertThat(Files.readString(generated)).contains("org.apache.jsp.tag.web.hello_tag");
      assertThat(output.resolve("org/apache/jsp/tag/web/hello_tag.class")).exists();
    } finally {
      Thread.currentThread().setContextClassLoader(previousLoader);
    }
  }

  private static Object call(ClassLoader loader, String api, Object target, String method) throws Exception {
    return loader.loadClass(api).getMethod(method).invoke(target);
  }
}
