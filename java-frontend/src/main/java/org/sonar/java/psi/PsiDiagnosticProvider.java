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
package org.sonar.java.psi;

import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.sonar.sslr.api.RecognitionException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.LineColumnConverter;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;

final class PsiDiagnosticProvider {
  private final PsiJavaFile file;
  private final LineColumnConverter positions;
  private final int[] offsets;
  private final List<PsiWarning> warnings = new ArrayList<>();
  private final Set<PsiProblem> problems = new LinkedHashSet<>();

  private PsiDiagnosticProvider(PsiJavaFile file, String source) {
    this.file = file;
    positions = new LineColumnConverter(source);
    offsets = PsiTokenIndex.unicodeInput(source).offsets();
  }

  static PsiDiagnosticProvider create(PsiJavaFile file, JavaPsiConfiguration configuration, String source) {
    var provider = new PsiDiagnosticProvider(file, source);
    provider.language(configuration);
    provider.collect();
    return provider;
  }

  void attach(JavaTree.CompilationUnitTreeImpl tree) {
    tree.setSemanticModel(tree.sema, problems, ((PsiSema) tree.sema).getEnvironmentCleaner());
    warnings.sort(Comparator.comparing(PsiWarning::start).thenComparing(PsiWarning::end));
    PsiWarning.Mapper.warningsFor(warnings).mappedInto(tree);
  }

  private void language(JavaPsiConfiguration configuration) {
    for (var method : PsiTreeUtil.findChildrenOfType(file, PsiMethod.class)) {
      if (method.isConstructor() && method.getContainingClass() != null && method.getContainingClass().isInterface()) {
        var following = method.getNextSibling();
        while (following instanceof PsiWhiteSpace || following != null && following.getText().equals(";")) {
          following = following.getNextSibling();
        }
        if (following instanceof PsiComment && following.getText().startsWith("//")
          || PsiTreeUtil.findChildrenOfType(method, PsiComment.class).stream()
            .anyMatch(comment -> comment.getText().startsWith("//") && comment.getTextRange().getStartOffset() > method.getParameterList().getTextRange().getEndOffset())) {
          throw new IndexOutOfBoundsException("Missing method return type");
        }
        problems.add(new PsiProblem("Return type for the method is missing", ProblemKind.PREVIEW_FEATURE_USED));
      }
    }
    var result = PsiLanguageValidator.validate(file, configuration);
    var fatal = java.util.stream.Stream.of(result.diagnostics(), result.rawSyntaxErrors(), PsiParseDiagnostics.additional(file, configuration))
      .flatMap(List::stream)
      .filter(diagnostic -> diagnostic.kind() == PsiLanguageValidator.Kind.FATAL)
      .min(Comparator.comparingInt(PsiLanguageValidator.Diagnostic::start));
    if (fatal.isPresent()) {
      var diagnostic = fatal.get();
      var pos = positions.toPos(offset(diagnostic.start()));
      throw new RecognitionException(pos.line(), "Parse error at line " + pos.line() + " column " + (pos.columnOffset() + 1) + ": " + diagnostic.message());
    }
    for (var diagnostic : result.diagnostics()) {
      if (diagnostic.kind() != PsiLanguageValidator.Kind.FATAL) {
        problems.add(new PsiProblem(diagnostic.message(), diagnostic.kind() == PsiLanguageValidator.Kind.UNDEFINED_TYPE
          ? ProblemKind.UNDEFINED_TYPE : ProblemKind.PREVIEW_FEATURE_USED));
      }
    }
  }

  private void collect() {
    var imports = new PsiImportDiagnostics(file);
    file.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitReferenceElement(PsiJavaCodeReferenceElement reference) {
        imports.reference(reference);
        if ((reference.getParent() instanceof PsiTypeElement || reference.getParent() instanceof PsiReferenceList
          || reference.getParent() instanceof PsiAnnotation || reference.getParent() instanceof PsiNewExpression)
          && reference.resolve() == null && !Set.of("var", "record").contains(reference.getText())) {
          var missing = reference;
          while (missing.getQualifier() instanceof PsiJavaCodeReferenceElement qualifier && qualifier.resolve() == null) {
            missing = qualifier;
          }
          problems.add(new PsiProblem(missing.getQualifiedName() + " cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE));
        }
        super.visitReferenceElement(reference);
      }

      @Override
      public void visitReferenceExpression(PsiReferenceExpression reference) {
        imports.reference(reference);
        if (reference.getQualifierExpression() == null && reference.resolve() == null
          && reference.getParent() instanceof PsiReferenceExpression parent && parent.getQualifierExpression() == reference) {
          var lambda = PsiTreeUtil.getParentOfType(reference, PsiLambdaExpression.class);
          var invocation = lambda == null ? null : PsiTreeUtil.getParentOfType(lambda, PsiMethodCallExpression.class);
          if (invocation == null || invocation.resolveMethod() != null) {
            var receiver = parent;
            while (receiver.getParent() instanceof PsiReferenceExpression outer && outer.getQualifierExpression() == receiver) {
              receiver = outer;
            }
            boolean methodQualifier = receiver.getParent() instanceof PsiMethodCallExpression call && call.getMethodExpression() == receiver
              || receiver instanceof PsiMethodReferenceExpression || receiver.getParent() instanceof PsiMethodReferenceExpression;
            problems.add(new PsiProblem(reference.getReferenceName() + " cannot be resolved" + (methodQualifier ? "" : " to a variable"), ProblemKind.PREVIEW_FEATURE_USED));
          }
        }
        super.visitReferenceExpression(reference);
      }

      @Override
      public void visitTypeCastExpression(PsiTypeCastExpression cast) {
        if (PsiCastDiagnostics.isRedundant(cast)) {
          warning(ProblemKind.REDUNDANT_CAST, cast.getTextRange().getStartOffset(),
            "Unnecessary cast from " + PsiDiagnosticTypes.name(cast.getOperand().getType()) + " to " + PsiDiagnosticTypes.name(cast.getCastType().getType()));
        }
        super.visitTypeCastExpression(cast);
      }

      @Override
      public void visitAssignmentExpression(PsiAssignmentExpression assignment) {
        if (PsiAssignmentDiagnostics.hasNoEffect(assignment)) {
          warning(ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, assignment.getLExpression().getTextRange().getStartOffset(),
            "The assignment to variable " + ((PsiReferenceExpression) PsiUtil.skipParenthesizedExprDown(assignment.getLExpression())).getReferenceName() + " has no effect");
        }
        super.visitAssignmentExpression(assignment);
      }

      @Override
      public void visitTryStatement(PsiTryStatement statement) {
        PsiCatchDiagnostics.collect(statement, (element, message) -> warning(ProblemKind.MASKED_CATCH, element.getTextRange().getStartOffset(), message));
        super.visitTryStatement(statement);
      }
    });
    imports.collect((element, message) -> warning(ProblemKind.UNUSED_IMPORT, element.getTextRange().getStartOffset(), message), problems);
    PsiResolutionDiagnostics.collect(file, problems);
  }

  private void warning(ProblemKind type, int start, String message) {
    var pos = positions.toPos(offset(start));
    warnings.add(new PsiWarning(message, type, pos.line(), pos.columnOffset(), pos.line(), pos.columnOffset()));
  }

  private int offset(int logicalOffset) {
    return offsets[Math.max(0, Math.min(logicalOffset, file.getTextLength()))];
  }
}
