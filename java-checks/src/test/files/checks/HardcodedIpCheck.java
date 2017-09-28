class A {
  String ip = "0.0.0.0"; // Noncompliant [[sc=15;ec=24]] {{Make this IP "0.0.0.0" address configurable.}}
  String url = "http://192.168.0.1/admin.html"; // Noncompliant {{Make this IP "192.168.0.1" address configurable.}}
  String url2 = "http://www.example.org";
  int a = 42;
  String notAnIp1 = "0.0.0.1234";
  String notAnIp2 = "1234.0.0.0";
  String notAnIp3 = "1234.0.0.0.0.1234";
  String notAnIp4 = ".0.0.0.0";
  String notAnIp5 = "0.256.0.0";

  String fileName = "v0.0.1.200__do_something.sql"; // Compliant - suffixed and prefixed
  String version = "1.0.0.0-1"; // Compliant - suffixed
}
