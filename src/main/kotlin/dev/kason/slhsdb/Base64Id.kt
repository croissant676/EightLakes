package dev.kason.slhsdb

import com.github.jershell.kbson.BsonEncoder
import com.github.jershell.kbson.FlexibleDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonType
import java.security.SecureRandom
import java.util.*
import kotlin.random.asKotlinRandom


private val base64UrlEncoder = Base64.getUrlEncoder()!!
private val base64UrlDecoder = Base64.getUrlDecoder()!!
val random = SecureRandom().asKotlinRandom()

@kotlinx.serialization.Serializable(with = Base64IdSerializer::class)
class Base64Id internal constructor(internal val data: ByteArray) {
    override fun equals(other: Any?): Boolean =
        (this === other) || (other is Base64Id && other.data.contentEquals(data))

    override fun hashCode(): Int = data.contentHashCode()
    override fun toString(): String = data.encodeBase64Url()
}

fun randomId(): Base64Id = Base64Id(random.nextBytes(15))
fun idFromStringOrNull(source: String): Base64Id? = runCatching {
    Base64Id(source.decodeBase64Url())
}.getOrNull()

fun idFromString(source: String) = Base64Id(source.decodeBase64Url())

fun ByteArray.encodeBase64Url(): String = base64UrlEncoder.encodeToString(this)
fun String.decodeBase64UrlOrNull(): ByteArray? = kotlin.runCatching {
    base64UrlDecoder.decode(this@decodeBase64UrlOrNull)
}.getOrNull()

fun String.decodeBase64Url(): ByteArray =
    decodeBase64UrlOrNull() ?: throw IllegalArgumentException("\"$this\" is not a valid base64 url string.")

object Base64IdSerializer : KSerializer<Base64Id> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64Id", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Base64Id) =
        if (encoder is BsonEncoder) encoder.encodeByteArray(value.data)
        else encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Base64Id =
        if (decoder is FlexibleDecoder && decoder.reader.currentBsonType == BsonType.BINARY)
            Base64Id(decoder.reader.readBinaryData().data)
        else idFromString(decoder.decodeString())
}