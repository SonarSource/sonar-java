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
package org.sonar.java.model.springcontext;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.TestUtils;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpringContextCacheHelperTest {

  private static final InputFile INPUT_FILE = TestUtils.inputFile(new File("src/test/files/springcontext/SimpleComponent.java"));

  @Nested
  class BeanDefinitions {

    @Test
    void writes_beans_in_the_documented_json_shape() {
      var written = writeBeans(List.of(beanWithDependencies()));

      var document = JsonParser.parseString(written).getAsJsonObject();
      assertThat(document.get("version").getAsInt()).isOne();
      var bean = document.getAsJsonArray("beans").get(0).getAsJsonObject();
      assertThat(bean.get("name").getAsString()).isEqualTo("qualifiedFieldDependencies");
      assertThat(bean.get("type").getAsString()).isEqualTo("checks.spring.context.QualifiedFieldDependencies");
      assertThat(bean.get("package").getAsString()).isEqualTo("checks.spring.context");
      assertThat(bean.get("primary").getAsBoolean()).isFalse();
      assertThat(bean.get("profiles").getAsString()).isEqualTo("prod");
      assertThat(bean.getAsJsonObject("span").get("startLine").getAsInt()).isEqualTo(12);
      assertThat(bean.getAsJsonArray("typeHierarchy")).extracting(JsonElement::getAsString)
        .containsExactlyInAnyOrder("checks.spring.context.QualifiedFieldDependencies");
      assertThat(bean.getAsJsonArray("dependencies")).extracting(element -> element.getAsJsonObject().get("type").getAsString())
        .containsExactlyInAnyOrder("org.springframework.context.ApplicationContext", "org.springframework.core.env.Environment");
    }

    @Test
    void no_input_file_is_serialized_nor_needed_to_read_an_entry_back() {
      var written = writeBeans(List.of(simpleComponent()));

      assertThat(written).doesNotContain(INPUT_FILE.key());
      assertThat(readBeans(written)).containsExactly(simpleComponent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("beansToRoundTrip")
    void beans_round_trip_through_the_cache(String description, List<BeanDefinitionHolder.InputFileData> beans) {
      var restored = readBeans(writeBeans(beans));

      assertThat(restored).isEqualTo(beans);
    }

    static Stream<Arguments> beansToRoundTrip() {
      return Stream.of(
        Arguments.of("no bean at all", List.of()),
        Arguments.of("bean without profiles nor dependencies", List.of(simpleComponent())),
        Arguments.of("bean with profiles and dependencies", List.of(beanWithDependencies())),
        Arguments.of("primary bean", List.of(primaryBean())),
        Arguments.of("bean whose profile expression uses operators", List.of(beanWithExpressionProfile())),
        Arguments.of("bean whose profile expression could not be parsed", List.of(beanWithUnknownProfile())),
        Arguments.of("several beans defined in the same file", List.of(simpleComponent(), primaryBean(), beanWithDependencies())));
    }

    @Test
    void a_compound_profile_expression_is_written_as_its_canonical_form() {
      var written = writeBeans(List.of(beanWithExpressionProfile()));

      var bean = JsonParser.parseString(written).getAsJsonObject().getAsJsonArray("beans").get(0).getAsJsonObject();
      assertThat(bean.get("profiles").getAsString()).isEqualTo("(!test & dev)");
    }

    @Test
    void an_unconditional_bean_is_written_with_a_null_profile() {
      var written = writeBeans(List.of(simpleComponent()));

      var bean = JsonParser.parseString(written).getAsJsonObject().getAsJsonArray("beans").get(0).getAsJsonObject();
      assertThat(bean.get("profiles").isJsonNull()).isTrue();
    }

    @Test
    void a_profile_expression_that_cannot_be_parsed_is_restored_as_unknown() {
      String entry = writeBeans(List.of(simpleComponent())).replace("\"profiles\":null", "\"profiles\":\"a & b | c\"");

      assertThat(readBeans(entry)).singleElement().satisfies(bean -> assertThat(bean.profileExpression().isUnknown()).isTrue());
    }

    @Test
    void injection_point_spans_survive_the_round_trip() {
      var restored = readBeans(writeBeans(List.of(beanWithDependencies())));

      var dependencies = restored.getFirst().dependencies();
      assertThat(dependencies.get("org.springframework.context.ApplicationContext"))
        .containsExactly(new InjectionPoint.InputFileData("primaryContext", new TextSpan(16, 2, 16, 55)));
      assertThat(dependencies.get("org.springframework.core.env.Environment"))
        .containsExactly(new InjectionPoint.InputFileData("environment", new TextSpan(19, 2, 19, 38)));
    }

    /**
     * Rejecting a corrupted entry is what {@link org.sonar.java.caching.FileCachingCheck#readFromCache} turns into a
     * cache miss. It catches every {@link RuntimeException}, which is what these inputs produce: our own
     * {@link IllegalArgumentException}, Gson's {@code JsonParseException}, or an {@link ArithmeticException} for a
     * number that is not an exact {@code int}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("corruptedEntries")
    void deserialize_rejects_a_corrupted_entry(String description, String content) {
      assertThatThrownBy(() -> readBeans(content)).isInstanceOf(RuntimeException.class);
    }

    static Stream<Arguments> corruptedEntries() {
      String bean = "{\"name\":\"n\",\"type\":\"t\",\"package\":\"p\",\"span\":%s,\"primary\":%s,\"profiles\":null,\"qualifier\":null,"
        + "\"dependencies\":[],\"typeHierarchy\":[]}";
      String span = "{\"startLine\":1,\"startCharacter\":0,\"endLine\":1,\"endCharacter\":5}";
      return Stream.of(
        Arguments.of("not json at all", "not|valid|cache|content"),
        Arguments.of("empty content", ""),
        Arguments.of("json array instead of object", "[]"),
        Arguments.of("unsupported version", "{\"version\":2,\"beans\":[]}"),
        Arguments.of("missing version", "{\"beans\":[]}"),
        Arguments.of("non-numeric version", "{\"version\":\"1\",\"beans\":[]}"),
        Arguments.of("version not a primitive", "{\"version\":{},\"beans\":[]}"),
        Arguments.of("missing beans", "{\"version\":1}"),
        Arguments.of("beans not an array", "{\"version\":1,\"beans\":\"x\"}"),
        Arguments.of("bean not an object", "{\"version\":1,\"beans\":[\"x\"]}"),
        Arguments.of("bean missing properties", "{\"version\":1,\"beans\":[{}]}"),
        Arguments.of("primary not a boolean", "{\"version\":1,\"beans\":[" + bean.formatted(span, "\"yes\"") + "]}"),
        Arguments.of("profiles neither string nor null",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"profiles\":null", "\"profiles\":42") + "]}"),
        Arguments.of("type hierarchy holds a non-string",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"typeHierarchy\":[]", "\"typeHierarchy\":[1]") + "]}"),
        Arguments.of("span with a zero start line",
          "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"startLine\":1", "\"startLine\":0"), "false") + "]}"),
        Arguments.of("span ending before it starts",
          "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"endLine\":1", "\"endLine\":0"), "false") + "]}"),
        Arguments.of("span with a fractional line number",
          "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"startLine\":1", "\"startLine\":1.5"), "false") + "]}"),
        Arguments.of("span not an object",
          "{\"version\":1,\"beans\":[" + bean.formatted("3", "false") + "]}"),
        Arguments.of("dependency not an object",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"dependencies\":[]", "\"dependencies\":[\"x\"]") + "]}"),
        Arguments.of("dependency missing its injection points",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false")
            .replace("\"dependencies\":[]", "\"dependencies\":[{\"type\":\"T\"}]") + "]}"),
        Arguments.of("injection point missing its name",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false")
            .replace("\"dependencies\":[]", "\"dependencies\":[{\"type\":\"T\",\"injectionPoints\":[{\"span\":" + span + "}]}]") + "]}"),
        Arguments.of("name given as a number",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"name\":\"n\"", "\"name\":1") + "]}"),
        Arguments.of("span with a line number given as a string",
          "{\"version\":1,\"beans\":[" + bean.formatted(span.replace("\"startLine\":1", "\"startLine\":\"1\""), "false") + "]}"),
        Arguments.of("missing primary",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"primary\":false,", "") + "]}"),
        Arguments.of("missing profiles",
          "{\"version\":1,\"beans\":[" + bean.formatted(span, "false").replace("\"profiles\":null,", "") + "]}"));
    }

    @Test
    void read_ignores_unknown_properties() {
      String content = """
        {"version":1,"beans":[{"name":"simpleComponent","type":"checks.spring.context.SimpleComponent","package":"checks.spring.context",\
        "span":{"startLine":8,"startCharacter":13,"endLine":8,"endCharacter":28,"unknown":[]},"primary":false,"profiles":null,"qualifier":null,\
        "dependencies":[{"type":"T","injectionPoints":[{"name":"t","span":{"startLine":9,"startCharacter":2,"endLine":9,"endCharacter":5},\
        "unknown":{}}],"unknown":0}],"typeHierarchy":[],"unknown":"ignored"}],"unknown":true}
        """;

      var restored = readBeans(content);

      assertThat(restored).hasSize(1);
      assertThat(restored.getFirst().beanName()).isEqualTo("simpleComponent");
      assertThat(restored.getFirst().dependencies().get("T")).extracting(InjectionPoint.InputFileData::name).containsOnly("t");
    }
  }

  @Nested
  class ComponentScanPackages {

    @Test
    void writes_packages_in_the_documented_json_shape() {
      var written = writePackages(List.of("com.example.service", "com.example.web"));

      var document = JsonParser.parseString(written).getAsJsonObject();
      assertThat(document.get("version").getAsInt()).isOne();
      assertThat(document.getAsJsonArray("packages")).extracting(JsonElement::getAsString)
        .containsExactlyInAnyOrder("com.example.service", "com.example.web");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("packagesToRoundTrip")
    void packages_round_trip_through_the_cache(String description, List<String> packages) {
      var restored = readPackages(writePackages(packages));

      assertThat(restored).containsExactlyElementsOf(packages);
    }

    static Stream<Arguments> packagesToRoundTrip() {
      return Stream.of(
        Arguments.of("no package at all", List.of()),
        Arguments.of("a single package", List.of("com.example.service")),
        Arguments.of("several packages", List.of("com.example.service", "com.example.web", "checks.spring.context")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corruptedEntries")
    void deserialize_rejects_a_corrupted_entry(String description, String content) {
      assertThatThrownBy(() -> readPackages(content)).isInstanceOf(RuntimeException.class);
    }

    static Stream<Arguments> corruptedEntries() {
      return Stream.of(
        Arguments.of("not json at all", "com.example.service;com.example.web"),
        Arguments.of("empty content", ""),
        Arguments.of("json array instead of object", "[\"com.example.service\"]"),
        Arguments.of("unsupported version", "{\"version\":2,\"packages\":[]}"),
        Arguments.of("missing version", "{\"packages\":[]}"),
        Arguments.of("non-numeric version", "{\"version\":\"1\",\"packages\":[]}"),
        Arguments.of("missing packages", "{\"version\":1}"),
        Arguments.of("packages not an array", "{\"version\":1,\"packages\":\"com.example.service\"}"),
        Arguments.of("package not a string", "{\"version\":1,\"packages\":[1]}"),
        Arguments.of("package given as an object", "{\"version\":1,\"packages\":[{\"name\":\"com.example.service\"}]}"));
    }
  }

  // ---- Bean fixtures --------------------------------------------------------

  private static BeanDefinitionHolder.InputFileData simpleComponent() {
    return beanData("simpleComponent", "checks.spring.context.SimpleComponent", new TextSpan(8, 13, 8, 28), false, ProfileExpression.UNCONDITIONAL,
      Map.of(), Set.of("checks.spring.context.SimpleComponent"));
  }

  private static BeanDefinitionHolder.InputFileData primaryBean() {
    return beanData("primaryBean", "checks.spring.context.PrimaryBean", new TextSpan(9, 13, 9, 24), true, ProfileExpression.UNCONDITIONAL,
      Map.of(), Set.of("checks.spring.context.PrimaryBean", "java.lang.Object"));
  }

  private static BeanDefinitionHolder.InputFileData beanWithExpressionProfile() {
    return beanData("expressionProfiledComponent", "checks.spring.context.ExpressionProfiledComponent", new TextSpan(10, 13, 10, 40), false,
      ProfileExpression.and(List.of(ProfileExpression.profile("dev"), ProfileExpression.not(ProfileExpression.profile("test")))),
      Map.of(), Set.of("checks.spring.context.ExpressionProfiledComponent"));
  }

  private static BeanDefinitionHolder.InputFileData beanWithUnknownProfile() {
    return beanData("malformedProfileComponent", "checks.spring.context.MalformedProfileComponent", new TextSpan(10, 13, 10, 38), false,
      ProfileExpression.UNKNOWN, Map.of(), Set.of("checks.spring.context.MalformedProfileComponent"));
  }

  private static BeanDefinitionHolder.InputFileData beanWithDependencies() {
    Map<String, Set<InjectionPoint.InputFileData>> injectionPoints = new LinkedHashMap<>();
    injectionPoints.put("org.springframework.context.ApplicationContext",
      Set.of(new InjectionPoint.InputFileData("primaryContext", new TextSpan(16, 2, 16, 55))));
    injectionPoints.put("org.springframework.core.env.Environment",
      Set.of(new InjectionPoint.InputFileData("environment", new TextSpan(19, 2, 19, 38))));
    return beanData("qualifiedFieldDependencies", "checks.spring.context.QualifiedFieldDependencies",
      new TextSpan(12, 6, 12, 32), false, ProfileExpression.profile("prod"), injectionPoints,
      Set.of("checks.spring.context.QualifiedFieldDependencies"));
  }

  private static BeanDefinitionHolder.InputFileData beanData(String beanName, String type, TextSpan span, boolean isPrimary,
    ProfileExpression profileExpression, Map<String, Set<InjectionPoint.InputFileData>> injectionPoints, Set<String> typeHierarchy) {
    return new BeanDefinitionHolder.InputFileData(beanName, type, "checks.spring.context", span, isPrimary, profileExpression, null, injectionPoints, typeHierarchy);
  }

  // ---- Serialization plumbing ----------------------------------------------

  private static String writeBeans(List<BeanDefinitionHolder.InputFileData> beans) {
    return new String(SpringContextCacheHelper.serializeBeans(beans), StandardCharsets.UTF_8);
  }

  private static List<BeanDefinitionHolder.InputFileData> readBeans(String content) {
    return SpringContextCacheHelper.deserializeBeans(content.getBytes(StandardCharsets.UTF_8));
  }

  private static String writePackages(List<String> packages) {
    return new String(SpringContextCacheHelper.serializeComponentScanPackages(packages), StandardCharsets.UTF_8);
  }

  private static Set<String> readPackages(String content) {
    return SpringContextCacheHelper.deserializeComponentScanPackages(content.getBytes(StandardCharsets.UTF_8));
  }
}
