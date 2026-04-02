import 'dart:typed_data';
import 'smp_client.dart';

class SmpStats {
  final SmpClient _client;

  SmpStats(SmpClient client) : _client = client;

  Future<Uint8List> getStats(String name) async {
    final response = await _client.send(_statsFrame(name));
    return response.length > 8 ? response.sublist(8) : response;
  }

  static Uint8List _statsFrame(String name) {
    final nameBytes = name.codeUnits;
    final int nameLen = nameBytes.length;

    final List<int> nameCbor;
    if (nameLen <= 23) {
      nameCbor = [0x60 + nameLen, ...nameBytes];
    } else if (nameLen <= 255) {
      nameCbor = [0x78, nameLen, ...nameBytes];
    } else {
      nameCbor = [0x79, (nameLen >> 8) & 0xff, nameLen & 0xff, ...nameBytes];
    }

    final payload = [0xa1, 0x64, 0x6e, 0x61, 0x6d, 0x65, ...nameCbor];
    final int payloadLen = payload.length;

    return Uint8List.fromList([
      0x08,
      0x00,
      (payloadLen >> 8) & 0xff,
      payloadLen & 0xff,
      0x00,
      0x02,
      0x01,
      0x00,
      ...payload,
    ]);
  }
}
