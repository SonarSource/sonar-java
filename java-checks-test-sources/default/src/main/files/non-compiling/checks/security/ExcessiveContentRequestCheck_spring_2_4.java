package checks.security;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import static org.springframework.util.unit.DataUnit.TERABYTES;

public class ExcessiveContentRequestCheck_spring_2_4 {


  void springMultipartConfigFactory() {
    MultipartConfigFactory factory = new MultipartConfigFactory();

    factory.setMaxRequestSize(DataSize.ofBytes(8388609)); // Noncompliant {{The content length limit of 8388609 bytes is greater than the defined limit of 8388608; make sure it is safe here.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    factory.setMaxRequestSize(DataSize.ofBytes(8388608)); // Compliant
    factory.setMaxRequestSize(DataSize.ofBytes(8000000)); // Compliant

    factory.setMaxRequestSize(DataSize.ofKilobytes(8193)); // Noncompliant
    factory.setMaxRequestSize(DataSize.ofKilobytes(8192)); // Compliant
    factory.setMaxRequestSize(DataSize.ofKilobytes(8000)); // Compliant

    factory.setMaxRequestSize(DataSize.ofMegabytes(9)); // Noncompliant
    factory.setMaxRequestSize(DataSize.ofMegabytes(8)); // Compliant

    factory.setMaxRequestSize(DataSize.ofGigabytes(1)); // Noncompliant

    factory.setMaxRequestSize(DataSize.ofTerabytes(1)); // Noncompliant

    factory.setMaxRequestSize(DataSize.of(8388609, DataUnit.BYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.of(8388608, DataUnit.BYTES)); // Compliant
    factory.setMaxRequestSize(DataSize.of(8193, DataUnit.KILOBYTES)); // Noncompliant {{The content length limit of 8389632 bytes is greater than the defined limit of 8388608; make sure it is safe here.}}
    factory.setMaxRequestSize(DataSize.of(8192, DataUnit.KILOBYTES)); // Compliant
    factory.setMaxRequestSize(DataSize.of(8000, DataUnit.KILOBYTES)); // Compliant
    factory.setMaxRequestSize(DataSize.of(9, DataUnit.MEGABYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.of(8, DataUnit.MEGABYTES)); // Compliant
    factory.setMaxRequestSize(DataSize.of(1, DataUnit.GIGABYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.of(1, DataUnit.TERABYTES)); // Noncompliant

    factory.setMaxFileSize(DataSize.parse("8388609")); // Noncompliant {{The content length limit of 8388609 bytes is greater than the defined limit of 8388608; make sure it is safe here.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    factory.setMaxFileSize(DataSize.parse("8388609B")); // Noncompliant
    factory.setMaxFileSize(DataSize.parse("9000KB")); // Noncompliant
    factory.setMaxFileSize(DataSize.parse("8193KB")); // Noncompliant {{The content length limit of 8389632 bytes is greater than the defined limit of 8388608; make sure it is safe here.}}
    factory.setMaxFileSize(DataSize.parse("9MB")); // Noncompliant
    factory.setMaxFileSize(DataSize.parse("1GB")); // Noncompliant
    factory.setMaxFileSize(DataSize.parse("1TB")); // Noncompliant {{The content length limit of 1099511627776 bytes is greater than the defined limit of 8388608; make sure it is safe here.}}

    factory.setMaxFileSize(DataSize.parse("8388608")); // Compliant
    factory.setMaxFileSize(DataSize.parse("8388608B")); // Compliant
    factory.setMaxFileSize(DataSize.parse("8192KB")); // Compliant
    factory.setMaxFileSize(DataSize.parse("8000KB")); // Compliant
    factory.setMaxFileSize(DataSize.parse("900KB")); // Compliant
    factory.setMaxFileSize(DataSize.parse("9KB")); // Compliant
    factory.setMaxFileSize(DataSize.parse("8MB")); // Compliant

    factory.setMaxRequestSize(DataSize.parse("8388609", DataUnit.BYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.parse("8388608", DataUnit.BYTES)); // Compliant
    factory.setMaxRequestSize(DataSize.parse("8193", DataUnit.KILOBYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.parse("8192", DataUnit.KILOBYTES)); // Compliant
    factory.setMaxRequestSize(DataSize.parse("9", DataUnit.MEGABYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.parse("8", DataUnit.MEGABYTES)); // Compliant
    factory.setMaxRequestSize(DataSize.parse("1", DataUnit.GIGABYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.parse("1", DataUnit.TERABYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.parse("1", TERABYTES)); // Noncompliant
    factory.setMaxRequestSize(DataSize.parse("1", org.springframework.util.unit.DataUnit.TERABYTES)); // Noncompliant

    factory.setMaxFileSize(DataSize.parse("+8MB")); // Compliant
    factory.setMaxFileSize(DataSize.parse("+9MB")); // Noncompliant
    factory.setMaxFileSize(DataSize.parse("-9MB")); // Compliant

    factory.setMaxRequestSize(getDataSize()); // Compliant, no cross function resolution
    factory.setMaxFileSize(DataSize.parse("0.1TB")); // Compliant, not a valid datasize

    factory.setMaxRequestSize(DataSize.parse("8388609", getDataUnit())); // Compliant, does not compile
  }

  private DataSize getDataSize() {
    return DataSize.parse("8388608");
  }

  private DataUnit getDataUnit() {
    return DataUnit.TERABYTES;
  }

}
