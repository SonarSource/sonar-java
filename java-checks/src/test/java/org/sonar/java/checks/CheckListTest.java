/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.java.JavaAstScanner;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class CheckListTest {

  /**
   * Enforces that each check declared in list.
   */
  @Test
  public void count() {
    int count = 0;
    List<File> files = (List<File>) FileUtils.listFiles(new File("src/main/java/org/sonar/java/checks/"), new String[] {"java"}, false);
    for (File file : files) {
      if (file.getName().endsWith("Check.java")) {
        count++;
      }
    }
    assertThat(CheckList.getChecks().size()).isEqualTo(count);
  }

  /**
   * Enforces that each check has test, name and description.
   */
  @Test
  public void test() {
    List<Class> checks = CheckList.getChecks();

    for (Class cls : checks) {
      String testName = '/' + cls.getName().replace('.', '/') + "Test.class";
      assertThat(getClass().getResource(testName))
        .overridingErrorMessage("No test for " + cls.getSimpleName())
        .isNotNull();
    }

    ResourceBundle resourceBundle = ResourceBundle.getBundle("org.sonar.l10n.java", Locale.ENGLISH);

    Set<String> keys = Sets.newHashSet();
    List<Rule> rules = new AnnotationRuleParser().parse("repositoryKey", checks);
    for (Rule rule : rules) {
      assertThat(keys).as("Duplicate key " + rule.getKey()).excludes(rule.getKey());
      keys.add(rule.getKey());

      resourceBundle.getString("rule." + CheckList.REPOSITORY_KEY + "." + rule.getKey() + ".name");
      assertThat(getClass().getResource("/org/sonar/l10n/java/rules/" + CheckList.REPOSITORY_KEY + "/" + rule.getKey() + ".html"))
        .overridingErrorMessage("No description for " + rule.getKey())
        .isNotNull();

      assertThat(rule.getDescription())
        .overridingErrorMessage("Description of " + rule.getKey() + " should be in separate file")
        .isNull();

      for (RuleParam param : rule.getParams()) {
        resourceBundle.getString("rule." + CheckList.REPOSITORY_KEY + "." + rule.getKey() + ".param." + param.getKey());

        assertThat(param.getDescription())
          .overridingErrorMessage("Description for param " + param.getKey() + " of " + rule.getKey() + " should be in separate file")
          .isEmpty();
      }
    }
  }

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = CheckList.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  /**
   * Ensures that all checks are able to deal with unparsable files
   */
  @Test
  public void should_not_fail_on_invalid_file() throws Exception {
    List<Class> checks = CheckList.getChecks();

    for (Class check : checks) {
      CodeVisitor visitor = (CodeVisitor) check.newInstance();
      if (visitor instanceof SquidAstVisitor) {
        JavaAstScanner.scanSingleFile(new File("src/test/files/CheckListParseErrorTest.java"), (SquidAstVisitor) visitor);
      }
    }
  }

}
