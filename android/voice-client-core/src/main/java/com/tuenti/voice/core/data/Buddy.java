package com.tuenti.voice.core.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.tuenti.voice.core.XmppPresenceAvailable;
import com.tuenti.voice.core.XmppPresenceShow;

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

    private XmppPresenceAvailable available;

    private String nick;

    private String remoteJid;

    private XmppPresenceShow show;

// --------------------------- CONSTRUCTORS ---------------------------

    public Buddy()
    {
    }

    public Buddy( Parcel in )
    {
        nick = in.readString();
        remoteJid = in.readString();
        available = XmppPresenceAvailable.fromInteger( in.readInt() );
        show = XmppPresenceShow.fromInteger( in.readInt() );
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public XmppPresenceAvailable getAvailable()
    {
        return available;
    }

    public void setAvailable( XmppPresenceAvailable available )
    {
        this.available = available;
    }

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

    public XmppPresenceShow getShow()
    {
        return show;
    }

    public void setShow( XmppPresenceShow show )
    {
        this.show = show;
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
        out.writeString( remoteJid );
        out.writeInt( available.ordinal() );
        out.writeInt( show.ordinal() );
    }

// -------------------------- OTHER METHODS --------------------------

    public String getName()
    {
        if ( !TextUtils.isEmpty( nick ) )
        {
            return nick;
        }
        return remoteJid;
    }
}
