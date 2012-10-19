package com.tuenti.voice.example.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Call
    implements Parcelable
{
// ------------------------------ FIELDS ------------------------------

    public static final Parcelable.Creator<Call> CREATOR = new Parcelable.Creator<Call>()
    {
        public Call createFromParcel( Parcel in )
        {
            return new Call( in );
        }

        public Call[] newArray( int size )
        {
            return new Call[size];
        }
    };

    private long callId;

    private long callStartTime;

    private boolean hold;

    private boolean mute;

    private String remoteJid;

// --------------------------- CONSTRUCTORS ---------------------------

    public Call( Parcel in )
    {
        callId = in.readLong();
        hold = in.readByte() == 1;
        mute = in.readByte() == 1;
        remoteJid = in.readString();
        callStartTime = in.readLong();
    }

    public Call( long callId, String remoteJid )
    {
        this.callId = callId;
        this.remoteJid = remoteJid;
        this.hold = false;
        this.mute = false;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public long getCallId()
    {
        return callId;
    }

    public void setCallId( long callId )
    {
        this.callId = callId;
    }

    public long getCallStartTime()
    {
        return callStartTime;
    }

    public void setCallStartTime( long callStartTime )
    {
        this.callStartTime = callStartTime;
    }

    public String getRemoteJid()
    {
        return remoteJid;
    }

    public void setRemoteJid( String remoteJid )
    {
        this.remoteJid = remoteJid;
    }

    public boolean isHold()
    {
        return hold;
    }

    public void setHold( boolean hold )
    {
        this.hold = hold;
    }

    public boolean isMute()
    {
        return mute;
    }

    public void setMute( boolean mute )
    {
        this.mute = mute;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Parcelable ---------------------

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel( Parcel out, int flags )
    {
        out.writeLong( callId );
        out.writeByte( (byte) ( hold ? 1 : 0 ) );
        out.writeByte( ( (byte) ( mute ? 1 : 0 ) ) );
        out.writeString( remoteJid );
        out.writeLong( callStartTime );
    }
}
