package checks;

import checks.s5128.CrossPackageApi;
import checks.s5128.CrossPackageDefinition;
import checks.s5128.CrossPackageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class ValidatedBean {
  @NotBlank
  String title;
}

class CrossPackageController implements CrossPackageApi {
  @Override
  public void create(CrossPackageRequest body) { // Compliant, @Valid is inherited from an interface resolved from bytecode
    System.out.println(body.definition());
  }
}

class CrossPackageConverter {
  public String convert(CrossPackageDefinition definition) { // Noncompliant {{Add missing "@Valid" on "definition" to validate it with "Bean Validation".}}
//                      ^^^^^^^^^^^^^^^^^^^^^^
    return definition.title();
  }
}

interface LocalApi {
  void create(@Valid ValidatedBean body);
}

class LocalApiImpl implements LocalApi {
  @Override
  public void create(ValidatedBean body) { // Compliant, @Valid is inherited from the interface
  }
}

abstract class AbstractHandler {
  public abstract void handle(@Valid ValidatedBean bean);
}

class ConcreteHandler extends AbstractHandler {
  @Override
  public void handle(ValidatedBean bean) { // Compliant, @Valid is inherited from the abstract class
  }
}

interface BaseProcessApi {
  void process(@Valid ValidatedBean bean);
}

abstract class MidProcessor implements BaseProcessApi {
}

class LeafProcessor extends MidProcessor {
  @Override
  public void process(ValidatedBean bean) { // Compliant, @Valid is inherited transitively
  }
}

interface GenericApi<T> {
  void handle(@Valid T payload);
}

class GenericApiImpl implements GenericApi<ValidatedBean> {
  @Override
  public void handle(ValidatedBean payload) { // Compliant, @Valid is inherited from the generic interface
  }
}

interface PartiallyValidatedApi {
  void handle(ValidatedBean unvalidated, @Valid ValidatedBean validated);
}

class PartiallyValidatedImpl implements PartiallyValidatedApi {
  @Override
  public void handle(ValidatedBean unvalidated, ValidatedBean validated) { // Noncompliant {{Add missing "@Valid" on "unvalidated" to validate it with "Bean Validation".}}
//                   ^^^^^^^^^^^^^
  }
}

interface PlainApi {
  void store(ValidatedBean bean);
}

class PlainApiImpl implements PlainApi {
  @Override
  public void store(ValidatedBean bean) { // Noncompliant {{Add missing "@Valid" on "bean" to validate it with "Bean Validation".}}
//                  ^^^^^^^^^^^^^
  }
}
