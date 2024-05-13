package checks.tests;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.atIndex;

public class AssertJTestForEmptinessCheckSample {

  List<String> logs = getLogs();

  @Test
  void simple_all_match() {
    List<String> logs = getLogs();
    assertThat(logs).allMatch(e -> e.contains("error")); // Noncompliant {{Test the emptiness of the list before calling this assertion predicate.}}
//                   ^^^^^^^^
//             ^^^^@-1<
  }

  @Test
  void all_match_with_predicate_description() {
    List<String> logs = getLogs();
    assertThat(logs).allMatch(e -> e.contains("error"), ""); // Noncompliant
  }

  @Test
  void with_message() {
    List<String> logs = getLogs();
    assertThat(logs).as("message").allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void with_message_2() {
    List<String> logs = getLogs();
    assertThat(logs).describedAs("message").allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void tested_for_emptiness_before() {
    List<String> logs = getLogs();
    assertThat(logs).isNotEmpty().allMatch(e -> e.contains("error")); // Compliant
  }

  @Test
  void not_tested_for_emptiness_before() {
    List<String> logs = getLogs();
    assertThat(logs).allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void tested_for_emptiness_after() {
    List<String> logs = getLogs();
    assertThat(logs).allMatch(e -> e.contains("error")).isNotNull().isNotEmpty(); // Compliant
  }

  @Test
  void not_tested_for_emptiness_after() {
    List<String> logs = getLogs();
    assertThat(logs).allMatch(e -> e.contains("error")).isInstanceOf(List.class); // Noncompliant
  }

  @Test
  void not_tested_for_emptiness_before_after() {
    List<String> logs = getLogs();
    assertThat(logs)
      .withFailMessage("")
      .overridingErrorMessage("")
      .usingComparator(null)
      .allMatch(e -> true) // Noncompliant
      .filteredOnNull("")
      .doesNotContain("") // Noncompliant
      .isNotNull();
  }

  @Test
  void with_extracting() {
    List<String> logs = getLogs();
    assertThat(logs).extracting("field").asList().allMatch(e -> true); // Noncompliant
  }

  @Test
  void with_contains() {
    List<String> logs = getLogs();
    assertThat(logs).contains("something").allMatch(e -> e.contains("error")); // Compliant
  }

  @Test
  void with_has_size() {
    List<String> logs = getLogs();
    assertThat(logs).hasSize(4).allMatch(e -> e.contains("error")); // Compliant
  }

  @Test
  void from_method_call_used_once() {
    assertThat(getLogsUsedOnce()).allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void from_method_call_used_multiple_times() {
    assertThat(getLogs()).allMatch(e -> e.contains("error")); // Compliant
  }

  @Test
  void stream_map_tested() {
    List<String> logs = getLogs();
    assertThat(logs).isNotEmpty();
    assertThat(logs.stream().map(s -> "")).allMatch(e -> e.contains("error")); // Compliant
  }

  @Test
  void stream_map_not_tested_2() {
    List<String> logs = getLogs();
    assertThat(logs.size()).isEqualTo(5);
    assertThat(logs.stream().map(s -> "")).allMatch(e -> e.contains("error")); // FN
  }

  @Test
  void stream_map_not_tested() {
    List<String> logs = getLogs();
    assertThat(logs.stream().map(s -> "")).allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void in_parenthesis() {
    List<String> logs = getLogs();
    assertThat(((logs))).allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void cast_list() {
    assertThat((List<String>) getObject()).allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void new_list_for_coverage() {
    assertThat(new ArrayList<String>()).allMatch(e -> e.contains("error")); // Compliant
  }

  @Test
  void this_list() {
    assertThat(this.logs).allMatch(e -> e.contains("error")); // Compliant
  }

  // No issue if assertion subject argument is used elsewhere to avoid FP. It can lead to FN.

  @Test
  void tested_for_emptiness_before_2() {
    List<String> logs = getLogs();
    assertThat(logs).isNotEmpty();
    assertThat(logs).allMatch(e -> e.contains("error")); // Compliant
  }

  @Test
  void tested_for_emptiness_after_2() {
    List<String> logs = getLogs();
    assertThat(logs).allMatch(e -> e.contains("error")); // Compliant
    assertThat(logs).isNotEmpty();
  }

  @Test
  void used_but_not_tested_for_emptiness() {
    List<String> logs = getLogs();
    assertThat(logs).contains("Log");
    assertThat(logs).allMatch(e -> e.contains("error")); // Compliant, with previous invocation, we know it contains something
  }

  @Test
  void used_but_not_tested_for_emptiness_2() {
    List<String> logs = getLogs();
    assertThat(logs).isNotNull();
    assertThat(logs).allMatch(e -> e.contains("error")); // FN, don't report if "logs" is used elsewhere
  }

  // Other methods

  @Test
  void simple_all_satisfy() {
    List<String> logs = getLogs();
    assertThat(logs).allSatisfy(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void simple_does_not_contain() {
    List<String> logs = getLogs();
    assertThat(logs).doesNotContain("a"); // Noncompliant
  }

  @Test
  void does_not_contain_with_index() {
    List<String> logs = getLogs();
    assertThat(logs).doesNotContain("a", atIndex(2)); // Compliant
  }

  @Test
  void does_not_contain_on_string() {
    assertThat(getString()).doesNotContain("a"); // Compliant
  }

  @Test
  void does_not_contain_on_string_2() {
    List<String> logs = getLogs();
    assertThat(logs).extracting("field").asString().doesNotContain(""); // Compliant
  }

  @Test
  void simple_does_not_contain_sequence() {
    List<String> logs = getLogs();
    assertThat(logs).doesNotContainSequence("a"); // Noncompliant
  }

  @Test
  void simple_does_not_contain_sub_sequence() {
    List<String> logs = getLogs();
    assertThat(logs).doesNotContainSubsequence("a"); // Noncompliant
  }

  @Test
  void simple_does_not_contain_any_element_of() {
    List<String> logs = getLogs();
    assertThat(logs).doesNotContainAnyElementsOf(new ArrayList<>()); // Noncompliant
  }

  @Test
  void mutliple_issues() {
    List<String> logs = getLogs();
    assertThat(logs).doesNotContain("") // Noncompliant
      .allMatch(e -> true); // Noncompliant
  }

  // Other assertion subject type

  @Test
  void for_interface_type() {
    List<String> logs = getLogs();
    org.assertj.core.api.AssertionsForInterfaceTypes.assertThat(logs).allMatch(e -> e.contains("error")); // Noncompliant
  }

  @Test
  void for_class_type() {
    List<String> logs = getLogs();
    org.assertj.core.api.AssertionsForClassTypes.assertThat(logs).asList().allMatch(e -> e.equals("error")); // Noncompliant
  }

  @Test
  void assert_object() {
    List<String> logs = getLogs();
    assertThatObject(logs).extracting("mylist").asList().allMatch(e -> e.equals("error")); // Noncompliant
  }


  List<String> getLogs() {
    return new ArrayList<>();
  }

  List<String> getLogsUsedOnce() {
    return new ArrayList<>();
  }

  Object getObject() {
    return new ArrayList<>();
  }

  String getString() {
    return "";
  }
}
