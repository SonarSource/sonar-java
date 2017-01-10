/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileLinesVisitorTest {

  private JavaConfiguration conf;
  private File baseDir;

  @Before
  public void setUp() throws Exception {
    conf = new JavaConfiguration(StandardCharsets.UTF_8);
    baseDir = new File("src/test/files/metrics");
  }
  private void checkLines(String filename, FileLinesContext context) {
    checkLines(filename, context, false);
  }

  private void checkLines(String filename, FileLinesContext context, boolean sqGreaterThan62) {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.fileLength(Mockito.any(File.class))).thenAnswer(invocation -> {
      File arg = (File) invocation.getArguments()[0];
      return Files.readLines(arg, StandardCharsets.UTF_8).size();
    });
    when(sonarComponents.isSQGreaterThan62()).thenReturn(sqGreaterThan62);
    when(sonarComponents.fileLinesContextFor(Mockito.any(File.class))).thenReturn(context);

    JavaSquid squid = new JavaSquid(conf, null, null, null, null, new FileLinesVisitor(sonarComponents));
    squid.scan(Lists.newArrayList(new File(baseDir, filename)), Collections.emptyList());
  }

  private int countTrivia(String filename) {
    TriviaVisitor triviaVisitor = new TriviaVisitor();
    JavaSquid squid = new JavaSquid(conf, null, null, null, null, triviaVisitor);
    squid.scan(Lists.newArrayList(new File(baseDir, filename)), Collections.emptyList());
    return triviaVisitor.numberTrivia;
  }

  @Test
  public void lines_of_code_data() {
    FileLinesContext context = mock(FileLinesContext.class);
    checkLines("LinesOfCode.java", context);

    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 1, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 2, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 3, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 4, 0);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 5, 1);
    verify(context).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 6, 1);

    verify(context).save();
  }

  @Test
  public void comment_lines_data() {
    FileLinesContext context = mock(FileLinesContext.class);
    checkLines("Comments.java", context);

    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 1, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 2, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 3, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 4, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 5, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 6, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 7, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 8, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 9, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 10, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 11, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 12, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 13, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 14, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 15, 1);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 16, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 17, 0);
    verify(context).setIntValue(CoreMetrics.COMMENT_LINES_DATA_KEY, 18, 0);

    verify(context).save();
  }

  @Test
  public void comments_full() {
    String filename = "CommentsFull.java";
    CommentsVerifier context = new CommentsVerifier();
    checkLines(filename, context);

    List<Integer> reportedCommentLines = context.commentedLine;
    CommentsCounter counter = CommentsCounter.getComments(baseDir, filename);
    assertThatContainsAllLines(counter, reportedCommentLines);

    int expectedNumberComments = counter.numberComments - counter.numberFixme;
    assertThat(context.numberComments).isEqualTo(expectedNumberComments);
    assertThat(countTrivia(filename)).isEqualTo(
      expectedNumberComments
      // FIXME variable declarations sharing types are using the same type when iterating over the tokens. see line 109
      + 1);
  }

  @Test
  public void executable_lines_should_be_counted_withSQGreaterThan62() throws Exception {
    FileLinesContext context = mock(FileLinesContext.class);
    checkLines("ExecutableLines.java", context, true);
    int[] expected = new int[] {0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1,
      0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0};
    assertThat(expected).hasSize(56);
    for (int i = 0; i < expected.length; i++) {
      int line = i + 1;
      verify(context).setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, expected[i]);
    }
    verify(context).save();
  }
  @Test
  public void executable_lines_should_NOT_be_counted_withSQLessThan62() throws Exception {
    FileLinesContext context = mock(FileLinesContext.class);
    checkLines("ExecutableLines.java", context);
    verify(context, times(0)).setIntValue(eq(CoreMetrics.EXECUTABLE_LINES_DATA_KEY), anyInt(), anyInt());
    verify(context).save();
  }


  private static void assertThatContainsAllLines(CommentsCounter counter, List<Integer> reportedCommentLines) {
    for (Integer line : reportedCommentLines) {
      if (counter.commentedLines.contains(line)) {
        counter.commentedLines.remove(line);
      } else {
        Fail.fail("should not have extra lines");
      }
    }
    assertThat(counter.commentedLines).containsExactly(counter.casesNotCoveredLines.toArray(new Integer[0]));
  }

  private static class CommentsCounter {
    private int numberComments = 0;
    private int numberFixme = 0;
    private List<Integer> commentedLines = Lists.newLinkedList();
    private List<Integer> casesNotCoveredLines = Lists.newLinkedList();

    private CommentsCounter() {
    }

    private static CommentsCounter getComments(File baseDir, String filename) {
      CommentsCounter counter = new CommentsCounter();
      try {
        int lineNumber = 1;
        for (String line : Files.readLines(new File(baseDir, filename), StandardCharsets.UTF_8)) {
          int commentCount = StringUtils.countMatches(line, "comment");
          for (int i = 0; i < commentCount; i++) {
            counter.commentedLines.add(lineNumber);
          }
          counter.numberComments += commentCount;
          int fixmeCount = StringUtils.countMatches(line, "FIXME");
          for (int i = 0; i < fixmeCount; i++) {
            counter.casesNotCoveredLines.add(lineNumber);
          }
          counter.numberFixme += fixmeCount;
          lineNumber++;
        }
      } catch (IOException e) {
        Fail.fail(e.getMessage());
      }
      return counter;
    }
  }

  private static class CommentsVerifier implements FileLinesContext {
    private List<Integer> commentedLine = Lists.newLinkedList();
    private int numberComments = 0;

    @Override
    public void setStringValue(String metricKey, int line, String value) {
    }

    @Override
    public void setIntValue(String metricKey, int line, int value) {
      if (CoreMetrics.COMMENT_LINES_DATA_KEY.equals(metricKey)) {
        for (int i = 0; i < value; i++) {
          commentedLine.add(line);
        }
        numberComments += value;
      }
    }

    @Override
    public void save() {
    }

    @Override
    public String getStringValue(String metricKey, int line) {
      return null;
    }

    @Override
    public Integer getIntValue(String metricKey, int line) {
      return null;
    }
  }

  private static class TriviaVisitor extends SubscriptionVisitor {

    int numberTrivia = 0;

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      numberTrivia++;
    }
  }

}
