/**
 * In Java SE 7 and later, any number of underscore characters (_) can appear anywhere <strong>between</strong> digits in a numerical literal.
 */
class UnderscoresInNumericLiterals {

  public void example() {
    int oneMillion = 1000000;
    int oneMillionWithUnderscores = 1_000_000;

    long maxLong = 0x7fff_ffff_ffff_ffffL;

    float pi = 3.14_15F;

    double exp = 1_234e1_0;
    double maxDouble = 0x1.ffff_ffff_ffff_fP1_023;

    // cannot put underscores adjacent to a decimal point
    // float pi1 = 3_.1415F;

    // cannot put underscores adjacent to a decimal point
    // float pi2 = 3._1415F;

    // cannot put underscores prior to an L suffix
    // long socialSecurityNumber1 = 999_99_9999_L;

    // This is an identifier, not a numeric literal
    // int x1 = _51;

    // cannot put underscores at the end of a literal
    // int x2 = 52_;

    int x3 = 5_______2;

    // cannot put underscores in the 0x radix prefix
    // int x4 = 0_x52;

    // cannot put underscores at the beginning of a number
    // int x5 = 0x_52;

    // cannot put underscores at the end of a number
    // int x6 = 0x52_;
  }

}
