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

import com.intellij.psi.impl.java.stubs.JavaStubElementTypePsiElementMappingRegistry;
import com.intellij.psi.impl.java.stubs.JavaStubElementTypes;
import com.intellij.psi.impl.java.stubs.factories.JavaAnnotationParameterListStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaAnnotationStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaClassInitializerStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaClassReferenceListStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaClassStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaFieldStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaImportListStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaImportStatementStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaLiteralExpressionStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaMethodStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaModifierListStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaModuleStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaNameValuePairStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaPackageAccessibilityStatementStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaPackageStatementStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaParameterListStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaParameterStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaProvidesStatementStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaRecordComponentStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaRecordHeaderStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaRequiresStatementStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaTypeParameterListStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaTypeParameterStubFactory;
import com.intellij.psi.impl.java.stubs.factories.JavaUsesStatementStubFactory;
import com.intellij.psi.impl.java.stubs.factories.LambdaExpressionStubFactory;
import com.intellij.psi.impl.java.stubs.factories.MethodReferenceStubFactory;
import com.intellij.psi.stubs.StubRegistry;
import com.intellij.psi.stubs.StubRegistryExtension;

/** Registers in-memory Java stub factories without persistent index serializers. */
final class JavaPsiStubFactories implements StubRegistryExtension {

  @Override
  public void register(StubRegistry registry) {
    JavaStubElementTypePsiElementMappingRegistry.getInstance();
    registry.registerLightStubFactory(JavaStubElementTypes.LITERAL_EXPRESSION, new JavaLiteralExpressionStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.MODIFIER_LIST, new JavaModifierListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.ANNOTATION, new JavaAnnotationStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.ANNOTATION_PARAMETER_LIST, new JavaAnnotationParameterListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.NAME_VALUE_PAIR, new JavaNameValuePairStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.LAMBDA_EXPRESSION, new LambdaExpressionStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.METHOD_REF_EXPRESSION, new MethodReferenceStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.PARAMETER_LIST, new JavaParameterListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.PARAMETER, new JavaParameterStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.TYPE_PARAMETER, new JavaTypeParameterStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.TYPE_PARAMETER_LIST, new JavaTypeParameterListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.CLASS_INITIALIZER, new JavaClassInitializerStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.IMPORT_LIST, new JavaImportListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.MODULE, new JavaModuleStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.REQUIRES_STATEMENT, new JavaRequiresStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.USES_STATEMENT, new JavaUsesStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.PROVIDES_STATEMENT, new JavaProvidesStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.RECORD_COMPONENT, new JavaRecordComponentStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.RECORD_HEADER, new JavaRecordHeaderStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.PACKAGE_STATEMENT, new JavaPackageStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.EXPORTS_STATEMENT, new JavaPackageAccessibilityStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.OPENS_STATEMENT, new JavaPackageAccessibilityStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.CLASS, new JavaClassStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.IMPLICIT_CLASS, new JavaClassStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.ANONYMOUS_CLASS, new JavaClassStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.ENUM_CONSTANT_INITIALIZER, new JavaClassStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.METHOD, new JavaMethodStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.ANNOTATION_METHOD, new JavaMethodStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.FIELD, new JavaFieldStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.ENUM_CONSTANT, new JavaFieldStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.EXTENDS_LIST, new JavaClassReferenceListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.PERMITS_LIST, new JavaClassReferenceListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.IMPLEMENTS_LIST, new JavaClassReferenceListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.THROWS_LIST, new JavaClassReferenceListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.EXTENDS_BOUND_LIST, new JavaClassReferenceListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.PROVIDES_WITH_LIST, new JavaClassReferenceListStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.IMPORT_STATEMENT, new JavaImportStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.IMPORT_STATIC_STATEMENT, new JavaImportStatementStubFactory());
    registry.registerLightStubFactory(JavaStubElementTypes.IMPORT_MODULE_STATEMENT, new JavaImportStatementStubFactory());
  }
}
