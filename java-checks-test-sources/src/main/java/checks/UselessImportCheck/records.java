package checks.UselessImportCheck;

import java.time.LocalDate;
import java.util.List; // Noncompliant

record records(LocalDate localDate) {

}
