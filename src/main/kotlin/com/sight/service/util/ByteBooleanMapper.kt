package com.sight.service.util

object ByteBooleanMapper {
    fun map(byte: Byte): Boolean = byte != 0.toByte()
}
