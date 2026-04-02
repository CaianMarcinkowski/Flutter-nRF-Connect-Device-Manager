package no.nordicsemi.android.mcumgr_flutter

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Handler
import io.flutter.plugin.common.BinaryMessenger
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.DefaultManager
import io.runtime.mcumgr.managers.StatsManager
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.response.dflt.McuMgrEchoResponse
import io.runtime.mcumgr.response.stat.McuMgrStatResponse
import io.runtime.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr_flutter.logging.LoggableMcuMgrBleTransport
import no.nordicsemi.android.mcumgr_flutter.utils.ConnectionStateStreamHandler
import no.nordicsemi.android.mcumgr_flutter.utils.StreamHandler

class DataStoreManagerPlugin(
    private val context: Context,
    private val logStreamHandler: StreamHandler,
    binaryMessenger: BinaryMessenger,
    private val mainHandler: Handler,
) : DataStoreManagerApi {

    companion object {
        private val FORWARD_TREE = byteArrayOf(0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        private const val DATA_STORE_OP: Byte    = 0x08.toByte()
        private const val DATA_STORE_FLAGS: Byte = 0x80.toByte()
    }

    private val connectionStateStreamHandler = ConnectionStateStreamHandler()
    private val transports: MutableMap<String, LoggableMcuMgrBleTransport> = mutableMapOf()

    private val dataStoreManagers: MutableMap<String, DataStoreManager> = mutableMapOf()

    init {
        GetConnectionStateEventsStreamHandler.register(binaryMessenger, connectionStateStreamHandler)
        DataStoreManagerApi.setUp(binaryMessenger, this)
    }

    private fun makeObserver(remoteId: String) = object : McuMgrTransport.ConnectionObserver {
        override fun onConnected() {
            mainHandler.post {
                connectionStateStreamHandler.onEvent(ConnectionStateEvent(remoteId, true))
            }
        }
        override fun onDisconnected() {
            mainHandler.post {
                connectionStateStreamHandler.onEvent(ConnectionStateEvent(remoteId, false))
            }
        }
    }

    private fun getTransport(remoteId: String): LoggableMcuMgrBleTransport {
        synchronized(this) {
            return transports[remoteId] ?: run {
                val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(remoteId)
                val transport = LoggableMcuMgrBleTransport(context, device, logStreamHandler)
                    .apply { setLoggingEnabled(true) }
                transport.addObserver(makeObserver(remoteId))
                transports[remoteId] = transport
                transport
            }
        }
    }

    private fun getDataStoreManager(remoteId: String): DataStoreManager {
        synchronized(this) {
            return dataStoreManagers[remoteId] ?: run {
                val bleTransport = getTransport(remoteId)
                val wrappedTransport = ForwardTreeTransport(bleTransport, FORWARD_TREE, DATA_STORE_OP, DATA_STORE_FLAGS)
                val manager = DataStoreManager(wrappedTransport)
                dataStoreManagers[remoteId] = manager
                manager
            }
        }
    }

    override fun smpEcho(remoteId: String, message: String, callback: (Result<ByteArray>) -> Unit) {
        val transport = getTransport(remoteId)
        val mgr = DefaultManager(transport)
        mgr.echo(message, object : McuMgrCallback<McuMgrEchoResponse> {
            override fun onResponse(response: McuMgrEchoResponse) {
                val bytes = response.bytes
                callback(Result.success(if (bytes != null && bytes.size > 8) bytes.drop(8).toByteArray() else (bytes ?: ByteArray(0))))
            }

            override fun onError(error: McuMgrException) {
                callback(Result.failure(error))
            }
        })
    }

    override fun smpStats(remoteId: String, groupName: String, callback: (Result<ByteArray>) -> Unit) {
        val transport = getTransport(remoteId)
        val mgr = StatsManager(transport)
        mgr.read(groupName, object : McuMgrCallback<McuMgrStatResponse> {
            override fun onResponse(response: McuMgrStatResponse) {
                val bytes = response.bytes
                callback(Result.success(if (bytes != null && bytes.size > 8) bytes.drop(8).toByteArray() else (bytes ?: ByteArray(0))))
            }

            override fun onError(error: McuMgrException) {
                callback(Result.failure(error))
            }
        })
    }

    override fun smpDataStoreGroupRead(remoteId: String, partition: Long, cell: Long, callback: (Result<ByteArray>) -> Unit) {
        getDataStoreManager(remoteId).groupRead(partition.toInt(), cell.toInt(), object : McuMgrCallback<McuMgrResponse> {
            override fun onResponse(response: McuMgrResponse) {
                val bytes = response.bytes
                callback(Result.success(if (bytes != null && bytes.size > 8) bytes.drop(8).toByteArray() else (bytes ?: ByteArray(0))))
            }

            override fun onError(error: McuMgrException) {
                callback(Result.failure(error))
            }
        })
    }

    override fun smpDataStoreGetMetadata(remoteId: String, partition: Long, callback: (Result<ByteArray>) -> Unit) {
        getDataStoreManager(remoteId).getMetadata(partition.toInt(), object : McuMgrCallback<McuMgrResponse> {
            override fun onResponse(response: McuMgrResponse) {
                val bytes = response.bytes
                callback(Result.success(if (bytes != null && bytes.size > 8) bytes.drop(8).toByteArray() else (bytes ?: ByteArray(0))))
            }

            override fun onError(error: McuMgrException) {
                callback(Result.failure(error))
            }
        })
    }

    override fun smpKill(remoteId: String) {
        dataStoreManagers.remove(remoteId)
        transports.remove(remoteId)?.release()
    }
}
