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

import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CoreJavaDirectoryService;
import com.intellij.core.CorePsiPackageImplementationHelper;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.java.frontback.psi.impl.syntax.JavaSyntaxDefinitionExtension;
import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.lang.java.syntax.JavaElementTypeConverterExtension;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.projectRoots.JavaVersionService;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem;
import com.intellij.platform.syntax.psi.CommonElementTypeConverterFactory;
import com.intellij.platform.syntax.psi.ElementTypeConverters;
import com.intellij.platform.syntax.psi.LanguageSyntaxDefinitions;
import com.intellij.platform.syntax.psi.PsiSyntaxBuilderFactory;
import com.intellij.platform.syntax.psi.PsiSyntaxBuilderFactoryImpl;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.ClassFileViewProviderFactory;
import com.intellij.psi.FileTypeFileViewProviders;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaModuleGraphHelper;
import com.intellij.psi.PsiSubstitutorFactory;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.JavaClassSupersImpl;
import com.intellij.psi.impl.LanguageConstantExpressionEvaluator;
import com.intellij.psi.impl.PsiExpressionEvaluator;
import com.intellij.psi.impl.PsiSubstitutorFactoryImpl;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.compiled.ClsDecompilerImpl;
import com.intellij.psi.impl.file.PsiPackageImplementationHelper;
import com.intellij.psi.impl.source.tree.JavaASTFactory;
import com.intellij.psi.stubs.BinaryFileStubBuilders;
import com.intellij.psi.stubs.StubElementRegistryServiceImplKt;
import com.intellij.psi.util.JavaClassSupers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

final class JavaPsiApplication extends CoreApplicationEnvironment {

  private static JavaPsiApplication shared;
  private static int users;
  private static final List<String> DESCRIPTORS = List.of(
    "META-INF/Core.xml", "META-INF/Core.analyzer.xml", "META-INF/CoreImpl.xml", "META-INF/CoreImpl.analyzer.xml",
    "intellij.java.frontback.psi.xml", "intellij.java.frontback.psi.impl.xml",
    "intellij.java.psi.xml", "intellij.java.psi.impl.xml");

  private JavaPsiApplication(Disposable owner) {
    super(owner, false);
    registerExtensionPoints(application.getExtensionArea(), false);
    Registry.get("javac.fresh.variables.for.captured.wildcards.only").setValue(true);
    Registry.get("unsound.capture.conversion.java.spec.change").setValue(false);
    Registry.get("java.highest.language.level").setValue(Integer.toString(LanguageLevel.HIGHEST.feature()));
    registerFileType(JavaClassFileType.INSTANCE, "class");
    registerFileType(JavaFileType.INSTANCE, "java");
    addExtension(StubElementRegistryServiceImplKt.STUB_REGISTRY_EP, new JavaPsiStubFactories());
    addExplicitExtension(FileTypeFileViewProviders.INSTANCE, JavaClassFileType.INSTANCE, new ClassFileViewProviderFactory());
    addExplicitExtension(BinaryFileStubBuilders.INSTANCE, JavaClassFileType.INSTANCE, new ClassFileStubBuilder());
    addExplicitExtension(LanguageASTFactory.INSTANCE, JavaLanguage.INSTANCE, new JavaASTFactory());
    addExplicitExtension(LanguageSyntaxDefinitions.getINSTANCE(), JavaLanguage.INSTANCE, new JavaSyntaxDefinitionExtension());
    addExplicitExtension(ElementTypeConverters.getInstance(), JavaLanguage.INSTANCE, new CommonElementTypeConverterFactory());
    addExplicitExtension(ElementTypeConverters.getInstance(), JavaLanguage.INSTANCE, new JavaElementTypeConverterExtension());
    registerParserDefinition(new JavaParserDefinition());
    addExtension(com.intellij.psi.augment.PsiAugmentProvider.EP_NAME, new com.intellij.psi.impl.RecordAugmentProvider());
    addExplicitExtension(LanguageConstantExpressionEvaluator.INSTANCE, JavaLanguage.INSTANCE, new PsiExpressionEvaluator());
    registerApplicationService(PsiPackageImplementationHelper.class, new CorePsiPackageImplementationHelper());
    registerApplicationService(PsiSubstitutorFactory.class, new PsiSubstitutorFactoryImpl());
    registerApplicationService(JavaClassSupers.class, new JavaClassSupersImpl());
    registerApplicationService(JavaModuleGraphHelper.class, new JavaPsiModuleGraphHelper());
    registerApplicationService(JavaDirectoryService.class, new CoreJavaDirectoryService());
    registerApplicationService(JavaVersionService.class, new JavaVersionService());
    registerApplicationService(PsiSyntaxBuilderFactory.class, new PsiSyntaxBuilderFactoryImpl());
    registerApplicationDynamicExtensionPoint("com.intellij.psi.classFileDecompiler", ClassFileDecompilers.Decompiler.class);
    addExtension(ClassFileDecompilers.STATIC_EP_NAME, new ClsDecompilerImpl());
    BinaryFileTypeDecompilers.getInstance();
  }

