package com.hfc.netty.pack

/**
 * 可用于传输Bitmap、String等可转成ByteArray的数据
 * data: 传输的数据
 * length: 数据长度
 * type: 0:Bitmap, 1:String
 */
data class ByteArrayMessage(var data: ByteArray, var length: Int, var type: Int = 0) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayMessage

        if (!data.contentEquals(other.data)) return false
        return length == other.length
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + length
        return result
    }
}
