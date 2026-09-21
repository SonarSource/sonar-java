package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
  scanBasePackages = {PackageNames.EXTRA, PackageNames.CONTROLLER},
  scanBasePackageClasses = ServiceMarker.class)
class SpringBootAppWithMixedScanAttributes {
}

final class PackageNames {
  static final String EXTRA = "com.example.extra";
  static final String CONTROLLER = "com.example.controller";
}

interface ServiceMarker {
}
