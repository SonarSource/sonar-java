/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.servlet.jsp.JspFactory;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.SmapStratum;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.apache.jasper.servlet.JspCServletContext;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.GeneratedFile;
import org.sonar.java.model.SmapFile;

@ScannerSide
public class Jasper {

  private static final String SONAR_EXCLUSIONS_PROPERTY = "sonar.exclusions";

  private static final Logger LOG = Loggers.get(Jasper.class);

  public Collection<GeneratedFile> generateFiles(SensorContext sensorContext, List<File> javaClasspath) {
    List<String> sonarExclusions = Arrays.asList(sensorContext.config().getStringArray(SONAR_EXCLUSIONS_PROPERTY));
    if (isAllJavaJspExcluded(sonarExclusions)) {
      return Collections.emptyList();
    }
    Predicate<String> javaExclusionFilter = createExclusionFilter(sonarExclusions);
    List<InputFile> jspFiles = jspFiles(sensorContext.fileSystem());
    LOG.debug("Found {} JSP files.", jspFiles.size());
    if (jspFiles.isEmpty()) {
      return Collections.emptyList();
    }
    Path uriRoot = findWebInfParentDirectory(sensorContext.fileSystem())
      .orElse(sensorContext.fileSystem().baseDir().getAbsoluteFile().toPath());
    LOG.debug("Context root set to {}", uriRoot);
    Path outputDir = outputDir(sensorContext);
    // Jasper internally calls Thread#getContextClassLoader to instantiate some classes. ContextClassLoader is set by scanner
    // and doesn't contain plugin jar, so we need to configure ContextClassLoader with the class loader of the plugin to be able
    // to run Jasper. Original classloader is restored in finally.
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      ClassLoader classLoader = initClassLoader(javaClasspath);
      Thread.currentThread().setContextClassLoader(classLoader);
      JspFactory.setDefaultFactory(new JspFactoryImpl());
      JspCServletContext servletContext = new ServletContext(uriRoot.toUri().toURL(), classLoader);
      JasperOptions options = getJasperOptions(outputDir, servletContext);
      JspRuntimeContext runtimeContext = new JspRuntimeContext(servletContext, options);

      boolean errorTranspiling = false;
      Map<Path, GeneratedFile> generatedJavaFiles = new HashMap<>();
      for (InputFile jsp : jspFiles) {
        try {
          transpileJsp(jsp.path(), uriRoot, classLoader, servletContext, options, runtimeContext, javaExclusionFilter)
            .ifPresent(generatedFile -> generatedJavaFiles.put(generatedFile, new GeneratedFile(generatedFile)));
        } catch (Exception | LinkageError e) {
          errorTranspiling = true;
          StringWriter w = new StringWriter();
          e.printStackTrace(new PrintWriter(w));
          LOG.debug("Error transpiling {}. Error:\n{}", jsp, w.toString());
        }
      }
      if (errorTranspiling) {
        LOG.warn("Some JSP pages failed to transpile. Enable debug log for details.");
      }
      runtimeContext.getSmaps().values().forEach(smap ->
        processSourceMap(uriRoot, generatedJavaFiles, smap, sensorContext.fileSystem()));
      return generatedJavaFiles.values();
    } catch (Exception e) {
      LOG.warn("Failed to transpile JSP files.", e);
      return Collections.emptyList();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private static boolean isAllJavaJspExcluded(List<String> sonarExclusions) {
    return sonarExclusions.contains("**/*_jsp.java");
  }

  @VisibleForTesting
  static Predicate<String> createExclusionFilter(List<String> sonarExclusions) {
    if (sonarExclusions.isEmpty()) {
      return file -> false;
    }
    List<WildcardPattern> exclusionsPatterns = sonarExclusions.stream()
      .map(pattern -> WildcardPattern.create(pattern.trim().replace('\\', '/'), "/"))
      .collect(Collectors.toList());
    return path -> {
      String sanitizedPath = PathUtils.sanitize(path);
      return sanitizedPath == null || exclusionsPatterns.stream().anyMatch(pattern -> pattern.match(sanitizedPath));
    };
  }

  private static void processSourceMap(Path uriRoot, Map<Path, GeneratedFile> generatedJavaFiles, SmapStratum smap, FileSystem fileSystem) {
    Path smapRoot = Paths.get(smap.getClassFileName()).getParent();
    SmapFile smapFile = new SmapFile(smapRoot, smap.getSmapString(), uriRoot, fileSystem);
    GeneratedFile generatedFile = generatedJavaFiles.get(smapFile.getGeneratedFile());
    if (generatedFile != null) {
      generatedFile.addSmap(smapFile);
    }
  }

  private static Optional<Path> transpileJsp(Path jsp, Path uriRoot, ClassLoader classLoader, JspCServletContext servletContext,
    JasperOptions options, JspRuntimeContext runtimeContext, Predicate<String> javaExclusionFilter) throws Exception {
    LOG.debug("Transpiling JSP: {}", jsp);
    // on windows we need to replace \ in path to / to form uri (see org.apache.jasper.JspC#processFile)
    String jspUri = "/" + uriRoot.relativize(jsp).toString().replace('\\', '/');
    JspCompilationContext compilationContext = new JspCompilationContext(jspUri, options, servletContext, null, runtimeContext);
    String javaFileName = compilationContext.getServletJavaFileName();
    if (javaExclusionFilter.test(javaFileName)) {
      return Optional.empty();
    }
    compilationContext.setClassLoader(classLoader);
    Compiler compiler = compilationContext.createCompiler();
    compiler.compile(false, true);
    return Optional.of(Paths.get(javaFileName));
  }

  JasperOptions getJasperOptions(Path outputDir, JspCServletContext servletContext) {
    return new JasperOptions(servletContext, outputDir);
  }

  private static Optional<Path> findWebInfParentDirectory(FileSystem fs) {
    FilePredicates predicates = fs.predicates();
    List<InputFile> inputFiles = new ArrayList<>();
    fs.inputFiles(predicates.matchesPathPattern("**/WEB-INF/**")).forEach(inputFiles::add);
    if (!inputFiles.isEmpty()) {
      Path path = Paths.get(inputFiles.get(0).absolutePath());
      Path parent = path.getParent();
      while (parent != null) {
        if (parent.endsWith("WEB-INF")) {
          return Optional.ofNullable(parent.getParent());
        }
        parent = parent.getParent();
      }
    }
    LOG.debug("WEB-INF directory not found, will use basedir as context root");
    return Optional.empty();
  }

  private static List<InputFile> jspFiles(FileSystem fs) {
    Iterable<InputFile> inputFiles = fs.inputFiles(fs.predicates().hasLanguage("jsp"));
    return StreamSupport.stream(inputFiles.spliterator(), false)
      .collect(Collectors.toList());
  }

  private static ClassLoader initClassLoader(List<File> classPath) {
    URL[] urls = classPath.stream().map(Jasper::toUrl).toArray(URL[]::new);
    return new JasperClassLoader(urls, Jasper.class.getClassLoader());
  }

  private static class JasperClassLoader extends URLClassLoader {

    public JasperClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
    }

    @Override
    public URL findResource(String name) {
      URL resource = super.findResource(name);
      if (resource == null) {
        resource = ClassLoader.getSystemResource(name);
      }
      return resource;
    }
  }

  private static URL toUrl(File f) {
    try {
      return f.toURI().toURL();
    } catch (MalformedURLException e) {
      // this should never happen when converting url from file
      throw new IllegalStateException(e);
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

