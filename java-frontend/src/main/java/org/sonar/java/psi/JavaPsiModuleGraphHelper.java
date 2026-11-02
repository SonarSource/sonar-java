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

import com.intellij.psi.JavaModuleGraphHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import java.util.Set;

final class JavaPsiModuleGraphHelper extends JavaModuleGraphHelper {

  private static JavaPsiModuleGraph graph(PsiElement element) {
    return ((ExplicitJavaFileManager) JavaFileManager.getInstance(element.getProject())).moduleGraph();
  }

  @Override
  public PsiJavaModule findDescriptorByElement(PsiElement element) {
    return element == null ? null : graph(element).descriptor(element);
  }

  @Override
  public Set<PsiJavaModule> getAllTransitiveDependencies(PsiJavaModule module) {
    return graph(module).transitiveDependencies(module);
  }

  @Override
  public boolean isAccessible(PsiClass target, PsiElement place) {
    return super.isAccessible(target, place) && !graph(place).ambiguousModuleImport(target, place);
  }

  @Override
  public boolean isAccessible(String packageName, PsiFile targetFile, PsiElement place) {
    return graph(place).accessible(packageName, targetFile, place);
  }

  @Override
  public boolean isAccessible(PsiJavaModule targetModule, PsiElement place) {
    return graph(place).accessible(targetModule, place);
  }
}
