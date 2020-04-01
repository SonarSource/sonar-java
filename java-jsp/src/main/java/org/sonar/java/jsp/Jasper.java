/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.jsp;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.servlet.jsp.JspFactory;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.apache.jasper.servlet.JspCServletContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.SmapFile;

@ScannerSide
public class Jasper {

  private static final Logger LOG = Loggers.get(Jasper.class);

  public Collection<GeneratedFile> generateFiles(SensorContext sensorContext, List<File> javaClasspath) {
    List<Path> jspFiles = jspFiles(sensorContext.fileSystem());
    LOG.debug("Found {} JSP files.", jspFiles.size());
    if (jspFiles.isEmpty()) {
      return Collections.emptyList();
    }
    Path outputDir = outputDir(sensorContext);
    File baseDir = sensorContext.fileSystem().baseDir();
    // Jasper internally calls Thread#getContextClassLoader to instantiate some classes. ContextClassLoader is set by scanner
    // and doesn't contain plugin jar, so we need to configure ContextClassLoader with the class loader of the plugin to be able
    // to run Jasper. Original classloader is restored in finally.
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      ClassLoader classLoader = initClassLoader(javaClasspath);
      Thread.currentThread().setContextClassLoader(classLoader);
      JspFactory.setDefaultFactory(new JspFactoryImpl());
      JspCServletContext servletContext = new ServletContext(toUrl(baseDir), classLoader);
      JasperOptions options = new JasperOptions(servletContext, outputDir);
      JspRuntimeContext runtimeContext = new JspRuntimeContext(servletContext, options);

      boolean errorTranspiling = false;
      for (Path file : jspFiles) {
        LOG.debug("Transpiling JSP: {}", file);
        try {
          String relativePath = file.toAbsolutePath().toString().substring(baseDir.getAbsolutePath().length());
          JspCompilationContext compilationContext = new JspCompilationContext(relativePath, options, servletContext, null, runtimeContext);
          compilationContext.setClassLoader(classLoader);
          Compiler compiler = compilationContext.createCompiler();
          compiler.compile(false, true);
        } catch (Exception e) {
          errorTranspiling = true;
          LOG.debug("Error transpiling " + file, e);
        }
      }
      if (errorTranspiling) {
        LOG.warn("Some JSP pages failed to transpile. Enable debug log for details.");
      }
      return findGeneratedFiles(outputDir);
    } catch (Exception e) {
      LOG.warn("Failed to transpile JSP files.", e);
      return Collections.emptyList();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private static List<Path> jspFiles(FileSystem fs) {
    Iterable<InputFile> inputFiles = fs.inputFiles(fs.predicates().hasLanguage("jsp"));
    return StreamSupport.stream(inputFiles.spliterator(), false)
      .map(InputFile::path)
      .collect(Collectors.toList());
  }

  @VisibleForTesting
  ClassLoader initClassLoader(List<File> classPath) {
    URL[] urls = classPath.stream().map(Jasper::toUrl).toArray(URL[]::new);
    return new URLClassLoader(urls, Jasper.class.getClassLoader());
  }

  private static URL toUrl(File f) {
    try {
      return f.toURI().toURL();
    } catch (MalformedURLException e) {
      // can't happen
      throw new RuntimeException(e);
    }
  }

  private static Collection<GeneratedFile> findGeneratedFiles(Path outputDir) {
    Map<Path, GeneratedFile> generatedFiles = new HashMap<>();
    try (Stream<Path> fileStream = walk(outputDir)) {
      fileStream
        .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".smap"))
        .map(SmapFile::fromPath)
        .forEach(smap -> {
          GeneratedFile generatedFile = generatedFiles.computeIfAbsent(smap.getGeneratedFile(), p -> new GeneratedFile(smap.getGeneratedFile()));
          generatedFile.addSmap(smap);
        });
      LOG.debug("Generated {} Java files.", generatedFiles.size());
      return generatedFiles.values();
    }
  }

  static Path outputDir(SensorContext sensorContext) {
    Path path = sensorContext.fileSystem().workDir().toPath().resolve("jsp");
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to create output dir for jsp files", ex);
    }
    return path;
  }

  static Stream<Path> walk(Path dir) {
    try {
      return Files.walk(dir);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to walk " + dir, e);
    }
  }

  /**
   * Overloading log methods so messages are redirected to scanner log
   */
  static class ServletContext extends JspCServletContext {

    public ServletContext(URL aResourceBaseURL, ClassLoader classLoader) throws JasperException {
      super(/* not used */ null, aResourceBaseURL, classLoader, false, true);
    }

    @Override
    public void log(String message) {
      LOG.debug(message);
    }

    @Override
    public void log(String message, Throwable exception) {
      LOG.debug(message, exception);
    }
  }

}

