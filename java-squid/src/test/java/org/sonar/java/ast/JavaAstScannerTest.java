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
package org.sonar.java.ast;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.typed.ActionParser;
import com.sonar.sslr.api.typed.GrammarBuilder;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.java.Measurer;
import org.sonar.java.ast.parser.JavaNodeBuilder;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.squidbridge.AstScannerExceptionHandler;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class JavaAstScannerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  SensorContext context;
  private DefaultFileSystem fs;

  @Before
  public void setUp() throws Exception {
    context = mock(SensorContext.class);
    fs = new DefaultFileSystem(null);
  }

  @Test
  public void comments() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/Comments.java"), new VisitorsBridge(new Measurer(fs, context, false)));
    assertThat(file.getNoSonarTagLines()).contains(15).hasSize(1);
  }

  @Test
  public void noSonarLines() throws Exception {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/metrics/NoSonar.java"), new VisitorsBridge(new Measurer(fs, context, false)));
    assertThat(file.getNoSonarTagLines()).hasSize(1);
    assertThat(file.getNoSonarTagLines()).contains(8);
  }

  @Test
  public void scan_single_file_with_dumb_file_should_fail() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    String filename = "!!dummy";
    thrown.expectMessage(filename);
    JavaAstScanner.scanSingleFile(new File(filename), new VisitorsBridge(null));
  }

  @Test
  public void should_not_fail_whole_analysis_upon_parse_error_and_notify_audit_listeners() {
    FakeAuditListener listener = spy(new FakeAuditListener());
    JavaAstScanner scanner = new JavaAstScanner(new ActionParser<Tree>(Charsets.UTF_8, FakeLexer.builder(), FakeGrammar.class, new FakeTreeFactory(), new JavaNodeBuilder(), FakeLexer.ROOT));
    scanner.setVisitorBridge(new VisitorsBridge(listener));

    scanner.scan(ImmutableList.of(new File("src/test/resources/AstScannerParseError.txt")));
    verify(listener).processRecognitionException(Mockito.any(RecognitionException.class));
  }

  @Test
  public void should_propagate_visitor_exception_when_there_also_is_a_parse_error() {
    JavaAstScanner scanner = new JavaAstScanner(new ActionParser<Tree>(Charsets.UTF_8, FakeLexer.builder(), FakeGrammar.class, new FakeTreeFactory(), new JavaNodeBuilder(), FakeLexer.ROOT));
    scanner.setVisitorBridge(new VisitorsBridge(new JavaFileScanner() {

      @Override
      public void scanFile(JavaFileScannerContext context) {
        throw new NullPointerException("foo");
      }
    }));

    thrown.expectMessage("SonarQube is unable to analyze file");
    thrown.expect(new BaseMatcher() {

      @Override
      public boolean matches(Object item) {
        return item instanceof AnalysisException &&
          ((AnalysisException) item).getCause() instanceof NullPointerException;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("instanceof AnalysisException with NullPointerException cause");
      }

    });
    scanner.scan(ImmutableList.of(new File("src/test/resources/AstScannerParseError.txt")));
  }

  @Test
  public void should_propagate_visitor_exception_when_no_parse_error() {
    JavaAstScanner scanner = new JavaAstScanner(new ActionParser<Tree>(Charsets.UTF_8, FakeLexer.builder(), FakeGrammar.class, new FakeTreeFactory(), new JavaNodeBuilder(), FakeLexer.ROOT));
    scanner.setVisitorBridge(new VisitorsBridge(new JavaFileScanner() {

      @Override
      public void scanFile(JavaFileScannerContext context) {
        throw new NullPointerException("foo");
      }
    }));


    thrown.expectMessage("SonarQube is unable to analyze file");
    thrown.expect(new BaseMatcher() {

      @Override
      public boolean matches(Object item) {
        return item instanceof AnalysisException &&
          ((AnalysisException) item).getCause() instanceof NullPointerException;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("instanceof AnalysisException with NullPointerException cause");
      }

    });
    scanner.scan(ImmutableList.of(new File("src/test/resources/AstScannerNoParseError.txt")));
  }

  private static class FakeAuditListener implements JavaFileScanner, AstScannerExceptionHandler {

    @Override
    public void processRecognitionException(RecognitionException e) {
    }

    @Override
    public void processException(Exception e) {
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {

    }
  }

  public static class FakeTreeFactory {
    public FakeTreeFactory(){}
    public Tree root(JavaTree javaTree) {
      return new Tree() {
        @Override
        public boolean is(Kind... kind) {
          return false;
        }

        @Override
        public Kind kind() {
          return null;
        }

        @Override
        public void accept(TreeVisitor visitor) {

        }
      };
    }
  }

  public static class FakeGrammar {
    final GrammarBuilder<InternalSyntaxToken> b;
    final FakeTreeFactory f;

    public FakeGrammar(GrammarBuilder<InternalSyntaxToken> b, FakeTreeFactory f) {
      this.b = b;
      this.f = f;
    }

    public Tree ROOT() {
      return b.<Tree>nonterminal(FakeLexer.ROOT).is(f.root(b.token(FakeLexer.TOKEN)));
    }
  }

  public enum FakeLexer implements GrammarRuleKey {
    ROOT, TOKEN;

    public static LexerlessGrammarBuilder builder() {
      LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

      b.rule(TOKEN).is("foo");
      b.setRootRule(ROOT);

      return b;
    }

  }

}
