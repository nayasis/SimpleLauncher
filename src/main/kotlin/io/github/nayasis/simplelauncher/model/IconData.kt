package io.github.nayasis.simplelauncher.model

import java.nio.ByteBuffer
import java.util.Base64

// Java serialization header for a single byte[], followed by its length and image bytes.
private val LEGACY_ICON_HEADER = Base64.getDecoder().decode("rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHA=")

/** Strip the old byte-array wrapper without deserializing Java objects. */
internal fun ByteArray.unwrapLegacyIcon(): ByteArray {
    if (size < LEGACY_ICON_HEADER.size || !LEGACY_ICON_HEADER.indices.all { this[it] == LEGACY_ICON_HEADER[it] }) {
        return this
    }
    val offset = LEGACY_ICON_HEADER.size + Int.SIZE_BYTES
    require(size >= offset)
    val length = ByteBuffer.wrap(this, LEGACY_ICON_HEADER.size, Int.SIZE_BYTES).int
    require(length == size - offset)
    return copyOfRange(offset, size)
}
