/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.test.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;

public final class TestClasspathUtils {

  private static final String PATH_NOT_FOUND = "";

  public static final Module AWS_MODULE = new Module("java-checks-test-sources/aws");
  public static final Module DEFAULT_MODULE = new Module("java-checks-test-sources/default");
  public static final Module JAVA_17_MODULE = new Module("java-checks-test-sources/java-17");
  public static final Module SPRING_32_MODULE = new Module("java-checks-test-sources/spring-3.2");
  public static final Module SPRING_WEB_40_MODULE = new Module("java-checks-test-sources/spring-web-4.0");

  public static class Module {
    private final String relativePath;
    private String path;
    private List<File> classPath;

    public Module(String relativePath) {
      this.relativePath = relativePath;
    }

    @Nullable
    public String getPath() {
      if (path == null) {
        path =  findAncestorExistingSibling(relativePath);
      }
      return path.equals(PATH_NOT_FOUND) ? null : path;
    }

    public List<File> getClassPath() {
      if (classPath == null) {
        var fileList = new ArrayList<File>();
        String existingPath = getPath();
        if (existingPath != null) {
          File targetClassesDirectory = new File(existingPath, Path.of("target", "classes").toString());
          if (targetClassesDirectory.exists()) {
            fileList.add(targetClassesDirectory);
          }
          Path classpathFilePath = toPath(existingPath + "/target/test-classpath.txt");
          if (Files.exists(classpathFilePath)) {
            fileList.addAll(loadFromFile(classpathFilePath.toString()));
          }
          classPath = unmodifiableList(fileList);
        } else {
          return List.of();
        }
      }
      return classPath;
    }

    private static String findAncestorExistingSibling(String relativePath) {
      Path siglingPath = toPath(relativePath);
      Path lookUpPath = Path.of(System.getProperty("user.dir"));
      do {
        Path siblingPath = lookUpPath.resolve(siglingPath);
        if (Files.exists(siblingPath)) {
          return siblingPath.toString();
        }
        lookUpPath = Files.exists(lookUpPath.resolve("sonarpedia.json")) ? null : lookUpPath.getParent();
      } while (lookUpPath != null);
      return PATH_NOT_FOUND;
    }
  }

  private TestClasspathUtils() {
    // utility class
  }

  public static Path findModuleJarPath(String modulePath) {
    Path moduleAbsolutePath;
    try {
      moduleAbsolutePath = toPath(modulePath).toRealPath();
    } catch (IOException e) {
      throw new IllegalArgumentException("Module path exception '" + modulePath + "': " + e.getMessage(), e);
    }
    Document pom = loadXml(moduleAbsolutePath.resolve("pom.xml"));
    String groupId = xmlNodeValue(pom, "project/groupId/text()|project/parent/groupId/text()");
    String artifactId = xmlNodeValue(pom, "project/artifactId/text()");
    String version = xmlNodeValue(pom, "project/version/text()|project/parent/version/text()");
    Path moduleJarPath = moduleAbsolutePath.resolve("target").resolve(artifactId + "-" + version + ".jar");
    if (Files.exists(moduleJarPath)) {
      return moduleJarPath;
    }
    String mavenRepository = findMavenLocalRepository(System::getenv, System::getProperty);
    Path localRepositoryJarPath = toPath(mavenRepository, groupId.replace('.', '/'), artifactId, version, artifactId + "-" + version + ".jar");
    if (!Files.exists(localRepositoryJarPath)) {
      throw new IllegalArgumentException("Missing jar for module '" + modulePath + "', not found in '" + moduleJarPath + "' nor in '" + localRepositoryJarPath + "'");
    }
    return localRepositoryJarPath;
  }

  // VisibleForTesting
  static Document loadXml(Path xmlPath) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(xmlPath.toFile());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new IllegalArgumentException("Exception reading '" + xmlPath + "': " + e.getMessage(), e);
    }
  }

  // VisibleForTesting
  static String xmlNodeValue(Document document, String xpath) {
    try {
      XPathExpression expression = XPathFactory.newInstance().newXPath().compile(xpath);
      Node node = (Node) expression.evaluate(document, XPathConstants.NODE);
      if (node == null) {
        throw new IllegalArgumentException("Missing node for xpath '" + xpath + "'");
      }
      return node.getNodeValue();
    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException("Exception evaluating '" + xpath + "': " + e.getMessage(), e);
    }
  }

  public static List<File> loadFromFile(String classpathTextFilePath) {
    List<File> classpath = new ArrayList<>();
    String mavenRepository = findMavenLocalRepository(System::getenv, System::getProperty);
    try {
      String content = Files.readString(toPath(classpathTextFilePath), UTF_8);
      Arrays.stream(content.split(":"))
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .map(TestClasspathUtils::fixSeparator)
        .map(line -> line.replace("${M2_REPO}", mavenRepository))
        .map(Paths::get)
        .forEach(dependencyPath -> {
          if (!Files.exists(dependencyPath)) {
            throw new IllegalArgumentException("Missing dependency: " + dependencyPath);
          }
          classpath.add(dependencyPath.toFile());
        });
    } catch (IOException e) {
      throw new IllegalArgumentException("Exception while loading '" + classpathTextFilePath + "': " + e.getMessage(), e);
    }
    return classpath;
  }

  // VisibleForTesting
  static String findMavenLocalRepository(UnaryOperator<String> systemEnvProvider, UnaryOperator<String> systemPropertyProvider) {
    String repository = systemEnvProvider.apply("M2_REPO");
    if (repository == null || repository.isEmpty()) {
      // In the root pom.xml file, the surefire plugin's configuration always set the M2_REPO env variable to the right directory.
      // Here we default to ~/.m2/repository only for IDE execution that doesn't use the maven surefire plugin configuration.
      repository = toPath(systemPropertyProvider.apply("user.home"), ".m2", "repository").toString();
    }
    return repository;
  }

  public static String fixSeparator(String path) {
    return path.replace(File.separatorChar == '/' ? '\\' : '/', File.separatorChar == '/' ? '/' : '\\');
  }

  // VisibleForTesting
  static Path toPath(String first, String... more) {
    for (int i = 0; i < more.length; i++) {
      more[i] = fixSeparator(more[i]);
    }
    return Paths.get(fixSeparator(first), more);
  }

}
