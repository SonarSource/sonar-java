package org.sonar.java.ecj;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
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
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
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
        throw new NotImplementedException("variable in a static or instance initializer");
      }
      if (declaringMethod != null) {
        // local variable
        return ast.methodSymbol(declaringMethod);
      }
      // field
      return ast.typeSymbol(b.getDeclaringClass());

    } else if (binding.getKind() == IBinding.METHOD) {
      // TODO what about FileHandlingCheck: new FileReader("") {}

      IMethodBinding b = (IMethodBinding) binding;
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
    if (isTypeSymbol()) {
      return ast.type((ITypeBinding) binding);
    }
    if (isVariableSymbol()) {
      return ast.type(((IVariableBinding) binding).getType());
    }
    // TODO Method in StandardCharsetsConstantsCheck and RedundantTypeCastCheck
    throw new NotImplementedException("Kind: " + binding.getKind());
  }

  @Override
  public final boolean isVariableSymbol() {
    return binding.getKind() == IBinding.VARIABLE;
  }

  @Override
  public final boolean isTypeSymbol() {
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
class EVariableSymbol extends ESymbol implements Symbol.VariableSymbol {
  /**
   * Use {@link Ctx#variableSymbol(IVariableBinding)}
   */
  EVariableSymbol(Ctx ast, IVariableBinding binding) {
    super(ast, binding);
  }

  @Nullable
  @Override
  public TypeSymbol enclosingClass() {
    IVariableBinding b = (IVariableBinding) binding;
    ITypeBinding declaringClass = b.getDeclaringClass();
    // FIXME Godin: likely incorrect for local variables
    if (declaringClass == null) {
      throw new NotImplementedException();
    }
    return ast.typeSymbol(declaringClass);
  }

  @Nullable
  @Override
  public VariableTree declaration() {
    return (VariableTree) super.declaration();
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
  private final IMethodBinding binding;

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
    IMethodBinding overrides = find(binding::overrides, binding.getDeclaringClass());
    if (overrides != null) {
      return ast.methodSymbol(overrides);
    }
    return null;
  }

  @Nullable
  static IMethodBinding find(Predicate<IMethodBinding> predicate, ITypeBinding t) {
    for (IMethodBinding candidate : t.getDeclaredMethods()) {
      if (predicate.test(candidate)) {
        return candidate;
      }
    }
    for (ITypeBinding i : t.getInterfaces()) {
      IMethodBinding r = find(predicate, i);
      if (r != null) {
        return r;
      }
    }
    if (t.getSuperclass() != null) {
      return find(predicate, t.getSuperclass());
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

@MethodsAreNonnullByDefault
class EType implements Type {
  private final Ctx ast;
  private final ITypeBinding typeBinding;

  /**
   * Use {@link Ctx#type(ITypeBinding)}
   */
  EType(Ctx ast, ITypeBinding typeBinding) {
    this.ast = Objects.requireNonNull(ast);
    this.typeBinding = Objects.requireNonNull(typeBinding);
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    // TODO unsure about erasure, but it allowed to pass GetClassLoaderCheckTest
    return fullyQualifiedName.equals(
      typeBinding.getErasure().getQualifiedName()
    );
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    // for example "byte" in IntegerToHexStringCheck
    ITypeBinding type = ast.ast.resolveWellKnownType(fullyQualifiedName);

    if (type == null) {
      type = findType(ast.ast, fullyQualifiedName);
    }
    return typeBinding.isSubTypeCompatible(type);
  }

  private static ITypeBinding findType(AST ast, String fqn) {
    try {
      // BindingResolver bindingResolver = ast.getBindingResolver();
      // ReferenceBinding referenceBinding = bindingResolver
      //  .lookupEnvironment()
      //  .getType(CharOperation.splitOn('.', fqn.toCharArray()));

      Method methodGetBindingResolver = ast.getClass().getDeclaredMethod("getBindingResolver");
      methodGetBindingResolver.setAccessible(true);
      Object bindingResolver = methodGetBindingResolver.invoke(ast);

      Method methodLookupEnvironment = bindingResolver.getClass().getDeclaredMethod("lookupEnvironment");
      methodLookupEnvironment.setAccessible(true);
      LookupEnvironment lookupEnvironment = (LookupEnvironment) methodLookupEnvironment.invoke(bindingResolver);

      ReferenceBinding referenceBinding = lookupEnvironment.getType(CharOperation.splitOn('.', fqn.toCharArray()));

      Method methodGetTypeBinding = bindingResolver.getClass().getDeclaredMethod("getTypeBinding", TypeBinding.class);
      methodGetTypeBinding.setAccessible(true);
      return (ITypeBinding) methodGetTypeBinding.invoke(bindingResolver, referenceBinding);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    return isSubtypeOf(superType.fullyQualifiedName());
  }

  @Override
  public boolean isArray() {
    return typeBinding.isArray();
  }

  @Override
  public boolean isClass() {
    return typeBinding.isClass();
  }

  @Override
  public boolean isVoid() {
    return "void".equals(typeBinding.getName());
  }

  @Override
  public boolean isPrimitive() {
    return typeBinding.isPrimitive();
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return isPrimitive() && primitive.name().toLowerCase().equals(typeBinding.getName());
  }

  @Override
  public boolean isUnknown() {
    return typeBinding.isRecovered();
  }

  @Override
  public boolean isNumerical() {
    // Godin: suboptimal
    return isPrimitive(Primitives.BYTE)
      || isPrimitive(Primitives.CHAR)
      || isPrimitive(Primitives.SHORT)
      || isPrimitive(Primitives.INT)
      || isPrimitive(Primitives.LONG)
      || isPrimitive(Primitives.FLOAT)
      || isPrimitive(Primitives.DOUBLE);
  }

  /**
   * TODO typeBinding.getBinaryName() for ThrowsSeveralCheckedExceptionCheck ?
   *
   * @see JavaSymbol.TypeJavaSymbol#getFullyQualifiedName()
   */
  @Override
  public String fullyQualifiedName() {
    return typeBinding.getErasure().getQualifiedName();
  }

  @Override
  public String name() {
    // TODO unsure about erasure, but it allowed to pass ValueBasedObjectUsedForLockCheck
    return typeBinding.getErasure().getName();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return ast.typeSymbol(typeBinding);
  }

  @Override
  public Type erasure() {
    throw new UnexpectedAccessException();
  }

  /**
   * @see JavaType#toString()
   */
  @Override
  public final String toString() {
    return symbol().toString();
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }
}
