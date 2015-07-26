/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.ast.visitors;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Highlightable.HighlightingBuilder;
import org.sonar.java.JavaConfiguration;
import org.sonar.java.JavaSquid;
import org.sonar.java.SonarComponents;
import org.sonar.squidbridge.api.CodeVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class SyntaxHighlighterVisitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final SonarComponents sonarComponents = mock(SonarComponents.class);
  private final Highlightable highlightable = mock(Highlightable.class);
  private final HighlightingBuilderTester highlighting = spy(new HighlightingBuilderTester());

  private final SyntaxHighlighterVisitor syntaxHighlighterVisitor = new SyntaxHighlighterVisitor(sonarComponents, Charsets.UTF_8);

  private List<String> lines;
  private String eol;

  @Before
  public void setUp() throws Exception {
    when(sonarComponents.highlightableFor(Mockito.any(File.class))).thenReturn(highlightable);
    when(highlightable.newHighlighting()).thenReturn(highlighting);
  }

  @Test
  public void parse_error() throws Exception {
    File file = temp.newFile();
    Files.write("ParseError", file, Charsets.UTF_8);
    scan(file);
    Mockito.verify(highlightable, times(1)).newHighlighting();
    Mockito.verify(highlighting, never()).highlight(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString());
    assertThat(highlighting.done).isTrue();
    assertThat(highlighting.entries).isEmpty();
  }

  @Test
  public void test_LF() throws Exception {
    this.eol = "\n";
    File file = generateTestFile();
    scan(file);
    verifyHighlighting(file);
  }

  @Test
  public void test_CR_LF() throws Exception {
    this.eol = "\r\n";
    File file = generateTestFile();
    scan(file);
    verifyHighlighting(file);
  }

  @Test
  public void test_CR() throws Exception {
    this.eol = "\r";
    File file = generateTestFile();
    scan(file);
    verifyHighlighting(file);
  }

  private void scan(File file) {
    JavaSquid squid = new JavaSquid(new JavaConfiguration(Charsets.UTF_8), null, null, null, new CodeVisitor[] {syntaxHighlighterVisitor});
    squid.scan(Lists.newArrayList(file), Collections.<File>emptyList(), Collections.<File>emptyList());
  }

  private File generateTestFile() throws IOException {
    File file = temp.newFile();
    Files.write(Files.toString(new File("src/test/files/highlighter/Example.java"), Charsets.UTF_8).replaceAll("\\r\\n", "\n").replaceAll("\\n", eol), file, Charsets.UTF_8);
    return file;
  }

  private void verifyHighlighting(File file) throws IOException {
    lines = Files.readLines(file, Charsets.UTF_8);
    assertThatHasBeenHighlighted(offset(1, 1), offset(3, 4), "cppd");
    assertThatHasBeenHighlighted(offset(5, 1), offset(7, 4), "cppd");
    assertThatHasBeenHighlighted(offset(8, 1), offset(8, 18), "a");
    assertThatHasBeenHighlighted(offset(8, 19), offset(8, 27), "s");
    assertThatHasBeenHighlighted(offset(9, 1), offset(9, 6), "k");
    assertThatHasBeenHighlighted(offset(11, 3), offset(11, 24), "a");
    assertThatHasBeenHighlighted(offset(12, 3), offset(12, 6), "k");
    assertThatHasBeenHighlighted(offset(13, 5), offset(13, 11), "k");
    assertThatHasBeenHighlighted(offset(13, 12), offset(13, 14), "c");
    assertThatHasBeenHighlighted(offset(18, 2), offset(18, 11), "k");
    assertThatHasBeenHighlighted(offset(19, 21), offset(19, 28), "k");
    assertThatHasBeenHighlighted(offset(19, 29), offset(19, 30), "c");
    assertThat(highlighting.done).isTrue();
    assertThat(highlighting.entries).isEmpty();
  }

  private int offset(int line, int column) {
    int result = 0;
    for (int i = 0; i < line - 1; i++) {
      result += lines.get(i).length() + eol.length();
    }
    result += column - 1;
    return result;
  }

  private void assertThatHasBeenHighlighted(int start, int end, String type) {
    assertThat(hasBeenHighlighted(start, end, type)).isTrue();
  }

  private boolean hasBeenHighlighted(int start, int end, String type) {
    HighlightingBuilderTester.Entry expected = new HighlightingBuilderTester.Entry(start, end, type);
    HighlightingBuilderTester.Entry observed = null;
    for (HighlightingBuilderTester.Entry entry : highlighting.entries) {
      if (entry.equals(expected)) {
        observed = entry;
        break;
      }
    }
    if (observed == null) {
      return false;
    }

    // consume the entry
    highlighting.entries.remove(observed);
    return true;
  }

  private static class HighlightingBuilderTester implements Highlightable.HighlightingBuilder {
    private Set<Entry> entries = Sets.newHashSet();
    private boolean done = false;

    @Override
    public HighlightingBuilder highlight(int startOffset, int endOffset, String typeOfText) {
      entries.add(new Entry(startOffset, endOffset, typeOfText));
      return this;
    }

    @Override
    public void done() {
      done = true;
    }

    private static class Entry {
      private final int start;
      private final int end;
      private final String type;

      public Entry(int start, int end, String type) {
        this.start = start;
        this.end = end;
        this.type = type;
      }

      @Override
      public boolean equals(Object obj) {
        Entry other = (Entry) obj;
        if (end != other.end)
          return false;
        if (start != other.start)
          return false;
        if (!type.equals(other.type))
          return false;
        return true;
      }

      @Override
      public String toString() {
        return "[" + start + ", " + end + ", \"" + type + "\"]";
      }
    }
  }

}
