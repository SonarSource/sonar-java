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
package org.sonar.java.ast;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AuditListener;
import com.sonar.sslr.api.RecognitionException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.resources.InputFile;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AstScannerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_not_fail_whole_analysis_upon_parse_error_and_notify_audit_listeners() {
    FakeAuditListener listener = spy(new FakeAuditListener());

    AstScanner scanner = new AstScanner(new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, FakeGrammar.builder().build()));
    scanner.withSquidAstVisitor(listener);

    scanner.scan(ImmutableList.of(mockInputFile(new File("src/test/resources/AstScannerParseError.txt"))));
    verify(listener).processRecognitionException(Mockito.any(RecognitionException.class));
  }

  @Test
  public void should_propagate_visitor_exception_when_there_also_is_a_parse_error() {
    AstScanner scanner = new AstScanner(new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, FakeGrammar.builder().build()));
    scanner.withSquidAstVisitor(new SquidAstVisitor<LexerlessGrammar>() {

      @Override
      public void visitFile(AstNode node) {
        throw new NullPointerException("foo");
      }

    });

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
    scanner.scan(ImmutableList.of(mockInputFile(new File("src/test/resources/AstScannerParseError.txt"))));
  }

  @Test
  public void should_propagate_visitor_exception_when_no_parse_error() {
    AstScanner scanner = new AstScanner(new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, FakeGrammar.builder().build()));
    scanner.withSquidAstVisitor(new SquidAstVisitor<LexerlessGrammar>() {

      @Override
      public void visitFile(AstNode node) {
        throw new NullPointerException("foo");
      }

    });

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
    scanner.scan(ImmutableList.of(mockInputFile(new File("src/test/resources/AstScannerNoParseError.txt"))));
  }

  private static class FakeAuditListener extends SquidAstVisitor<LexerlessGrammar> implements AuditListener {

    @Override
    public void processRecognitionException(RecognitionException e) {
    }

    @Override
    public void processException(Exception e) {
    }

  }

  private static enum FakeGrammar implements GrammarRuleKey {
    ROOT;

    public static LexerlessGrammarBuilder builder() {
      LexerlessGrammarBuilder b = LexerlessGrammarBuilder.create();

      b.rule(ROOT).is("foo");
      b.setRootRule(ROOT);

      return b;
    }

  }

  private static InputFile mockInputFile(File file) {
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.getFile()).thenReturn(file);
    return inputFile;
  }

}
