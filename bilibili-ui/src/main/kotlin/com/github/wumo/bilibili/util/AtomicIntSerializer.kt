package com.github.wumo.bilibili.util

import kotlinx.serialization.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Serializer(forClass = AtomicInteger::class)
object AtomicIntSerializer: KSerializer<AtomicInteger> {
  override val descriptor: SerialDescriptor =
    PrimitiveDescriptor(AtomicInteger::class.qualifiedName!!, PrimitiveKind.INT)
  
  override fun deserialize(decoder: Decoder) = AtomicInteger(decoder.decodeInt())
  
  override fun serialize(encoder: Encoder, value: AtomicInteger) = encoder.encodeInt(value.get())
}