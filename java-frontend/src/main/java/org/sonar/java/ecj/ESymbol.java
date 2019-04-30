package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@MethodsAreNonnullByDefault
abstract class ESymbol implements Symbol {
  final Ctx ast;
  final IBinding binding;

  ESymbol(Ctx ast, IBinding binding) {
    this.ast = Objects.requireNonNull(ast);
    this.binding = Objects.requireNonNull(binding);
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * @see JavaSymbol.VariableJavaSymbol#toString()
   * @see JavaSymbol.MethodJavaSymbol#toString()
   * @see JavaSymbol.TypeJavaSymbol#toString()
   */
  @Override
  public final String toString() {
    switch (binding.getKind()) {
      case IBinding.VARIABLE:
        return owner().name() + "#" + name();
      case IBinding.METHOD:
        return owner().name() + "#" + name() + "()";
      case IBinding.TYPE:
        return name();
      default:
        throw new UnexpectedAccessException();
    }
  }

  /**
   * @see JavaSymbol.MethodJavaSymbol#isConstructor() name of constructor
   */
  @Override
  public final String name() {
    if (binding.getKind() == IBinding.METHOD && ((IMethodBinding) binding).isConstructor()) {
      return "<init>";
    }

    if (binding.getKind() == IBinding.TYPE) {
      return ((ITypeBinding) binding).getErasure().getName();
    }

    return binding.getName();
  }

  @Override
  public final Symbol owner() {
    if (binding.getKind() == IBinding.VARIABLE) {
      IVariableBinding b = (IVariableBinding) binding;
      ITypeBinding declaringClass = b.getDeclaringClass();
      IMethodBinding declaringMethod = b.getDeclaringMethod();
      if (declaringClass == null && declaringMethod == null) {
        // variable in a static or instance initializer
        // FIXME see HiddenFieldCheck
        return Symbols.unknownSymbol;
      }
      if (declaringMethod != null) {
        // local variable
        return ast.methodSymbol(declaringMethod);
      }
      // field
      return ast.typeSymbol(b.getDeclaringClass());

    } else if (binding.getKind() == IBinding.METHOD) {
      IMethodBinding b = (IMethodBinding) binding;

      if (b.getDeclaringClass().isAnonymous() && b.isConstructor()) {
        // TODO
        // see constructor in FileHandlingCheck: new FileReader("") {}
        // and method of interface in HostnameVerifierImplementationCheck
        return ast.typeSymbol(b.getDeclaringClass().getSuperclass());
      }

      return ast.typeSymbol(b.getDeclaringClass());

    } else if (binding.getKind() == IBinding.TYPE) {
      ITypeBinding b = (ITypeBinding) binding;
      IMethodBinding declaringMethod = b.getDeclaringMethod();
      if (declaringMethod != null) {
        // local class
        return ast.methodSymbol(declaringMethod);
      }
      ITypeBinding declaringClass = b.getDeclaringClass();
      if (declaringClass == null) {
        // TODO e.g. owner of top-level classes should be b.getPackage() , otherwise NPE in UtilityClassWithPublicConstructorCheck ?
        return new ESymbol(ast, b.getPackage()) {
          @Nullable
          @Override
          public TypeSymbol enclosingClass() {
            return null;
          }
        };
      }
      return ast.typeSymbol(declaringClass);

    } else {
      throw new NotImplementedException("kind: " + binding.getKind());
    }
  }

  @Override
  public final Type type() {
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return ast.type((ITypeBinding) binding);
      case IBinding.VARIABLE:
        return ast.type(((IVariableBinding) binding).getType());
      case IBinding.PACKAGE:
        return Symbols.unknownType;
      case IBinding.METHOD:
        // TODO METHOD in StandardCharsetsConstantsCheck , RedundantTypeCastCheck and MethodIdenticalImplementationsCheck , PACKAGE in InnerClassTooManyLinesCheck
        throw new NotImplementedException("METHOD");
      default:
        throw new NotImplementedException("Kind: " + binding.getKind());
    }
  }

