/*
 * SonarQube Java
 * Copyright (C) 2024-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaCheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckListGeneratorTest {
  private CheckListGenerator generator;
  private final String directory = System.getProperty("user.dir").replace("check-list", "");

  @BeforeEach
  void setUp() throws IOException {
    Gson gson = new Gson();
    generator = new CheckListGenerator(gson,
      Path.of(directory, "java-checks/src/main/java"),
      Path.of(directory, "java-checks-aws/src/main/java"),
      Files.createTempFile("testGeneratedCheckList", ".java"),
      directory + CheckListGenerator.RULES_PATH);
  }

  @Test
  void test_Main_throwsException_withDefaultProperties() {
    assertThrows(IllegalStateException.class, () -> CheckListGenerator.main(new String[] {}));
  }

  @Test
  void test_generateCheckList() {
    generator.generateCheckList();
    assertTrue(Files.exists(generator.pathToWriteList));
  }

  @Test
  void test_generateCheckList_fail() {
    generator.pathToWriteList = null;
    assertThrows(NullPointerException.class, () -> generator.generateCheckList());
  }

  @Test
  void testGetCheckClasses() {
    var classes = generator.getCheckClasses();
    assertNotNull(classes);
    assertFalse(classes.isEmpty());
    assertTrue(classes.stream().allMatch(c -> c.isAnnotationPresent(Rule.class)));
  }

  @Test
  void testGenerateCheckList() {
    var checks = generator.getCheckClasses();
    List<Class<?>> mainClasses = new ArrayList<>();
    List<Class<?>> testClasses = new ArrayList<>();
    List<Class<?>> allClasses = new ArrayList<>();
    generator.generateCheckListClasses(checks, mainClasses, testClasses, allClasses, directory + "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/");
    assertTrue(Files.exists(generator.pathToWriteList));
  }

  @Test
  void testGenerateCheckList_fail() {
    @Rule(key = "exampleKey")
    class ExampleCheck1 implements JavaCheck {
    }
    @Rule(key = "exampleKey2")
    class ExampleCheck2 implements JavaCheck {
    }
    List<Class<?>> checks = List.of(ExampleCheck1.class, ExampleCheck2.class);
    List<Class<?>> mainClasses = new ArrayList<>();
    List<Class<?>> testClasses = new ArrayList<>();
    List<Class<?>> allClasses = new ArrayList<>();
    assertThrows(IllegalStateException.class,
      () -> generator.generateCheckListClasses(checks, mainClasses, testClasses, allClasses, directory + "sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/"),
      "Could not find rule file /Users/irina.batinic/Projects/sonar-java/sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/exampleKey.json");
  }

  @Test
  void testGenerateCheckList_fail_wrong_metadata() {
    @Rule(key = "exampleKey")
    class ExampleCheck1 implements JavaCheck {
    }
    @Rule(key = "exampleKey2")
    class ExampleCheck2 implements JavaCheck {
    }
    List<Class<?>> checks = List.of(ExampleCheck1.class, ExampleCheck2.class);
    List<Class<?>> mainClasses = new ArrayList<>();
    List<Class<?>> testClasses = new ArrayList<>();
    List<Class<?>> allClasses = new ArrayList<>();
    assertThrows(IllegalStateException.class,
      () -> generator.generateCheckListClasses(checks, mainClasses, testClasses, allClasses, directory + "check-list/src/test/files/metadata/"),
      "Unknown scope Something for class class org.sonar.java.CheckListGeneratorTest$2ExampleCheck1");
  }

  @Test
  void testGetCheckClasses_fail() throws IOException {
    Path relativePath = Path.of("java-checks/src/main/java");
    Path awsRelativePath = Path.of(directory, "java-checks-aws/src/main/java");
    generator = new CheckListGenerator(new Gson(), relativePath, awsRelativePath, Files.createTempFile("testGeneratedCheckList", ".java"), directory + CheckListGenerator.RULES_PATH);
    assertThrows(IllegalStateException.class, () -> generator.getCheckClasses());
  }

  @Test
  void testGetCheckClasses_fail_getClassByName() throws IOException {
    Path relativePath = Path.of(directory, "check-list/src/test/files");
    Path awsRelativePath = Path.of(directory, "java-checks-aws/src/main/java");
    generator = new CheckListGenerator(new Gson(), relativePath, awsRelativePath, Files.createTempFile("testGeneratedCheckList", ".java"), directory + CheckListGenerator.RULES_PATH);
    assertThrows(IllegalStateException.class, () -> generator.getCheckClasses(), "Cannot find the class for name org.sonar.java.checks.ExampleCheck");
  }

  @Test
  void testGetRuleKey() {
    @Rule(key = "exampleKey")
    class ExampleCheck implements JavaCheck {
    }
    String ruleKey = generator.getRuleKey(ExampleCheck.class);
    assertEquals("exampleKey", ruleKey);
  }

  @Test
  void testGetMetadata() {
    String json = "{\"scope\":\"Main\"}";
    CheckListGenerator.Metadata metadata = generator.getMetadata(new StringReader(json));
    assertNotNull(metadata);
    assertEquals("Main", metadata.scope);
  }

  @Test
  void testGenerateImportStatements() {
    @Rule(key = "exampleKey")
    class ExampleCheck1 implements JavaCheck {
    }
    @Rule(key = "exampleKey2")
    class ExampleCheck2 implements JavaCheck {
    }
    List<Class<?>> checks = List.of(ExampleCheck1.class, ExampleCheck2.class);
    String importStatements = CheckListGenerator.generateImportStatements(checks);
    assertTrue(importStatements.contains("import org.sonar.java.ExampleCheck1;"));
    assertTrue(importStatements.contains("import org.sonar.java.ExampleCheck2;"));
  }

  @Test
  void testCollectChecks() {
    @Rule(key = "exampleKey")
    class ExampleCheck1 implements JavaCheck {
    }
    @Rule(key = "exampleKey2")
    class ExampleCheck2 implements JavaCheck {
    }
    List<Class<?>> classes = List.of(ExampleCheck1.class, ExampleCheck2.class);
    String checks = generator.collectChecks(classes);
    assertTrue(checks.contains("ExampleCheck1.class"));
    assertTrue(checks.contains("ExampleCheck2.class"));
  }

  @Test
  void testWriteToFile() throws IOException {
    Path tempFile = Files.createTempFile("testGeneratedCheckList", ".java");
    generator.writeToFile(
      "importStatements",
      "collectMainChecks",
      "collectTestChecks",
      "collectAllChecks", tempFile);
    assertTrue(Files.exists(tempFile));
    String content = Files.readString(tempFile);
    assertNotNull(content);
    assertTrue(content.contains("GeneratedCheckList"));
    assertTrue(content.contains("importStatements"));
    Files.deleteIfExists(tempFile);
  }

  @Test
  void test_writingToFile_ToInvalidPath_ThrowsException() {
    Path invalidPath = Paths.get("/invalid/directory/test.txt");
    assertThrows(IllegalStateException.class, () -> generator.writeToFile(
      "importStatements",
      "collectMainChecks",
      "collectTestChecks",
      "collectAllChecks", invalidPath));
  }

}
