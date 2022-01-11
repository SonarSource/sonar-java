/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.eclipse.jdt.core.dom;

import javax.annotation.Nullable;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public final class ASTUtils {

  private static final Logger LOG = Loggers.get(ASTUtils.class);

  private static final IAnnotationBinding[] NO_ANNOTATIONS = new IAnnotationBinding[0];

  private ASTUtils() {
  }

  public static void mayTolerateMissingType(AST ast) {
    ast.getBindingResolver().lookupEnvironment().mayTolerateMissingType = true;
  }

  public static void cleanupEnvironment(AST ast) {
    ast.getBindingResolver().lookupEnvironment().nameEnvironment.cleanup();
  }

  @Nullable
  public static ITypeBinding resolveType(AST ast, String name) {
    try {
      BindingResolver bindingResolver = ast.getBindingResolver();
      ReferenceBinding referenceBinding = bindingResolver
        .lookupEnvironment()
        .getType(CharOperation.splitOn('.', name.toCharArray()));
      return bindingResolver.getTypeBinding(referenceBinding);
    } catch (Exception e) {
      // exception on ECJ side when trying to resolve a Type, recover on null type
      LOG.error(String.format("ECJ Unable to resolve type %s", name), e);
      return null;
    }
  }

  public static IAnnotationBinding[] resolvePackageAnnotations(AST ast, String packageName) {
    // See org.eclipse.jdt.core.dom.PackageBinding#getAnnotations()
    BindingResolver bindingResolver = ast.getBindingResolver();
    LookupEnvironment lookupEnvironment = bindingResolver.lookupEnvironment();
    NameEnvironmentAnswer answer = lookupEnvironment.nameEnvironment.findType(
      TypeConstants.PACKAGE_INFO_NAME,
      CharOperation.splitOn('.', packageName.toCharArray())
    );
    if (answer == null) {
      return NO_ANNOTATIONS;
    }
    IBinaryType type = answer.getBinaryType();
    if (type == null) {
      // Can happen for instance with ant, as ant only generates 'package-info.class'
      // when there is annotations in the package-info.java file.
      return NO_ANNOTATIONS;
    }
    IBinaryAnnotation[] binaryAnnotations = type.getAnnotations();
    AnnotationBinding[] binaryInstances =
      BinaryTypeBinding.createAnnotations(binaryAnnotations, lookupEnvironment, type.getMissingTypeNames());
    AnnotationBinding[] allInstances =
      AnnotationBinding.addStandardAnnotations(binaryInstances, type.getTagBits(), lookupEnvironment);

    IAnnotationBinding[] domInstances = new IAnnotationBinding[allInstances.length];
    for (int i = 0; i < allInstances.length; i++) {
      // FIXME can be null if annotation can not be resolved e.g. due to incomplete classpath
      domInstances[i] = bindingResolver.getAnnotationInstance(allInstances[i]);
    }
    return domInstances;
  }

  public static String signature(IMethodBinding methodBinding) {
    char[] signature = ((MethodBinding) methodBinding).binding.signature();
    return new String(signature);
  }

}
