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

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiDeconstructionPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiInstanceOfExpression;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiSwitchBlock;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeTestPattern;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.testFramework.LightVirtualFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.sonar.java.psi.PsiLanguageFeatures.Feature;

/**
 * Applies source-level policy separately from raw parser errors. The result keeps
 * fatal syntax failures distinct from diagnostics exposed on a converted unit.
 * It is a bounded language validator, not a semantic diagnostic provider.
 */
public final class PsiLanguageValidator {
  public enum Kind {
    FATAL, UNDEFINED_TYPE, PREVIEW_FEATURE_USED
  }

  public record Diagnostic(Kind kind, int start, int end, String message) {
  }

  public record Result(List<Diagnostic> diagnostics, List<Diagnostic> rawSyntaxErrors) {
    public Result {
      diagnostics = List.copyOf(diagnostics);
      rawSyntaxErrors = List.copyOf(rawSyntaxErrors);
    }

    public boolean hasFatalError() {
      return diagnostics.stream().anyMatch(diagnostic -> diagnostic.kind() == Kind.FATAL) || !rawSyntaxErrors.isEmpty();
    }
  }

  private static final Set<String> LEGACY_SWITCH_SELECTORS = Set.of(
    "java.lang.String", "java.lang.Byte", "java.lang.Short", "java.lang.Character", "java.lang.Integer");

  private PsiLanguageValidator() {
  }

