package checks.S2638_ChangeMethodContractCheck.nullableApi;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

class ChangeMethodContractCheckSample {
  void nullableArguments(Object a) { }

  void argAnnotatedNonNull(@Nonnull Object a) { }
}

class ChangeMethodContractCheckSample_Child extends ChangeMethodContractCheckSample {
  @Override
  void nullableArguments(@javax.annotation.Nonnull Object a) { } // Noncompliant [[sc=59;ec=60;secondary=+0]] {{Fix the incompatibility of the annotation @Nonnull to honor @ParametersAreNullableByDefault at package level of the overridden method.}}

  @Override
  void argAnnotatedNonNull(Object a) { } // Nonnull to Nullable is compliant
}

@ParametersAreNonnullByDefault
class ChangeMethodContractCheckSample_Child_Annotated extends ChangeMethodContractCheckSample {
  @Override
  void nullableArguments(Object a) { } // Noncompliant [[secondary=-3]] {{Fix the incompatibility of the annotation @ParametersAreNonnullByDefault at class level to honor @ParametersAreNullableByDefault at package level of the overridden method.}}

  @Override
  void argAnnotatedNonNull(Object a) { } // Compliant: Nonnull to Nonnull
}
