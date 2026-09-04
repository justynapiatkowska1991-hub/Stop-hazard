package pl.stophazard.app

import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Controlled TUN reader.
 * It only reads packets and exposes them to a callback; it does not claim
 * that packets were forwarded unless the callback confirms it.
 */
class VpnPacketLoop(
    private val state: VpnSessionState,
    private val onPacket: (ByteArray) -> Boolean
) {
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null

    fun start(tun: ParcelFileDescriptor) {
        if (!running.compareAndSet(false, true)) return
        worker = Thread {
            try {
                FileInputStream(tun.fileDescriptor).use { input ->
                    val buffer = ByteArray(32767)
                    while (running.get()) {
                        val count = input.read(buffer)
                        if (count <= 0) continue
                        state.packetRead()
                        val packet = buffer.copyOf(count)
                        if (onPacket(packet)) state.packetForwarded()
                        else state.packetDropped()
                    }
                }
            } catch (e: Exception) {
                if (running.get()) state.error(e.message ?: "tun-read-failed")
            } finally {
                running.set(false)
            }
        }.apply {
            name = "StopHazard-TunReader"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        worker?.interrupt()
        worker = null
    }

    fun isRunning(): Boolean = running.get()
}