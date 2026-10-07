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
package com.sonar.it.java.suite;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

record RepositoryScope(String name, List<Module> modules) {

  private static final Set<String> EXCLUDED_MODULES = Set.of("its", "docs", "java-checks-test-sources");
  private static final String CLASSPATH_READER = "java-checks-test-sources/test-classpath-reader";
  private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(".git", ".agents", ".codex", "target", "node_modules");

  RepositoryScope {
    modules = List.copyOf(modules);
    if (modules.isEmpty() || modules.stream().map(Module::path).distinct().count() != modules.size()) {
      throw new IllegalArgumentException("At least one unique production module is required");
    }
  }

  record Module(String path, String artifactId, List<String> javaFiles) {
    Module {
      javaFiles = List.copyOf(javaFiles);
    }

    String sourceRoot() {
      return path + "/src/main/java";
    }
  }

  static RepositoryScope discover(Path checkout) throws IOException {
    Element project = readPom(checkout.resolve("pom.xml"));
    String artifact = text(project, "artifactId");
    String name = switch (artifact) {
      case "java" -> "sonar-java";
      case "xml" -> "sonar-xml";
      default -> artifact;
    };
    List<Module> modules = new ArrayList<>();
    for (String module : declaredModules(project)) {
      if (!EXCLUDED_MODULES.contains(module)) {
        addModule(checkout, module, modules);
      }
    }
    if (modules.stream().anyMatch(module -> module.artifactId().equals("java-checks-testkit"))) {
      addModule(checkout, CLASSPATH_READER, modules);
    }
    return new RepositoryScope(name, modules);
  }

  List<String> sourceRoots() {
    return modules.stream().map(Module::sourceRoot).toList();
  }

  List<String> modulePaths() {
    return modules.stream().map(Module::path).toList();
  }

  List<String> expectedFiles() {
    return modules.stream().flatMap(module -> module.javaFiles().stream()).sorted().toList();
  }

  Set<String> projectArtifactIds() {
    return modules.stream().map(Module::artifactId).collect(Collectors.toUnmodifiableSet());
  }

  private static void addModule(Path checkout, String module, List<Module> modules) throws IOException {
    Path root = checkout.toAbsolutePath().normalize();
    Path directory = root.resolve(module).normalize();
    if (!directory.startsWith(root)) {
      throw new IOException("Production module must stay inside the checkout: " + module);
    }
    Path sources = directory.resolve("src/main/java");
    if (!Files.isDirectory(sources)) {
      return;
    }
    List<String> files;
    try (var entries = Files.walk(sources)) {
      files = entries.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
        .map(root::relativize).map(Path::toString).map(path -> path.replace('\\', '/')).sorted().toList();
    }
    if (!files.isEmpty()) {
      modules.add(new Module(module.replace('\\', '/'), text(readPom(directory.resolve("pom.xml")), "artifactId"), files));
    }
  }

  static Path copyReactorPoms(Path checkout, Path workspace) throws IOException {
    Path root = checkout.toAbsolutePath().normalize();
    Path copy = workspace.toAbsolutePath().normalize();
    if (copy.startsWith(root)) {
      throw new IllegalArgumentException("Dependency resolution must use a copy outside the target checkout");
    }
    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
        return !directory.equals(root) && EXCLUDED_DIRECTORIES.contains(directory.getFileName().toString())
          ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        if (file.getFileName().toString().equals("pom.xml")) {
          Path target = copy.resolve(root.relativize(file));
          Files.createDirectories(target.getParent());
          Files.copy(file, target);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return copy.resolve("pom.xml");
  }

  private static List<String> declaredModules(Element project) {
    Element modules = child(project, "modules");
    List<String> paths = new ArrayList<>();
    if (modules != null) {
      for (var node = modules.getFirstChild(); node != null; node = node.getNextSibling()) {
        if (node instanceof Element element && element.getTagName().equals("module")) {
          paths.add(element.getTextContent().strip());
        }
      }
    }
    return List.copyOf(paths);
  }

  private static Element readPom(Path pom) throws IOException {
    var factory = DocumentBuilderFactory.newDefaultInstance();
    try {
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      return factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException("Cannot read Maven module structure from " + pom, e);
    }
  }

  private static String text(Element project, String name) throws IOException {
    Element element = child(project, name);
    if (element == null || element.getTextContent().isBlank()) {
      throw new IOException("Missing project " + name + " in Maven module");
    }
    return element.getTextContent().strip();
  }

  private static Element child(Element parent, String name) {
    for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && element.getTagName().equals(name)) {
        return element;
      }
    }
    return null;
  }
}
