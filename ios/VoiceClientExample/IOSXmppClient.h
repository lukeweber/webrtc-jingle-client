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

#include "talk/base/messagehandler.h"

#ifdef IOS_XMPP_FRAMEWORK
#include "VoiceClientExample/VoiceClientDelegate.h"
#endif

namespace tictok {
    class IOSXmppClient : public talk_base::MessageHandler, public buzz::XmppTaskParentInterface, public buzz::XmppClientInterface, public sigslot::has_slots<>
    {
    private:
        void OnMessage(talk_base::Message* msg);
    public:
#ifdef IOS_XMPP_FRAMEWORK
        explicit IOSXmppClient(talk_base::TaskParent * parent, VoiceClientDelegate* voiceClientDelegate);
#else
        explicit IOSXmppClient(talk_base::TaskParent * parent);
#endif
        virtual ~IOSXmppClient();
        
        void Connect(const buzz::XmppClientSettings & settings, const std::string & lang);
        
        void HandleInput(char* bytes, size_t len);
        
        virtual int ProcessStart();
        virtual int ProcessResponse();
        buzz::XmppReturnStatus Disconnect();
        
        sigslot::signal1<buzz::XmppEngine::State> SignalStateChange;
        sigslot::signal1<int> SignalCloseEvent;
        
        buzz::XmppEngine::Error GetError(int *subcode);
        
        // When there is a <stream:error> stanza, return the stanza
        // so that they can be handled.
        const buzz::XmlElement *GetStreamError();
                
        buzz::XmppReturnStatus SendRaw(const std::string & text);
        
        buzz::XmppEngine* engine();
        
        void EnsureClosed();
        
        sigslot::signal2<const char *, int> SignalLogInput;
        sigslot::signal2<const char *, int> SignalLogOutput;
        
        // As XmppTaskParentIntreface
        virtual XmppClientInterface* GetClient() { return this; }
        
        // As XmppClientInterface
        virtual buzz::XmppEngine::State GetState() const;
        virtual const buzz::Jid& jid() const;
        virtual std::string NextId();
        virtual buzz::XmppReturnStatus SendStanza(const buzz::XmlElement *stanza);
        virtual buzz::XmppReturnStatus SendStanzaError(const buzz::XmlElement * pelOriginal,
                                                 buzz::XmppStanzaError code,
                                                 const std::string & text);
        virtual void AddXmppTask(buzz::XmppTask *, buzz::XmppEngine::HandlerLevel);
        virtual void RemoveXmppTask(buzz::XmppTask *);
        
        void ConnectionConnected(const char* fullJid);
        void ConnectionClosed(int code);
    private:
        friend class XmppTask;
        class Private;
        friend class Private;
        talk_base::scoped_ptr<Private> d_;
        bool started;
        bool delivering_signal_;
        bool valid_;
#ifdef IOS_XMPP_FRAMEWORK
        VoiceClientDelegate* voiceClientDelegate_;
#endif
    };
}


#endif /* defined(__Viny__IOSXmppClient__) */
