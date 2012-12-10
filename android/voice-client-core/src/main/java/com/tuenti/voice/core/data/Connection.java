package com.tuenti.voice.core.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Connection
    implements Parcelable
{
// ------------------------------ FIELDS ------------------------------

    public static final Creator<Connection> CREATOR = new Creator<Connection>()
    {
        public Connection createFromParcel( Parcel in )
        {
            return new Connection( in );
        }

        public Connection[] newArray( int size )
        {
            return new Connection[size];
        }
    };

    private String password;

    private String relayHost;

    private String stunHost;

    private String turnHost;

    private String turnPassword;

    private String turnUsername;

    private String username;

    private String xmppHost;

    private int xmppPort;

    private boolean xmppUseSsl;

// --------------------------- CONSTRUCTORS ---------------------------

    public Connection()
    {
    }

    public Connection( Parcel in )
    {
        password = in.readString();
        relayHost = in.readString();
        stunHost = in.readString();
        turnHost = in.readString();
        turnUsername = in.readString();
        turnPassword = in.readString();
        username = in.readString();
        xmppHost = in.readString();
        xmppPort = in.readInt();
        xmppUseSsl = in.readByte() == 1;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getRelayHost()
    {
        return relayHost;
    }

    public void setRelayHost( String relayHost )
    {
        this.relayHost = relayHost;
    }

    public String getStunHost()
    {
        return stunHost;
    }

    public void setStunHost( String stunHost )
    {
        this.stunHost = stunHost;
    }

    public String getTurnHost()
    {
        return turnHost;
    }

    public void setTurnHost( String turnHost )
    {
        this.turnHost = turnHost;
    }

    public String getTurnPassword()
    {
        return turnPassword;
    }

    public void setTurnPassword( String turnPassword )
    {
        this.turnPassword = turnPassword;
    }

    public String getTurnUsername()
    {
        return turnUsername;
    }

    public void setTurnUsername( String turnUsername )
    {
        this.turnUsername = turnUsername;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getXmppHost()
    {
        return xmppHost;
    }

    public void setXmppHost( String xmppHost )
    {
        this.xmppHost = xmppHost;
    }

    public int getXmppPort()
    {
        return xmppPort;
    }

    public void setXmppPort( int xmppPort )
    {
        this.xmppPort = xmppPort;
    }

    public boolean getXmppUseSsl()
    {
        return xmppUseSsl;
    }

    public void setXmppUseSsl( boolean xmppUseSsl )
    {
        this.xmppUseSsl = xmppUseSsl;
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
        out.writeString( password );
        out.writeString( relayHost );
        out.writeString( stunHost );
        out.writeString( turnHost );
        out.writeString( turnUsername );
        out.writeString( turnPassword );
        out.writeString( username );
        out.writeString( xmppHost );
        out.writeInt( xmppPort );
        out.writeByte( (byte) ( xmppUseSsl ? 1 : 0 ) );
    }
}
