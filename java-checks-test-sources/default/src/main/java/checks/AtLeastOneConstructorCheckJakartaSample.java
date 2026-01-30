package checks;

import jakarta.inject.Inject;

class InjectedService {
}

class JakartaInject {
  @jakarta.inject.Inject
  private InjectedService myService; // Compliant, fields annotated with Jakarta's @Inject should be ignored.
}

class JakartaImportedInject {
  @Inject
  private InjectedService myService; // Compliant, fields annotated with Jakarta's @Inject should be ignored.
}
