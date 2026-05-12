class A {

  LocalDate date = LocalDate.of(2024, 1, 15); // Noncompliant {{Use a Month enum instead of this numeric value}}
  //                                  ^
  LocalDateTime dateTime = LocalDateTime.of(2024, 10, 15, 1, 20); // Noncompliant {{Use a Month enum instead of this numeric value}}
  //                                              ^^
  OffsetDateTime offsetDateTime = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC) // Noncompliant {{Use a Month enum instead of this numeric value}}
  //                                                      ^
  ZonedDateTime zdt = ZonedDateTime.of(2026, 12, 21, 3, 33, 33, 4, ZoneId.of("Europe/Paris")); // Noncompliant {{Use a Month enum instead of this numeric value}}
  //                                         ^^
  YearMonth ym = YearMonth.of(3,25); // Noncompliant
  //                          ^
  MonthDay md = MonthDay.of(2, 16); // Noncompliant

  if (date.getMonthValue() == 9) { // Noncompliant
    // Handle September
  }
}