  @Override
  public final boolean isVariableSymbol() {
    return binding.getKind() == IBinding.VARIABLE;
  }

  @Override
  public final boolean isTypeSymbol() {
    if (binding.isRecovered()) {
      // TODO see SAMAnnotatedCheck
      return false;
    }
    return binding.getKind() == IBinding.TYPE;
  }

  @Override
  public final boolean isMethodSymbol() {
    return binding.getKind() == IBinding.METHOD;
  }

  @Override
  public final boolean isPackageSymbol() {
    return binding.getKind() == IBinding.PACKAGE;
  }

  @Override
  public final boolean isStatic() {
    return Modifier.isStatic(binding.getModifiers());
  }

  @Override
  public final boolean isFinal() {
    return Modifier.isFinal(binding.getModifiers());
  }

  @Override
  public final boolean isEnum() {
    switch (binding.getKind()) {
      case IBinding.VARIABLE:
        return ((IVariableBinding) binding).isEnumConstant();
      case IBinding.TYPE:
        return ((ITypeBinding) binding).isEnum();
      default:
        return false;
    }
  }

  @Override
  public final boolean isInterface() {
    return binding.getKind() == IBinding.TYPE
      && ((ITypeBinding) binding).isInterface();
  }

  @Override
  public final boolean isAbstract() {
    return Modifier.isAbstract(binding.getModifiers());
  }

  @Override
  public final boolean isPublic() {
    return Modifier.isPublic(binding.getModifiers());
  }

  @Override
  public final boolean isPrivate() {
    return Modifier.isPrivate(binding.getModifiers());
  }

  @Override
  public final boolean isProtected() {
    return Modifier.isProtected(binding.getModifiers());
  }

  @Override
  public final boolean isPackageVisibility() {
    return !isPublic() && !isProtected() && !isPrivate();
  }

  @Override
  public final boolean isDeprecated() {
    return binding.isDeprecated();
  }

  @Override
  public final boolean isVolatile() {
    return Modifier.isVolatile(binding.getModifiers());
  }

  @Override
  public final boolean isUnknown() {
    return false;
  }

  @Override
  public final SymbolMetadata metadata() {
    return new SymbolMetadata() {
      @Override
      public boolean isAnnotatedWith(String fullyQualifiedName) {
        for (IAnnotationBinding a : binding.getAnnotations()) {
          if (fullyQualifiedName.equals(a.getAnnotationType().getBinaryName())) {
            return true;
          }
        }
        return false;
      }

      @CheckForNull
      @Override
      public List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
        // FIXME note that AnnotationValue.value can be Tree
        return Collections.emptyList();
      }

      @Override
      public List<AnnotationInstance> annotations() {
        return Arrays.stream(binding.getAnnotations())
          .map(x -> new EAnnotationInstance(ast, x))
          .collect(Collectors.toList());
      }
    };
  }

  @Override
  public final List<IdentifierTree> usages() {
    List<IdentifierTree> result = ast.usages.get(binding);
    return result == null ? Collections.emptyList() : result;
  }

  @Nullable
  @Override
  public Tree declaration() {
    return ast.declarations.get(binding);
  }
}

@MethodsAreNonnullByDefault
class EAnnotationInstance implements SymbolMetadata.AnnotationInstance {
  private final Ctx ast;
  private final IAnnotationBinding binding;

  EAnnotationInstance(Ctx ast, IAnnotationBinding binding) {
    this.ast = ast;
    this.binding = binding;
  }

  @Override
  public Symbol symbol() {
    return ast.typeSymbol(binding.getAnnotationType());
  }

  @Override
  public List<SymbolMetadata.AnnotationValue> values() {
    // FIXME
    return Collections.emptyList();
  }
}

@MethodsAreNonnullByDefault
class ETypeSymbol extends ESymbol implements Symbol.TypeSymbol {
  private final ITypeBinding binding;

  /**
   * Use {@link Ctx#typeSymbol(ITypeBinding)}
   */
  ETypeSymbol(Ctx ast, ITypeBinding binding) {
    super(ast, binding);
    this.binding = binding;
  }

