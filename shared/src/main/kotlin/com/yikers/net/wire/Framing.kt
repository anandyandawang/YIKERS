package com.yikers.net.wire

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

// Length-prefixed framing: 4-byte length + that many CBOR bytes (TCP has no boundaries).
object Framing {
    // Cap a frame so a corrupt/hostile length can't force a huge allocation.
    const val MAX_FRAME_BYTES = 8 * 1024 * 1024

    // Caller holds the write lock: writeInt + write must not interleave.
    fun writeFrame(out: OutputStream, payload: ByteArray) {
        require(payload.size <= MAX_FRAME_BYTES) { "frame too large: ${payload.size}" }
        val dos = if (out is DataOutputStream) out else DataOutputStream(out)
        dos.writeInt(payload.size)
        dos.write(payload)
        dos.flush()
    }

    // Returns null on a clean EOF between frames; a truncated frame throws.
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
