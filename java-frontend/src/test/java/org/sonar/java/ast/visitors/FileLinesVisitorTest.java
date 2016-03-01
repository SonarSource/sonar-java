/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileLinesVisitorTest {

  private JavaConfiguration conf;
  private File baseDir;
  private Project sonarProject;

  @Before
  public void setUp() throws Exception {
    sonarProject = mock(Project.class);
    conf = new JavaConfiguration(Charsets.UTF_8);
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    baseDir = new File("src/test/files/metrics");
    when(sonarProject.getFileSystem()).thenReturn(pfs);
    when(pfs.getBasedir()).thenReturn(baseDir);
  }

  private void checkLines(String filename, FileLinesContext context) {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    when(sonarComponents.fileLinesContextFor(Mockito.any(File.class))).thenReturn(context);

    JavaSquid squid = new JavaSquid(conf, null, null, null, new CodeVisitor[] {new FileLinesVisitor(sonarComponents, conf.getCharset())});
    squid.scan(Lists.newArrayList(new File(baseDir, filename)), Collections.<File>emptyList(), Collections.<File>emptyList());
  }

  private int countTrivia(String filename) {
    TriviaVisitor triviaVisitor = new TriviaVisitor();
    JavaSquid squid = new JavaSquid(conf, null, null, null, new CodeVisitor[] {triviaVisitor});
    squid.scan(Lists.newArrayList(new File(baseDir, filename)), Collections.<File>emptyList(), Collections.<File>emptyList());
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

  private static void assertThatContainsAllLines(CommentsCounter counter, List<Integer> reportedCommentLines) {
    for (Integer line : reportedCommentLines) {
      if (counter.commentedLines.contains(line)) {
        counter.commentedLines.remove(line);
      } else {
        fail("should not have extra lines");
      }
    }
    assertThat(counter.commentedLines).containsExactly(counter.casesNotCoveredLines.toArray());
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
        for (String line : Files.readLines(new File(baseDir, filename), Charsets.UTF_8)) {
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
        fail();
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