  @Override
  protected MockApplication createApplication(Disposable owner) {
    return new MockApplication(owner) {
      @Override
      public boolean isUnitTestMode() {
        return false;
      }

      @Override
      public void invokeLater(Runnable action) {
        action.run();
      }

      @Override
      public void invokeLater(Runnable action, ModalityState state) {
        action.run();
      }

      @Override
      public void invokeLater(Runnable action, Condition<?> expired) {
        if (!expired.value(null)) {
          action.run();
        }
      }

      @Override
      public void invokeLater(Runnable action, ModalityState state, Condition<?> expired) {
        invokeLater(action, expired);
      }
    };
  }

  static synchronized JavaPsiApplication acquire() {
    var current = ApplicationManager.getApplication();
    if (current != null && (shared == null || current != shared.getApplication())) {
      throw new IllegalStateException("Cannot replace an existing IntelliJ application");
    }
    if (shared == null) {
      PathManager.setExplicitConfigPath(Path.of(System.getProperty("java.io.tmpdir"), "sonar-java-psi-" + UUID.randomUUID()));
      var owner = Disposer.newDisposable("SonarJava PSI application");
      try {
        shared = new JavaPsiApplication(owner);
      } catch (RuntimeException | Error failure) {
        try {
          Disposer.dispose(owner);
        } catch (RuntimeException | Error cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        } finally {
          ApplicationManager.setApplication(null);
        }
        throw failure;
      }
    }
    users++;
    return shared;
  }

  static synchronized void release() {
    if (--users == 0) {
      var previous = shared;
      shared = null;
      try {
        ((CoreJarFileSystem) previous.getJarFileSystem()).clearHandlersCache();
        Disposer.dispose(previous.getParentDisposable());
      } finally {
        ApplicationManager.setApplication(null);
      }
    }
  }

  static void registerExtensionPoints(ExtensionsArea area, boolean project) {
    for (var resource : DESCRIPTORS) {
      try (var input = JavaPsiApplication.class.getClassLoader().getResourceAsStream(resource)) {
        if (input == null) {
          throw new IllegalStateException("Missing PSI descriptor: " + resource);
        }
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(input);
        var points = document.getElementsByTagName("extensionPoint");
        for (int i = 0; i < points.getLength(); i++) {
          var point = (Element) points.item(i);
          if (project != "IDEA_PROJECT".equals(point.getAttribute("area"))) {
            continue;
          }
          var name = point.hasAttribute("qualifiedName") ? point.getAttribute("qualifiedName") : "com.intellij." + point.getAttribute("name");
          if (!area.hasExtensionPoint(name)) {
            var bean = point.hasAttribute("beanClass");
            area.registerExtensionPoint(name, point.getAttribute(bean ? "beanClass" : "interface"),
              bean ? ExtensionPoint.Kind.BEAN_CLASS : ExtensionPoint.Kind.INTERFACE);
          }
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException e) {
        throw new IllegalStateException("Cannot register PSI extensions", e);
      }
    }
  }
}
