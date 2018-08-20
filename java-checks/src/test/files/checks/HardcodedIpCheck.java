class A {
  String ip = "10.0.0.0"; // Noncompliant [[sc=15;ec=25]] {{Make sure using this hardcoded IP address is safe here.}}
  String ipAndPort = "10.0.0.0:0"; // Noncompliant [[sc=22;ec=34]] {{Make sure using this hardcoded IP address is safe here.}}
  String url = "http://192.168.0.1/admin.html"; // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
  String urlWithPort = "http://192.168.0.1:8000/admin.html"; // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
  String url2 = "http://www.example.org";
  int a = 42;

  String broadcastAddress = "255.255.255.255";
  String loopbackAddress1 = "127.0.0.1";
  String loopbackAddress2 = "127.2.3.4";
  String nonRoutableAddress = "0.0.0.0";

  String notAnIp1 = "0.0.0.1234";
  String notAnIp2 = "1234.0.0.0";
  String notAnIp3 = "1234.0.0.0.0.1234";
  String notAnIp4 = ".10.0.0.0";
  String notAnIp5 = "0.256.0.0";

  String country_oid = "2.5.6.2";
  String subschema_oid = "2.5.20.1";
  String not_considered_as_an_oid = "2.51.6.2"; // Noncompliant

  String empty = "";

  // IPV6 uncompressed format has 8 parts
  String ipV6_1 = "1:a:0:0:0:0:0:0:0";
  String ipV6_2 = "1:a:0:0:0:0:0:0"; // Noncompliant
  String ipV6_3 = "1:a:0:0:0:0:0";
  String ipV6_4 = "a134:ABEF:1231:2312:734f:FAB2:3000:4123"; // Noncompliant
  String ipV6_5 = "[aaaa:AAAA:1111:2222:ffff:FFFF:3333:4444]"; // Noncompliant

  // IPV6 compressed format has up to 7 parts
  String ipV6_a = "1:a:0::0:0:0:0"; // Noncompliant
  String ipV6_b = "1:a:0::0:0:0:0:0";
  String ipV6_c = "0:a::0:0:0:0"; // Noncompliant
  String ipV6_d = "0:a::0:0::0";
  String ipV6_e = "[1::0:0:0:0]"; // Noncompliant
  String ipV6_f = "[1::0:0:0:0";
  String ipV6_g = "::a:b:c:d"; // Noncompliant
  String ipV6_h = "a:b:c:d::"; // Noncompliant
  String ipV6_i = "[ffff::]"; // Noncompliant
  String ipV6_j = "::ffff"; // Noncompliant

  // IPV6 part has 1 to 4 hexa
  String ipV6_k = "::A"; // Noncompliant
  String ipV6_l = "::A3"; // Noncompliant
  String ipV6_m = "::A3D"; // Noncompliant
  String ipV6_n = "::FOO";
  String ipV6_o = "::A3D7"; // Noncompliant
  String ipV6_p = "::AXDX";
  String ipV6_q = "::A3D79";

  // IPV6 in url
  String ipV6_r = "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html"; // Noncompliant
  String ipV6_s = "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]"; // Noncompliant
  String ipV6_t = "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]/"; // Noncompliant
  String ipV6_u = "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80"; // Noncompliant
  String ipV6_v = "prefixed_[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]";
  String ipV6_w = "[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]_suffixed";

  // IPV6 loopback
  String loopbackAddressV6_1 = "::1";
  String loopbackAddressV6_2 = "0:0:0:0:0:0:0:1";
  String loopbackAddressV6_3 = "0:0::0:1";
  String loopbackAddressV6_4 = "0000:0000:0000:0000:0000:0000:0000:0001";

  // IPV6 non routable
  String nonRoutableAddressV6_1 = "::";
  String nonRoutableAddressV6_2 = "0:0:0:0:0:0:0:0";
  String nonRoutableAddressV6_3 = "0::0";
  String nonRoutableAddressV6_4 = "0000:0000:0000:0000:0000:0000:0000:0000";

  String fileName = "v0.0.1.200__do_something.sql"; // Compliant - suffixed and prefixed
  String version = "1.0.0.0-1"; // Compliant - suffixed
}
