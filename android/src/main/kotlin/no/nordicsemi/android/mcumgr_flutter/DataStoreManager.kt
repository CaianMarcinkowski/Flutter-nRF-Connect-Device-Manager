package no.nordicsemi.android.mcumgr_flutter

import android.util.Log
import io.runtime.mcumgr.McuManager
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.response.McuMgrResponse

class DataStoreManager(transport: McuMgrTransport) : McuManager(GROUP_DATA_STORE, transport) {

    companion object {
        private const val GROUP_DATA_STORE = 0x41
        private const val ID_GROUP_READ = 0x02
        private const val ID_GET_METADATA = 0x01
    }

    fun groupRead(partition: Int, cell: Int, callback: McuMgrCallback<McuMgrResponse>) {
        Log.d("DataStore", "groupRead: GROUP=0x${GROUP_DATA_STORE.toString(16)}, CMD=0x${ID_GROUP_READ.toString(16)}, partition=$partition, cell=$cell")
        val payloadMap = hashMapOf<String, Any>(
            "cell" to cell,
            "partition" to partition
        )
        send(OP_READ, ID_GROUP_READ, payloadMap, McuMgrResponse::class.java, object : McuMgrCallback<McuMgrResponse> {
            override fun onResponse(response: McuMgrResponse) {
                Log.d("DataStore", "groupRead response rc=${response.rc} bytes=${response.bytes?.let { it.joinToString(" ") { b -> "%02x".format(b) } }}")
                callback.onResponse(response)
            }
            override fun onError(error: io.runtime.mcumgr.exception.McuMgrException) {
                Log.e("DataStore", "groupRead error: $error")
                callback.onError(error)
            }
        })
    }

    fun getMetadata(partition: Int, callback: McuMgrCallback<McuMgrResponse>) {
        Log.d("DataStore", "getMetadata: GROUP=0x${GROUP_DATA_STORE.toString(16)}, CMD=0x${ID_GET_METADATA.toString(16)}, partition=$partition")
        val payloadMap = hashMapOf<String, Any>(
            "partition" to partition
        )
        send(OP_READ, ID_GET_METADATA, payloadMap, McuMgrResponse::class.java, object : McuMgrCallback<McuMgrResponse> {
            override fun onResponse(response: McuMgrResponse) {
                Log.d("DataStore", "getMetadata response rc=${response.rc} bytes=${response.bytes?.let { it.joinToString(" ") { b -> "%02x".format(b) } }}")
                callback.onResponse(response)
            }
            override fun onError(error: io.runtime.mcumgr.exception.McuMgrException) {
                Log.e("DataStore", "getMetadata error: $error")
                callback.onError(error)
            }
        })
    }
}
