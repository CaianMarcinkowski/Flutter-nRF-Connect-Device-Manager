package no.nordicsemi.android.mcumgr_flutter

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.McuMgrScheme
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.response.McuMgrResponse
import no.nordicsemi.android.mcumgr_flutter.logging.LoggableMcuMgrBleTransport

class ForwardTreeTransport(
    private val wrapped: LoggableMcuMgrBleTransport,
    private val forwardTree: ByteArray,
    private val opByte: Byte? = null,
    private val flagsByte: Byte? = null,
) : McuMgrTransport {

    override fun getScheme(): McuMgrScheme = wrapped.scheme

    override fun <T : McuMgrResponse> send(
        data: ByteArray,
        timeout: Long,
        responseType: Class<T>,
        callback: McuMgrCallback<T>,
    ) = wrapped.send(appendForwardTree(data), timeout, responseType, callback)

    @Throws(McuMgrException::class)
    override fun <T : McuMgrResponse> send(
        data: ByteArray,
        timeout: Long,
        responseType: Class<T>,
    ): T = wrapped.send(appendForwardTree(data), timeout, responseType)

    override fun connect(callback: McuMgrTransport.ConnectionCallback?) =
        wrapped.connect(callback)

    override fun release() = wrapped.release()

    override fun addObserver(observer: McuMgrTransport.ConnectionObserver) =
        wrapped.addObserver(observer)

    override fun removeObserver(observer: McuMgrTransport.ConnectionObserver) =
        wrapped.removeObserver(observer)

    private fun appendForwardTree(data: ByteArray): ByteArray {
        if (data.size < 4) return data
        val result = ByteArray(data.size + forwardTree.size)
        System.arraycopy(data, 0, result, 0, data.size)
        System.arraycopy(forwardTree, 0, result, data.size, forwardTree.size)
        if (opByte != null) result[0] = opByte
        if (flagsByte != null) result[1] = flagsByte
        val originalLen = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        val newLen = originalLen + forwardTree.size
        result[2] = ((newLen shr 8) and 0xFF).toByte()
        result[3] = (newLen and 0xFF).toByte()
        return result
    }
}
