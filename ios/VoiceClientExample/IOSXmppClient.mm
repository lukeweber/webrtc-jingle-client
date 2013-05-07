//
//  IOSXmppClient.cpp
//  Viny
//
//  Created by Hai Le Gia on 2/28/13.
//  Copyright (c) 2013 Hai Le Gia. All rights reserved.
//

#include "IOSXmppClient.h"
#include "talk/base/stringutils.h"

#include "talk/xmpp/constants.h"
#include "talk/base/messagehandler.h"

using namespace buzz;

namespace tictok
{
    enum MessageDataType {
        HANDLE_INPUT = 0,
        CONNECTION_CONNECTED = 1
    };
    
    struct ConnectionConnectedData: talk_base::MessageData
    {
        ConnectionConnectedData(const char* fullJid) : fullJid_(fullJid) {}
        const char* fullJid_;
    };
    
    struct ConnectionClosedData: talk_base::MessageData
    {
        
    };
    
    struct StateData: talk_base::MessageData {
        StateData(XmppEngine::State state) : state_(state) {}
        XmppEngine::State state_;
    };
    
    struct InputStateData : talk_base::MessageData {
        InputStateData(char* data, size_t len)
        : len_(len) {
            data_ = new char[len];
            memcpy(data_, data, len);
        }
        ~InputStateData() {
            delete[] data_;
        }
        char* data_;
        size_t len_;
    };
    
    class IOSXmppClient::Private : public sigslot::has_slots<>, public XmppSessionHandler, public XmppOutputHandler, public talk_base::MessageHandler
    {
    public:
        explicit Private(IOSXmppClient* client) :
        client_(client),
        engine_(NULL),
        signal_closed_(false)
        {}
        virtual ~Private() {
            
        }
        // the owner
        IOSXmppClient* const client_;
        
        // the two main objects
        talk_base::scoped_ptr<XmppEngine> engine_;
        bool signal_closed_;
        
        // implementations of interfaces
        void OnStateChange(int state);
        void WriteOutput(const char* bytes, size_t len);
        void StartTls(const std::string& domainname);
        void CloseConnection();
        void OnMessage(talk_base::Message* msg);
    };
    
#ifdef IOS_XMPP_FRAMEWORK
    IOSXmppClient::IOSXmppClient(TaskParent* parent, VoiceClientDelegate* voiceClientDelegate) : XmppTaskParentInterface(parent), delivering_signal_(false),valid_(false),voiceClientDelegate_(voiceClientDelegate)
    {
        d_.reset(new Private(this));
        valid_ = true;
    }
#else
    IOSXmppClient::IOSXmppClient(TaskParent* parent) : XmppTaskParentInterface(parent), delivering_signal_(false),valid_(false)
    {
        d_.reset(new Private(this));
        valid_ = true;
    }
#endif
    
    IOSXmppClient::~IOSXmppClient() {
        valid_ = false;
    }
    
    bool IsTestServer(const std::string& server_name,
                      const std::string& test_server_domain) {
        return (!test_server_domain.empty() &&
                talk_base::ends_with(server_name.c_str(),
                                     test_server_domain.c_str()));
    }
    
    void IOSXmppClient::Connect(const buzz::XmppClientSettings &settings, const std::string &lang)
    {
        d_->engine_.reset(XmppEngine::Create());
        d_->engine_->SetSessionHandler(d_.get());
        d_->engine_->SetOutputHandler(d_.get());
        if (!settings.resource().empty()) {
            d_->engine_->SetRequestedResource(settings.resource());
        }
        d_->engine_->SetTls(settings.use_tls());
        
        // The talk.google.com server returns a certificate with common-name:
        //   CN="gmail.com" for @gmail.com accounts,
        //   CN="googlemail.com" for @googlemail.com accounts,
        //   CN="talk.google.com" for other accounts (such as @example.com),
        // so we tweak the tls server setting for those other accounts to match the
        // returned certificate CN of "talk.google.com".
        // For other servers, we leave the strings empty, which causes the jid's
        // domain to be used.  We do the same for gmail.com and googlemail.com as the
        // returned CN matches the account domain in those cases.
        std::string server_name = settings.server().HostAsURIString();
        if (server_name == buzz::STR_TALK_GOOGLE_COM ||
            server_name == buzz::STR_TALKX_L_GOOGLE_COM ||
            server_name == buzz::STR_XMPP_GOOGLE_COM ||
            server_name == buzz::STR_XMPPX_L_GOOGLE_COM ||
            IsTestServer(server_name, settings.test_server_domain())) {
            if (settings.host() != STR_GMAIL_COM &&
                settings.host() != STR_GOOGLEMAIL_COM) {
                d_->engine_->SetTlsServer("", STR_TALK_GOOGLE_COM);
            }
        }
        
        // Set language
        d_->engine_->SetLanguage(lang);
        
        d_->engine_->SetUser(buzz::Jid(settings.user(), settings.host(), STR_EMPTY));
        
        d_->engine_->Connect();
        
    }
    
