/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.bytecode.se.testdata;

public class MaxRelationBytecode {

  public static boolean isXMLLetter(final char c) {
    // Note that order is very important here.  The search proceeds
    // from lowest to highest values, so that no searching occurs
    // above the character's value.  BTW, the first line is equivalent to:
    // if (c >= 0x0041 && c <= 0x005A) return true;

    if (c < 0x0041) return false;  if (c <= 0x005a) return true;
    if (c < 0x0061) return false;  if (c <= 0x007A) return true;
    if (c < 0x00C0) return false;  if (c <= 0x00D6) return true;
    if (c < 0x00D8) return false;  if (c <= 0x00F6) return true;
    if (c < 0x00F8) return false;  if (c <= 0x00FF) return true;
    if (c < 0x0100) return false;  if (c <= 0x0131) return true;
    if (c < 0x0134) return false;  if (c <= 0x013E) return true;
    if (c < 0x0141) return false;  if (c <= 0x0148) return true;
    if (c < 0x014A) return false;  if (c <= 0x017E) return true;
    if (c < 0x0180) return false;  if (c <= 0x01C3) return true;
    if (c < 0x01CD) return false;  if (c <= 0x01F0) return true;
    if (c < 0x01F4) return false;  if (c <= 0x01F5) return true;
    if (c < 0x01FA) return false;  if (c <= 0x0217) return true;
    if (c < 0x0250) return false;  if (c <= 0x02A8) return true;
    if (c < 0x02BB) return false;  if (c <= 0x02C1) return true;
    if (c == 0x0386) return true;
    if (c < 0x0388) return false;  if (c <= 0x038A) return true;
    if (c == 0x038C) return true;
    if (c < 0x038E) return false;  if (c <= 0x03A1) return true;
    if (c < 0x03A3) return false;  if (c <= 0x03CE) return true;
    if (c < 0x03D0) return false;  if (c <= 0x03D6) return true;
    if (c == 0x03DA) return true;
    if (c == 0x03DC) return true;
    if (c == 0x03DE) return true;
    if (c == 0x03E0) return true;
    if (c < 0x03E2) return false;  if (c <= 0x03F3) return true;
    if (c < 0x0401) return false;  if (c <= 0x040C) return true;
    if (c < 0x040E) return false;  if (c <= 0x044F) return true;
    if (c < 0x0451) return false;  if (c <= 0x045C) return true;
    if (c < 0x045E) return false;  if (c <= 0x0481) return true;
    if (c < 0x0490) return false;  if (c <= 0x04C4) return true;
    if (c < 0x04C7) return false;  if (c <= 0x04C8) return true;
    if (c < 0x04CB) return false;  if (c <= 0x04CC) return true;
    if (c < 0x04D0) return false;  if (c <= 0x04EB) return true;
    if (c < 0x04EE) return false;  if (c <= 0x04F5) return true;
    if (c < 0x04F8) return false;  if (c <= 0x04F9) return true;
    if (c < 0x0531) return false;  if (c <= 0x0556) return true;
    if (c == 0x0559) return true;
    if (c < 0x0561) return false;  if (c <= 0x0586) return true;
    if (c < 0x05D0) return false;  if (c <= 0x05EA) return true;
    if (c < 0x05F0) return false;  if (c <= 0x05F2) return true;
    if (c < 0x0621) return false;  if (c <= 0x063A) return true;
    if (c < 0x0641) return false;  if (c <= 0x064A) return true;
    if (c < 0x0671) return false;  if (c <= 0x06B7) return true;
    if (c < 0x06BA) return false;  if (c <= 0x06BE) return true;
    if (c < 0x06C0) return false;  if (c <= 0x06CE) return true;
    if (c < 0x06D0) return false;  if (c <= 0x06D3) return true;
    if (c == 0x06D5) return true;
    if (c < 0x06E5) return false;  if (c <= 0x06E6) return true;
    if (c < 0x0905) return false;  if (c <= 0x0939) return true;
    if (c == 0x093D) return true;
    if (c < 0x0958) return false;  if (c <= 0x0961) return true;
    if (c < 0x0985) return false;  if (c <= 0x098C) return true;
    if (c < 0x098F) return false;  if (c <= 0x0990) return true;
    if (c < 0x0993) return false;  if (c <= 0x09A8) return true;
    if (c < 0x09AA) return false;  if (c <= 0x09B0) return true;
    if (c == 0x09B2) return true;
    if (c < 0x09B6) return false;  if (c <= 0x09B9) return true;
    if (c < 0x09DC) return false;  if (c <= 0x09DD) return true;
    if (c < 0x09DF) return false;  if (c <= 0x09E1) return true;
    if (c < 0x09F0) return false;  if (c <= 0x09F1) return true;
    if (c < 0x0A05) return false;  if (c <= 0x0A0A) return true;
    if (c < 0x0A0F) return false;  if (c <= 0x0A10) return true;
    if (c < 0x0A13) return false;  if (c <= 0x0A28) return true;
    if (c < 0x0A2A) return false;  if (c <= 0x0A30) return true;
    if (c < 0x0A32) return false;  if (c <= 0x0A33) return true;
    if (c < 0x0A35) return false;  if (c <= 0x0A36) return true;
    if (c < 0x0A38) return false;  if (c <= 0x0A39) return true;
    if (c < 0x0A59) return false;  if (c <= 0x0A5C) return true;
    if (c == 0x0A5E) return true;
    if (c < 0x0A72) return false;  if (c <= 0x0A74) return true;
    if (c < 0x0A85) return false;  if (c <= 0x0A8B) return true;
    if (c == 0x0A8D) return true;
    if (c < 0x0A8F) return false;  if (c <= 0x0A91) return true;
    if (c < 0x0A93) return false;  if (c <= 0x0AA8) return true;
    if (c < 0x0AAA) return false;  if (c <= 0x0AB0) return true;
    if (c < 0x0AB2) return false;  if (c <= 0x0AB3) return true;
    if (c < 0x0AB5) return false;  if (c <= 0x0AB9) return true;
    if (c == 0x0ABD) return true;
    if (c == 0x0AE0) return true;
    if (c < 0x0B05) return false;  if (c <= 0x0B0C) return true;
    if (c < 0x0B0F) return false;  if (c <= 0x0B10) return true;
    if (c < 0x0B13) return false;  if (c <= 0x0B28) return true;
    if (c < 0x0B2A) return false;  if (c <= 0x0B30) return true;
    if (c < 0x0B32) return false;  if (c <= 0x0B33) return true;
    if (c < 0x0B36) return false;  if (c <= 0x0B39) return true;
    if (c == 0x0B3D) return true;
    if (c < 0x0B5C) return false;  if (c <= 0x0B5D) return true;
    if (c < 0x0B5F) return false;  if (c <= 0x0B61) return true;
    if (c < 0x0B85) return false;  if (c <= 0x0B8A) return true;
    if (c < 0x0B8E) return false;  if (c <= 0x0B90) return true;
    if (c < 0x0B92) return false;  if (c <= 0x0B95) return true;
    if (c < 0x0B99) return false;  if (c <= 0x0B9A) return true;
    if (c == 0x0B9C) return true;
    if (c < 0x0B9E) return false;  if (c <= 0x0B9F) return true;
    if (c < 0x0BA3) return false;  if (c <= 0x0BA4) return true;
    if (c < 0x0BA8) return false;  if (c <= 0x0BAA) return true;
    if (c < 0x0BAE) return false;  if (c <= 0x0BB5) return true;
    if (c < 0x0BB7) return false;  if (c <= 0x0BB9) return true;
    if (c < 0x0C05) return false;  if (c <= 0x0C0C) return true;
    if (c < 0x0C0E) return false;  if (c <= 0x0C10) return true;
    if (c < 0x0C12) return false;  if (c <= 0x0C28) return true;
    if (c < 0x0C2A) return false;  if (c <= 0x0C33) return true;
    if (c < 0x0C35) return false;  if (c <= 0x0C39) return true;
    if (c < 0x0C60) return false;  if (c <= 0x0C61) return true;
    if (c < 0x0C85) return false;  if (c <= 0x0C8C) return true;
    if (c < 0x0C8E) return false;  if (c <= 0x0C90) return true;
    if (c < 0x0C92) return false;  if (c <= 0x0CA8) return true;
    if (c < 0x0CAA) return false;  if (c <= 0x0CB3) return true;
    if (c < 0x0CB5) return false;  if (c <= 0x0CB9) return true;
    if (c == 0x0CDE) return true;
    if (c < 0x0CE0) return false;  if (c <= 0x0CE1) return true;
    if (c < 0x0D05) return false;  if (c <= 0x0D0C) return true;
    if (c < 0x0D0E) return false;  if (c <= 0x0D10) return true;
    if (c < 0x0D12) return false;  if (c <= 0x0D28) return true;
    if (c < 0x0D2A) return false;  if (c <= 0x0D39) return true;
    if (c < 0x0D60) return false;  if (c <= 0x0D61) return true;
    if (c < 0x0E01) return false;  if (c <= 0x0E2E) return true;
    if (c == 0x0E30) return true;
    if (c < 0x0E32) return false;  if (c <= 0x0E33) return true;
    if (c < 0x0E40) return false;  if (c <= 0x0E45) return true;
    if (c < 0x0E81) return false;  if (c <= 0x0E82) return true;
    if (c == 0x0E84) return true;
    if (c < 0x0E87) return false;  if (c <= 0x0E88) return true;
    if (c == 0x0E8A) return true;
    if (c == 0x0E8D) return true;
    if (c < 0x0E94) return false;  if (c <= 0x0E97) return true;
    if (c < 0x0E99) return false;  if (c <= 0x0E9F) return true;
    if (c < 0x0EA1) return false;  if (c <= 0x0EA3) return true;
    if (c == 0x0EA5) return true;
    if (c == 0x0EA7) return true;
    if (c < 0x0EAA) return false;  if (c <= 0x0EAB) return true;
    if (c < 0x0EAD) return false;  if (c <= 0x0EAE) return true;
    if (c == 0x0EB0) return true;
    if (c < 0x0EB2) return false;  if (c <= 0x0EB3) return true;
    if (c == 0x0EBD) return true;
    if (c < 0x0EC0) return false;  if (c <= 0x0EC4) return true;
    if (c < 0x0F40) return false;  if (c <= 0x0F47) return true;
    if (c < 0x0F49) return false;  if (c <= 0x0F69) return true;
    if (c < 0x10A0) return false;  if (c <= 0x10C5) return true;
    if (c < 0x10D0) return false;  if (c <= 0x10F6) return true;
    if (c == 0x1100) return true;
    if (c < 0x1102) return false;  if (c <= 0x1103) return true;
    if (c < 0x1105) return false;  if (c <= 0x1107) return true;
    if (c == 0x1109) return true;
    if (c < 0x110B) return false;  if (c <= 0x110C) return true;
    if (c < 0x110E) return false;  if (c <= 0x1112) return true;
    if (c == 0x113C) return true;
    if (c == 0x113E) return true;
    if (c == 0x1140) return true;
    if (c == 0x114C) return true;
    if (c == 0x114E) return true;
    if (c == 0x1150) return true;
    if (c < 0x1154) return false;  if (c <= 0x1155) return true;
    if (c == 0x1159) return true;
    if (c < 0x115F) return false;  if (c <= 0x1161) return true;
    if (c == 0x1163) return true;
    if (c == 0x1165) return true;
    if (c == 0x1167) return true;
    if (c == 0x1169) return true;
    if (c < 0x116D) return false;  if (c <= 0x116E) return true;
    if (c < 0x1172) return false;  if (c <= 0x1173) return true;
    if (c == 0x1175) return true;
    if (c == 0x119E) return true;
    if (c == 0x11A8) return true;
    if (c == 0x11AB) return true;
    if (c < 0x11AE) return false;  if (c <= 0x11AF) return true;
    if (c < 0x11B7) return false;  if (c <= 0x11B8) return true;
    if (c == 0x11BA) return true;
    if (c < 0x11BC) return false;  if (c <= 0x11C2) return true;
    if (c == 0x11EB) return true;
    if (c == 0x11F0) return true;
    if (c == 0x11F9) return true;
    if (c < 0x1E00) return false;  if (c <= 0x1E9B) return true;
    if (c < 0x1EA0) return false;  if (c <= 0x1EF9) return true;
    if (c < 0x1F00) return false;  if (c <= 0x1F15) return true;
    if (c < 0x1F18) return false;  if (c <= 0x1F1D) return true;
    if (c < 0x1F20) return false;  if (c <= 0x1F45) return true;
    if (c < 0x1F48) return false;  if (c <= 0x1F4D) return true;
    if (c < 0x1F50) return false;  if (c <= 0x1F57) return true;
    if (c == 0x1F59) return true;
    if (c == 0x1F5B) return true;
    if (c == 0x1F5D) return true;
    if (c < 0x1F5F) return false;  if (c <= 0x1F7D) return true;
    if (c < 0x1F80) return false;  if (c <= 0x1FB4) return true;
    if (c < 0x1FB6) return false;  if (c <= 0x1FBC) return true;
    if (c == 0x1FBE) return true;
    if (c < 0x1FC2) return false;  if (c <= 0x1FC4) return true;
    if (c < 0x1FC6) return false;  if (c <= 0x1FCC) return true;
    if (c < 0x1FD0) return false;  if (c <= 0x1FD3) return true;
    if (c < 0x1FD6) return false;  if (c <= 0x1FDB) return true;
    if (c < 0x1FE0) return false;  if (c <= 0x1FEC) return true;
    if (c < 0x1FF2) return false;  if (c <= 0x1FF4) return true;
    if (c < 0x1FF6) return false;  if (c <= 0x1FFC) return true;
    if (c == 0x2126) return true;
    if (c < 0x212A) return false;  if (c <= 0x212B) return true;
    if (c == 0x212E) return true;
    if (c < 0x2180) return false;  if (c <= 0x2182) return true;
    if (c == 0x3007) return true;                          // ideographic
    if (c < 0x3021) return false;  if (c <= 0x3029) return true;  // ideo
    if (c < 0x3041) return false;  if (c <= 0x3094) return true;
    if (c < 0x30A1) return false;  if (c <= 0x30FA) return true;
    if (c < 0x3105) return false;  if (c <= 0x312C) return true;
    if (c < 0x4E00) return false;  if (c <= 0x9FA5) return true;  // ideo
    if (c < 0xAC00) return false;  if (c <= 0xD7A3) return true;

    return false;

  }

}
