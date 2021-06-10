/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model;

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTUtils;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExportsDirective;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.ModuleDirective;
import org.eclipse.jdt.core.dom.ModuleModifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.OpensDirective;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ProvidesDirective;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.RequiresDirective;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.UsesDirective;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jdt.internal.formatter.Token;
import org.eclipse.jdt.internal.formatter.TokenManager;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.ExecutionTimeReport;
import org.sonar.java.PerformanceMeasure;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.BlockStatementListTreeImpl;
import org.sonar.java.ast.parser.BoundListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementExpressionListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.ast.parser.TypeUnionListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
import org.sonar.java.model.declaration.ModuleNameListTreeImpl;
import org.sonar.java.model.declaration.OpensDirectiveTreeImpl;
import org.sonar.java.model.declaration.ProvidesDirectiveTreeImpl;
import org.sonar.java.model.declaration.RequiresDirectiveTreeImpl;
import org.sonar.java.model.declaration.UsesDirectiveTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.InternalPostfixUnaryExpression;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.ProgressReport;

@ParametersAreNonnullByDefault
public class JParser {

  private static final Set<String> JRE_JARS = new HashSet<>(Arrays.asList("rt.jar", "jrt-fs.jar", "android.jar"));

  private static final Logger LOG = Loggers.get(JParser.class);

  public static final String MAXIMUM_SUPPORTED_JAVA_VERSION = "15";

  private static final String MAXIMUM_ECJ_WARNINGS = "42000";

  private static final Predicate<IProblem> IS_SYNTAX_ERROR = error -> (error.getID() & IProblem.Syntax) != 0;
  private static final Predicate<IProblem> IS_UNDEFINED_TYPE_ERROR = error -> (error.getID() & IProblem.UndefinedType) != 0;

  public static class Result {
    private final Exception e;
    private final JavaTree.CompilationUnitTreeImpl t;

    Result(Exception e) {
      this.e = e;
      this.t = null;
    }

    Result(JavaTree.CompilationUnitTreeImpl t) {
      this.e = null;
      this.t = t;
    }

    public JavaTree.CompilationUnitTreeImpl get() throws Exception {
      if (e != null) {
        throw e;
      }
      return t;
    }
  }

  public static void parseFileByFile(
    String version,
    List<File> classpath,
    Iterable<? extends InputFile> inputFiles,
    BooleanSupplier isCanceled,
    BiConsumer<InputFile, Result> action) {

    boolean successfullyCompleted = false;
    boolean cancelled = false;

    ExecutionTimeReport executionTimeReport = new ExecutionTimeReport(Clock.systemUTC());
    ProgressReport progressReport = new ProgressReport("Report about progress of Java AST analyzer", TimeUnit.SECONDS.toMillis(10));
    List<String> filesNames = StreamSupport.stream(inputFiles.spliterator(), false).map(InputFile::toString).collect(Collectors.toList());
    progressReport.start(filesNames);
    try {
      for (InputFile inputFile : inputFiles) {
        if (isCanceled.getAsBoolean()) {
          cancelled = true;
          break;
        }
        executionTimeReport.start(inputFile);

        Result result;
        PerformanceMeasure.Duration parseDuration = PerformanceMeasure.start("JParser");
        try {
          result = new Result(parse(
            version,
            inputFile.filename(),
            inputFile.contents(),
            classpath
          ));
        } catch (Exception e) {
          result = new Result(e);
        } finally {
          parseDuration.stop();
        }

        action.accept(inputFile, result);

        executionTimeReport.end();
        progressReport.nextFile();
      }
      successfullyCompleted = !cancelled;
    } finally {
      if (successfullyCompleted) {
        progressReport.stop();
      } else {
        progressReport.cancel();
      }
      executionTimeReport.report();
    }
  }

