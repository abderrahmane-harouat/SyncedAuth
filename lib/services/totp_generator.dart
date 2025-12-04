import 'dart:math';
import 'dart:typed_data';
import 'package:crypto/crypto.dart';

class TOTPGenerator {
  static String generateTOTP(String secret, {int digits = 6, int period = 30}) {
    final int timestamp = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    final int timeStep = timestamp ~/ period;

    // Decode base32 secret
    final List<int> secretBytes = _base32Decode(secret);

    // Create time-based counter
    final ByteData timeData = ByteData(8);
    timeData.setUint64(0, timeStep, Endian.big);

    // HMAC-SHA1
    final Hmac hmac = Hmac(sha1, secretBytes);
    final List<int> hash = hmac.convert(timeData.buffer.asUint8List()).bytes;

    // Dynamic truncation
    final int offset = hash[hash.length - 1] & 0x0F;
    final int truncatedHash =
        ((hash[offset] & 0x7F) << 24) |
        ((hash[offset + 1] & 0xFF) << 16) |
        ((hash[offset + 2] & 0xFF) << 8) |
        (hash[offset + 3] & 0xFF);

    final int otp = truncatedHash % pow(10, digits).toInt();
    return otp.toString().padLeft(digits, '0');
  }

  static int getRemainingSeconds({int period = 30}) {
    final int timestamp = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    return period - (timestamp % period);
  }

  static List<int> _base32Decode(String input) {
    const String base32Chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    input = input.toUpperCase().replaceAll(RegExp(r'[^A-Z2-7]'), '');

    final List<int> output = [];
    int buffer = 0;
    int bitsLeft = 0;

    for (int i = 0; i < input.length; i++) {
      final int val = base32Chars.indexOf(input[i]);
      if (val == -1) continue;

      buffer = (buffer << 5) | val;
      bitsLeft += 5;

      if (bitsLeft >= 8) {
        output.add((buffer >> (bitsLeft - 8)) & 255);
        bitsLeft -= 8;
      }
    }

    return output;
  }
}