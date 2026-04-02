import 'dart:async';
import 'dart:typed_data';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

class SmpClient {
  static const String _smpServiceUuid = "8d53dc1d-1db7-4cd3-868b-8a527460aa84";
  static const String _smpCharUuid = "da2e7828-fbce-4e01-ae9e-261174997c48";
  static const Duration _timeout = Duration(seconds: 10);

  final BluetoothDevice device;
  BluetoothCharacteristic? _cachedChar;
  StreamSubscription<BluetoothConnectionState>? _connSub;

  int _seq = 0;

  Future<void> _lock = Future.value();

  SmpClient(this.device);

  Future<T> _withLock<T>(Future<T> Function() fn) {
    final prev = _lock;
    final gate = Completer<void>();
    _lock = gate.future;
    return prev.then((_) => fn()).whenComplete(gate.complete);
  }

  Future<BluetoothCharacteristic> _getChar() async {
    if (_cachedChar != null && device.isConnected) return _cachedChar!;
    final services = await device.discoverServices();
    for (final svc in services) {
      if (svc.uuid.toString().toLowerCase() == _smpServiceUuid.toLowerCase()) {
        for (final c in svc.characteristics) {
          if (c.uuid.toString().toLowerCase() == _smpCharUuid.toLowerCase()) {
            _cachedChar = c;
            await device.requestMtu(512);
            await _cachedChar!.setNotifyValue(true);
            _connSub?.cancel();
            _connSub = device.connectionState.listen((state) {
              if (state == BluetoothConnectionState.disconnected) {
                _cachedChar = null;
                _connSub?.cancel();
                _connSub = null;
              }
            });
            return _cachedChar!;
          }
        }
      }
    }
    throw StateError('Serviço/Characteristic SMP não encontrado no dispositivo');
  }

  Future<Uint8List> send(Uint8List frame) => _withLock(() => _send(frame));

  Future<Uint8List> _send(Uint8List frame) async {
    if (device.isDisconnected) {
      _cachedChar = null;
      await device.connect(timeout: const Duration(seconds: 15));
    }

    final char = await _getChar();

    final seq = _seq & 0xFF;
    _seq = (_seq + 1) & 0xFF;
    final patchedFrame = Uint8List.fromList(frame);
    patchedFrame[6] = seq;

    final completer = Completer<Uint8List>();
    List<int> buffer = [];
    int expected = 0;

    final sub = char.onValueReceived.listen((value) {
      if (value.isEmpty) return;

      if (buffer.isEmpty) {
        if (value.length < 8) return;
        final op = value[0];
        if (op != 0x09 && op != 0x0B) return;
        if (value[6] != seq) return;
        final payloadLen = (value[2] << 8) | value[3];
        expected = payloadLen + 8;
        buffer.addAll(value);
      } else {
        buffer.addAll(value);
      }

      if (buffer.length >= expected && !completer.isCompleted) {
        completer.complete(Uint8List.fromList(buffer.sublist(0, expected)));
      }
    });

    try {
      await char.write(patchedFrame, withoutResponse: true);
      return await completer.future.timeout(_timeout);
    } finally {
      await sub.cancel();
    }
  }

  Future<void> disconnect() => device.disconnect();
}
