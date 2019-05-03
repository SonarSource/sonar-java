/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.se.checks.SECheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class CheckListTest {

  private static final String ARTIFICIAL_DESCRIPTION = "-1";

  private static List<String> SE_CHEKS;
  private final Gson gson = new Gson();

  private static final Set<String> BLACK_LIST = ImmutableSet.of("AbstractXPathBasedCheck.java", "AbstractWebXmlXPathBasedCheck.java");

  @BeforeClass
  public static void before() throws Exception {
    SE_CHEKS = ClassPath.from(CheckListTest.class.getClassLoader())
      .getTopLevelClasses("org.sonar.java.se.checks")
      .stream()
      .map(ClassPath.ClassInfo::getSimpleName)
      .filter(name -> name.endsWith("Check") && !name.equals(SECheck.class.getSimpleName()))
      .collect(Collectors.toList());
  }

  /**
   * Enforces that each check declared in list.
   */
  @Test
  public void count() {
    int count = 0;
    List<File> files = (List<File>) FileUtils.listFiles(new File("src/main/java/org/sonar/java/checks/"), new String[] {"java"}, true);
    for (File file : files) {
      if (file.getName().endsWith("Check.java") && !BLACK_LIST.contains(file.getName())) {
        count++;
      }
    }
    assertThat(CheckList.getChecks().size()).isEqualTo(count + SE_CHEKS.size());
  }

  private static class CustomRulesDefinition implements RulesDefinition {

    @Override
    public void define(Context context) {
      String language = "java";
      NewRepository repository = context
        .createRepository(CheckList.REPOSITORY_KEY, language)
        .setName("SonarQube");

      List<Class> checks = CheckList.getChecks();
      new RulesDefinitionAnnotationLoader().load(repository, checks.toArray(new Class[checks.size()]));

      for (NewRule rule : repository.rules()) {
        try {
          rule.setName("Artificial Name (set via JSON files, no need to test it)");
          rule.setMarkdownDescription(ARTIFICIAL_DESCRIPTION);
        } catch (IllegalStateException e) {
          // it means that the html description was already set in Rule annotation
          fail("Description of " + rule.key() + " should be in separate file");
        }
      }
      repository.done();
    }
  }

  @Test
  public void debugTests() {
    assertThat(CheckList.getDebugChecks()).hasSize(3);
  }

  /**
   * Enforces that each check has test, name and description.
   */
  @Test
  public void test() {
    List<Class> checks = CheckList.getChecks();
    Map<String, String> keyMap = new HashMap<>();
    for (Class cls : checks) {
      String testName = '/' + cls.getName().replace('.', '/') + "Test.class";
      String simpleName = cls.getSimpleName();
      // Handle legacy keys.
      Rule ruleAnnotation = AnnotationUtils.getAnnotation(cls, Rule.class);
      String key = getKey(cls, ruleAnnotation);
      keyMap.put(ruleAnnotation.key(), key);
      if (SE_CHEKS.contains(simpleName)) {
        continue;
      }
      assertThat(getClass().getResource(testName))
        .overridingErrorMessage("No test for " + simpleName)
        .isNotNull();
    }

    Set<String> keys = new HashSet<>();
    Set<String> names = Sets.newHashSet();
    CustomRulesDefinition definition = new CustomRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    List<RulesDefinition.Rule> rules = context.repository(CheckList.REPOSITORY_KEY).rules();
    for (RulesDefinition.Rule rule : rules) {
      assertThat(keys).as("Duplicate key " + rule.key()).doesNotContain(rule.key());
      keys.add(rule.key());
      names.add(rule.name());
      assertThat(getClass().getResource("/org/sonar/l10n/java/rules/" + CheckList.REPOSITORY_KEY + "/" + keyMap.get(rule.key()) + "_java.html"))
        .overridingErrorMessage("No description for " + rule.key()+ " " +keyMap.get(rule.key()))
        .isNotNull();
      assertThat(getClass().getResource("/org/sonar/l10n/java/rules/" + CheckList.REPOSITORY_KEY + "/" + keyMap.get(rule.key()) + "_java.json"))
        .overridingErrorMessage("No json metadata file for " + rule.key()+ " " +keyMap.get(rule.key()))
        .isNotNull();

      assertThat(rule.htmlDescription()).isNull();
      assertThat(rule.markdownDescription()).isEqualTo(ARTIFICIAL_DESCRIPTION);

      for (RulesDefinition.Param param : rule.params()) {
        assertThat(param.description()).overridingErrorMessage(rule.key() + " missing description for param " + param.key()).isNotEmpty();
      }
    }
  }

  private static String getKey(Class cls, Rule ruleAnnotation) {
    String key = ruleAnnotation.key();
    RspecKey rspecKeyAnnotation = AnnotationUtils.getAnnotation(cls, RspecKey.class);
    if (rspecKeyAnnotation != null) {
      return rspecKeyAnnotation.value();
    }
    return key;
  }

  @Test
  public void enforce_CheckList_registration() {
    List<File> files = (List<File>) FileUtils.listFiles(new File("src/main/java/org/sonar/java/checks/"), new String[]{"java"}, false);
    List<Class> checks = CheckList.getChecks();
    for (File file : files) {
      String name = file.getName();
      if (name.endsWith("Check.java")) {
        String className = name.substring(0, name.length() - 5);
        try {
          Class aClass = Class.forName("org.sonar.java.checks." + className);
          assertThat(checks).as(className + " is not declared in CheckList").contains(aClass);
        } catch (ClassNotFoundException e) {
          Throwables.propagate(e);
        }
      }
    }
  }

  @Test
  public void rules_targeting_tests_should_have_tests_tag() throws Exception {
    Set<Class> testChecks = new HashSet<>(CheckList.getJavaTestChecks());

    for (Class cls : CheckList.getChecks()) {
      String key = getKey(cls, AnnotationUtils.getAnnotation(cls, Rule.class));
      URL metadataURL = getClass().getResource("/org/sonar/l10n/java/rules/" + CheckList.REPOSITORY_KEY + "/" + key + "_java.json");
      File metadataFile = new File(metadataURL.toURI());
      assertThat(metadataFile).exists();
      try (FileReader jsonReader = new FileReader(metadataFile)) {
        DummyMetatada metatada = gson.fromJson(jsonReader, DummyMetatada.class);
        if (testChecks.contains(cls)) {
          assertThat(metatada.tags).as("Rule " + key + " is targeting tests sources and should contain the 'tests' tag.").contains("tests");
        } else {
          assertThat(metatada.tags).as("Rule " + key + " is targeting main sources and should not contain the 'tests' tag.").doesNotContain("tests");
        }
      }
    }
  }

  private static class DummyMetatada {
    // ignore all the other fields
    String[] tags;
  }

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = CheckList.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

}
