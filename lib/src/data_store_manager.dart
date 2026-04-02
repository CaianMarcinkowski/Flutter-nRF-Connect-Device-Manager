part of mcumgr_flutter;

abstract class DataStoreManager {
  late final String remoteId;

  static final Map<String, DataStoreManager> _cache = {};

  factory DataStoreManager(String remoteId) {
    return _cache.putIfAbsent(
        remoteId, () => _DataStoreManagerImpl._internal(remoteId));
  }

  Future<Uint8List> echo(String message);

  Future<Uint8List> getStats(String name);

  Future<Uint8List> groupRead(int partition, {int cell = 0});

  Future<Uint8List> getMetadata(int partition);

  Future<void> kill();
}

class _DataStoreManagerImpl implements DataStoreManager {
  @override
  String remoteId;

  final DataStoreManagerApi _api = DataStoreManagerApi();

  _DataStoreManagerImpl._internal(this.remoteId);

  @override
  Future<Uint8List> echo(String message) => _api.smpEcho(remoteId, message);

  @override
  Future<Uint8List> getStats(String name) => _api.smpStats(remoteId, name);

  @override
  Future<Uint8List> groupRead(int partition, {int cell = 0}) =>
      _api.smpDataStoreGroupRead(remoteId, partition, cell);

  @override
  Future<Uint8List> getMetadata(int partition) =>
      _api.smpDataStoreGetMetadata(remoteId, partition);

  @override
  Future<void> kill() => _api.smpKill(remoteId);
}
