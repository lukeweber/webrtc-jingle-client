package com.tuenti.voice.core.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Buddy
    implements Parcelable
{
// ------------------------------ FIELDS ------------------------------

    public static final Creator<Buddy> CREATOR = new Creator<Buddy>()
    {
        public Buddy createFromParcel( Parcel in )
        {
            return new Buddy( in );
        }

        public Buddy[] newArray( int size )
        {
            return new Buddy[size];
        }
    };

    private String nick;

    private boolean online;

    private String remoteJid;

// --------------------------- CONSTRUCTORS ---------------------------

    public Buddy()
    {
    }

    public Buddy( Parcel in )
    {
        nick = in.readString();
        online = in.readByte() == 1;
        remoteJid = in.readString();
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getNick()
    {
        return nick;
    }

    public void setNick( String nick )
    {
        this.nick = nick;
    }

    public String getRemoteJid()
    {
        return remoteJid;
    }

    public void setRemoteJid( String remoteJid )
    {
        this.remoteJid = remoteJid;
    }

    public boolean isOnline()
    {
        return online;
    }

    public void setOnline( boolean online )
    {
        this.online = online;
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
        out.writeString( nick );
        out.writeByte( (byte) ( online ? 1 : 0 ) );
        out.writeString( remoteJid );
    }
}
