/*
 * JsonKotlinFormat.kt
 *
 * Copyright 2019 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.VisibleForTesting
import kotlinx.io.InputStream
import kotlinx.io.OutputStream
import kotlinx.io.charsets.Charsets
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.Json

// TODO: Remove when https://github.com/Kotlin/kotlinx.serialization/issues/204 is fixed
private fun InputStream.readToString(maxSize: Int = 64*1024) : String {
    val buf = ByteArray(maxSize) { 0 }
    val actualLen = this.read(buf, 0, buf.size)
    return kotlinx.io.core.String(
            bytes = buf.sliceArray(0 until actualLen),
            charset = Charsets.UTF_8)
}

class JsonKotlinFormat : CardExporter, CardImporter {
    override fun writeCard(s: OutputStream, card: Card) {
        s.write(writeCard(card).toUtf8Bytes())
    }
    fun writeCard(card: Card) = Json(indented = true, encodeDefaults = false).stringify(Card.serializer(), card)

    /**
     * Note: This currently reads 64 KiB of the file, and passes it to kotlinx.serialization.
     * This is only really suitable for tests, so it is marked [VisibleForTesting].
     *
     * Once https://github.com/Kotlin/kotlinx.serialization/issues/204 is fixed, this can be
     * implemented properly.
     */
    @VisibleForTesting
    override fun readCard(stream: InputStream) =
            readCard(stream.readToString())

    // This intentionally runs in non-strict mode.
    //
    // This allows us to skip unknown fields:
    // 1. This lets us remove old fields, without keeping attributes hanging around. There doesn't
    //    seem to be a simple way to explicitly ignore single fields in JSON inputs.
    // 2. Dumps from a newer version of Metrodroid can still be read (though, without these fields).
    override fun readCard(input: String): Card =
            Json.nonstrict.parse(Card.serializer(), input)
}

// Standard polymorphic serializer works fine but let's avoid putting
// full class names in stored formats
abstract class MultiTypeSerializer<T : Any> : KSerializer<T> {
    abstract val name: String
    override val descriptor: SerialDescriptor
    get() = object : SerialClassDescImpl(name) {
        override val kind: SerialKind = UnionKind.POLYMORPHIC

        init {
            addElement("type")
            addElement("contents")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, obj: T) {
        @Suppress("NAME_SHADOWING")
        val output = encoder.beginStructure(descriptor)
        val (str, serializer) = obj2serializer(obj)
        output.encodeStringElement(descriptor, 0, str)
        output.encodeSerializableElement(descriptor, 1, serializer as KSerializer<T>,
                obj)
        output.endStructure(descriptor)
    }

    abstract fun obj2serializer(obj: T): Pair<String, KSerializer<out T>>
    abstract fun str2serializer(name: String): KSerializer<out T>

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): T {
        @Suppress("NAME_SHADOWING")
        val input = decoder.beginStructure(descriptor)
        var klassName: String? = null
        var value: T? = null
        mainLoop@ while (true) {
            when (input.decodeElementIndex(descriptor)) {
                READ_ALL -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                    val loader = str2serializer(klassName) as KSerializer<T>
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                    break@mainLoop
                }
                READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                }
                1 -> {
                    val loader = str2serializer(klassName!!) as KSerializer<T>
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                }
                else -> throw SerializationException("Invalid index")
            }
        }

        input.endStructure(descriptor)
        return value!!
    }
}