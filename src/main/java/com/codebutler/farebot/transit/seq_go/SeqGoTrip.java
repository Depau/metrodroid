/*
 * SeqGoTrip.java
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
package com.codebutler.farebot.transit.seq_go;

import android.os.Parcel;
import android.os.Parcelable;

import com.codebutler.farebot.transit.Station;
import com.codebutler.farebot.transit.Trip;

import java.text.NumberFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Represents trip events on Go Card.
 */
public class SeqGoTrip extends Trip implements Comparable<SeqGoTrip> {
    int mJourneyId;
    Mode mMode;
    GregorianCalendar mStartTime;
    GregorianCalendar mEndTime;
    int mStartStation;
    int mEndStation;
    boolean mContinuation;
    int mTripCost = 0;
    boolean mKnownCost = false;

    @Override
    public long getTimestamp() {
        if (mStartTime != null) {
            return mStartTime.getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public long getExitTimestamp() {
        if (mEndTime != null) {
            return mEndTime.getTimeInMillis() / 1000;
        } else {
            return 0;
        }
    }

    @Override
    public String getRouteName() {
        return null;
    }

    @Override
    public String getAgencyName() {
        switch (mMode) {
            case FERRY:
                return "Transdev Brisbane Ferries";
            case TRAIN:
                // Domestic Airport == 9
                if (mStartStation == 9 || mEndStation == 9) {
                    // TODO: Detect International Airport station.
                    return "Airtrain";
                } else {
                    return "Queensland Rail";
                }
            default:
                return "TransLink";
        }
    }

    @Override
    public String getShortAgencyName() {
        return getAgencyName();
    }

    @Override
    public String getFareString() {
        // We can't use the public accessors here, because we want access to the extra zone info
        SeqGoStation startStation = SeqGoUtil.getStation(mStartStation);
        SeqGoStation endStation = SeqGoUtil.getStation(mEndStation);

        if (startStation != null && endStation != null) {
            return NumberFormat.getCurrencyInstance(Locale.US).format((double)mTripCost / 100.);
        } else {
            return null;
        }
    }

    public String getStartZone() {
        SeqGoStation startStation = SeqGoUtil.getStation(mStartStation);

        if (startStation != null) {
            return startStation.getZone();
        } else {
            return null;
        }
    }

    public String getEndZone() {
        SeqGoStation endStation = SeqGoUtil.getStation(mEndStation);
        if (endStation != null) {
            return endStation.getZone();
        } else {
            return null;
        }
    }

    public boolean isAirtrainZoneExempt() {
        SeqGoStation startStation = SeqGoUtil.getStation(mStartStation);
        SeqGoStation endStation = SeqGoUtil.getStation(mEndStation);

        if (startStation == null || endStation == null) {
            // We don't know. :(
            return false;
        }

        if (startStation.getZone().equals("airtrain") && endStation.isAirtrainZoneExempt()) {
            return true;
        }

        if (endStation.getZone().equals("airtrain") && startStation.isAirtrainZoneExempt()) {
            return true;
        }

        return false;
    }

    @Override
    public String getBalanceString() {
        return null;
    }

    @Override
    public String getStartStationName() {
        if (mStartStation == 0) {
            return null;
        } else {
            Station s = getStartStation();
            if (s == null) {
                return "Unknown (" + Integer.toString(mStartStation) + ")";
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getStartStation() {
        return SeqGoUtil.getStation(mStartStation);
    }

    @Override
    public String getEndStationName() {
        if (mEndStation == 0) {
            return null;
        } else {
            Station s = getEndStation();
            if (s == null) {
                return "Unknown (" + Integer.toString(mEndStation) + ")";
            } else {
                return s.getStationName();
            }
        }
    }

    @Override
    public Station getEndStation() {
        return SeqGoUtil.getStation(mEndStation);
    }

    @Override
    public boolean hasFare() {
        return mKnownCost;
    }

    @Override
    public Mode getMode() {
        return mMode;
    }

    @Override
    public boolean hasTime() {
        return mStartTime != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mJourneyId);
        parcel.writeLong(mStartTime == null ? 0 : mStartTime.getTimeInMillis());
        parcel.writeLong(mEndTime == null ? 0 : mEndTime.getTimeInMillis());
        parcel.writeString(mMode.toString());
        parcel.writeInt(mStartStation);
        parcel.writeInt(mEndStation);
    }

    public SeqGoTrip(Parcel parcel) {
        mJourneyId = parcel.readInt();
        long startTime = parcel.readLong();
        if (startTime != 0) {
            mStartTime = new GregorianCalendar();
            mStartTime.setTimeInMillis(startTime);
        }

        long endTime = parcel.readLong();
        if (endTime != 0) {
            mEndTime = new GregorianCalendar();
            mEndTime.setTimeInMillis(endTime);
        }

        mMode = Mode.valueOf(parcel.readString());
        mStartStation = parcel.readInt();
        mEndStation = parcel.readInt();


    }

    /**
     * This constructor is used for unit tests outside of the package
     * @param startStation Starting station ID.
     * @param endStation Ending station ID.
     * @param startTime Start time of the journey.
     * @param endTime End time of the journey.
     * @param journeyId Journey ID.
     * @param continuation True if this is a continuation of a previous journey (transfer).
     */
    public SeqGoTrip(int startStation, int endStation, GregorianCalendar startTime, GregorianCalendar endTime, int journeyId, boolean continuation) {
        mStartStation = startStation;
        mEndStation = endStation;
        mStartTime = startTime;
        mEndTime = endTime;
        mJourneyId = journeyId;
        mContinuation = continuation;
    }

    public int getJourneyId() {
        return mJourneyId;
    }

    public boolean isContinuation() {
        return mContinuation;
    }

    public SeqGoTrip() {}

    public static final Parcelable.Creator<SeqGoTrip> CREATOR = new Parcelable.Creator<SeqGoTrip>() {

        public SeqGoTrip createFromParcel(Parcel in) {
            return new SeqGoTrip(in);
        }

        public SeqGoTrip[] newArray(int size) {
            return new SeqGoTrip[size];
        }
    };

    @Override
    public int compareTo(SeqGoTrip other) {
        return Long.valueOf(this.getTimestamp()).compareTo(Long.valueOf(other.getTimestamp()));
    }
}
