package checks.quarkus;

import jakarta.inject.Singleton;

// No io.quarkus.* import: rule should not trigger
@Singleton
class NonQuarkusSingleton {
}
