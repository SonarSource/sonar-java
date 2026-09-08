package checks.spring.context;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = ScanBaseMarker.class)
class SpringBootAppWithScanBasePackageClasses {}

interface ScanBaseMarker {}
