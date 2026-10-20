package checks.s5128;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CrossPackageRequest(@NotNull @Valid CrossPackageDefinition definition) {
}
