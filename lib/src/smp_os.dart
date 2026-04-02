import 'dart:typed_data';
import 'smp_client.dart';

class SmpOs {
  final SmpClient _client;

  SmpOs(SmpClient client) : _client = client;

  Future<Uint8List> echo(String message) async {
    final response = await _client.send(_echoFrame(message));
    return response.length > 8 ? response.sublist(8) : response;
  }

  static Uint8List _echoFrame(String message) {
    final msgBytes = message.codeUnits;
    final int msgLen = msgBytes.length;

    final List<int> valueCbor;
    if (msgLen <= 23) {
      valueCbor = [0x60 + msgLen, ...msgBytes];
    } else if (msgLen <= 255) {
      valueCbor = [0x78, msgLen, ...msgBytes];
    } else {
      valueCbor = [0x79, (msgLen >> 8) & 0xff, msgLen & 0xff, ...msgBytes];
    }

    final payload = [0xa1, 0x61, 0x64, ...valueCbor];
    final int payloadLen = payload.length;

    return Uint8List.fromList([
      0x0a,
      0x00,
      (payloadLen >> 8) & 0xff,
      payloadLen & 0xff,
      0x00,
      0x00,
      0x01,
      0x00,
      ...payload,
    ]);
  }
}
