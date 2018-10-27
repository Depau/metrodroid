/*
 * AtHopStubTransitData.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.stub;

import android.os.Parcel;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;

/**
 * Stub implementation for AT HOP (Auckland, NZ).
 * <p>
 * https://github.com/micolous/metrodroid/wiki/AT-HOP
 */
public class AtHopStubTransitData extends StubTransitData {
    public static final Creator<AtHopStubTransitData> CREATOR = new Creator<AtHopStubTransitData>() {
        public AtHopStubTransitData createFromParcel(Parcel parcel) {
            return new AtHopStubTransitData(parcel);
        }

        public AtHopStubTransitData[] newArray(int size) {
            return new AtHopStubTransitData[size];
        }
    };

    public AtHopStubTransitData(Card card) {
    }

    public AtHopStubTransitData(Parcel parcel) {
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, 0x4055) && ArrayUtils.contains(appIds, 0xffffff);
        }

        @Override
        protected CardInfo getCardInfo() {
            return null;
        }

        @Override
        public TransitData parseTransitData(DesfireCard desfireCard) {
            return new AtHopStubTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(DesfireCard desfireCard) {
            return new TransitIdentity("AT HOP", null);
        }
    };

    @Override
    public String getCardName() {
        return "AT HOP";
    }
}
