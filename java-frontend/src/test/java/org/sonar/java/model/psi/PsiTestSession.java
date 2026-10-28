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
package org.sonar.java.model.psi;

import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.Disposer;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.sonar.java.model.JavaTree;

final class PsiTestSession implements AutoCloseable {

  private final JavaPsiApplication application = JavaPsiApplication.acquire();
  private final com.intellij.openapi.Disposable owner = Disposer.newDisposable("PSI syntax test");
  private final JavaCoreProjectEnvironment environment = new JavaCoreProjectEnvironment(owner, application);

  PsiTestSession() {
    JavaPsiApplication.registerExtensionPoints(environment.getProject().getExtensionArea(), true);
    ApplicationManager.getApplication().runWriteAction(() ->
      LanguageLevelProjectExtension.getInstance(environment.getProject()).setLanguageLevel(LanguageLevel.JDK_26_PREVIEW));
  }

  <T> T read(String name, String source, Function<PsiJavaFile, T> action) {
    return ApplicationManager.getApplication().runReadAction((com.intellij.openapi.util.Computable<T>) () -> {
      var virtualFile = new LightVirtualFile(name, JavaFileType.INSTANCE, source, StandardCharsets.UTF_8, 0);
      virtualFile.setWritable(false);
      return action.apply((PsiJavaFile) PsiManager.getInstance(environment.getProject()).findFile(virtualFile));
    });
  }

  JavaTree.CompilationUnitTreeImpl convert(String name, String source) {
    var snapshot = PsiSourceSnapshot.from(source);
    return read(name, snapshot.psiText(), file -> PsiTreeConverter.convert(file, snapshot));
  }

  @Override
  public void close() {
    try {
      Disposer.dispose(owner);
    } finally {
      JavaPsiApplication.release();
    }
  }
}
