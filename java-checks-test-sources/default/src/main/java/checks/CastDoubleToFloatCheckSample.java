package checks;

class CastDoubleToFloatCheckSample {

  double someDouble = 1.0;

  double getDouble() {
    return 1.0;
  }

  void noncompliant() {
    float a = (float) 3.14; // Noncompliant {{Use a float literal instead of casting a double literal to float.}} [[quickfixes=qf1]]
//            ^^^^^^^^^^^^
    // fix@qf1 {{Replace with a float literal}}
    // edit@qf1 [[sc=15;ec=27]] {{3.14f}}

    float b = (float) 0.5; // Noncompliant [[quickfixes=qf2]]
//            ^^^^^^^^^^^
    // fix@qf2 {{Replace with a float literal}}
    // edit@qf2 [[sc=15;ec=26]] {{0.5f}}

    float c = (float) 1.0; // Noncompliant [[quickfixes=qf3]]
//            ^^^^^^^^^^^
    // fix@qf3 {{Replace with a float literal}}
    // edit@qf3 [[sc=15;ec=26]] {{1.0f}}

    float d = (float) 3.14159; // Noncompliant [[quickfixes=qf4]]
//            ^^^^^^^^^^^^^^^
    // fix@qf4 {{Replace with a float literal}}
    // edit@qf4 [[sc=15;ec=30]] {{3.14159f}}

    float e = (float) 1e10; // Noncompliant [[quickfixes=qf5]]
//            ^^^^^^^^^^^^
    // fix@qf5 {{Replace with a float literal}}
    // edit@qf5 [[sc=15;ec=27]] {{1e10f}}

    float f = (float) 1.5e-3; // Noncompliant [[quickfixes=qf6]]
//            ^^^^^^^^^^^^^^
    // fix@qf6 {{Replace with a float literal}}
    // edit@qf6 [[sc=15;ec=29]] {{1.5e-3f}}

    float g = (float) .5; // Noncompliant [[quickfixes=qf7]]
//            ^^^^^^^^^^
    // fix@qf7 {{Replace with a float literal}}
    // edit@qf7 [[sc=15;ec=25]] {{.5f}}

    float h = (float) 3.14d; // Noncompliant [[quickfixes=qf8]]
//            ^^^^^^^^^^^^^
    // fix@qf8 {{Replace with a float literal}}
    // edit@qf8 [[sc=15;ec=28]] {{3.14f}}

    float i = (float) 3.14D; // Noncompliant [[quickfixes=qf9]]
//            ^^^^^^^^^^^^^
    // fix@qf9 {{Replace with a float literal}}
    // edit@qf9 [[sc=15;ec=28]] {{3.14f}}

    float j = (float) (3.14); // Noncompliant [[quickfixes=qf10]]
//            ^^^^^^^^^^^^^^
    // fix@qf10 {{Replace with a float literal}}
    // edit@qf10 [[sc=15;ec=29]] {{3.14f}}

    float k = (float) 0x1.0p10; // Noncompliant [[quickfixes=qf11]]
//            ^^^^^^^^^^^^^^^^
    // fix@qf11 {{Replace with a float literal}}
    // edit@qf11 [[sc=15;ec=31]] {{0x1.0p10f}}

    float l = (float) -0.5; // Noncompliant [[quickfixes=qf12]]
//            ^^^^^^^^^^^^
    // fix@qf12 {{Replace with a float literal}}
    // edit@qf12 [[sc=15;ec=27]] {{-0.5f}}

    float m = (float) +3.14; // Noncompliant [[quickfixes=qf13]]
//            ^^^^^^^^^^^^^
    // fix@qf13 {{Replace with a float literal}}
    // edit@qf13 [[sc=15;ec=28]] {{3.14f}}

    float n = (float) -0.0; // Noncompliant [[quickfixes=qf14]]
//            ^^^^^^^^^^^^
    // fix@qf14 {{Replace with a float literal}}
    // edit@qf14 [[sc=15;ec=27]] {{-0.0f}}
  }

  void compliant() {
    float a = 3.14f;
    float b = 3.14F;
    float c = (float) someDouble;
    float d = (float) getDouble();
    float e = (float) (someDouble + 1.0);
    float f = (float) 42;
    float g = (float) 42L;
    int h = (int) 3.14;
    double i = (double) 3.14f;
    float j = (float) Math.PI;
    float k = (float) 1e300; // compliant - overflow, 1e300f is not a valid float literal
    float l = (float) 1e-46; // compliant - underflow, 1e-46f would be zero but 1e-46 is not
    float m = (float) 1.000000059604644775390625001; // compliant - double rounding difference
  }
}
