package checks.s5128;

import jakarta.validation.Valid;

public interface CrossPackageApi {
  void create(@Valid CrossPackageRequest body);
}