    void IOSXmppClient::OnMessage(talk_base::Message *message)
    {
        switch (message->message_id) {
            case HANDLE_INPUT:
            {
                InputStateData* data = static_cast<InputStateData*>(message->pdata);
                char* bytes = data->data_;
                size_t len = data->len_;
#ifdef _DEBUG
                SignalLogInput(bytes, len);
#endif
                d_->engine_->HandleInput(bytes, len);
                delete message->pdata;
            }
                break;
            case CONNECTION_CONNECTED:
            {
                ConnectionConnectedData* data = static_cast<ConnectionConnectedData*>(message->pdata);
                d_->engine_->SignalBound(buzz::Jid(data->fullJid_));
                d_->OnStateChange(XmppEngine::STATE_OPEN);
                Wake();
            }
                break;
            default:
                break;
        }
    }
    
    void IOSXmppClient::HandleInput(char* bytes, size_t len)
    {
        if (!IsDone())
        {
            talk_base::Thread* signalThread = voiceClientDelegate_->GetSignalThread();
            signalThread->Post(this, 0, new InputStateData(bytes, len));
        }
        else
        {
            printf("IGNORE MESSAGE BECAUSE I'M DONE.");
        }
        
    }
    
    int IOSXmppClient::ProcessStart()
    {
        return STATE_RESPONSE;
    }
    
    int IOSXmppClient::ProcessResponse()
    {
        if (!delivering_signal_ &&
            (!d_->engine_ || d_->engine_->GetState() == XmppEngine::STATE_CLOSED))
            return STATE_DONE;
        return STATE_BLOCKED;
    }
    
    XmppReturnStatus IOSXmppClient::Disconnect() {
        Abort();
        d_->engine_->Disconnect();
        return XMPP_RETURN_OK;
    }
    
    XmppEngine::Error IOSXmppClient::GetError(int* subcode) {
        if (subcode) {
            *subcode = 0;
        }
        if (!d_->engine_)
            return XmppEngine::ERROR_NONE;
        return d_->engine_->GetError(subcode);
    }
    
    const XmlElement* IOSXmppClient::GetStreamError() {
        if (!d_->engine_) {
            return NULL;
        }
        return d_->engine_->GetStreamError();
    }
    
    XmppReturnStatus IOSXmppClient::SendRaw(const std::string& text) {
        return d_->engine_->SendRaw(text);
    }
    
    XmppEngine* IOSXmppClient::engine() {
        return d_->engine_.get();
    }
    
    void IOSXmppClient::EnsureClosed() {
        if (!d_->signal_closed_) {
            d_->signal_closed_ = true;
            delivering_signal_ = true;
            SignalStateChange(XmppEngine::STATE_CLOSED);
            delivering_signal_ = false;
        }
    }
    
    XmppEngine::State IOSXmppClient::GetState() const {
        if (!d_->engine_)
            return XmppEngine::STATE_NONE;
        return d_->engine_->GetState();
    }
    
    const Jid& IOSXmppClient::jid() const {
        return d_->engine_->FullJid();
    }
    
    std::string IOSXmppClient::NextId() {
        return d_->engine_->NextId();
    }
    
    XmppReturnStatus IOSXmppClient::SendStanza(const XmlElement* stanza) {
        return d_->engine_->SendStanza(stanza);
    }
    
    XmppReturnStatus IOSXmppClient::SendStanzaError(
                                                 const XmlElement* old_stanza, XmppStanzaError xse,
                                                 const std::string& message) {
        return d_->engine_->SendStanzaError(old_stanza, xse, message);
    }

    void IOSXmppClient::AddXmppTask(XmppTask* task, XmppEngine::HandlerLevel level) {
        d_->engine_->AddStanzaHandler(task, level);
    }
    
    void IOSXmppClient::RemoveXmppTask(XmppTask* task) {
        d_->engine_->RemoveStanzaHandler(task);
    }
    
    void IOSXmppClient::ConnectionConnected(const char* fullJid)
    {
        voiceClientDelegate_->GetSignalThread()->Post(this, CONNECTION_CONNECTED, new ConnectionConnectedData(fullJid));
    }
    
    void IOSXmppClient::ConnectionClosed(int code)
    {
        d_->engine_->ConnectionClosed(code);
        SignalCloseEvent(GetState());
    }
    
    void IOSXmppClient::Private::OnStateChange(int state) {
        client_->voiceClientDelegate_->GetSignalThread()->Post(this, 0, new StateData((XmppEngine::State)state));
    }
    
    void IOSXmppClient::Private::OnMessage(talk_base::Message *msg)
    {
        StateData* stateData = static_cast<StateData*>(msg->pdata);
        XmppEngine::State state = stateData->state_;
        if (state == XmppEngine::STATE_CLOSED) {
            client_->EnsureClosed();
        }
        else {
            client_->SignalStateChange(state);
        }
        delete msg->pdata;
        client_->Wake();
    }
    
    void IOSXmppClient::Private::WriteOutput(const char* bytes, size_t len) {
#ifdef _DEBUG
        client_->SignalLogOutput(bytes, len);
#endif
        client_->voiceClientDelegate_->WriteOutput(bytes, len);
        // TODO: deal with error information
    }
    
    void IOSXmppClient::Private::StartTls(const std::string& domain) {
        client_->voiceClientDelegate_->StartTls(domain);
    }
    
    void IOSXmppClient::Private::CloseConnection() {
        client_->voiceClientDelegate_->CloseConnection();
    }
}
