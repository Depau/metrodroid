/*
 * SuicaTransitData.kt
 *
 * Copyright 2011 Kazzz
 * Copyright 2014-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google Inc.
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

package au.id.micolous.metrodroid.transit.suica

import au.id.micolous.metrodroid.time.Duration
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.felica.FelicaBlock
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.felica.FelicaCardTransitFactory
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.NumberUtils

@Parcelize
class SuicaTransitData (override val trips: List<SuicaTrip>): TransitData() {

    public override val balance: TransitBalance?
        get() {
            if (!trips.isEmpty()) {
                val expiry = (trips[0].endTimestamp ?: trips[0].startTimestamp)?.plus(Duration.yearsLocal(10))
                return TransitBalanceStored(TransitCurrency.JPY(trips[0].balance), null, expiry)
            }
            return null
        }

    // FIXME: Find where this is on the card.
    override val serialNumber: String?
        get() = null

    // FIXME: Could be ICOCA, etc.
    override val cardName: String
        get() = Localizer.localizeString(R.string.card_name_suica)

    companion object {
        private fun parse(card: FelicaCard): SuicaTransitData? {
            val system = card.getSystem(SYSTEMCODE_SUICA) ?: return null
            val service = system.getService(SERVICE_SUICA_HISTORY)
            val tapService = system.getService(SERVICE_SUICA_INOUT)

            val matchedTaps = mutableSetOf<Int>()
            val trips = mutableListOf<SuicaTrip>()

            // Read blocks oldest-to-newest to calculate fare.
            val blocks = service?.blocks ?: return null
            var tapBlocks: List<FelicaBlock>? = tapService?.blocks
            for (i in 0 until blocks.size) {
                val block = blocks[i]

                var previousBalance = -1
                if (i + 1 < blocks.size)
                    previousBalance = blocks[i + 1].data.byteArrayToIntReversed(
                            10, 2)
                val trip = SuicaTrip.parse(block, previousBalance)

                if (trip.startTimestamp == null) {
                    continue
                }

                /*
                Log.d(TAG, String.format(Locale.ENGLISH,
                        "On %s, consoletype = %s, station = %s -> %s",
                        NumberUtils.isoDateFormat(trip.getStartTimestamp()),
                        Integer.toHexString(trip.getConsoleTypeInt()),
                        Integer.toHexString(trip.getStartStationId()),
                        Integer.toHexString(trip.getEndStationId())));
                */

                if (tapBlocks != null && trip.consoleTypeInt == 0x16) {
                    for (matchingTap in 0 until tapBlocks.size) {
                        if (matchedTaps.contains(matchingTap)) continue
                        val tapBlock = tapBlocks[matchingTap].data

                        /*
                        Log.d(TAG, String.format(Locale.ENGLISH,
                                "tap off block %d; station %s (%s), datenum %s (%s), fare %s (%s), %d %d",
                                matchingTap,
                                station, trip.getEndStationId(),
                                dateNum, trip.getDateRaw(),
                                fare, trip.getFareRaw(),
                                (tapBlock[0] & 0x80), (tapBlock[4] >> 4)
                                ));
                        Log.d(TAG, String.format("time is %02d:%02d", NumberUtils.convertBCDtoInteger(tapBlock[8]),
                                NumberUtils.convertBCDtoInteger(tapBlock[9])));
                        */

                        // Skip tap-ons
                        // Don't check (tapBlock[4] >> 4) != 2, as this is only applicable on JR East.
                        // JR West and JR East use the same Area Code.
                        if (tapBlock[0].toInt() and 0x80 != 0)
                            continue

                        val station = tapBlock.byteArrayToInt(2, 2)
                        if (station != trip.endStationId)
                            continue

                        val dateNum = tapBlock.byteArrayToInt(6, 2)
                        if (dateNum != trip.dateRaw)
                            continue

                        val fare = tapBlock.byteArrayToIntReversed(10, 2)
                        if (fare != trip.fareRaw)
                            continue

                        trip.setEndTime(NumberUtils.convertBCDtoInteger(tapBlock[8]),
                                NumberUtils.convertBCDtoInteger(tapBlock[9]))
                        matchedTaps.add(matchingTap)
                        break
                    }
                    for (matchingTap in 0 until tapBlocks.size) {
                        if (matchedTaps.contains(matchingTap)) continue
                        val tapBlock = tapBlocks[matchingTap].data

                        /*
                        int fare = NumberUtils.byteArrayToIntReversed(tapBlock, 10, 2);
                        Log.d(TAG, String.format(Locale.ENGLISH,
                                "tap on block %d; station %s (%s), datenum %s (%s), fare %s (%s), %d %d",
                                matchingTap,
                                station, trip.getStartStationId(),
                                dateNum, trip.getDateRaw(),
                                fare, trip.getFareRaw(),
                                (tapBlock[0] & 0x80), (tapBlock[4] >> 4)
                        ));

                        Log.d(TAG, String.format("time is %02d:%02d", NumberUtils.convertBCDtoInteger(tapBlock[8]),
                                NumberUtils.convertBCDtoInteger(tapBlock[9])));
                        */

                        // Skip tap-offs
                        // Don't check (tapBlock[4] >> 4) != 1, as this is only applicable on JR East.
                        // JR West and JR East use the same Area Code.
                        if (tapBlock[0].toInt() and 0x80 == 0)
                            continue

                        val station = tapBlock.byteArrayToInt(2, 2)
                        if (station != trip.startStationId)
                            continue

                        val dateNum = tapBlock.byteArrayToInt(6, 2)
                        if (dateNum != trip.dateRaw)
                            continue

                        trip.setStartTime(NumberUtils.convertBCDtoInteger(tapBlock[8]),
                                NumberUtils.convertBCDtoInteger(tapBlock[9]))
                        matchedTaps.add(matchingTap)
                        break
                    }

                    // Check if we have matched every tap we can, if so, destroy the tap list so we
                    // don't peek again.
                    if (matchedTaps.size == tapBlocks.size) {
                        tapBlocks = null
                    }
                }

                trips.add(trip)
            }

            /*
            Log.d(TAG, String.format(Locale.ENGLISH, "Found %d unmatched taps", tapBlocks == null ? 0 : tapBlocks.size()));
            Log.d(TAG, String.format(Locale.ENGLISH, "Matched %d taps", matchedTaps.size()));
            */

            return SuicaTransitData(trips)
        }


        private val ICOCA_CARD_INFO = CardInfo(
                imageId = R.drawable.icoca_card,
                name =Localizer.localizeString(R.string.card_name_icoca),
                locationId = R.string.location_kansai,
                cardType = CardType.FeliCa)

        private val SUICA_CARD_INFO = CardInfo(
                imageId = R.drawable.suica_card,
                name = Localizer.localizeString(R.string.card_name_suica),
                locationId = R.string.location_tokyo,
                cardType = CardType.FeliCa)

        private val PASMO_CARD_INFO = CardInfo(
                imageId = R.drawable.pasmo_card,
                name = Localizer.localizeString(R.string.card_name_pasmo),
                locationId = R.string.location_tokyo,
                cardType = CardType.FeliCa)

        const val SYSTEMCODE_SUICA = 0x0003

        const val SERVICE_SUICA_INOUT = 0x108f
        const val SERVICE_SUICA_HISTORY = 0x090f

        internal const val TAG = "SuicaTransitData"

        val FACTORY: FelicaCardTransitFactory = object : FelicaCardTransitFactory {

            override val allCards: List<CardInfo>
                get() = listOf(SUICA_CARD_INFO, ICOCA_CARD_INFO, PASMO_CARD_INFO)

            override fun earlyCheck(systemCodes: List<Int>) = SYSTEMCODE_SUICA in systemCodes

            override fun parseTransitData(card: FelicaCard) = parse(card)

            override fun parseTransitIdentity(card: FelicaCard): TransitIdentity {
                return TransitIdentity(Localizer.localizeString(R.string.card_name_suica), null) // FIXME: Could be ICOCA, etc.
            }
        }
    }
}