  @CheckForNull
  @Override
  public Type superClass() {
    if ("java.lang.Object".equals(binding.getQualifiedName())) {
      return null;
    }
    if (binding.getSuperclass() == null) {
      return Symbols.unknownType;
    }
    return ast.type(binding.getSuperclass());
  }

  @Override
  public List<Type> interfaces() {
    return Arrays.stream(binding.getInterfaces())
      .map(ast::type)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<Symbol> memberSymbols() {
    Collection<Symbol> members = new ArrayList<>();
    ITypeBinding typeBinding = binding;
    for (ITypeBinding b : typeBinding.getDeclaredTypes()) {
      members.add(ast.typeSymbol(b));
    }
    for (IVariableBinding b : typeBinding.getDeclaredFields()) {
      members.add(ast.variableSymbol(b));
    }
    for (IMethodBinding b : typeBinding.getDeclaredMethods()) {
      members.add(ast.methodSymbol(b));
    }
    // TODO old implementation also provides "this" and "super" - see AbstractClassWithoutAbstractMethodCheck
    return members;
  }

  @Override
  public Collection<Symbol> lookupSymbols(String name) {
    throw new UnexpectedAccessException();
  }

  @Nullable
  @Override
  public TypeSymbol enclosingClass() {
    // FIXME Godin: likely incorrect
    ITypeBinding declaringClass = binding.getDeclaringClass();
    if (declaringClass == null) {
      return null;
    }
    return ast.typeSymbol(declaringClass);
  }

  @Nullable
  @Override
  public ClassTree declaration() {
    return (ClassTree) super.declaration();
  }
}

@MethodsAreNonnullByDefault
class EMethodSymbol extends ESymbol implements Symbol.MethodSymbol {
  final IMethodBinding binding;

  /**
   * Use {@link Ctx#methodSymbol(IMethodBinding)}
   */
  EMethodSymbol(Ctx ast, IMethodBinding binding) {
    super(ast, binding);
    this.binding = binding;
  }

  @Override
  public List<Type> parameterTypes() {
    return Arrays.stream(binding.getParameterTypes())
      .map(ast::type)
      .collect(Collectors.toList());
  }

  @Override
  public TypeSymbol returnType() {
    return ast.typeSymbol(binding.getReturnType());
  }

  @Override
  public List<Type> thrownTypes() {
    return Arrays.stream(binding.getExceptionTypes())
      .map(ast::type)
      .collect(Collectors.toList());
  }

  /**
   * @see JavaSymbol.MethodJavaSymbol#overriddenSymbol()
   */
  @Nullable
  @Override
  public MethodSymbol overriddenSymbol() {
    // TODO what about unresolved?
    IMethodBinding overrides = find(ast, binding::overrides, binding.getDeclaringClass());
    if (overrides != null) {
      return ast.methodSymbol(overrides);
    }
    return null;
  }

  @Nullable
  static IMethodBinding find(Ctx ctx, Predicate<IMethodBinding> predicate, ITypeBinding t) {
    for (IMethodBinding candidate : t.getDeclaredMethods()) {
      if (predicate.test(candidate)) {
        return candidate;
      }
    }
    for (ITypeBinding i : t.getInterfaces()) {
      IMethodBinding r = find(ctx, predicate, i);
      if (r != null) {
        return r;
      }
    }
    if (t.getSuperclass() != null) {
      return find(ctx, predicate, t.getSuperclass());
    } else {
      ITypeBinding objectType = ctx.ast.resolveWellKnownType("java.lang.Object");
      if (t != objectType) {
        return find(ctx, predicate, objectType);
      }
    }
    return null;
  }

  @Override
  public String signature() {
    throw new UnexpectedAccessException();
  }

  @Nullable
  @Override
  public TypeSymbol enclosingClass() {
    return ast.typeSymbol(binding.getDeclaringClass());
  }

  @Nullable
  @Override
  public MethodTree declaration() {
    return (MethodTree) super.declaration();
  }
}
