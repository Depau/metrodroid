/*
 * NextfareTapRecord.kt
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.nextfare.record

import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.hexString

/**
 * Tap record type
 * https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
 */
class NextfareTransactionRecord private constructor(
        val type: Type, val timestamp: TimestampFull, val mode: Int, val journey: Int,
        val station: Int, val value: Int, val checksum: Int,
        val isContinuation: Boolean) : NextfareRecord(), Comparable<NextfareTransactionRecord> {

    override fun compareTo(other: NextfareTransactionRecord): Int {
        // Group by journey, then by timestamp.
        // First trip in a journey goes first, and should (generally) be in pairs.

        return if (other.journey == this.journey) {
            this.timestamp.timeInMillis.compareTo(other.timestamp.timeInMillis)
        } else {
            this.journey.compareTo(other.journey)
        }

    }

    companion object {
        private const val TAG = "NextfareTxnRecord"

        enum class Type {
            UNKNOWN,
            IGNORE,
            TRAVEL_PASS_TRIP,
            TRAVEL_PASS_SALE,
            STORED_VALUE_TRIP,
            STORED_VALUE_SALE;

            val isSale get() = (this == TRAVEL_PASS_SALE || this == STORED_VALUE_SALE)
        }

        private val TRIP_TYPES = mapOf(
                // SEQ, LAX: 0x05 for "Travel Pass" trips.
                0x05 to Type.TRAVEL_PASS_TRIP,
                // SEQ, LAX: 0x31 for "Stored Value" trips / transfers
                0x31 to Type.STORED_VALUE_TRIP,
                // SEQ, LAX: 0x41 for "Travel Pass" sale.
                0x41 to Type.TRAVEL_PASS_SALE,
                // LAX:      0x71 for "Stored Value" sale -- effectively recorded twice
                // (ignored)
                0x71 to Type.IGNORE,
                // SEQ, LAX: input[0] == 0x79 for "Stored Value" sale
                // (ignored)
                0x79 to Type.IGNORE,
                // Minneapolis: input[0] == 0x89 unknown transaction type, no date, only a small
                // number around 100
                0x89 to Type.IGNORE
        )

        fun recordFromBytes(input: ImmutableByteArray, timeZone: MetroTimeZone): NextfareTransactionRecord? {
            //if (input[0] != 0x31) throw new AssertionError("not a tap record");

            val transhead = input[0].toInt() and 0xff
            val transType = TRIP_TYPES[transhead] ?: Type.UNKNOWN

            if (transType == Type.IGNORE) {
                Log.d(TAG, "Ignoring type ${transhead.hexString}")
                return null
            }

            // Check if all the other data is null
            if (input.byteArrayToLong(1, 8) == 0L) {
                Log.d(TAG, "Null transaction record, skipping")
                return null
            }

            val mode = input.byteArrayToInt(1, 1)

            val timestamp = unpackDate(input, 2, timeZone)
            val journey = input.byteArrayToIntReversed(5, 2) shr 5

            val continuation = input.byteArrayToIntReversed(5, 2) and 0x10 > 1

            var value = input.byteArrayToIntReversed(7, 2)
            if (value > 0x8000) {
                value = -(value and 0x7fff)
            }

            val station = input.byteArrayToIntReversed(12, 2)
            val checksum = input.byteArrayToIntReversed(14, 2)

            val record = NextfareTransactionRecord(
                    transType, timestamp, mode, journey, station, value, checksum, continuation)

            Log.d(TAG,"@${record.timestamp}: ${record.type}, mode ${record.mode}, " +
                            "station ${record.station}, value ${record.value}, " +
                            "journey ${record.journey}, cont=${record.isContinuation}")

            return record
        }
    }
}
