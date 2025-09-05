package io.github.nayasis.simplelauncher.model.types

@JvmInline
value class BlobByteArray(val value: ByteArray) {
    override fun toString(): String {
        return value.contentToString()
    }
}