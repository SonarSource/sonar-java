/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

public class SpringBeansShouldBeAccessibleCheckTest {

  private static final String DEFAULT_TEST_JARS_DIRECTORY = "target/test-jars";

  private final static List<String> CLASS_PATH_JAR_PARTIAL_NAMES = Arrays.asList(
    "spring-web",
    "spring-boot-autoconfigure",
    "spring-context");

  @Test
  public void testComponentScan() throws IOException {
    LocalVerifier localVerifier = new LocalVerifier();
    List<File> filesToScan = Arrays.asList(
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/A.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/B.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/C.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/DefaultPackage.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/FalsePositive.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/Y1.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/Y2.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/Z2.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/ComponentScan/ComponentScan.java"));

    Set<AnalyzerMessage> analysisResult = scanFiles(localVerifier, filesToScan);
    localVerifier.checkIssues(analysisResult, false);
  }

  @Test
  public void testSpringBootApplication() throws IOException {
    LocalVerifier localVerifier = new LocalVerifier();
    List<File> filesToScan = Arrays.asList(
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/SpringBootApplication/Ko.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/SpringBootApplication/SpringBoot.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/SpringBootApplication/AnotherSpringBoot.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/SpringBootApplication/AnotherOk.java"),
      new File("src/test/files/checks/spring/SpringBeansShouldBeAccessibleCheck/SpringBootApplication/Ok.java"));

    Set<AnalyzerMessage> analysisResult = scanFiles(localVerifier, filesToScan);
    localVerifier.checkIssues(analysisResult, false);
  }

  private Set<AnalyzerMessage> scanFiles(LocalVerifier localVerifier, List<File> filesToScan) throws IOException {

    List<File> classPath = getFilesRecursively();
    VisitorsBridgeForTests vb = new VisitorsBridgeForTests(
      Arrays.asList(new SpringBeansShouldBeAccessibleCheck(), new ExpectedIssueCollector(localVerifier)),
      classPath,
      null);
    vb.setJavaVersion(new JavaVersionImpl());

    JavaAstScanner astScanner = new JavaAstScanner(JavaParser.createParser(), null);
    astScanner.setVisitorBridge(vb);
    astScanner.scan(filesToScan);

    VisitorsBridgeForTests.TestJavaFileScannerContext testJavaFileScannerContext = vb.lastCreatedTestContext();
    return testJavaFileScannerContext.getIssues();
  }

  private static List<File> getFilesRecursively() throws IOException {
    SpringBootJarCollector springBootJarCollector = new SpringBootJarCollector();
    Files.walkFileTree(Paths.get(DEFAULT_TEST_JARS_DIRECTORY), springBootJarCollector);
    return springBootJarCollector.jarFiles;
  }

  private static class SpringBootJarCollector extends SimpleFileVisitor<Path> {
    final List<File> jarFiles = new ArrayList<>();

    @Override
    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attributes) {
      if (CLASS_PATH_JAR_PARTIAL_NAMES.stream().anyMatch(jarPartialName -> filePath.toString().contains(jarPartialName))) {
        jarFiles.add(filePath.toFile());
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      return FileVisitResult.CONTINUE;
    }
  }

  private static class LocalVerifier extends CheckVerifier {
    @Override
    public String getExpectedIssueTrigger() {
      return "// " + ISSUE_MARKER;
    }

    @Override
    protected void collectExpectedIssues(String comment, int line) {
      super.collectExpectedIssues(comment, line);
    }
  }

  private static class ExpectedIssueCollector extends SubscriptionVisitor {

    private final LocalVerifier verifier;

    ExpectedIssueCollector(LocalVerifier verifier) {
      this.verifier = verifier;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      verifier.collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
    }
  }

}
