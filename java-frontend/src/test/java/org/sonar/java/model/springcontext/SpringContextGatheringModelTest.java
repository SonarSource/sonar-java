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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.reporting.AnalyzerMessage.TextSpan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SpringContextGatheringModelTest {

  @Test
  void restore_preserves_current_files_fills_missing_files_and_runs_once() {
    var previous = new SpringContextGatheringModel();
    previous.collectPackages("module-a", "current", null, Set.of("old"));
    previous.collectPackages("module-a", "restored", null, Set.of("restored"));
    previous.collectPackages("module-b", "other", null, Set.of("other"));
    previous.ensureModule("empty");

    var model = new SpringContextGatheringModel();
    model.collectPackages("module-a", "current", mock(InputFile.class), Set.of("current"));
    assertThat(model.isRestored()).isFalse();

    model.restoreFrom(previous);

    assertThat(model.isRestored()).isTrue();
    assertThat(model.filesData()).containsOnlyKeys("module-a", "module-b", "empty");
    assertThat(model.filesData().get("module-a")).containsOnlyKeys("current", "restored");
    assertThat(model.filesData().get("module-a").get("current").packages()).containsExactly("current");
    assertThat(model.filesData().get("module-a").get("restored").packages()).containsExactly("restored");
    assertThat(model.filesData().get("module-b").get("other").packages()).containsExactly("other");
    assertThat(model.filesData().get("empty")).isEmpty();

    var later = new SpringContextGatheringModel();
    later.collectPackages("module-c", "new", null, Set.of("new"));
    model.restoreFrom(later);

    assertThat(model.filesData()).doesNotContainKey("module-c");
  }

  @Test
  void collection_replaces_one_kind_of_data_and_keeps_the_latest_known_input_file() {
    var model = new SpringContextGatheringModel();
    var firstBean = bean("first");
    var secondBean = bean("second");
    var thirdBean = bean("third");
    InputFile firstFile = mock(InputFile.class);
    InputFile secondFile = mock(InputFile.class);

    model.collectBeans("module", "file", null, List.of(firstBean));
    model.collectPackages("module", "file", null, Set.of("old.package"));
    model.collectBeans("module", "file", firstFile, List.of(secondBean));

    var data = model.filesData().get("module").get("file");
    assertThat(data.inputFile()).isSameAs(firstFile);
    assertThat(data.beans()).containsExactly(secondBean);
    assertThat(data.packages()).containsExactly("old.package");

    model.collectPackages("module", "file", firstFile, Set.of("new.package"));
    model.collectBeans("module", "file", secondFile, List.of(thirdBean));
    model.collectPackages("module", "file", null, Set.of("latest.package"));

    assertThat(model.filesData().get("module").get("file")).isSameAs(data);
    assertThat(data.inputFile()).isSameAs(secondFile);
    assertThat(data.beans()).containsExactly(thirdBean);
    assertThat(data.packages()).containsExactly("latest.package");
  }

  @Test
  void remove_unvisited_files_keeps_visited_files_and_empty_modules() {
    var previous = new SpringContextGatheringModel();
    previous.collectPackages("module", "stale", null, Set.of("stale.package"));
    previous.collectPackages("other", "stale", null, Set.of("other.package"));
    previous.ensureModule("empty");
    var model = new SpringContextGatheringModel();
    InputFile visitedFile = mock(InputFile.class);
    model.collectPackages("module", "visited", visitedFile, Set.of("current.package"));
    model.restoreFrom(previous);

    model.removeUnvisitedFiles();

    assertThat(model.filesData()).containsOnlyKeys("module", "other", "empty");
    assertThat(model.filesData().get("module")).containsOnlyKeys("visited");
    assertThat(model.filesData().get("module").get("visited").inputFile()).isSameAs(visitedFile);
    assertThat(model.filesData().get("other")).isEmpty();
    assertThat(model.filesData().get("empty")).isEmpty();
  }

  @Test
  void builds_deduplicated_packages_by_module() {
    var model = new SpringContextGatheringModel();
    model.collectPackages("module", "first", null, Set.of("shared", "first"));
    model.collectPackages("module", "second", null, Set.of("shared", "second"));
    model.collectPackages("other", "third", null, Set.of("third"));
    model.ensureModule("empty");
    model.ensureModule("empty");

    var projectPackageScan = SpringContextModel.of(model).getProjectPackageScan();
    assertThat(projectPackageScan.getPackagesForModule("module")).containsExactlyInAnyOrder("shared", "first", "second");
    assertThat(projectPackageScan.getPackagesForModule("other")).containsExactly("third");
    assertThat(projectPackageScan.getPackagesForModule("empty")).isEmpty();
  }

  @Test
  void files_data_returns_an_unmodifiable_outer_snapshot_with_shared_file_maps() {
    var model = new SpringContextGatheringModel();
    model.ensureModule("module");
    var snapshot = model.filesData();

    assertThatThrownBy(() -> snapshot.put("new", Map.of())).isInstanceOf(UnsupportedOperationException.class);
    model.ensureModule("later");
    model.collectPackages("module", "file", null, Set.of("package"));

    assertThat(snapshot).doesNotContainKey("later");
    assertThat(snapshot.get("module")).containsOnlyKeys("file");
  }

  private static BeanDefinitionHolder.InputFileData bean(String name) {
    return new BeanDefinitionHolder.InputFileData(name, "example.Bean", "example", new TextSpan(1, 0, 1, 4),
      false, ProfileExpression.UNCONDITIONAL, null, Map.of(), Set.of("example.Bean"));
  }
}