  public static Result validate(PsiJavaFile file, JavaPsiConfiguration configuration) {
    var input = new LightVirtualFile(file.getName(), JavaFileType.INSTANCE, file.getText());
    var syntax = (PsiJavaFile) PsiManager.getInstance(file.getProject()).findFile(input);
    syntax.putUserData(PsiUtil.FILE_LANGUAGE_LEVEL_KEY, LanguageLevel.JDK_26_PREVIEW);
    int version = configuration.sourceVersion();
    var diagnostics = new ArrayList<Diagnostic>();
    for (var occurrence : PsiLanguageFeatures.collect(syntax)) {
      var element = occurrence.element();
      switch (occurrence.feature()) {
        case LOCAL_VAR, LAMBDA_VAR -> {
          if (version < 10) {
            var reference = ((PsiTypeElement) element).getInnermostComponentReferenceElement();
            if (reference == null || reference.resolve() == null) {
              add(diagnostics, Kind.UNDEFINED_TYPE, element, "var cannot be resolved to a type");
            }
          } else if (version == 10 && occurrence.feature() == Feature.LAMBDA_VAR) {
            add(diagnostics, Kind.FATAL, element, "'var' is not allowed here");
          }
        }
        case SWITCH_EXPRESSION -> {
          if (version < 14) {
            add(diagnostics, Kind.FATAL, element, "Switch Expressions are supported from Java 14 onwards only");
          }
        }
        case SWITCH_RULE -> {
          if (version < 14) {
            add(diagnostics, Kind.FATAL, element, "Arrow in case statement supported from Java 14 onwards only");
          }
        }
        case TEXT_BLOCK -> feature(diagnostics, element, version, 15, "Text Blocks");
        case RECORD -> {
          if (version < 16 && element.getParent() instanceof PsiJavaFile) {
            feature(diagnostics, element, version, 25, "Compact Source Files and Instance Main Methods");
            add(diagnostics, Kind.PREVIEW_FEATURE_USED, element, "Implicitly declared class must have a candidate main method");
          }
        }
        case TYPE_PATTERN -> {
          feature(diagnostics, element, version, 16, "Type Patterns");
          var pattern = (PsiTypeTestPattern) element;
          var variable = pattern.getPatternVariable();
          if ((!configuration.previewEnabled() || version < 26) && variable != null
            && variable.getType() instanceof PsiPrimitiveType primitive
            && pattern.getParent() instanceof PsiInstanceOfExpression expression
            && expression.getOperand().getType() instanceof PsiClassType operand && operand.resolve() != null) {
            add(diagnostics, Kind.PREVIEW_FEATURE_USED, expression,
              "Incompatible conditional operand types " + operand.getPresentableText() + " and " + primitive.getPresentableText());
          }
        }
        case RECORD_PATTERN -> {
          feature(diagnostics, element, version, 21, "Record Pattern");
          var original = PsiTreeUtil.getParentOfType(file.findElementAt(element.getTextOffset()), PsiDeconstructionPattern.class, false);
          if (original != null) {
            var reference = original.getTypeElement().getInnermostComponentReferenceElement();
            if (reference != null && reference.getQualifier() == null && reference.resolve() == null) {
              add(diagnostics, Kind.UNDEFINED_TYPE, element, reference.getReferenceName() + " cannot be resolved to a type");
            }
          }
        }
        case SWITCH_PATTERN -> {
          feature(diagnostics, element, version, 21, "Pattern Matching in Switch");
          var block = PsiTreeUtil.getParentOfType(element, PsiSwitchBlock.class);
          if (version < 21 && block != null && block.getExpression() != null
            && block.getExpression().getType() instanceof PsiClassType type && !LEGACY_SWITCH_SELECTORS.contains(type.rawType().getCanonicalText())) {
            var resolved = type.resolve();
            if (resolved != null && !resolved.isEnum()) {
              add(diagnostics, Kind.PREVIEW_FEATURE_USED, block.getExpression(),
                "Cannot switch on a value of type " + type.getPresentableText() + ". Only convertible int values, strings or enum variables are permitted");
            }
          }
        }
        case SEALED -> {
          if (version < 17) {
            var keyword = PsiTreeUtil.getChildrenOfTypeAsList(element, PsiKeyword.class).stream()
              .filter(token -> token.getText().equals("class")).findFirst().orElse(null);
            add(diagnostics, Kind.FATAL, keyword == null ? element : keyword, "Syntax error on token \"class\", . expected");
          }
        }
        case UNNAMED_VARIABLE -> {
          if (version >= 9 && version < 22) {
            add(diagnostics, Kind.FATAL, element, "'_' is a keyword from source level 9 onwards, cannot be used as identifier");
          }
        }
        case MODULE_IMPORT -> feature(diagnostics, element, version, 25, "Module Import Declarations");
        case FLEXIBLE_CONSTRUCTOR -> feature(diagnostics, element, version, 25, "Flexible Constructor Bodies");
        case COMPACT_SOURCE -> feature(diagnostics, element, version, 25, "Compact Source Files and Instance Main Methods");
      }
    }
    diagnostics.sort(Comparator.comparingInt(Diagnostic::start));
    var syntaxErrors = PsiTreeUtil.findChildrenOfType(syntax, PsiErrorElement.class).stream()
      .filter(error -> !recoveredResource(error) && !recoveredRequires(error))
      .map(PsiLanguageValidator::syntaxError)
      .toList();
    return new Result(diagnostics, syntaxErrors);
  }

  /** C-style resource dimensions are accepted by the reference parser and converted as a recovered local declaration. */
  private static boolean recoveredRequires(PsiErrorElement error) {
    var requires = PsiTreeUtil.getParentOfType(error, com.intellij.psi.PsiRequiresStatement.class);
    return requires != null && requires.getModuleReference() == null && requires.hasModifierProperty("transitive")
      && "Identifier expected".equals(error.getErrorDescription());
  }

  private static boolean recoveredResource(PsiErrorElement error) {
    var resources = PsiTreeUtil.getParentOfType(error, com.intellij.psi.PsiResourceList.class);
    if (resources == null || PsiTreeUtil.findChildrenOfType(resources, com.intellij.psi.PsiResourceVariable.class).stream()
      .noneMatch(variable -> variable.getInitializer() == null && variable.getNameIdentifier() != null
        && resources.getText().substring(variable.getNameIdentifier().getTextRange().getEndOffset() - resources.getTextRange().getStartOffset()).stripLeading().startsWith("["))) {
      return false;
    }
    String text = resources.getText();
    if (!text.endsWith(")")) {
      return false;
    }
    var recovered = com.intellij.psi.JavaPsiFacade.getElementFactory(error.getProject())
      .createCodeBlockFromText("{" + text.substring(1, text.length() - 1) + ";}", resources);
    return !PsiTreeUtil.hasErrorElements(recovered);
  }

