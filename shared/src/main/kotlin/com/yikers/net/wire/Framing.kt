package com.yikers.net.wire

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

// Length-prefixed framing over a TCP stream: a 4-byte big-endian int length, then
// that many CBOR bytes of one Envelope. TCP has no message boundaries, so the length
// prefix tells the reader exactly how much to pull for the next whole frame.
object Framing {
    // Cap a single frame so a corrupt/hostile length can't make us allocate a huge
    // buffer. A snapshot of this game is a few KB; 8 MiB is enormous headroom.
    const val MAX_FRAME_BYTES = 8 * 1024 * 1024

    // Write one frame. Caller holds the per-connection write lock: writeInt + write
    // must not interleave with another thread's frame on the same stream.
    fun writeFrame(out: OutputStream, payload: ByteArray) {
        require(payload.size <= MAX_FRAME_BYTES) { "frame too large: ${payload.size}" }
        val dos = if (out is DataOutputStream) out else DataOutputStream(out)
        dos.writeInt(payload.size)
        dos.write(payload)
        dos.flush()
    }

    // Read one whole frame, or null on a clean EOF (peer closed between frames).
    // A truncated frame (EOF mid-payload) is a hard error, not a clean close.
    fun readFrame(input: InputStream): ByteArray? {
        val dis = if (input is DataInputStream) input else DataInputStream(input)
        val len = try {
            dis.readInt()
        } catch (_: EOFException) {
            return null
        }
        require(len in 0..MAX_FRAME_BYTES) { "bad frame length: $len" }
        val buf = ByteArray(len)
        dis.readFully(buf)
        return buf
    }
}
