//
//  IOSXmppClient.h
//  Viny
//
//  Created by Hai Le Gia on 2/28/13.
//  Copyright (c) 2013 Hai Le Gia. All rights reserved.
//

#ifndef __Viny__IOSXmppClient__
#define __Viny__IOSXmppClient__

#include "talk/base/basicdefs.h"
#include "talk/base/sigslot.h"
#include "talk/base/task.h"
#include "talk/xmpp/xmppclientsettings.h"
#include "talk/xmpp/xmppengine.h"
#include "talk/xmpp/xmpptask.h"
#include "talk/xmpp/xmppclient.h"

#import "XmppClientDelegate.h"
#import "talk/base/messagehandler.h"
using namespace buzz;

namespace tictok {
    class IOSXmppClient : public talk_base::MessageHandler, public XmppTaskParentInterface, public XmppClientInterface, public sigslot::has_slots<>
    {
    private:
        void OnMessage(talk_base::Message* msg);
    public:
        explicit IOSXmppClient(talk_base::TaskParent * parent, XmppClientDelegate* delegate);
        virtual ~IOSXmppClient();
        
        void Connect(const XmppClientSettings & settings, const std::string & lang);
        
        void HandleInput(NSData* data, size_t len);
        
        virtual int ProcessStart();
        virtual int ProcessResponse();
        XmppReturnStatus Disconnect();
        
        sigslot::signal1<XmppEngine::State> SignalStateChange;
        sigslot::signal1<int> SignalCloseEvent;
        
        XmppEngine::Error GetError(int *subcode);
        
        // When there is a <stream:error> stanza, return the stanza
        // so that they can be handled.
        const XmlElement *GetStreamError();
                
        XmppReturnStatus SendRaw(const std::string & text);
        
        XmppEngine* engine();
        
        void EnsureClosed();
        
        sigslot::signal2<const char *, int> SignalLogInput;
        sigslot::signal2<const char *, int> SignalLogOutput;
        
        // As XmppTaskParentIntreface
        virtual XmppClientInterface* GetClient() { return this; }
        
        // As XmppClientInterface
        virtual XmppEngine::State GetState() const;
        virtual const Jid& jid() const;
        virtual std::string NextId();
        virtual XmppReturnStatus SendStanza(const XmlElement *stanza);
        virtual XmppReturnStatus SendStanzaError(const XmlElement * pelOriginal,
                                                 XmppStanzaError code,
                                                 const std::string & text);
        virtual void AddXmppTask(XmppTask *, XmppEngine::HandlerLevel);
        virtual void RemoveXmppTask(XmppTask *);
        
        void ConnectionConnected(const char* fullJid);
        void ConnectionClosed(int code);
    private:
        friend class XmppTask;
        
        class Private;
        friend class Private;
        talk_base::scoped_ptr<Private> d_;
        
        
        XmppClientDelegate* delegate_;
        bool started;
        bool delivering_signal_;
        bool valid_;
    };
}


#endif /* defined(__Viny__IOSXmppClient__) */