  private static Diagnostic syntaxError(PsiErrorElement error) {
    var parameters = PsiTreeUtil.getParentOfType(error, PsiParameterList.class);
    if (parameters != null && parameters.getNode().findChildByType(JavaTokenType.RPARENTH) == null && parameters.getParent() instanceof PsiMethod method
      && method.getBody() == null && method.getContainingClass() != null
      && method.getContainingClass().getParent() instanceof PsiJavaFile) {
      PsiElement anchor = method.getContainingClass().getLBrace();
      for (var previous = method.getPrevSibling(); previous != null; previous = previous.getPrevSibling()) {
        if (previous instanceof PsiMember) {
          anchor = previous;
          break;
        }
      }
      if (anchor != null) {
        int position = anchor.getTextRange().getEndOffset() - 1;
        return new Diagnostic(Kind.FATAL, position, position, "Syntax error, insert \"}\" to complete ClassBody");
      }
    }
    var declaration = PsiTreeUtil.getParentOfType(error, com.intellij.psi.PsiClass.class);
    if (declaration != null && declaration.getLBrace() == null && declaration.getNameIdentifier() != null
      && error.getErrorDescription().equals("'{' expected")) {
      int start = declaration.getNameIdentifier().getTextRange().getEndOffset() - 1;
      return new Diagnostic(Kind.FATAL, start, start, "Syntax error, insert \"ClassBody\" to complete CompilationUnit");
    }
    var previous = PsiTreeUtil.prevLeaf(error);
    while (previous instanceof com.intellij.psi.PsiWhiteSpace || previous instanceof com.intellij.psi.PsiComment) {
      previous = PsiTreeUtil.prevLeaf(previous);
    }
    if (previous != null) {
      int position = previous.getTextRange().getEndOffset() - 1;
      if (error.getErrorDescription().equals("'}' expected") && error.getParent() instanceof com.intellij.psi.PsiClass) {
        return new Diagnostic(Kind.FATAL, position, position, "Syntax error, insert \"}\" to complete ClassBody");
      }
      if (error.getErrorDescription().equals("';' expected")) {
        boolean field = PsiTreeUtil.getParentOfType(error, com.intellij.psi.PsiField.class) != null;
        if (Set.of("return", "break", "continue").contains(previous.getText())) {
          position = previous.getTextRange().getStartOffset();
        }
        return new Diagnostic(Kind.FATAL, position, position,
          "Syntax error, insert \";\" to complete " + (field ? "ClassBodyDeclarations" : "BlockStatements"));
      }
      if (error.getErrorDescription().equals("Expression expected") && previous.getText().equals("=")
        && PsiTreeUtil.getParentOfType(error, com.intellij.psi.PsiVariable.class) != null) {
        return new Diagnostic(Kind.FATAL, position, position, "Syntax error on token \"=\", VariableInitializer expected after this token");
      }
    }
    int start = error.getTextRange().getStartOffset();
    return new Diagnostic(Kind.FATAL, start, Math.max(start, error.getTextRange().getEndOffset() - 1), error.getErrorDescription());
  }

  private static void feature(List<Diagnostic> diagnostics, PsiElement element, int version, int minimum, String name) {
    if (version < minimum) {
      add(diagnostics, Kind.PREVIEW_FEATURE_USED, element,
        "The Java feature '" + name + "' is only available with source level " + minimum + " and above");
    }
  }

  private static void add(List<Diagnostic> diagnostics, Kind kind, PsiElement element, String message) {
    diagnostics.add(new Diagnostic(kind, element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset() - 1, message));
  }
}
