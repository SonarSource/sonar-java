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
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.SmapStratum;
import org.apache.jasper.compiler.SmapUtil;

/** Generates JSP sources and compiles the tag helpers Jasper needs for introspection with the running JDK. */
public final class JasperCompiler extends Compiler {
  @Override
  protected void generateClass(Map<String, SmapStratum> smaps) throws Exception {
    var compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new JasperException("A JDK compiler is required to process JSP tag files");
    }
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    try (var files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, Charset.forName(options.getJavaEncoding()))) {
      List<String> arguments = new ArrayList<>(List.of("-proc:none", "-implicit:none", "-classpath", classpath(),
        "-d", options.getScratchDir().getAbsolutePath(), "-sourcepath", options.getScratchDir().getAbsolutePath(),
        options.getClassDebugInfo() ? "-g" : "-g:none"));
      if (options.getCompilerSourceVM() != null) {
        arguments.addAll(List.of("-source", options.getCompilerSourceVM()));
      }
      if (options.getCompilerTargetVM() != null) {
        arguments.addAll(List.of("-target", options.getCompilerTargetVM()));
      }
      var sources = files.getJavaFileObjects(ctxt.getServletJavaFileName());
      if (!compiler.getTask(null, files, diagnostics, arguments, null, sources).call()) {
        throw new JasperException("Unable to compile JSP tag file: " + diagnostics.getDiagnostics());
      }
    }
    if (!ctxt.isPrototypeMode() && !options.isSmapSuppressed()) {
      SmapUtil.installSmap(smaps);
    }
  }

  private String classpath() throws URISyntaxException {
    Set<String> entries = new LinkedHashSet<>();
    if (ctxt.getClassPath() != null) {
      entries.addAll(Arrays.asList(ctxt.getClassPath().split(File.pathSeparator)));
    }
    for (ClassLoader loader = ctxt.getJspLoader(); loader != null; loader = loader.getParent()) {
      if (loader instanceof URLClassLoader urls) {
        for (var url : urls.getURLs()) {
          if (url.getProtocol().equals("file")) {
            entries.add(Path.of(url.toURI()).toString());
          }
        }
      }
    }
    entries.add(Path.of(JasperCompiler.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
    entries.addAll(Arrays.asList(System.getProperty("java.class.path", "").split(File.pathSeparator)));
    return String.join(File.pathSeparator, entries);
  }
}
