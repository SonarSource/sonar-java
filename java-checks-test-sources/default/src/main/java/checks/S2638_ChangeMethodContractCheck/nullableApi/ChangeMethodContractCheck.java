package checks.S2638_ChangeMethodContractCheck.nullableApi;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

class ChangeMethodContractCheck {
  void nullableArguments(Object a) { }

  void argAnnotatedNonNull(@Nonnull Object a) { }
}

class ChangeMethodContractCheck_Child extends ChangeMethodContractCheck {
  @Override
  void nullableArguments(@javax.annotation.Nonnull Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @Nonnull to honor @ParametersAreNullableByDefault at package level of the overridden method.}}
//                                                        ^
//  ^^^<

  @Override
  void argAnnotatedNonNull(Object a) { } // Nonnull to Nullable is compliant
}

@ParametersAreNonnullByDefault
//  ^^^<
class ChangeMethodContractCheck_Child_Annotated extends ChangeMethodContractCheck {
  @Override
  void nullableArguments(Object a) { } // Noncompliant {{Fix the incompatibility of the annotation @ParametersAreNonnullByDefault at class level to honor @ParametersAreNullableByDefault at package level of the overridden method.}}

  @Override
  void argAnnotatedNonNull(Object a) { } // Compliant: Nonnull to Nonnull
}
