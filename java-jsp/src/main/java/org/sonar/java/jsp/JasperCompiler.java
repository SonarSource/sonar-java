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
package org.sonar.java.jsp;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.el.ExpressionFactory;
import javax.servlet.Servlet;
import javax.servlet.jsp.JspFactory;
import javax.tools.ToolProvider;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.SmapStratum;

/** Generates JSP sources and uses the JDK compiler only for tag files that Jasper must load during translation. */
public final class JasperCompiler extends Compiler {

  @Override
  protected void generateClass(Map<String, SmapStratum> sourceMaps) throws IOException, JasperException {
    var compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new JasperException("JSP tag files require a runtime with the jdk.compiler module");
    }
    StringWriter diagnostics = new StringWriter();
    try (var files = compiler.getStandardFileManager(null, null, Charset.forName(options.getJavaEncoding()))) {
      var sources = files.getJavaFileObjects(ctxt.getServletJavaFileName());
      List<String> arguments = List.of("-proc:none", "-implicit:none", "-classpath", compilationClasspath(),
        "-d", options.getScratchDir().getAbsolutePath());
      if (!Boolean.TRUE.equals(compiler.getTask(diagnostics, files, null, arguments, null, sources).call())) {
        throw new JasperException("Failed to compile JSP tag file: " + diagnostics);
      }
    }
  }

  /** Includes project dependencies, generated tags, and JSP APIs even when the plugin has an isolated classloader. */
  private String compilationClasspath() throws JasperException {
    Set<String> entries = new LinkedHashSet<>();
    entries.add(options.getScratchDir().getAbsolutePath());
    for (ClassLoader loader = ctxt.getClassLoader(); loader != null; loader = loader.getParent()) {
      if (loader instanceof URLClassLoader urls) {
        for (URL url : urls.getURLs()) {
          addClasspathEntry(entries, url);
        }
      }
    }
    for (Class<?> api : List.of(JasperCompiler.class, Compiler.class, Servlet.class, JspFactory.class, ExpressionFactory.class)) {
      var location = api.getProtectionDomain().getCodeSource();
      if (location != null) {
        addClasspathEntry(entries, location.getLocation());
      }
    }
    if (options.getClassPath() != null) {
      entries.addAll(Arrays.asList(options.getClassPath().split(File.pathSeparator)));
    }
    return String.join(File.pathSeparator, entries);
  }

  private static void addClasspathEntry(Set<String> entries, URL url) throws JasperException {
    if ("file".equals(url.getProtocol())) {
      try {
        entries.add(Path.of(url.toURI()).toString());
      } catch (URISyntaxException e) {
        throw new JasperException(e);
      }
    }
  }
}
