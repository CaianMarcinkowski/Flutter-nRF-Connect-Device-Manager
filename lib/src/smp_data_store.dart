import 'dart:typed_data';
import 'smp_client.dart';

class SmpDataStore {
  final SmpClient _client;

  SmpDataStore(SmpClient client) : _client = client;

  Future<Uint8List> groupRead(int partition, {int cell = 0}) async {
    final response = await _client.send(_groupReadFrame(partition, cell: cell));
    return response.length > 8 ? response.sublist(8) : response;
  }

  Future<Uint8List> getMetadata(int partition) async {
    final response = await _client.send(_getMetadataFrame(partition));
    return response.length > 8 ? response.sublist(8) : response;
  }

  static Uint8List _groupReadFrame(int partition, {int cell = 0}) {
    final List<int> cellCbor = cell <= 23 ? [cell] : [0x18, cell];
    final List<int> partitionCbor = partition <= 23 ? [partition] : [0x18, partition];
    final int payloadLen = 24 + cellCbor.length + partitionCbor.length;
    return Uint8List.fromList([
      0x08,
      0x80,
      (payloadLen >> 8) & 0xff,
      payloadLen & 0xff,
      0x00,
      0x41,
      0x01,
      0x02,
      0xa2,
      0x64,
      0x63,
      0x65,
      0x6c,
      0x6c,
      ...cellCbor,
      0x69,
      0x70,
      0x61,
      0x72,
      0x74,
      0x69,
      0x74,
      0x69,
      0x6f,
      0x6e,
      ...partitionCbor,
      0x10,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
    ]);
  }

  static Uint8List _getMetadataFrame(int partition) {
    final List<int> partitionCbor = partition <= 23 ? [partition] : [0x18, partition];
    final int payloadLen = 19 + partitionCbor.length;
    return Uint8List.fromList([
      0x08,
      0x80,
      (payloadLen >> 8) & 0xff,
      payloadLen & 0xff,
      0x00,
      0x41,
      0x02,
      0x01,
      0xa1,
      0x69,
      0x70,
      0x61,
      0x72,
      0x74,
      0x69,
      0x74,
      0x69,
      0x6f,
      0x6e,
      ...partitionCbor,
      0x10,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
    ]);
  }
}