  public static void parseAsBatch(
    String version,
    List<File> classpath,
    Iterable<? extends InputFile> inputFiles,
    BooleanSupplier isCanceled,
    BiConsumer<InputFile, Result> action
  ) {

    LOG.info("Using ECJ batch to parse source files.");

    ASTParser astParser = createASTParser(version, classpath);

    List<String> sourceFilePaths = new ArrayList<>();
    List<String> encodings = new ArrayList<>();
    Map<File, InputFile> inputs = new HashMap<>();
    for (InputFile inputFile : inputFiles) {
      String sourceFilePath = inputFile.absolutePath();
      inputs.put(
        new File(sourceFilePath),
        inputFile
      );
      sourceFilePaths.add(sourceFilePath);
      encodings.add(inputFile.charset().name());
    }

    PerformanceMeasure.Duration batchPerformance = PerformanceMeasure.start("ParseAsBatch");
    ExecutionTimeReport executionTimeReport = new ExecutionTimeReport(Clock.systemUTC());
    JProgressMonitor monitor = new JProgressMonitor(isCanceled);

    try {
      astParser.createASTs(
        sourceFilePaths.toArray(new String[0]),
        encodings.toArray(new String[0]),
        new String[0],
        new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            PerformanceMeasure.Duration convertDuration = PerformanceMeasure.start("Convert");

            InputFile inputFile = inputs.get(new File(sourceFilePath));
            executionTimeReport.start(inputFile);
            Result result;
            try {
              result = new Result(convert(
                version,
                inputFile.filename(),
                inputFile.contents(),
                ast
              ));
            } catch (Exception e) {
              result = new Result(e);
            }
            convertDuration.stop();
            PerformanceMeasure.Duration analyzeDuration = PerformanceMeasure.start("Analyze");
            action.accept(inputFile, result);

            executionTimeReport.end();
            analyzeDuration.stop();
          }
        },
        monitor
      );
    } finally {
      // ExecutionTimeReport will not include the parsing time by file when using batch mode.
      executionTimeReport.reportAsBatch();
      batchPerformance.stop();
      monitor.done();
    }
  }

  private static class JProgressMonitor implements IProgressMonitor, Runnable {

    private static final Logger LOG = Loggers.get(JProgressMonitor.class);
    private static final long PERIOD = TimeUnit.SECONDS.toMillis(10);
    private final Thread thread;

    private final BooleanSupplier isCanceled;

    private boolean success = false;
    private int totalWork = 0;
    private int processedWork = 0;

    public JProgressMonitor(BooleanSupplier isCanceled) {
      this.isCanceled = isCanceled;

      thread = new Thread(this);
      thread.setName("Report about progress of Java AST analyzer");
      thread.setDaemon(true);
    }

    @Override
    public void run() {
      while (!Thread.interrupted()) {
        try {
          Thread.sleep(PERIOD);
          double percentage = processedWork / (double) totalWork;
          log(String.format("%d%% analyzed", (int) (percentage * 100)));
        } catch (InterruptedException e) {
          thread.interrupt();
          break;
        }
      }
    }

    @Override
    public void beginTask(String name, int totalWork) {
      this.totalWork = totalWork;
      log("Starting batch processing.");
      thread.start();
    }

    @Override
    public void done() {
      if (success) {
        log("100% analyzed");
        log("Batch processing: Done!");
      }
      thread.interrupt();
      join();
    }

    @Override
    public boolean isCanceled() {
      if (isCanceled.getAsBoolean()) {
        log("Batch processing: Cancelled!");
        return true;
      }
      return false;
    }

    @Override
    public void setCanceled(boolean value) {
      // do nothing
    }

    private void join() {
      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void worked(int work) {
      processedWork += work;
      if (processedWork == totalWork) {
        success = true;
      }
    }

    @Override
    public void internalWorked(double work) {
      // do nothing
    }

    @Override
    public void setTaskName(String name) {
      // do nothing
    }

    @Override
    public void subTask(String name) {
      // do nothing
    }

    private static void log(String message) {
      synchronized (LOG) {
        LOG.info(message);
        LOG.notifyAll();
      }
    }
  }

  /**
   * @param unitName see {@link ASTParser#setUnitName(String)}
   * @throws RecognitionException in case of syntax errors
   */
  @VisibleForTesting
  public static JavaTree.CompilationUnitTreeImpl parse(
    String version,
    String unitName,
    String source,
    List<File> classpath
  ) {
    ASTParser astParser = createASTParser(version, classpath);

    astParser.setUnitName(unitName);
    char[] sourceChars = source.toCharArray();
    astParser.setSource(sourceChars);

    CompilationUnit astNode;
    try {
      astNode = (CompilationUnit) astParser.createAST(null);
    } catch (Exception e) {
      LOG.error("ECJ: Unable to parse file", e);
      throw new RecognitionException(-1, "ECJ: Unable to parse file.", e);
    }

    return convert(version, unitName, source, astNode);
  }

  private static ASTParser createASTParser(String version, List<File> classpath) {
    ASTParser astParser = ASTParser.newParser(AST.JLS15);
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_COMPLIANCE, version);
    options.put(JavaCore.COMPILER_SOURCE, version);
    options.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, MAXIMUM_ECJ_WARNINGS);
    if (MAXIMUM_SUPPORTED_JAVA_VERSION.equals(version)) {
      options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    }
    // enabling all supported compiler warnings
    JWarning.Type.compilerOptions().forEach(option -> options.put(option, "warning"));

    astParser.setCompilerOptions(options);

    boolean includeRunningVMBootclasspath = classpath.stream().noneMatch(f -> JRE_JARS.contains(f.getName()));

    astParser.setEnvironment(
      classpath.stream().map(File::getAbsolutePath).toArray(String[]::new),
      new String[]{},
      new String[]{},
      includeRunningVMBootclasspath
    );

    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);

    return astParser;
  }

  private static JavaTree.CompilationUnitTreeImpl convert(
    String version,
    String unitName,
    String source,
    CompilationUnit astNode
  ) {
    List<IProblem> errors = Stream.of(astNode.getProblems()).filter(IProblem::isError).collect(Collectors.toList());
    Optional<IProblem> possibleSyntaxError = errors.stream().filter(IS_SYNTAX_ERROR).findFirst();
    if (possibleSyntaxError.isPresent()) {
      IProblem syntaxError = possibleSyntaxError.get();
      int line = syntaxError.getSourceLineNumber();
      int column = astNode.getColumnNumber(syntaxError.getSourceStart());
      String message = String.format("Parse error at line %d column %d: %s", line, column, syntaxError.getMessage());
      // interrupt parsing
      throw new RecognitionException(line, message);
    }

    Set<String> undefinedTypes = errors.stream()
      .filter(IS_UNDEFINED_TYPE_ERROR)
      .map(IProblem::getMessage)
      .collect(Collectors.toSet());

    JParser converter = new JParser();
    converter.sema = new JSema(astNode.getAST());
    converter.sema.undefinedTypes.addAll(undefinedTypes);
    converter.compilationUnit = astNode;
    converter.tokenManager = new TokenManager(lex(version, unitName, source.toCharArray()), source, new DefaultCodeFormatterOptions(new HashMap<>()));

    JavaTree.CompilationUnitTreeImpl tree = converter.convertCompilationUnit(astNode);
    tree.sema = converter.sema;
    JWarning.Mapper.warningsFor(astNode).mappedInto(tree);

    ASTUtils.mayTolerateMissingType(astNode.getAST());

    setParents(tree);
    return tree;
  }

  private static void setParents(Tree node) {
    Iterator<Tree> childrenIterator = iteratorFor(node);
    while (childrenIterator.hasNext()) {
      Tree child = childrenIterator.next();
      ((JavaTree) child).setParent(node);
      setParents(child);
    }
  }

  private static Iterator<Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TOKEN) {
      // getChildren throws exception in this case
      return Collections.emptyIterator();
    }
    return ((JavaTree) node).getChildren().iterator();
  }

  private static List<Token> lex(String version, String unitName, char[] sourceChars) {
    List<Token> tokens = new ArrayList<>();
    Scanner scanner = new Scanner(
      true,
      false,
      false,
      CompilerOptions.versionToJdkLevel(version),
      null,
      null,
      false
    );
    scanner.fakeInModule = "module-info.java".equals(unitName);
    scanner.setSource(sourceChars);
    while (true) {
      try {
        int tokenType = scanner.getNextToken();
        Token token = Token.fromCurrent(scanner, tokenType);
        tokens.add(token);
        if (tokenType == TerminalTokens.TokenNameEOF) {
          break;
        }
      } catch (InvalidInputException e) {
        throw new IllegalStateException(e);
      }
    }
    return tokens;
  }

  private CompilationUnit compilationUnit;

  private TokenManager tokenManager;

  private JSema sema;

  private final Deque<JLabelSymbol> labels = new LinkedList<>();

  private void declaration(@Nullable IBinding binding, Tree node) {
    if (binding == null) {
      return;
    }
    sema.declarations.put(binding, node);
  }

  private void usage(@Nullable IBinding binding, IdentifierTree node) {
    if (binding == null) {
      return;
    }
    binding = JSema.declarationBinding(binding);
    sema.usages.computeIfAbsent(binding, k -> new ArrayList<>())
      .add(node);
  }

  private void usageLabel(@Nullable IdentifierTreeImpl node) {
    if (node == null) {
      return;
    }
    labels.stream()
      .filter(symbol -> symbol.name().equals(node.name()))
      .findFirst()
      .ifPresent(labelSymbol -> {
        labelSymbol.usages.add(node);
        node.labelSymbol = labelSymbol;
      });
  }

  private int firstTokenIndexAfter(ASTNode e) {
    int index = tokenManager.firstIndexAfter(e, ANY_TOKEN);
    while (tokenManager.get(index).isComment()) {
      index++;
    }
    return index;
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private int nextTokenIndex(int tokenIndex, int tokenType) {
    assert tokenType != ANY_TOKEN;
    do {
      tokenIndex += 1;
    } while (tokenManager.get(tokenIndex).tokenType != tokenType);
    return tokenIndex;
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken firstTokenBefore(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstIndexBefore(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken firstTokenAfter(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstIndexAfter(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken firstTokenIn(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.firstIndexIn(e, tokenType));
  }

  /**
   * @param tokenType {@link TerminalTokens}
   */
  private InternalSyntaxToken lastTokenIn(ASTNode e, int tokenType) {
    return createSyntaxToken(tokenManager.lastIndexIn(e, tokenType));
  }

  private InternalSyntaxToken createSyntaxToken(int tokenIndex) {
    Token t = tokenManager.get(tokenIndex);
    if (t.tokenType == TerminalTokens.TokenNameEOF) {
      if (t.originalStart == 0) {
        return new InternalSyntaxToken(1, 0, "", collectComments(tokenIndex), true);
      }
      final int position = t.originalStart - 1;
      final char c = tokenManager.getSource().charAt(position);
      int line = compilationUnit.getLineNumber(position);
      int column = compilationUnit.getColumnNumber(position);
      if (c == '\n' || c == '\r') {
        line++;
        column = 0;
      } else {
        column++;
      }
      return new InternalSyntaxToken(line, column, "", collectComments(tokenIndex), true);
    }
    return new InternalSyntaxToken(
      compilationUnit.getLineNumber(t.originalStart),
      compilationUnit.getColumnNumber(t.originalStart),
      t.toString(tokenManager.getSource()),
      collectComments(tokenIndex),
      false
    );
  }

  private InternalSyntaxToken createSpecialToken(int tokenIndex) {
    Token t = tokenManager.get(tokenIndex);
    List<SyntaxTrivia> comments = t.tokenType == TerminalTokens.TokenNameGREATER
      ? collectComments(tokenIndex)
      : Collections.emptyList();
    return new InternalSyntaxToken(
      compilationUnit.getLineNumber(t.originalEnd),
      compilationUnit.getColumnNumber(t.originalEnd),
      ">",
      comments,
      false
    );
  }

  private List<SyntaxTrivia> collectComments(int tokenIndex) {
    int commentIndex = tokenIndex;
    while (commentIndex > 0 && tokenManager.get(commentIndex - 1).isComment()) {
      commentIndex--;
    }
    List<SyntaxTrivia> comments = new ArrayList<>();
    for (int i = commentIndex; i < tokenIndex; i++) {
      Token t = tokenManager.get(i);
      comments.add(new InternalSyntaxTrivia(
        t.toString(tokenManager.getSource()),
        compilationUnit.getLineNumber(t.originalStart),
        compilationUnit.getColumnNumber(t.originalStart)
      ));
    }
    return comments;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addEmptyDeclarationsToList(int tokenIndex, List list) {
    while (true) {
      Token token;
      do {
        tokenIndex++;
        token = tokenManager.get(tokenIndex);
      } while (token.isComment());
      if (token.tokenType == TerminalTokens.TokenNameSEMICOLON) {
        list.add(
          new EmptyStatementTreeImpl(createSyntaxToken(tokenIndex))
        );
      } else {
        break;
      }
    }
  }

  private JavaTree.CompilationUnitTreeImpl convertCompilationUnit(CompilationUnit e) {
    PackageDeclarationTree packageDeclaration = null;
    if (e.getPackage() != null) {
      packageDeclaration = new JavaTree.PackageDeclarationTreeImpl(
        convertAnnotations(e.getPackage().annotations()),
        firstTokenIn(e.getPackage(), TerminalTokens.TokenNamepackage),
        convertExpression(e.getPackage().getName()),
        firstTokenIn(e.getPackage(), TerminalTokens.TokenNameSEMICOLON)
      );
    }

    List<ImportClauseTree> imports = new ArrayList<>();
    for (int i = 0; i < e.imports().size(); i++) {
      ImportDeclaration e2 = (ImportDeclaration) e.imports().get(i);
      ExpressionTree name = convertName(e2.getName());
      if (e2.isOnDemand()) {
        name = new MemberSelectExpressionTreeImpl(
          name,
          lastTokenIn(e2, TerminalTokens.TokenNameDOT),
          new IdentifierTreeImpl(lastTokenIn(e2, TerminalTokens.TokenNameMULTIPLY))
        );
      }
      JavaTree.ImportTreeImpl t = new JavaTree.ImportTreeImpl(
        firstTokenIn(e2, TerminalTokens.TokenNameimport),
        e2.isStatic() ? firstTokenIn(e2, TerminalTokens.TokenNamestatic) : null,
        name,
        lastTokenIn(e2, TerminalTokens.TokenNameSEMICOLON)
      );
      t.binding = e2.resolveBinding();
      imports.add(t);

      addEmptyDeclarationsToList(
        tokenManager.lastIndexIn(e2, TerminalTokens.TokenNameSEMICOLON),
        imports
      );
    }

    List<Tree> types = new ArrayList<>();
    for (Object type : e.types()) {
      processBodyDeclaration((AbstractTypeDeclaration) type, types);
    }

    if (e.imports().isEmpty() && e.types().isEmpty()) {
      addEmptyDeclarationsToList(-1, imports);
    }

    return new JavaTree.CompilationUnitTreeImpl(
      packageDeclaration,
      imports,
      types,
      convertModuleDeclaration(compilationUnit.getModule()),
      firstTokenAfter(e, TerminalTokens.TokenNameEOF)
    );
  }

  @Nullable
  private ModuleDeclarationTree convertModuleDeclaration(@Nullable ModuleDeclaration e) {
    if (e == null) {
      return null;
    }
    List<ModuleDirectiveTree> moduleDirectives = new ArrayList<>();
    for (Object o : e.moduleStatements()) {
      moduleDirectives.add(
        convertModuleDirective((ModuleDirective) o)
      );
    }
    return new ModuleDeclarationTreeImpl(
      convertAnnotations(e.annotations()),
      e.isOpen() ? firstTokenIn(e, TerminalTokens.TokenNameopen) : null,
      firstTokenBefore(e.getName(), TerminalTokens.TokenNamemodule),
      convertModuleName(e.getName()),
      firstTokenAfter(e.getName(), TerminalTokens.TokenNameLBRACE),
      moduleDirectives,
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    );
  }

  private ModuleNameTreeImpl convertModuleName(Name node) {
    switch (node.getNodeType()) {
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        ModuleNameTreeImpl t = convertModuleName(e.getQualifier());
        t.add(
          new IdentifierTreeImpl(firstTokenIn(e.getName(), TerminalTokens.TokenNameIdentifier))
        );
        return t;
      }
      case ASTNode.SIMPLE_NAME: {
        SimpleName e = (SimpleName) node;
        ModuleNameTreeImpl t = new ModuleNameTreeImpl(new ArrayList<>(), Collections.emptyList());
        t.add(
          new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNameIdentifier))
        );
        return t;
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private ModuleNameListTreeImpl convertModuleNames(List<?> list) {
    ModuleNameListTreeImpl t = new ModuleNameListTreeImpl(new ArrayList<>(), new ArrayList<>());
    for (int i = 0; i < list.size(); i++) {
      Name o = (Name) list.get(i);
      if (i > 0) {
        t.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      t.add(convertModuleName(o));
    }
    return t;
  }

  private ModuleDirectiveTree convertModuleDirective(ModuleDirective node) {
    switch (node.getNodeType()) {
      case ASTNode.REQUIRES_DIRECTIVE: {
        RequiresDirective e = (RequiresDirective) node;
        List<ModifierTree> modifiers = new ArrayList<>();
        for (Object o : e.modifiers()) {
          switch (((ModuleModifier) o).getKeyword().toString()) {
            case "static":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
            case "transitive":
              modifiers.add(new ModifierKeywordTreeImpl(Modifier.TRANSITIVE, firstTokenIn((ASTNode) o, ANY_TOKEN)));
              break;
            default:
              throw new IllegalStateException();
          }
        }
        return new RequiresDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamerequires),
          new ModifiersTreeImpl(modifiers),
          convertModuleName(e.getName()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.EXPORTS_DIRECTIVE: {
        ExportsDirective e = (ExportsDirective) node;
        return new ExportsDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameexports),
          convertExpression(e.getName()),
          e.modules().isEmpty() ? null : firstTokenAfter(e.getName(), TerminalTokens.TokenNameto),
          convertModuleNames(e.modules()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.OPENS_DIRECTIVE: {
        OpensDirective e = (OpensDirective) node;
        return new OpensDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameopens),
          convertExpression(e.getName()),
          e.modules().isEmpty() ? null : firstTokenAfter(e.getName(), TerminalTokens.TokenNameto),
          convertModuleNames(e.modules()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.USES_DIRECTIVE: {
        UsesDirective e = (UsesDirective) node;
        return new UsesDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameuses),
          (TypeTree) convertExpression(e.getName()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.PROVIDES_DIRECTIVE: {
        ProvidesDirective e = (ProvidesDirective) node;
        QualifiedIdentifierListTreeImpl typeNames = new QualifiedIdentifierListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.implementations().size(); i++) {
          Name o = (Name) e.implementations().get(i);
          if (i > 0) {
            typeNames.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          typeNames.add((TypeTree) convertExpression(o));
        }
        return new ProvidesDirectiveTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameprovides),
          (TypeTree) convertExpression(e.getName()),
          firstTokenAfter(e.getName(), TerminalTokens.TokenNamewith),
          typeNames,
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private ClassTreeImpl convertTypeDeclaration(AbstractTypeDeclaration e) {
    List<Tree> members = new ArrayList<>();
    if (e.getNodeType() == ASTNode.ENUM_DECLARATION) {
      for (Object o : ((EnumDeclaration) e).enumConstants()) {
        members.add(processEnumConstantDeclaration((EnumConstantDeclaration) o));
      }
    }

    // TODO try to simplify, note that type annotations can contain LBRACE
    final int leftBraceTokenIndex;
    if (e.getNodeType() == ASTNode.ENUM_DECLARATION) {
      EnumDeclaration enumDeclaration = (EnumDeclaration) e;
      if (!enumDeclaration.enumConstants().isEmpty()) {
        leftBraceTokenIndex = tokenManager.firstIndexBefore((ASTNode) enumDeclaration.enumConstants().get(0), TerminalTokens.TokenNameLBRACE);
      } else if (!enumDeclaration.bodyDeclarations().isEmpty()) {
        leftBraceTokenIndex = tokenManager.firstIndexBefore((ASTNode) e.bodyDeclarations().get(0), TerminalTokens.TokenNameLBRACE);
      } else {
        leftBraceTokenIndex = tokenManager.lastIndexIn(e, TerminalTokens.TokenNameLBRACE);
      }
    } else if (!e.bodyDeclarations().isEmpty()) {
      leftBraceTokenIndex = tokenManager.firstIndexBefore((ASTNode) e.bodyDeclarations().get(0), TerminalTokens.TokenNameLBRACE);
    } else {
      leftBraceTokenIndex = tokenManager.lastIndexIn(e, TerminalTokens.TokenNameLBRACE);
    }
    addEmptyDeclarationsToList(leftBraceTokenIndex, members);
    for (Object o : e.bodyDeclarations()) {
      processBodyDeclaration((BodyDeclaration) o, members);
    }

    Tree.Kind kind;
    switch (e.getNodeType()) {
      case ASTNode.ENUM_DECLARATION:
        kind = Tree.Kind.ENUM;
        break;
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
        kind = Tree.Kind.ANNOTATION_TYPE;
        break;
      case ASTNode.TYPE_DECLARATION:
        kind = ((TypeDeclaration) e).isInterface() ? Tree.Kind.INTERFACE : Tree.Kind.CLASS;
        break;
      case ASTNode.RECORD_DECLARATION:
        kind = Tree.Kind.RECORD;
        break;
      default:
        throw new IllegalStateException();
    }
    ClassTreeImpl t = new ClassTreeImpl(
      kind,
      createSyntaxToken(leftBraceTokenIndex),
      members,
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    ).completeModifiers(
      convertModifiers(e.modifiers())
    );
    switch (kind) {
      case ENUM:
        t.completeDeclarationKeyword(firstTokenBefore(e.getName(), TerminalTokens.TokenNameenum));
        t.completeIdentifier(convertSimpleName(e.getName()));
        break;
      case CLASS:
        t.completeDeclarationKeyword(firstTokenBefore(e.getName(), TerminalTokens.TokenNameclass));
        t.completeIdentifier(convertSimpleName(e.getName()));
        break;
      case INTERFACE:
        t.completeDeclarationKeyword(firstTokenBefore(e.getName(), TerminalTokens.TokenNameinterface));
        t.completeIdentifier(convertSimpleName(e.getName()));
        break;
      case RECORD:
        t.completeDeclarationKeyword(firstTokenBefore(e.getName(), TerminalTokens.TokenNameIdentifier));
        t.completeIdentifier(convertSimpleName(e.getName()));
        break;
      case ANNOTATION_TYPE:
        t.complete(
          firstTokenBefore(e.getName(), TerminalTokens.TokenNameAT),
          firstTokenBefore(e.getName(), TerminalTokens.TokenNameinterface),
          convertSimpleName(e.getName())
        );
        break;
      default:
        break;
    }

    if (kind == Tree.Kind.CLASS || kind == Tree.Kind.INTERFACE) {
      TypeDeclaration ee = (TypeDeclaration) e;
      t.completeTypeParameters(
        convertTypeParameters(ee.typeParameters())
      );

      if (e.getAST().isPreviewEnabled()) {
        List<?> permittedTypesToConvert = ((TypeDeclaration) e).permittedTypes();
        for (int i = 0; i < permittedTypesToConvert.size(); i++) {
          Type o = (Type) permittedTypesToConvert.get(i);
          if (i > 0) {
            t.permittedTypes().separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          t.permittedTypes().add(convertType(o));
        }
      }
    } else if (kind == Tree.Kind.RECORD) {
      RecordDeclaration ee = (RecordDeclaration) e;
      t.completeTypeParameters(
        convertTypeParameters(ee.typeParameters())
      );

      List<VariableTree> recordComponents = new ArrayList<>();
      for (int i = 0; i < ee.recordComponents().size(); i++) {
        SingleVariableDeclaration o = (SingleVariableDeclaration) ee.recordComponents().get(i);
        VariableTreeImpl recordComponent = createVariable(o);
        if (i < ee.recordComponents().size() - 1) {
          recordComponent.setEndToken(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
        }
        recordComponents.add(recordComponent);
      }
      t.completeRecordComponents(recordComponents);
    }

    switch (kind) {
      case CLASS: {
        TypeDeclaration ee = (TypeDeclaration) e;
        Type superclassType = ee.getSuperclassType();
        if (superclassType != null) {
          t.completeSuperclass(
            firstTokenBefore(superclassType, TerminalTokens.TokenNameextends),
            convertType(superclassType)
          );
        }
        // fall through
      }
      case INTERFACE:
      case RECORD:
      case ENUM: {
        List<?> superInterfaceTypes = superInterfaceTypes(e);
        if (!superInterfaceTypes.isEmpty()) {
          QualifiedIdentifierListTreeImpl superInterfaces = new QualifiedIdentifierListTreeImpl(
            new ArrayList<>(),
            new ArrayList<>()
          );
          for (int i = 0; i < superInterfaceTypes.size(); i++) {
            Type o = (Type) superInterfaceTypes.get(i);
            if (i > 0) {
              superInterfaces.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
            }
            superInterfaces.add(convertType(o));
          }
          t.completeInterfaces(
            firstTokenBefore((ASTNode) superInterfaceTypes.get(0), kind == Tree.Kind.INTERFACE ? TerminalTokens.TokenNameextends : TerminalTokens.TokenNameimplements),
            superInterfaces
          );
        }
        break;
      }
      default:
        break;
    }

    t.typeBinding = e.resolveBinding();
    declaration(t.typeBinding, t);

    return t;
  }

  private static List<?> superInterfaceTypes(AbstractTypeDeclaration e) {
    switch (e.getNodeType()) {
      case ASTNode.TYPE_DECLARATION:
        return ((TypeDeclaration) e).superInterfaceTypes();
      case ASTNode.ENUM_DECLARATION:
        return ((EnumDeclaration) e).superInterfaceTypes();
      case ASTNode.RECORD_DECLARATION:
        return ((RecordDeclaration) e).superInterfaceTypes();
      default:
        throw new IllegalStateException();
    }
  }

  private EnumConstantTreeImpl processEnumConstantDeclaration(EnumConstantDeclaration e) {
    final int openParTokenIndex = firstTokenIndexAfter(e.getName());
    final InternalSyntaxToken openParToken;
    final InternalSyntaxToken closeParToken;
    if (tokenManager.get(openParTokenIndex).tokenType == TerminalTokens.TokenNameLPAREN) {
      openParToken = createSyntaxToken(openParTokenIndex);
      closeParToken = e.arguments().isEmpty()
        ? firstTokenAfter(e.getName(), TerminalTokens.TokenNameRPAREN)
        : firstTokenAfter((ASTNode) e.arguments().get(e.arguments().size() - 1), TerminalTokens.TokenNameRPAREN);
    } else {
      openParToken = null;
      closeParToken = null;
    }

    final ArgumentListTreeImpl arguments = convertArguments(openParToken, e.arguments(), closeParToken);
    ClassTreeImpl classBody = null;
    if (e.getAnonymousClassDeclaration() != null) {
      List<Tree> members = new ArrayList<>();
      for (Object o : e.getAnonymousClassDeclaration().bodyDeclarations()) {
        processBodyDeclaration((BodyDeclaration) o, members);
      }
      classBody = new ClassTreeImpl(
        Tree.Kind.CLASS,
        firstTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameLBRACE),
        members,
        lastTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameRBRACE)
      );
      classBody.typeBinding = e.getAnonymousClassDeclaration().resolveBinding();
      declaration(classBody.typeBinding, classBody);
    }

    final int separatorTokenIndex = firstTokenIndexAfter(e);
    final InternalSyntaxToken separatorToken;
    switch (tokenManager.get(separatorTokenIndex).tokenType) {
      case TerminalTokens.TokenNameCOMMA:
      case TerminalTokens.TokenNameSEMICOLON:
        separatorToken = createSyntaxToken(separatorTokenIndex);
        break;
      case TerminalTokens.TokenNameRBRACE:
        separatorToken = null;
        break;
      default:
        throw new IllegalStateException();
    }

    IdentifierTreeImpl identifier = convertSimpleName(e.getName());
    if (e.getAnonymousClassDeclaration() == null) {
      identifier.binding = excludeRecovery(e.resolveConstructorBinding(), arguments.size());
    } else {
      identifier.binding = findConstructorForAnonymousClass(e.getAST(), identifier.typeBinding, e.resolveConstructorBinding());
    }
    usage(identifier.binding, identifier);

    EnumConstantTreeImpl t = new EnumConstantTreeImpl(
      convertModifiers(e.modifiers()),
      identifier,
      new NewClassTreeImpl(
        arguments,
        classBody
      ).completeWithIdentifier(
        identifier
      ),
      separatorToken
    );
    t.variableBinding = e.resolveVariable();
    declaration(t.variableBinding, t);
    return t;
  }

  private void processBodyDeclaration(ASTNode node, List<Tree> members) {
    final int lastTokenIndex;

    switch (node.getNodeType()) {
      case ASTNode.ANNOTATION_TYPE_DECLARATION:
      case ASTNode.ENUM_DECLARATION:
      case ASTNode.RECORD_DECLARATION:
      case ASTNode.TYPE_DECLARATION: {
        members.add(convertTypeDeclaration((AbstractTypeDeclaration) node));
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameRBRACE);
        break;
      }
      case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
        AnnotationTypeMemberDeclaration e = (AnnotationTypeMemberDeclaration) node;
        MethodTreeImpl t = new MethodTreeImpl(
          new FormalParametersListTreeImpl(
            firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
            firstTokenAfter(e.getName(), TerminalTokens.TokenNameRPAREN)
          ),
          e.getDefault() == null ? null : firstTokenBefore(e.getDefault(), TerminalTokens.TokenNamedefault),
          e.getDefault() == null ? null : convertExpression(e.getDefault())
        ).complete(
          convertType(e.getType()),
          convertSimpleName(e.getName()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        ).completeWithModifiers(
          convertModifiers(e.modifiers())
        );
        t.methodBinding = e.resolveBinding();
        declaration(t.methodBinding, t);
        members.add(t);
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameSEMICOLON);
        break;
      }
      case ASTNode.INITIALIZER: {
        Initializer e = (Initializer) node;
        BlockTreeImpl blockTree = convertBlock(e.getBody());
        if (org.eclipse.jdt.core.dom.Modifier.isStatic(e.getModifiers())) {
          members.add(new StaticInitializerTreeImpl(
            firstTokenIn(e, TerminalTokens.TokenNamestatic),
            (InternalSyntaxToken) blockTree.openBraceToken(),
            blockTree.body(),
            (InternalSyntaxToken) blockTree.closeBraceToken()
          ));
        } else {
          members.add(new BlockTreeImpl(
            Tree.Kind.INITIALIZER,
            (InternalSyntaxToken) blockTree.openBraceToken(),
            blockTree.body(),
            (InternalSyntaxToken) blockTree.closeBraceToken()
          ));
        }
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameRBRACE);
        break;
      }
      case ASTNode.METHOD_DECLARATION: {
        MethodDeclaration e = (MethodDeclaration) node;

        final FormalParametersListTreeImpl parameters;
        if (e.getAST().isPreviewEnabled() && e.isCompactConstructor()) {
          parameters = new FormalParametersListTreeImpl(null, null);
        } else {
          parameters = new FormalParametersListTreeImpl(
            firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
            firstTokenAfter(
              e.parameters().isEmpty() ? e.getName() : (ASTNode) e.parameters().get(e.parameters().size() - 1),
              TerminalTokens.TokenNameRPAREN
            ));
        }

        for (int i = 0; i < e.parameters().size(); i++) {
          SingleVariableDeclaration o = (SingleVariableDeclaration) e.parameters().get(i);
          VariableTreeImpl parameter = createVariable(o);
          if (i < e.parameters().size() - 1) {
            parameter.setEndToken(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
          }
          parameters.add(parameter);
        }

        QualifiedIdentifierListTreeImpl thrownExceptionTypes = new QualifiedIdentifierListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.thrownExceptionTypes().size(); i++) {
          Type o = (Type) e.thrownExceptionTypes().get(i);
          if (i > 0) {
            thrownExceptionTypes.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          thrownExceptionTypes.add(convertType(o));
        }

        Block body = e.getBody();
        Type returnType = e.getReturnType2();
        MethodTreeImpl t = new MethodTreeImpl(
          returnType == null ? null : applyExtraDimensions(convertType(returnType), e.extraDimensions()),
          convertSimpleName(e.getName()),
          parameters,
          e.thrownExceptionTypes().isEmpty() ? null : firstTokenBefore((Type) e.thrownExceptionTypes().get(0), TerminalTokens.TokenNamethrows),
          thrownExceptionTypes,
          body == null ? null : convertBlock(body),
          body == null ? lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON) : null
        ).completeWithModifiers(
          convertModifiers(e.modifiers())
        ).completeWithTypeParameters(
          convertTypeParameters(e.typeParameters())
        );
        t.methodBinding = e.resolveBinding();
        declaration(t.methodBinding, t);

        members.add(t);
        lastTokenIndex = tokenManager.lastIndexIn(node, body == null ? TerminalTokens.TokenNameSEMICOLON : TerminalTokens.TokenNameRBRACE);
        break;
      }
      case ASTNode.FIELD_DECLARATION: {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
        ModifiersTreeImpl modifiers = convertModifiers(fieldDeclaration.modifiers());
        TypeTree type = convertType(fieldDeclaration.getType());

        for (int i = 0; i < fieldDeclaration.fragments().size(); i++) {
          VariableDeclarationFragment fragment = (VariableDeclarationFragment) fieldDeclaration.fragments().get(i);
          VariableTreeImpl t = new VariableTreeImpl(
            convertSimpleName(fragment.getName())
          ).completeModifiersAndType(
            modifiers,
            applyExtraDimensions(type, fragment.extraDimensions())
          );
          if (fragment.getInitializer() != null) {
            t.completeTypeAndInitializer(
              t.type(),
              firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL),
              convertExpression(fragment.getInitializer())
            );
          }

          t.setEndToken(
            firstTokenAfter(fragment, i + 1 < fieldDeclaration.fragments().size() ? TerminalTokens.TokenNameCOMMA : TerminalTokens.TokenNameSEMICOLON)
          );
          t.variableBinding = fragment.resolveBinding();
          declaration(t.variableBinding, t);

          members.add(t);
        }
        lastTokenIndex = tokenManager.lastIndexIn(node, TerminalTokens.TokenNameSEMICOLON);
        break;
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }

    addEmptyDeclarationsToList(lastTokenIndex, members);
  }

  private ArgumentListTreeImpl convertArguments(
    InternalSyntaxToken openParen,
    List<?> list,
    InternalSyntaxToken closeParen
  ) {
    ArgumentListTreeImpl arguments = new ArgumentListTreeImpl(new ArrayList<>(), new ArrayList<>()).complete(openParen, closeParen);
    for (int i = 0; i < list.size(); i++) {
      Expression o = (Expression) list.get(i);
      arguments.add(convertExpression(o));
      if (i < list.size() - 1) {
        arguments.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
      }
    }
    return arguments;
  }

  @Nullable
  private TypeArgumentListTreeImpl convertTypeArguments(List<?> list) {
    if (list.isEmpty()) {
      return null;
    }
    ASTNode last = (ASTNode) list.get(list.size() - 1);
    int tokenIndex = tokenManager.firstIndexAfter(last, ANY_TOKEN);
    while (tokenManager.get(tokenIndex).isComment()) {
      tokenIndex++;
    }
    return convertTypeArguments(
      firstTokenBefore((ASTNode) list.get(0), TerminalTokens.TokenNameLESS),
      list,
      // TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT vs TerminalTokens.TokenNameGREATER
      createSpecialToken(tokenIndex)
    );
  }

  private TypeArgumentListTreeImpl convertTypeArguments(
    InternalSyntaxToken l,
    List<?> list,
    InternalSyntaxToken g
  ) {
    TypeArgumentListTreeImpl typeArguments = new TypeArgumentListTreeImpl(l, new ArrayList<>(), new ArrayList<>(), g);
    for (int i = 0; i < list.size(); i++) {
      Type o = (Type) list.get(i);
      if (i > 0) {
        typeArguments.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      typeArguments.add(convertType(o));
    }
    return typeArguments;
  }

  private TypeParameterListTreeImpl convertTypeParameters(List<?> list) {
    if (list.isEmpty()) {
      return new TypeParameterListTreeImpl();
    }
    ASTNode last = (ASTNode) list.get(list.size() - 1);
    int tokenIndex = tokenManager.firstIndexAfter(last, ANY_TOKEN);
    while (tokenManager.get(tokenIndex).isComment()) {
      tokenIndex++;
    }
    TypeParameterListTreeImpl t = new TypeParameterListTreeImpl(
      firstTokenBefore((ASTNode) list.get(0), TerminalTokens.TokenNameLESS),
      new ArrayList<>(), new ArrayList<>(),
      // TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT vs TerminalTokens.TokenNameGREATER
      createSpecialToken(tokenIndex)
    );
    for (int i = 0; i < list.size(); i++) {
      TypeParameter o = (TypeParameter) list.get(i);
      if (i > 0) {
        t.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
      }
      t.add(convertTypeParameter(o));
    }
    return t;
  }

  private TypeParameterTree convertTypeParameter(TypeParameter e) {
    IdentifierTreeImpl i = convertSimpleName(e.getName());
    // TODO why ECJ uses IExtendedModifier here instead of Annotation ?
    i.complete(convertAnnotations(e.modifiers()));
    TypeParameterTreeImpl t;
    List<?> typeBounds = e.typeBounds();
    if (typeBounds.isEmpty()) {
      t = new TypeParameterTreeImpl(i);
    } else {
      BoundListTreeImpl bounds = new BoundListTreeImpl(new ArrayList<>(), new ArrayList<>());
      for (int j = 0; j < typeBounds.size(); j++) {
        Object o = typeBounds.get(j);
        bounds.add(convertType((Type) o));
        if (j < typeBounds.size() - 1) {
          bounds.separators().add(firstTokenAfter((ASTNode) o, TerminalTokens.TokenNameAND));
        }
      }
      t = new TypeParameterTreeImpl(
        firstTokenAfter(e.getName(), TerminalTokens.TokenNameextends),
        bounds
      ).complete(
        i
      );
    }
    t.typeBinding = e.resolveBinding();
    return t;
  }

  /**
   * @param extraDimensions list of {@link org.eclipse.jdt.core.dom.Dimension}
   */
  private TypeTree applyExtraDimensions(TypeTree type, List<?> extraDimensions) {
    ITypeBinding typeBinding = ((AbstractTypedTree) type).typeBinding;
    for (int i = 0; i < extraDimensions.size(); i++) {
      Dimension e = (Dimension) extraDimensions.get(i);
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.annotations()),
        firstTokenIn(e, TerminalTokens.TokenNameLBRACKET),
        firstTokenIn(e, TerminalTokens.TokenNameRBRACKET)
      );
      if (typeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) type).typeBinding = typeBinding.createArrayType(i + 1);
      }
    }
    return type;
  }

  private VariableTreeImpl createVariable(SingleVariableDeclaration e) {
    // TODO are extraDimensions and varargs mutually exclusive?
    TypeTree type = convertType(e.getType());
    type = applyExtraDimensions(type, e.extraDimensions());
    if (e.isVarargs()) {
      ITypeBinding typeBinding = ((AbstractTypedTree) type).typeBinding;
      type = new JavaTree.ArrayTypeTreeImpl(
        type,
        (List) convertAnnotations(e.varargsAnnotations()),
        firstTokenAfter(e.getType(), TerminalTokens.TokenNameELLIPSIS)
      );
      if (typeBinding != null) {
        ((JavaTree.ArrayTypeTreeImpl) type).typeBinding = typeBinding.createArrayType(1);
      }
    }

    VariableTreeImpl t = new VariableTreeImpl(
      convertModifiers(e.modifiers()),
      type,
      convertSimpleName(e.getName())
    );
    if (e.getInitializer() != null) {
      t.completeTypeAndInitializer(
        t.type(),
        firstTokenAfter(e.getName(), TerminalTokens.TokenNameEQUAL),
        convertExpression(e.getInitializer())
      );
    }
    t.variableBinding = e.resolveBinding();
    declaration(t.variableBinding, t);
    return t;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addVariableToList(VariableDeclarationExpression e2, List list) {
    ModifiersTreeImpl modifiers = convertModifiers(e2.modifiers());
    TypeTree type = convertType(e2.getType());

    for (int i = 0; i < e2.fragments().size(); i++) {
      VariableDeclarationFragment fragment = (VariableDeclarationFragment) e2.fragments().get(i);
      VariableTreeImpl t = new VariableTreeImpl(convertSimpleName(fragment.getName()));
      t.completeModifiers(modifiers);
      if (fragment.getInitializer() == null) {
        t.completeType(type);
      } else {
        t.completeTypeAndInitializer(
          type,
          firstTokenBefore(fragment.getInitializer(), TerminalTokens.TokenNameEQUAL),
          convertExpression(fragment.getInitializer())
        );
      }
      if (i < e2.fragments().size() - 1) {
        t.setEndToken(firstTokenAfter(fragment, TerminalTokens.TokenNameCOMMA));
      }
      t.variableBinding = fragment.resolveBinding();
      declaration(t.variableBinding, t);
      list.add(t);
    }
  }

  private VarTypeTreeImpl convertVarType(SimpleType simpleType) {
    VarTypeTreeImpl varTree = new VarTypeTreeImpl(firstTokenIn(simpleType.getName(), TerminalTokens.TokenNameIdentifier));
    varTree.typeBinding = simpleType.resolveBinding();
    return varTree;
  }

  private IdentifierTreeImpl convertSimpleName(SimpleName e) {
    IdentifierTreeImpl t = new IdentifierTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameIdentifier)
    );
    t.typeBinding = e.resolveTypeBinding();
    t.binding = e.resolveBinding();
    return t;
  }

  private ExpressionTree convertName(Name node) {
    switch (node.getNodeType()) {
      case ASTNode.SIMPLE_NAME: {
        SimpleName e = (SimpleName) node;
        return new IdentifierTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameIdentifier)
        );
      }
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        IdentifierTreeImpl rhs = (IdentifierTreeImpl) convertName(e.getName());
        return new MemberSelectExpressionTreeImpl(
          convertName(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          rhs
        );
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private BlockTreeImpl convertBlock(Block e) {
    List<StatementTree> statements = new ArrayList<>();
    for (Object o : e.statements()) {
      addStatementToList((Statement) o, statements);
    }
    return new BlockTreeImpl(
      firstTokenIn(e, TerminalTokens.TokenNameLBRACE),
      statements,
      lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
    );
  }

  private void addStatementToList(Statement node, List<StatementTree> statements) {
    if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
      VariableDeclarationStatement e = (VariableDeclarationStatement) node;
      TypeTree tType = convertType(e.getType());
      ModifiersTreeImpl modifiers = convertModifiers(e.modifiers());
      for (int i = 0; i < e.fragments().size(); i++) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) e.fragments().get(i);
        VariableTreeImpl t = new VariableTreeImpl(
          convertSimpleName(fragment.getName())
        ).completeType(
          applyExtraDimensions(tType, fragment.extraDimensions())
        ).completeModifiers(
          modifiers
        );
        Expression initalizer = fragment.getInitializer();
        if (initalizer != null) {
          t.completeTypeAndInitializer(
            t.type(),
            firstTokenAfter(fragment.getName(), TerminalTokens.TokenNameEQUAL),
            convertExpression(initalizer)
          );
        }
        t.setEndToken(
          firstTokenAfter(fragment, i < e.fragments().size() - 1 ? TerminalTokens.TokenNameCOMMA : TerminalTokens.TokenNameSEMICOLON)
        );
        t.variableBinding = fragment.resolveBinding();
        declaration(t.variableBinding, t);
        statements.add(t);
      }
    } else if (node.getNodeType() == ASTNode.BREAK_STATEMENT && node.getLength() < "break".length()) {
      // skip implicit break-statement
    } else {
      statements.add(convertStatement(node));
    }
  }

  private StatementTree convertStatement(Statement node) {
    switch (node.getNodeType()) {
      case ASTNode.BLOCK:
        return convertBlock((Block) node);
      case ASTNode.EMPTY_STATEMENT: {
        EmptyStatement e = (EmptyStatement) node;
        return new EmptyStatementTreeImpl(
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.RETURN_STATEMENT: {
        ReturnStatement e = (ReturnStatement) node;
        Expression expression = e.getExpression();
        return new ReturnStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamereturn),
          expression == null ? null : convertExpression(expression),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.FOR_STATEMENT: {
        ForStatement e = (ForStatement) node;

        StatementExpressionListTreeImpl forInitStatement = new StatementExpressionListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.initializers().size(); i++) {
          Expression o = (Expression) e.initializers().get(i);
          if (i > 0) {
            forInitStatement.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
            addVariableToList(
              (VariableDeclarationExpression) o,
              forInitStatement
            );
          } else {
            forInitStatement.add(new ExpressionStatementTreeImpl(
              convertExpression(o),
              null
            ));
          }
        }

        StatementExpressionListTreeImpl forUpdateStatement = new StatementExpressionListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.updaters().size(); i++) {
          Expression o = (Expression) e.updaters().get(i);
          if (i > 0) {
            forUpdateStatement.separators().add(firstTokenBefore(o, TerminalTokens.TokenNameCOMMA));
          }
          forUpdateStatement.add(new ExpressionStatementTreeImpl(
            convertExpression(o),
            null
          ));
        }

        final int firstSemicolonTokenIndex = e.initializers().isEmpty()
          ? tokenManager.firstIndexIn(e, TerminalTokens.TokenNameSEMICOLON)
          : tokenManager.firstIndexAfter((ASTNode) e.initializers().get(e.initializers().size() - 1), TerminalTokens.TokenNameSEMICOLON);
        Expression expression = e.getExpression();
        final int secondSemicolonTokenIndex = expression == null
          ? nextTokenIndex(firstSemicolonTokenIndex, TerminalTokens.TokenNameSEMICOLON)
          : tokenManager.firstIndexAfter(expression, TerminalTokens.TokenNameSEMICOLON);

        return new ForStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamefor),
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          forInitStatement,
          createSyntaxToken(firstSemicolonTokenIndex),
          expression == null ? null : convertExpression(expression),
          createSyntaxToken(secondSemicolonTokenIndex),
          forUpdateStatement,
          firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN),
          convertStatement(e.getBody())
        );
      }
      case ASTNode.WHILE_STATEMENT: {
        WhileStatement e = (WhileStatement) node;
        return new WhileStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamewhile),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          convertStatement(e.getBody())
        );
      }
      case ASTNode.IF_STATEMENT: {
        IfStatement e = (IfStatement) node;
        Expression expression = e.getExpression();
        Statement elseStatement = e.getElseStatement();
        if (elseStatement != null) {
          ExpressionTree condition = convertExpression(expression);
          StatementTree thenStatement = convertStatement(e.getThenStatement());
          return new IfStatementTreeImpl(
            firstTokenAfter(e.getThenStatement(), TerminalTokens.TokenNameelse),
            convertStatement(elseStatement)
          ).complete(
            firstTokenIn(e, TerminalTokens.TokenNameif),
            firstTokenBefore(expression, TerminalTokens.TokenNameLPAREN),
            condition,
            firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
            thenStatement
          );
        } else {
          return new IfStatementTreeImpl(
            firstTokenIn(e, TerminalTokens.TokenNameif),
            firstTokenBefore(expression, TerminalTokens.TokenNameLPAREN),
            convertExpression(expression),
            firstTokenAfter(expression, TerminalTokens.TokenNameRPAREN),
            convertStatement(e.getThenStatement())
          );
        }
      }
      case ASTNode.BREAK_STATEMENT: {
        BreakStatement e = (BreakStatement) node;
        IdentifierTreeImpl identifier = e.getLabel() == null ? null : convertSimpleName(e.getLabel());
        usageLabel(identifier);
        return new BreakStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamebreak),
          identifier,
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.DO_STATEMENT: {
        DoStatement e = (DoStatement) node;
        return new DoWhileStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamedo),
          convertStatement(e.getBody()),
          firstTokenAfter(e.getBody(), TerminalTokens.TokenNamewhile),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.ASSERT_STATEMENT: {
        AssertStatement e = (AssertStatement) node;
        if (e.getMessage() != null) {
          return new AssertStatementTreeImpl(
            firstTokenBefore(e.getMessage(), TerminalTokens.TokenNameCOLON),
            convertExpression(e.getMessage())
          ).complete(
            firstTokenIn(e, TerminalTokens.TokenNameassert),
            convertExpression(e.getExpression()),
            lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
          );
        } else {
          return new AssertStatementTreeImpl(
            firstTokenIn(e, TerminalTokens.TokenNameassert),
            convertExpression(e.getExpression()),
            lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
          );
        }
      }
      case ASTNode.SWITCH_STATEMENT: {
        SwitchStatement e = (SwitchStatement) node;
        return new SwitchStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameswitch),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameLBRACE),
          convertSwitchStatements(e.statements()),
          lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
        );
      }
      case ASTNode.SYNCHRONIZED_STATEMENT: {
        SynchronizedStatement e = (SynchronizedStatement) node;
        return new SynchronizedStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamesynchronized),
          firstTokenBefore(e.getExpression(), TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          convertBlock(e.getBody())
        );
      }
      case ASTNode.EXPRESSION_STATEMENT: {
        ExpressionStatement e = (ExpressionStatement) node;
        return new ExpressionStatementTreeImpl(
          convertExpression(e.getExpression()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.CONTINUE_STATEMENT: {
        ContinueStatement e = (ContinueStatement) node;
        SimpleName label = e.getLabel();
        IdentifierTreeImpl i = label == null ? null : convertSimpleName(label);
        usageLabel(i);
        return new ContinueStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamecontinue),
          i,
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.LABELED_STATEMENT: {
        LabeledStatement e = (LabeledStatement) node;
        IdentifierTreeImpl i = convertSimpleName(e.getLabel());

        JLabelSymbol symbol = new JLabelSymbol(i.name());
        labels.push(symbol);

        LabeledStatementTreeImpl t = new LabeledStatementTreeImpl(
          i,
          firstTokenAfter(e.getLabel(), TerminalTokens.TokenNameCOLON),
          convertStatement(e.getBody())
        );

        labels.pop();
        symbol.declaration = t;
        t.labelSymbol = symbol;
        return t;
      }
      case ASTNode.ENHANCED_FOR_STATEMENT: {
        EnhancedForStatement e = (EnhancedForStatement) node;
        return new ForEachStatementImpl(
          firstTokenIn(e, TerminalTokens.TokenNamefor),
          firstTokenBefore(e.getParameter(), TerminalTokens.TokenNameLPAREN),
          createVariable(e.getParameter()),
          firstTokenAfter(e.getParameter(), TerminalTokens.TokenNameCOLON),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          convertStatement(e.getBody())
        );
      }
      case ASTNode.THROW_STATEMENT: {
        ThrowStatement e = (ThrowStatement) node;
        return new ThrowStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNamethrow),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.TRY_STATEMENT: {
        TryStatement e = (TryStatement) node;

        List<CatchTree> catches = new ArrayList<>();
        for (Object o : e.catchClauses()) {
          CatchClause e2 = (CatchClause) o;
          catches.add(new CatchTreeImpl(
            firstTokenIn(e2, TerminalTokens.TokenNamecatch),
            firstTokenBefore(e2.getException(), TerminalTokens.TokenNameLPAREN),
            createVariable(e2.getException()),
            firstTokenAfter(e2.getException(), TerminalTokens.TokenNameRPAREN),
            convertBlock(e2.getBody())
          ));
        }

        ResourceListTreeImpl resources = new ResourceListTreeImpl(
          new ArrayList<>(), new ArrayList<>()
        );
        for (int i = 0; i < e.resources().size(); i++) {
          Expression o = (Expression) e.resources().get(i);
          if (ASTNode.VARIABLE_DECLARATION_EXPRESSION == o.getNodeType()) {
            addVariableToList(
              (VariableDeclarationExpression) o,
              resources
            );
          } else {
            resources.add(convertExpression(o));
          }
          if (i < e.resources().size() - 1) {
            resources.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameSEMICOLON));
          } else {
            int tokenIndex = tokenManager.firstIndexBefore(e.getBody(), TerminalTokens.TokenNameRPAREN);
            while (true) {
              Token token;
              do {
                tokenIndex--;
                token = tokenManager.get(tokenIndex);
              } while (token.isComment());
              if (token.tokenType == TerminalTokens.TokenNameSEMICOLON) {
                resources.separators().add(
                  createSyntaxToken(tokenIndex)
                );
              } else {
                break;
              }
            }
          }
        }

        Block f = e.getFinally();
        return new TryStatementTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNametry),
          e.resources().isEmpty() ? null : firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          resources,
          e.resources().isEmpty() ? null : firstTokenBefore(e.getBody(), TerminalTokens.TokenNameRPAREN),
          convertBlock(e.getBody()),
          catches,
          f == null ? null : firstTokenBefore(f, TerminalTokens.TokenNamefinally),
          f == null ? null : convertBlock(f)
        );
      }
      case ASTNode.TYPE_DECLARATION_STATEMENT: {
        TypeDeclarationStatement e = (TypeDeclarationStatement) node;
        return convertTypeDeclaration(e.getDeclaration());
      }
      case ASTNode.CONSTRUCTOR_INVOCATION: {
        ConstructorInvocation e = (ConstructorInvocation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          e.arguments().isEmpty() ? lastTokenIn(e, TerminalTokens.TokenNameLPAREN) : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        IdentifierTreeImpl i = new IdentifierTreeImpl(e.arguments().isEmpty()
          ? lastTokenIn(e, TerminalTokens.TokenNamethis)
          : firstTokenBefore((ASTNode) e.arguments().get(0), TerminalTokens.TokenNamethis));
        MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
          i,
          convertTypeArguments(e.typeArguments()),
          arguments
        );
        t.methodBinding = e.resolveConstructorBinding();
        if (t.methodBinding != null) {
          t.typeBinding = t.methodBinding.getDeclaringClass();
          t.methodBinding = excludeRecovery(t.methodBinding, arguments.size());
        }
        i.binding = t.methodBinding;
        usage(i.binding, i);
        return new ExpressionStatementTreeImpl(
          t,
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.SUPER_CONSTRUCTOR_INVOCATION: {
        SuperConstructorInvocation e = (SuperConstructorInvocation) node;

        IdentifierTreeImpl i = new IdentifierTreeImpl(firstTokenIn(e, TerminalTokens.TokenNamesuper));
        ExpressionTree methodSelect = i;
        if (e.getExpression() != null) {
          methodSelect = new MemberSelectExpressionTreeImpl(
            convertExpression(e.getExpression()),
            firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
            i
          );
        }

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
          methodSelect,
          convertTypeArguments(e.typeArguments()),
          arguments
        );
        t.methodBinding = e.resolveConstructorBinding();
        if (t.methodBinding != null) {
          t.typeBinding = t.methodBinding.getDeclaringClass();
          t.methodBinding = excludeRecovery(t.methodBinding, arguments.size());
        }
        i.binding = t.methodBinding;
        usage(i.binding, i);
        return new ExpressionStatementTreeImpl(
          t,
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      case ASTNode.YIELD_STATEMENT: {
        YieldStatement e = (YieldStatement) node;
        return new YieldStatementTreeImpl(
          e.isImplicit() ? null : firstTokenIn(e, ANY_TOKEN),
          convertExpression(e.getExpression()),
          lastTokenIn(e, TerminalTokens.TokenNameSEMICOLON)
        );
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private List<CaseGroupTreeImpl> convertSwitchStatements(List<?> list) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> caselabels = null;
    BlockStatementListTreeImpl body = null;
    for (Object o : list) {
      if (o instanceof SwitchCase) {
        if (caselabels == null) {
          caselabels = new ArrayList<>();
          body = new BlockStatementListTreeImpl(new ArrayList<>());
        }

        SwitchCase c = (SwitchCase) o;

        List<ExpressionTree> expressions = new ArrayList<>();
        for (Object oo : c.expressions()) {
          expressions.add(
            convertExpression((Expression) oo)
          );
        }

        caselabels.add(new CaseLabelTreeImpl(
          firstTokenIn(c, c.isDefault() ? TerminalTokens.TokenNamedefault : TerminalTokens.TokenNamecase),
          expressions,
          lastTokenIn(c, /* TerminalTokens.TokenNameCOLON or TerminalTokens.TokenNameARROW */ ANY_TOKEN)
        ));
      } else {
        if (caselabels != null) {
          groups.add(new CaseGroupTreeImpl(
            caselabels,
            body
          ));
        }
        caselabels = null;
        addStatementToList((Statement) o, body);
      }
    }
    if (caselabels != null) {
      groups.add(new CaseGroupTreeImpl(
        caselabels,
        body
      ));
    }
    return groups;
  }

  private ExpressionTree convertExpression(Expression node) {
    ExpressionTree t = createExpression(node);
    ((AbstractTypedTree) t).typeBinding = node.resolveTypeBinding();
    return t;
  }

  private ExpressionTree createExpression(Expression node) {
    switch (node.getNodeType()) {
      case ASTNode.SIMPLE_NAME: {
        SimpleName e = (SimpleName) node;
        IdentifierTreeImpl t = convertSimpleName(e);
        usage(t.binding, t);
        return t;
      }
      case ASTNode.QUALIFIED_NAME: {
        QualifiedName e = (QualifiedName) node;
        IdentifierTreeImpl rhs = convertSimpleName(e.getName());
        usage(rhs.binding, rhs);
        return new MemberSelectExpressionTreeImpl(
          convertExpression(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          rhs
        );
      }
      case ASTNode.FIELD_ACCESS: {
        FieldAccess e = (FieldAccess) node;
        IdentifierTreeImpl rhs = convertSimpleName(e.getName());
        usage(rhs.binding, rhs);
        return new MemberSelectExpressionTreeImpl(
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
          rhs
        );
      }
      case ASTNode.SUPER_FIELD_ACCESS: {
        SuperFieldAccess e = (SuperFieldAccess) node;
        IdentifierTreeImpl rhs = convertSimpleName(e.getName());
        usage(rhs.binding, rhs);
        if (e.getQualifier() == null) {
          // super.name
          return new MemberSelectExpressionTreeImpl(
            unqualifiedKeywordSuper(e),
            firstTokenIn(e, TerminalTokens.TokenNameDOT),
            rhs
          );
        } else {
          // qualifier.super.name
          AbstractTypedTree qualifier = (AbstractTypedTree) convertExpression(e.getQualifier());
          KeywordSuper keywordSuper = new KeywordSuper(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper), null);
          MemberSelectExpressionTreeImpl qualifiedSuper = new MemberSelectExpressionTreeImpl(
            (ExpressionTree) qualifier,
            firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
            keywordSuper
          );
          if (qualifier.typeBinding != null) {
            keywordSuper.typeBinding = qualifier.typeBinding;
            qualifiedSuper.typeBinding = keywordSuper.typeBinding.getSuperclass();
          }
          return new MemberSelectExpressionTreeImpl(
            qualifiedSuper,
            firstTokenBefore(e.getName(), TerminalTokens.TokenNameDOT),
            rhs
          );
        }
      }
      case ASTNode.THIS_EXPRESSION: {
        ThisExpression e = (ThisExpression) node;
        if (e.getQualifier() == null) {
          return new KeywordThis(
            firstTokenIn(e, TerminalTokens.TokenNamethis),
            null
          );
        } else {
          KeywordThis keywordThis = new KeywordThis(
            firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamethis),
            e.resolveTypeBinding()
          );
          return new MemberSelectExpressionTreeImpl(
            convertExpression(e.getQualifier()),
            firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
            keywordThis
          );
        }
      }
      case ASTNode.TYPE_LITERAL: {
        TypeLiteral e = (TypeLiteral) node;
        return new MemberSelectExpressionTreeImpl(
          (ExpressionTree) convertType(e.getType()),
          lastTokenIn(e, TerminalTokens.TokenNameDOT),
          new IdentifierTreeImpl(
            lastTokenIn(e, TerminalTokens.TokenNameclass)
          )
        );
      }
      case ASTNode.ARRAY_ACCESS: {
        ArrayAccess e = (ArrayAccess) node;
        return new ArrayAccessExpressionTreeImpl(
          new ArrayDimensionTreeImpl(
            firstTokenBefore(e.getIndex(), TerminalTokens.TokenNameLBRACKET),
            convertExpression(e.getIndex()),
            firstTokenAfter(e.getIndex(), TerminalTokens.TokenNameRBRACKET)
          )
        ).complete(
          convertExpression(e.getArray())
        );
      }
      case ASTNode.ARRAY_CREATION: {
        ArrayCreation e = (ArrayCreation) node;

        List<ArrayDimensionTree> dimensions = new ArrayList<>();
        for (Object o : e.dimensions()) {
          dimensions.add(new ArrayDimensionTreeImpl(
            firstTokenBefore((Expression) o, TerminalTokens.TokenNameLBRACKET),
            convertExpression((Expression) o),
            firstTokenAfter((Expression) o, TerminalTokens.TokenNameRBRACKET)
          ));
        }
        InitializerListTreeImpl initializers = new InitializerListTreeImpl(new ArrayList<>(), new ArrayList<>());
        if (e.getInitializer() != null) {
          assert dimensions.isEmpty();

          TypeTree type = convertType(e.getType());
          while (type.is(Tree.Kind.ARRAY_TYPE)) {
            ArrayTypeTree arrayType = (ArrayTypeTree) type;
            ArrayDimensionTreeImpl dimension = new ArrayDimensionTreeImpl(
              arrayType.openBracketToken(),
              null,
              arrayType.closeBracketToken()
            ).completeAnnotations(arrayType.annotations());
            dimensions.add(/* TODO suboptimal */ 0, dimension);
            type = arrayType.type();
          }

          return ((NewArrayTreeImpl) convertExpression(e.getInitializer()))
            .completeWithNewKeyword(firstTokenIn(e, TerminalTokens.TokenNamenew))
            .complete(type)
            .completeDimensions(dimensions);
        } else {
          TypeTree type = convertType(e.getType());
          int index = dimensions.size() - 1;
          while (type.is(Tree.Kind.ARRAY_TYPE)) {
            if (!type.annotations().isEmpty()) {
              ((ArrayDimensionTreeImpl) dimensions.get(index))
                .completeAnnotations(type.annotations());
            }
            index--;
            type = ((ArrayTypeTree) type).type();
          }

          return new NewArrayTreeImpl(
            dimensions,
            initializers
          ).complete(
            type
          ).completeWithNewKeyword(
            firstTokenIn(e, TerminalTokens.TokenNamenew)
          );
        }
      }
      case ASTNode.ARRAY_INITIALIZER: {
        ArrayInitializer e = (ArrayInitializer) node;

        InitializerListTreeImpl initializers = new InitializerListTreeImpl(new ArrayList<>(), new ArrayList<>());
        for (int i = 0; i < e.expressions().size(); i++) {
          Expression o = (Expression) e.expressions().get(i);
          initializers.add(convertExpression(o));
          final int commaTokenIndex = firstTokenIndexAfter(o);
          if (tokenManager.get(commaTokenIndex).tokenType == TerminalTokens.TokenNameCOMMA) {
            initializers.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameCOMMA));
          }
        }
        return new NewArrayTreeImpl(
          Collections.emptyList(),
          initializers
        ).completeWithCurlyBraces(
          firstTokenIn(e, TerminalTokens.TokenNameLBRACE),
          lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
        );
      }
      case ASTNode.ASSIGNMENT: {
        Assignment e = (Assignment) node;
        Op op = operators.get(e.getOperator());
        return new AssignmentExpressionTreeImpl(
          op.kind,
          convertExpression(e.getLeftHandSide()),
          firstTokenAfter(e.getLeftHandSide(), op.tokenType),
          convertExpression(e.getRightHandSide())
        );
      }
      case ASTNode.CAST_EXPRESSION: {
        CastExpression e = (CastExpression) node;
        if (e.getType().getNodeType() == ASTNode.INTERSECTION_TYPE) {
          IntersectionType intersectionType = (IntersectionType) e.getType();
          TypeTree type = convertType((Type) intersectionType.types().get(0));
          BoundListTreeImpl bounds = new BoundListTreeImpl(
            new ArrayList<>(), new ArrayList<>()
          );
          for (int i = 1; i < intersectionType.types().size(); i++) {
            Type o = (Type) intersectionType.types().get(i);
            bounds.add(
              convertType(o)
            );
            if (i < intersectionType.types().size() - 1) {
              bounds.separators().add(
                firstTokenAfter(o, TerminalTokens.TokenNameAND)
              );
            }
          }
          return new TypeCastExpressionTreeImpl(
            type,
            firstTokenAfter((Type) intersectionType.types().get(0), TerminalTokens.TokenNameAND),
            bounds,
            firstTokenAfter(e.getType(), TerminalTokens.TokenNameRPAREN),
            convertExpression(e.getExpression())
          ).complete(
            firstTokenBefore(e.getType(), TerminalTokens.TokenNameLPAREN)
          );
        } else {
          return new TypeCastExpressionTreeImpl(
            convertType(e.getType()),
            firstTokenAfter(e.getType(), TerminalTokens.TokenNameRPAREN),
            convertExpression(e.getExpression())
          ).complete(
            firstTokenIn(e, TerminalTokens.TokenNameLPAREN)
          );
        }
      }
      case ASTNode.CLASS_INSTANCE_CREATION: {
        ClassInstanceCreation e = (ClassInstanceCreation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenAfter(e.getType(), TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          firstTokenAfter(e.arguments().isEmpty() ? e.getType() : (ASTNode) e.arguments().get(e.arguments().size() - 1), TerminalTokens.TokenNameRPAREN)
        );

        ClassTreeImpl classBody = null;
        if (e.getAnonymousClassDeclaration() != null) {
          List<Tree> members = new ArrayList<>();
          for (Object o : e.getAnonymousClassDeclaration().bodyDeclarations()) {
            processBodyDeclaration((BodyDeclaration) o, members);
          }
          classBody = new ClassTreeImpl(
            Tree.Kind.CLASS,
            firstTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameLBRACE),
            members,
            lastTokenIn(e.getAnonymousClassDeclaration(), TerminalTokens.TokenNameRBRACE)
          );
          classBody.typeBinding = e.getAnonymousClassDeclaration().resolveBinding();
          declaration(classBody.typeBinding, classBody);
        }

        NewClassTreeImpl t = new NewClassTreeImpl(
          arguments,
          classBody
        ).completeWithNewKeyword(
          e.getExpression() == null ? firstTokenIn(e, TerminalTokens.TokenNamenew) : firstTokenAfter(e.getExpression(), TerminalTokens.TokenNamenew)
        ).completeWithIdentifier(
          convertType(e.getType())
        ).completeWithTypeArguments(
          convertTypeArguments(e.typeArguments())
        );
        if (e.getExpression() != null) {
          t.completeWithEnclosingExpression(convertExpression(e.getExpression()));
          t.completeWithDotToken(firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT));
        }

        IdentifierTreeImpl i = (IdentifierTreeImpl) t.getConstructorIdentifier();
        int nbArguments = arguments.size();
        if (e.getAnonymousClassDeclaration() == null) {
          i.binding = excludeRecovery(e.resolveConstructorBinding(), nbArguments);
        } else {
          i.binding = excludeRecovery(findConstructorForAnonymousClass(e.getAST(), i.typeBinding, e.resolveConstructorBinding()), nbArguments);
        }
        usage(i.binding, i);

        return t;
      }
      case ASTNode.CONDITIONAL_EXPRESSION: {
        ConditionalExpression e = (ConditionalExpression) node;
        return new ConditionalExpressionTreeImpl(
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameQUESTION),
          convertExpression(e.getThenExpression()),
          firstTokenAfter(e.getThenExpression(), TerminalTokens.TokenNameCOLON),
          convertExpression(e.getElseExpression())
        ).complete(
          convertExpression(e.getExpression())
        );
      }
      case ASTNode.INFIX_EXPRESSION: {
        InfixExpression e = (InfixExpression) node;
        Op op = operators.get(e.getOperator());
        BinaryExpressionTreeImpl t = new BinaryExpressionTreeImpl(
          op.kind,
          convertExpression(e.getLeftOperand()),
          firstTokenAfter(e.getLeftOperand(), op.tokenType),
          convertExpression(e.getRightOperand())
        );
        for (Object o : e.extendedOperands()) {
          Expression e2 = (Expression) o;
          t.typeBinding = e.resolveTypeBinding();
          t = new BinaryExpressionTreeImpl(
            op.kind,
            t,
            firstTokenBefore(e2, op.tokenType),
            convertExpression(e2)
          );
        }
        return t;
      }
      case ASTNode.METHOD_INVOCATION: {
        MethodInvocation e = (MethodInvocation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenAfter(e.getName(), TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        IdentifierTreeImpl rhs = convertSimpleName(e.getName());
        ExpressionTree memberSelect;
        if (e.getExpression() == null) {
          memberSelect = rhs;
        } else {
          memberSelect = new MemberSelectExpressionTreeImpl(
            convertExpression(e.getExpression()),
            firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameDOT),
            rhs
          );
        }
        MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
          memberSelect,
          convertTypeArguments(e.typeArguments()),
          arguments
        );
        t.methodBinding = excludeRecovery(e.resolveMethodBinding(), arguments.size());
        rhs.binding = t.methodBinding;
        usage(rhs.binding, rhs);
        return t;
      }
      case ASTNode.SUPER_METHOD_INVOCATION: {
        SuperMethodInvocation e = (SuperMethodInvocation) node;

        ArgumentListTreeImpl arguments = convertArguments(
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          e.arguments(),
          lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
        );

        IdentifierTreeImpl rhs = convertSimpleName(e.getName());

        ExpressionTree outermostSelect;
        if (e.getQualifier() == null) {
          outermostSelect = new MemberSelectExpressionTreeImpl(
            unqualifiedKeywordSuper(e),
            firstTokenIn(e, TerminalTokens.TokenNameDOT),
            rhs
          );
        } else {
          final int firstDotTokenIndex = tokenManager.firstIndexAfter(e.getQualifier(), TerminalTokens.TokenNameDOT);
          AbstractTypedTree qualifier = (AbstractTypedTree) convertExpression(e.getQualifier());
          KeywordSuper keywordSuper = new KeywordSuper(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper), null);
          MemberSelectExpressionTreeImpl qualifiedSuper = new MemberSelectExpressionTreeImpl(
            (ExpressionTree) qualifier,
            createSyntaxToken(firstDotTokenIndex),
            keywordSuper
          );
          if (qualifier.typeBinding != null) {
            keywordSuper.typeBinding = qualifier.typeBinding;
            qualifiedSuper.typeBinding = keywordSuper.typeBinding.getSuperclass();
          }
          outermostSelect = new MemberSelectExpressionTreeImpl(
            qualifiedSuper,
            createSyntaxToken(nextTokenIndex(firstDotTokenIndex, TerminalTokens.TokenNameDOT)),
            rhs
          );
        }

        MethodInvocationTreeImpl t = new MethodInvocationTreeImpl(
          outermostSelect,
          null,
          arguments
        );
        t.methodBinding = excludeRecovery(e.resolveMethodBinding(), arguments.size());
        rhs.binding = t.methodBinding;
        usage(rhs.binding, rhs);
        return t;
      }
      case ASTNode.PARENTHESIZED_EXPRESSION: {
        ParenthesizedExpression e = (ParenthesizedExpression) node;
        return new ParenthesizedTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN)
        );
      }
      case ASTNode.POSTFIX_EXPRESSION: {
        PostfixExpression e = (PostfixExpression) node;
        Op op = operators.get(e.getOperator());
        return new InternalPostfixUnaryExpression(
          op.kind,
          convertExpression(e.getOperand()),
          firstTokenAfter(e.getOperand(), op.tokenType)
        );
      }
      case ASTNode.PREFIX_EXPRESSION: {
        PrefixExpression e = (PrefixExpression) node;
        Op op = operators.get(e.getOperator());
        return new InternalPrefixUnaryExpression(
          op.kind,
          firstTokenIn(e, op.tokenType),
          convertExpression(e.getOperand())
        );
      }
      case ASTNode.INSTANCEOF_EXPRESSION: {
        InstanceofExpression e = (InstanceofExpression) node;
        return new InstanceOfTreeImpl(
          firstTokenAfter(e.getLeftOperand(), TerminalTokens.TokenNameinstanceof),
          convertType(e.getRightOperand()),
          e.getAST().isPreviewEnabled() && e.getPatternVariable() != null ? convertSimpleName(e.getPatternVariable()) : null
        ).complete(
          convertExpression(e.getLeftOperand())
        );
      }
      case ASTNode.LAMBDA_EXPRESSION: {
        LambdaExpression e = (LambdaExpression) node;
        List<VariableTree> parameters = new ArrayList<>();
        for (int i = 0; i < e.parameters().size(); i++) {
          VariableDeclaration o = (VariableDeclaration) e.parameters().get(i);
          VariableTreeImpl t;
          if (o.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
            t = new VariableTreeImpl(convertSimpleName(o.getName()));
            IVariableBinding variableBinding = o.resolveBinding();
            if (variableBinding != null) {
              t.variableBinding = variableBinding;
              ((InferedTypeTree) t.type()).typeBinding = variableBinding.getType();
              declaration(t.variableBinding, t);
            }
          } else {
            t = createVariable((SingleVariableDeclaration) o);
          }
          parameters.add(t);
          if (i < e.parameters().size() - 1) {
            t.setEndToken(
              firstTokenAfter(o, TerminalTokens.TokenNameCOMMA)
            );
          }
        }
        ASTNode body = e.getBody();
        return new LambdaExpressionTreeImpl(
          e.hasParentheses() ? firstTokenIn(e, TerminalTokens.TokenNameLPAREN) : null,
          parameters,
          e.hasParentheses() ? firstTokenBefore(body, TerminalTokens.TokenNameRPAREN) : null,
          firstTokenBefore(body, TerminalTokens.TokenNameARROW),
          body.getNodeType() == ASTNode.BLOCK ? convertBlock((Block) body) : convertExpression((Expression) body)
        );
      }
      case ASTNode.CREATION_REFERENCE: {
        CreationReference e = (CreationReference) node;
        MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
          convertType(e.getType()),
          lastTokenIn(e, TerminalTokens.TokenNameCOLON_COLON)
        );
        IdentifierTreeImpl i = new IdentifierTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamenew));
        i.binding = e.resolveMethodBinding();
        usage(i.binding, i);
        t.complete(convertTypeArguments(e.typeArguments()), i);
        return t;
      }
      case ASTNode.EXPRESSION_METHOD_REFERENCE: {
        ExpressionMethodReference e = (ExpressionMethodReference) node;
        MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameCOLON_COLON)
        );
        IdentifierTreeImpl i = convertSimpleName(e.getName());
        usage(i.binding, i);
        t.complete(convertTypeArguments(e.typeArguments()), i);
        return t;
      }
      case ASTNode.TYPE_METHOD_REFERENCE: {
        TypeMethodReference e = (TypeMethodReference) node;
        MethodReferenceTreeImpl t = new MethodReferenceTreeImpl(
          convertType(e.getType()),
          firstTokenAfter(e.getType(), TerminalTokens.TokenNameCOLON_COLON)
        );
        IdentifierTreeImpl i = convertSimpleName(e.getName());
        usage(i.binding, i);
        t.complete(convertTypeArguments(e.typeArguments()), i);
        return t;
      }
      case ASTNode.SUPER_METHOD_REFERENCE: {
        SuperMethodReference e = (SuperMethodReference) node;
        MethodReferenceTreeImpl t;
        if (e.getQualifier() != null) {
          t = new MethodReferenceTreeImpl(
            new MemberSelectExpressionTreeImpl(
              convertExpression(e.getQualifier()),
              firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
              new IdentifierTreeImpl(firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNamesuper))
            ),
            firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameCOLON_COLON)
          );
        } else {
          t = new MethodReferenceTreeImpl(
            unqualifiedKeywordSuper(e),
            firstTokenIn(e, TerminalTokens.TokenNameCOLON_COLON)
          );
        }
        IdentifierTreeImpl i = convertSimpleName(e.getName());
        usage(i.binding, i);
        t.complete(convertTypeArguments(e.typeArguments()), i);
        return t;
      }
      case ASTNode.SWITCH_EXPRESSION: {
        SwitchExpression e = (SwitchExpression) node;
        return new SwitchExpressionTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameswitch),
          firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
          convertExpression(e.getExpression()),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameRPAREN),
          firstTokenAfter(e.getExpression(), TerminalTokens.TokenNameLBRACE),
          convertSwitchStatements(e.statements()),
          lastTokenIn(e, TerminalTokens.TokenNameRBRACE)
        );
      }
      case ASTNode.NULL_LITERAL: {
        NullLiteral e = (NullLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.NULL_LITERAL,
          firstTokenIn(e, TerminalTokens.TokenNamenull)
        );
      }
      case ASTNode.NUMBER_LITERAL: {
        NumberLiteral e = (NumberLiteral) node;
        int tokenIndex = tokenManager.findIndex(e.getStartPosition(), ANY_TOKEN, true);
        int tokenType = tokenManager.get(tokenIndex).tokenType;
        boolean unaryMinus = tokenType == TerminalTokens.TokenNameMINUS;
        if (unaryMinus) {
          tokenIndex++;
          tokenType = tokenManager.get(tokenIndex).tokenType;
        }
        ExpressionTree result;
        switch (tokenType) {
          case TerminalTokens.TokenNameIntegerLiteral:
            result = new LiteralTreeImpl(Tree.Kind.INT_LITERAL, createSyntaxToken(tokenIndex));
            break;
          case TerminalTokens.TokenNameLongLiteral:
            result = new LiteralTreeImpl(Tree.Kind.LONG_LITERAL, createSyntaxToken(tokenIndex));
            break;
          case TerminalTokens.TokenNameFloatingPointLiteral:
            result = new LiteralTreeImpl(Tree.Kind.FLOAT_LITERAL, createSyntaxToken(tokenIndex));
            break;
          case TerminalTokens.TokenNameDoubleLiteral:
            result = new LiteralTreeImpl(Tree.Kind.DOUBLE_LITERAL, createSyntaxToken(tokenIndex));
            break;
          default:
            throw new IllegalStateException();
        }
        ((LiteralTreeImpl) result).typeBinding = e.resolveTypeBinding();
        if (unaryMinus) {
          result = new InternalPrefixUnaryExpression(Tree.Kind.UNARY_MINUS, createSyntaxToken(tokenIndex - 1), result);
        }
        return result;
      }
      case ASTNode.CHARACTER_LITERAL: {
        CharacterLiteral e = (CharacterLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.CHAR_LITERAL,
          firstTokenIn(e, TerminalTokens.TokenNameCharacterLiteral)
        );
      }
      case ASTNode.BOOLEAN_LITERAL: {
        BooleanLiteral e = (BooleanLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.BOOLEAN_LITERAL,
          firstTokenIn(e, e.booleanValue() ? TerminalTokens.TokenNametrue : TerminalTokens.TokenNamefalse)
        );
      }
      case ASTNode.STRING_LITERAL: {
        StringLiteral e = (StringLiteral) node;
        return new LiteralTreeImpl(
          Tree.Kind.STRING_LITERAL,
          firstTokenIn(e, TerminalTokens.TokenNameStringLiteral)
        );
      }
      case ASTNode.TEXT_BLOCK: {
        TextBlock e = (TextBlock) node;
        return new LiteralTreeImpl(
          Tree.Kind.TEXT_BLOCK,
          firstTokenIn(e, TerminalTokens.TokenNameTextBlock)
        );
      }
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION: {
        Annotation e = (Annotation) node;
        ArgumentListTreeImpl arguments = new ArgumentListTreeImpl(
          new ArrayList<>(), new ArrayList<>()
        );
        if (e.getNodeType() == ASTNode.SINGLE_MEMBER_ANNOTATION) {
          arguments.add(
            convertExpression(((SingleMemberAnnotation) e).getValue())
          );
          arguments.complete(
            firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
            lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
          );
        } else if (e.getNodeType() == ASTNode.NORMAL_ANNOTATION) {
          for (int i = 0; i < ((NormalAnnotation) e).values().size(); i++) {
            MemberValuePair o = (MemberValuePair) ((NormalAnnotation) e).values().get(i);
            arguments.add(new AssignmentExpressionTreeImpl(
              Tree.Kind.ASSIGNMENT,
              convertSimpleName(o.getName()),
              firstTokenAfter(o.getName(), TerminalTokens.TokenNameEQUAL),
              convertExpression(o.getValue())
            ));
            if (i < ((NormalAnnotation) e).values().size() - 1) {
              arguments.separators().add(
                firstTokenAfter(o, TerminalTokens.TokenNameCOMMA)
              );
            }
          }
          arguments.complete(
            firstTokenIn(e, TerminalTokens.TokenNameLPAREN),
            lastTokenIn(e, TerminalTokens.TokenNameRPAREN)
          );
        }
        return new AnnotationTreeImpl(
          firstTokenIn(e, TerminalTokens.TokenNameAT),
          (TypeTree) convertExpression(e.getTypeName()),
          arguments
        );
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  private KeywordSuper unqualifiedKeywordSuper(ASTNode node) {
    InternalSyntaxToken token = firstTokenIn(node, TerminalTokens.TokenNamesuper);
    do {
      if (node instanceof AbstractTypeDeclaration) {
        return new KeywordSuper(token, ((AbstractTypeDeclaration) node).resolveBinding());
      } else if (node instanceof AnonymousClassDeclaration) {
        return new KeywordSuper(token, ((AnonymousClassDeclaration) node).resolveBinding());
      }
      node = node.getParent();
    } while (true);
  }

  private TypeTree convertType(Type node) {
    switch (node.getNodeType()) {
      case ASTNode.PRIMITIVE_TYPE: {
        PrimitiveType e = (PrimitiveType) node;
        final JavaTree.PrimitiveTypeTreeImpl t;
        switch (e.getPrimitiveTypeCode().toString()) {
          case "byte":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamebyte));
            break;
          case "short":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNameshort));
            break;
          case "char":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamechar));
            break;
          case "int":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNameint));
            break;
          case "long":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamelong));
            break;
          case "float":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamefloat));
            break;
          case "double":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamedouble));
            break;
          case "boolean":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNameboolean));
            break;
          case "void":
            t = new JavaTree.PrimitiveTypeTreeImpl(lastTokenIn(e, TerminalTokens.TokenNamevoid));
            break;
          default:
            throw new IllegalStateException(e.getPrimitiveTypeCode().toString());
        }
        t.complete(
          convertAnnotations(e.annotations())
        );
        t.typeBinding = e.resolveBinding();
        return t;
      }
      case ASTNode.SIMPLE_TYPE: {
        SimpleType e = (SimpleType) node;
        List<AnnotationTree> annotations = new ArrayList<>();
        for (Object o : e.annotations()) {
          annotations.add((AnnotationTree) convertExpression(
            ((Annotation) o)
          ));
        }
        JavaTree.AnnotatedTypeTree t = e.isVar() ? convertVarType(e) : (JavaTree.AnnotatedTypeTree) convertExpression(e.getName());
        t.complete(annotations);
        // typeBinding is assigned by convertVarType or convertExpression
        return t;
      }
      case ASTNode.UNION_TYPE: {
        UnionType e = (UnionType) node;
        TypeUnionListTreeImpl alternatives = new TypeUnionListTreeImpl(
          new ArrayList<>(), new ArrayList<>()
        );
        for (int i = 0; i < e.types().size(); i++) {
          Type o = (Type) e.types().get(i);
          alternatives.add(convertType(o));
          if (i < e.types().size() - 1) {
            alternatives.separators().add(firstTokenAfter(o, TerminalTokens.TokenNameOR));
          }
        }
        JavaTree.UnionTypeTreeImpl t = new JavaTree.UnionTypeTreeImpl(alternatives);
        t.typeBinding = e.resolveBinding();
        return t;
      }
      case ASTNode.ARRAY_TYPE: {
        ArrayType e = (ArrayType) node;
        @Nullable ITypeBinding elementTypeBinding = e.getElementType().resolveBinding();
        TypeTree t = convertType(e.getElementType());
        int tokenIndex = tokenManager.firstIndexAfter(e.getElementType(), TerminalTokens.TokenNameLBRACKET);
        for (int i = 0; i < e.dimensions().size(); i++) {
          if (i > 0) {
            tokenIndex = nextTokenIndex(tokenIndex, TerminalTokens.TokenNameLBRACKET);
          }
          t = new JavaTree.ArrayTypeTreeImpl(
            t,
            (List) convertAnnotations(((Dimension) e.dimensions().get(i)).annotations()),
            createSyntaxToken(tokenIndex),
            createSyntaxToken(nextTokenIndex(tokenIndex, TerminalTokens.TokenNameRBRACKET))
          );
          if (elementTypeBinding != null) {
            ((JavaTree.ArrayTypeTreeImpl) t).typeBinding = elementTypeBinding.createArrayType(i + 1);
          }
        }
        return t;
      }
      case ASTNode.PARAMETERIZED_TYPE: {
        ParameterizedType e = (ParameterizedType) node;
        int pos = e.getStartPosition() + e.getLength() - 1;
        JavaTree.ParameterizedTypeTreeImpl t = new JavaTree.ParameterizedTypeTreeImpl(
          convertType(e.getType()),
          convertTypeArguments(
            firstTokenAfter(e.getType(), TerminalTokens.TokenNameLESS),
            e.typeArguments(),
            new InternalSyntaxToken(
              compilationUnit.getLineNumber(pos),
              compilationUnit.getColumnNumber(pos),
              ">",
              /* TODO */ Collections.emptyList(),
              false
            )
          )
        );
        t.typeBinding = e.resolveBinding();
        return t;
      }
      case ASTNode.QUALIFIED_TYPE: {
        QualifiedType e = (QualifiedType) node;
        MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
          (ExpressionTree) convertType(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          convertSimpleName(e.getName())
        );
        ((IdentifierTreeImpl) t.identifier()).complete(
          convertAnnotations(e.annotations())
        );
        t.typeBinding = e.resolveBinding();
        return t;
      }
      case ASTNode.NAME_QUALIFIED_TYPE: {
        NameQualifiedType e = (NameQualifiedType) node;
        MemberSelectExpressionTreeImpl t = new MemberSelectExpressionTreeImpl(
          convertExpression(e.getQualifier()),
          firstTokenAfter(e.getQualifier(), TerminalTokens.TokenNameDOT),
          convertSimpleName(e.getName())
        );
        ((IdentifierTreeImpl) t.identifier()).complete(
          convertAnnotations(e.annotations())
        );
        t.typeBinding = e.resolveBinding();
        return t;
      }
      case ASTNode.WILDCARD_TYPE: {
        WildcardType e = (WildcardType) node;
        final InternalSyntaxToken questionToken = e.annotations().isEmpty()
          ? firstTokenIn(e, TerminalTokens.TokenNameQUESTION)
          : firstTokenAfter((ASTNode) e.annotations().get(e.annotations().size() - 1), TerminalTokens.TokenNameQUESTION);
        JavaTree.WildcardTreeImpl t;
        Type bound = e.getBound();
        if (bound == null) {
          t = new JavaTree.WildcardTreeImpl(
            questionToken
          );
        } else {
          t = new JavaTree.WildcardTreeImpl(
            e.isUpperBound() ? Tree.Kind.EXTENDS_WILDCARD : Tree.Kind.SUPER_WILDCARD,
            e.isUpperBound() ? firstTokenBefore(bound, TerminalTokens.TokenNameextends) : firstTokenBefore(bound, TerminalTokens.TokenNamesuper),
            convertType(bound)
          ).complete(
            questionToken
          );
        }
        t.complete(
          convertAnnotations(e.annotations())
        );
        t.typeBinding = e.resolveBinding();
        return t;
      }
      default:
        throw new IllegalStateException(ASTNode.nodeClassForType(node.getNodeType()).toString());
    }
  }

  @Nullable
  private static IMethodBinding excludeRecovery(@Nullable IMethodBinding methodBinding, int arguments) {
    if (methodBinding == null) {
      return null;
    }
    if (methodBinding.isVarargs()) {
      if (arguments + 1 < methodBinding.getParameterTypes().length) {
        return null;
      }
    } else {
      if (arguments != methodBinding.getParameterTypes().length) {
        return null;
      }
    }
    return methodBinding;
  }

  @Nullable
  private static IMethodBinding findConstructorForAnonymousClass(AST ast, @Nullable ITypeBinding typeBinding, @Nullable IMethodBinding methodBinding) {
    if (typeBinding == null || methodBinding == null) {
      return null;
    }
    if (typeBinding.isInterface()) {
      typeBinding = ast.resolveWellKnownType("java.lang.Object");
    }
    for (IMethodBinding m : typeBinding.getDeclaredMethods()) {
      if (methodBinding.isSubsignature(m)) {
        return m;
      }
    }
    return null;
  }

  private List<AnnotationTree> convertAnnotations(List<?> e) {
    List<AnnotationTree> annotations = new ArrayList<>();
    for (Object o : e) {
      annotations.add((AnnotationTree) convertExpression(
        ((Annotation) o)
      ));
    }
    return annotations;
  }

  private ModifiersTreeImpl convertModifiers(List<?> source) {
    List<ModifierTree> modifiers = new ArrayList<>();
    for (Object o : source) {
      modifiers.add(convertModifier((IExtendedModifier) o));
    }
    return new ModifiersTreeImpl(modifiers);
  }

  private ModifierTree convertModifier(IExtendedModifier node) {
    switch (((ASTNode) node).getNodeType()) {
      case ASTNode.NORMAL_ANNOTATION:
      case ASTNode.MARKER_ANNOTATION:
      case ASTNode.SINGLE_MEMBER_ANNOTATION:
        return (AnnotationTree) convertExpression((Expression) node);
      case ASTNode.MODIFIER:
        return convertModifier((org.eclipse.jdt.core.dom.Modifier) node);
      default:
        throw new IllegalStateException();
    }
  }

  private ModifierTree convertModifier(org.eclipse.jdt.core.dom.Modifier node) {
    switch (node.getKeyword().toString()) {
      case "public":
        return new ModifierKeywordTreeImpl(Modifier.PUBLIC, firstTokenIn(node, TerminalTokens.TokenNamepublic));
      case "protected":
        return new ModifierKeywordTreeImpl(Modifier.PROTECTED, firstTokenIn(node, TerminalTokens.TokenNameprotected));
      case "private":
        return new ModifierKeywordTreeImpl(Modifier.PRIVATE, firstTokenIn(node, TerminalTokens.TokenNameprivate));
      case "static":
        return new ModifierKeywordTreeImpl(Modifier.STATIC, firstTokenIn(node, TerminalTokens.TokenNamestatic));
      case "abstract":
        return new ModifierKeywordTreeImpl(Modifier.ABSTRACT, firstTokenIn(node, TerminalTokens.TokenNameabstract));
      case "final":
        return new ModifierKeywordTreeImpl(Modifier.FINAL, firstTokenIn(node, TerminalTokens.TokenNamefinal));
      case "native":
        return new ModifierKeywordTreeImpl(Modifier.NATIVE, firstTokenIn(node, TerminalTokens.TokenNamenative));
      case "synchronized":
        return new ModifierKeywordTreeImpl(Modifier.SYNCHRONIZED, firstTokenIn(node, TerminalTokens.TokenNamesynchronized));
      case "transient":
        return new ModifierKeywordTreeImpl(Modifier.TRANSIENT, firstTokenIn(node, TerminalTokens.TokenNametransient));
      case "volatile":
        return new ModifierKeywordTreeImpl(Modifier.VOLATILE, firstTokenIn(node, TerminalTokens.TokenNamevolatile));
      case "strictfp":
        return new ModifierKeywordTreeImpl(Modifier.STRICTFP, firstTokenIn(node, TerminalTokens.TokenNamestrictfp));
      case "default":
        return new ModifierKeywordTreeImpl(Modifier.DEFAULT, firstTokenIn(node, TerminalTokens.TokenNamedefault));
      case "sealed":
        return new ModifierKeywordTreeImpl(Modifier.SEALED, firstTokenIn(node, ANY_TOKEN));
      case "non-sealed": {
        // in ECJ 3.24.0 "non-sealed" are three separate tokens
        int tokenIndex = tokenManager.firstIndexIn(node, ANY_TOKEN);
        Token t = tokenManager.get(tokenIndex);
        return new ModifierKeywordTreeImpl(Modifier.NON_SEALED, new InternalSyntaxToken(
          compilationUnit.getLineNumber(t.originalStart),
          compilationUnit.getColumnNumber(t.originalStart),
          "non-sealed",
          collectComments(tokenIndex),
          false
        ));
      }
      default:
        throw new IllegalStateException(node.getKeyword().toString());
    }
  }

  private static final int ANY_TOKEN = -1;

  private static final Map<Object, Op> operators = new HashMap<>();

  private static class Op {
    final Tree.Kind kind;

    /**
     * {@link TerminalTokens}
     */
    final int tokenType;

    Op(Tree.Kind kind, int tokenType) {
      this.kind = kind;
      this.tokenType = tokenType;
    }
  }

  static {
    operators.put(PrefixExpression.Operator.PLUS, new Op(Tree.Kind.UNARY_PLUS, TerminalTokens.TokenNamePLUS));
    operators.put(PrefixExpression.Operator.MINUS, new Op(Tree.Kind.UNARY_MINUS, TerminalTokens.TokenNameMINUS));
    operators.put(PrefixExpression.Operator.NOT, new Op(Tree.Kind.LOGICAL_COMPLEMENT, TerminalTokens.TokenNameNOT));
    operators.put(PrefixExpression.Operator.COMPLEMENT, new Op(Tree.Kind.BITWISE_COMPLEMENT, TerminalTokens.TokenNameTWIDDLE));
    operators.put(PrefixExpression.Operator.DECREMENT, new Op(Tree.Kind.PREFIX_DECREMENT, TerminalTokens.TokenNameMINUS_MINUS));
    operators.put(PrefixExpression.Operator.INCREMENT, new Op(Tree.Kind.PREFIX_INCREMENT, TerminalTokens.TokenNamePLUS_PLUS));

    operators.put(PostfixExpression.Operator.DECREMENT, new Op(Tree.Kind.POSTFIX_DECREMENT, TerminalTokens.TokenNameMINUS_MINUS));
    operators.put(PostfixExpression.Operator.INCREMENT, new Op(Tree.Kind.POSTFIX_INCREMENT, TerminalTokens.TokenNamePLUS_PLUS));

    operators.put(InfixExpression.Operator.TIMES, new Op(Tree.Kind.MULTIPLY, TerminalTokens.TokenNameMULTIPLY));
    operators.put(InfixExpression.Operator.DIVIDE, new Op(Tree.Kind.DIVIDE, TerminalTokens.TokenNameDIVIDE));
    operators.put(InfixExpression.Operator.REMAINDER, new Op(Tree.Kind.REMAINDER, TerminalTokens.TokenNameREMAINDER));
    operators.put(InfixExpression.Operator.PLUS, new Op(Tree.Kind.PLUS, TerminalTokens.TokenNamePLUS));
    operators.put(InfixExpression.Operator.MINUS, new Op(Tree.Kind.MINUS, TerminalTokens.TokenNameMINUS));
    operators.put(InfixExpression.Operator.LEFT_SHIFT, new Op(Tree.Kind.LEFT_SHIFT, TerminalTokens.TokenNameLEFT_SHIFT));
    operators.put(InfixExpression.Operator.RIGHT_SHIFT_SIGNED, new Op(Tree.Kind.RIGHT_SHIFT, TerminalTokens.TokenNameRIGHT_SHIFT));
    operators.put(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, new Op(Tree.Kind.UNSIGNED_RIGHT_SHIFT, TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT));
    operators.put(InfixExpression.Operator.LESS, new Op(Tree.Kind.LESS_THAN, TerminalTokens.TokenNameLESS));
    operators.put(InfixExpression.Operator.GREATER, new Op(Tree.Kind.GREATER_THAN, TerminalTokens.TokenNameGREATER));
    operators.put(InfixExpression.Operator.LESS_EQUALS, new Op(Tree.Kind.LESS_THAN_OR_EQUAL_TO, TerminalTokens.TokenNameLESS_EQUAL));
    operators.put(InfixExpression.Operator.GREATER_EQUALS, new Op(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, TerminalTokens.TokenNameGREATER_EQUAL));
    operators.put(InfixExpression.Operator.EQUALS, new Op(Tree.Kind.EQUAL_TO, TerminalTokens.TokenNameEQUAL_EQUAL));
    operators.put(InfixExpression.Operator.NOT_EQUALS, new Op(Tree.Kind.NOT_EQUAL_TO, TerminalTokens.TokenNameNOT_EQUAL));
    operators.put(InfixExpression.Operator.XOR, new Op(Tree.Kind.XOR, TerminalTokens.TokenNameXOR));
    operators.put(InfixExpression.Operator.OR, new Op(Tree.Kind.OR, TerminalTokens.TokenNameOR));
    operators.put(InfixExpression.Operator.AND, new Op(Tree.Kind.AND, TerminalTokens.TokenNameAND));
    operators.put(InfixExpression.Operator.CONDITIONAL_OR, new Op(Tree.Kind.CONDITIONAL_OR, TerminalTokens.TokenNameOR_OR));
    operators.put(InfixExpression.Operator.CONDITIONAL_AND, new Op(Tree.Kind.CONDITIONAL_AND, TerminalTokens.TokenNameAND_AND));

    operators.put(Assignment.Operator.ASSIGN, new Op(Tree.Kind.ASSIGNMENT, TerminalTokens.TokenNameEQUAL));
    operators.put(Assignment.Operator.PLUS_ASSIGN, new Op(Tree.Kind.PLUS_ASSIGNMENT, TerminalTokens.TokenNamePLUS_EQUAL));
    operators.put(Assignment.Operator.MINUS_ASSIGN, new Op(Tree.Kind.MINUS_ASSIGNMENT, TerminalTokens.TokenNameMINUS_EQUAL));
    operators.put(Assignment.Operator.TIMES_ASSIGN, new Op(Tree.Kind.MULTIPLY_ASSIGNMENT, TerminalTokens.TokenNameMULTIPLY_EQUAL));
    operators.put(Assignment.Operator.DIVIDE_ASSIGN, new Op(Tree.Kind.DIVIDE_ASSIGNMENT, TerminalTokens.TokenNameDIVIDE_EQUAL));
    operators.put(Assignment.Operator.BIT_AND_ASSIGN, new Op(Tree.Kind.AND_ASSIGNMENT, TerminalTokens.TokenNameAND_EQUAL));
    operators.put(Assignment.Operator.BIT_OR_ASSIGN, new Op(Tree.Kind.OR_ASSIGNMENT, TerminalTokens.TokenNameOR_EQUAL));
    operators.put(Assignment.Operator.BIT_XOR_ASSIGN, new Op(Tree.Kind.XOR_ASSIGNMENT, TerminalTokens.TokenNameXOR_EQUAL));
    operators.put(Assignment.Operator.REMAINDER_ASSIGN, new Op(Tree.Kind.REMAINDER_ASSIGNMENT, TerminalTokens.TokenNameREMAINDER_EQUAL));
    operators.put(Assignment.Operator.LEFT_SHIFT_ASSIGN, new Op(Tree.Kind.LEFT_SHIFT_ASSIGNMENT, TerminalTokens.TokenNameLEFT_SHIFT_EQUAL));
    operators.put(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN, new Op(Tree.Kind.RIGHT_SHIFT_ASSIGNMENT, TerminalTokens.TokenNameRIGHT_SHIFT_EQUAL));
    operators.put(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN, new Op(Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT, TerminalTokens.TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL));
  }

}
