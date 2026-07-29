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
package org.sonar.java.checks;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.ConditionalRuleCacheUtils.CachedFileData;
import org.sonar.java.checks.ConditionalRuleCacheUtils.CachedIssue;
import org.sonar.java.checks.ConditionalRuleCacheUtils.ImportEditData;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalRuleCacheUtilsTest {

  @Test
  void serialize_and_deserialize_no_issues() {
    byte[] data = ConditionalRuleCacheUtils.serialize(7, 3, List.of());
    CachedFileData result = ConditionalRuleCacheUtils.deserialize(data);

    assertThat(result.totalCount()).isEqualTo(7);
    assertThat(result.issueCount()).isEqualTo(3);
    assertThat(result.issues()).isEmpty();
  }

  @Test
  void serialize_and_deserialize_issue_without_import_edit() {
    var issue = new CachedIssue(10, 5, 10, 20, "Use an enum constant.", "Month.JUNE", null);
    byte[] data = ConditionalRuleCacheUtils.serialize(4, 1, List.of(issue));
    CachedFileData result = ConditionalRuleCacheUtils.deserialize(data);

    assertThat(result.totalCount()).isEqualTo(4);
    assertThat(result.issueCount()).isEqualTo(1);
    assertThat(result.issues()).hasSize(1);

    CachedIssue deserialized = result.issues().get(0);
    assertThat(deserialized.startLine()).isEqualTo(10);
    assertThat(deserialized.startCol()).isEqualTo(5);
    assertThat(deserialized.endLine()).isEqualTo(10);
    assertThat(deserialized.endCol()).isEqualTo(20);
    assertThat(deserialized.message()).isEqualTo("Use an enum constant.");
    assertThat(deserialized.replacement()).isEqualTo("Month.JUNE");
    assertThat(deserialized.importEdit()).isNull();
  }

  @Test
  void serialize_and_deserialize_issue_with_import_edit() {
    var importEdit = new ImportEditData(1, 0, 1, 0, "import java.time.Month;\n");
    var issue = new CachedIssue(10, 5, 10, 20, "Use an enum constant.", "Month.JUNE", importEdit);
    byte[] data = ConditionalRuleCacheUtils.serialize(4, 1, List.of(issue));
    CachedFileData result = ConditionalRuleCacheUtils.deserialize(data);

    assertThat(result.issues()).hasSize(1);
    CachedIssue deserialized = result.issues().get(0);
    assertThat(deserialized.importEdit()).isNotNull();

    ImportEditData deserializedImport = deserialized.importEdit();
    assertThat(deserializedImport.startLine()).isEqualTo(1);
    assertThat(deserializedImport.startCol()).isZero();
    assertThat(deserializedImport.endLine()).isEqualTo(1);
    assertThat(deserializedImport.endCol()).isZero();
    assertThat(deserializedImport.replacement()).isEqualTo("import java.time.Month;\n");
  }

  @Test
  void serialize_and_deserialize_multiple_issues_mixed() {
    var withImport = new CachedIssue(5, 10, 5, 11, "msg1", "DayOfWeek.MONDAY",
      new ImportEditData(1, 0, 1, 0, "import java.time.DayOfWeek;\n"));
    var withoutImport = new CachedIssue(12, 3, 12, 15, "msg2", "Month.MARCH", null);
    byte[] data = ConditionalRuleCacheUtils.serialize(10, 2, List.of(withImport, withoutImport));
    CachedFileData result = ConditionalRuleCacheUtils.deserialize(data);

    assertThat(result.totalCount()).isEqualTo(10);
    assertThat(result.issueCount()).isEqualTo(2);
    assertThat(result.issues()).hasSize(2);
    assertThat(result.issues().get(0).importEdit()).isNotNull();
    assertThat(result.issues().get(1).importEdit()).isNull();
  }

  @Test
  void deserialize_empty_data_returns_empty_file_data() {
    CachedFileData result = ConditionalRuleCacheUtils.deserialize(new byte[0]);

    assertThat(result.totalCount()).isZero();
    assertThat(result.issueCount()).isZero();
    assertThat(result.issues()).isEmpty();
  }

  @Test
  void serialize_and_deserialize_message_with_special_characters() {
    // Pipe '|' and newline '\n' in message/replacement would break naive line/field splitting
    // — Base64 encoding must handle them correctly.
    var issue = new CachedIssue(1, 0, 1, 5,
      "Use \"java.time.Month\" enum|constant\ninstead", "Month.JUNE|value", null);
    byte[] data = ConditionalRuleCacheUtils.serialize(1, 1, List.of(issue));
    CachedFileData result = ConditionalRuleCacheUtils.deserialize(data);

    CachedIssue deserialized = result.issues().get(0);
    assertThat(deserialized.message()).isEqualTo("Use \"java.time.Month\" enum|constant\ninstead");
    assertThat(deserialized.replacement()).isEqualTo("Month.JUNE|value");
  }
}
