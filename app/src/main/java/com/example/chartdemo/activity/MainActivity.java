package com.example.chartdemo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chartdemo.util.Constants;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView panel;
    private ScrollView scrollerPanel;
    private EditText account, password,to,content;
    private Button login,logout,send;
    private  XMPPTCPConnection connection;
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what){
                case 1:
                    Toast.makeText(getApplicationContext(),msg.obj+"",Toast.LENGTH_SHORT).show();
                    panel.append(msg.obj+"\n");
                    scrollerPanel.scrollTo(0,panel.getBottom());
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        try {
            initView();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.login:{
                final String a = account.getText().toString();
                final String p = password.getText().toString();
                if (TextUtils.isEmpty(a) || TextUtils.isEmpty(p)) {
                    Toast.makeText(getApplicationContext(), "账号或密码不能为空", Toast.LENGTH_LONG).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            connection.connect();
                            connection.login(a, p);
                            Presence presence = new Presence(Presence.Type.available);
                            presence.setStatus("我是在线状态");
                            connection.sendStanza(presence);
                            ChatManager chatManager = ChatManager.getInstanceFor(connection);
                            chatManager.addIncomingListener(new IncomingChatMessageListener() {
                                @Override
                                public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                                    String content=message.getBody();
                                    if (content!=null){
                                        Log.e("TAG", "from:" + message.getFrom() + " to:" + message.getTo() + " message:" + message.getBody());
                                        android.os.Message message1= android.os.Message.obtain();
                                        message1.what=1;
                                        message1.obj="收到消息：" + message.getBody()+" 来自:"+message.getFrom();
                                        mHandler.sendMessage(message1);
                                    }
                                }
                            });
                        } catch (SmackException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (XMPPException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
                break;
            }
            case R.id.logout:
                connection.disconnect();
                break;
            case R.id.send:
                final String t = to.getText().toString();
                final String c = content.getText().toString();
                if (TextUtils.isEmpty(t)||TextUtils.isEmpty(c)) {
                    Toast.makeText(getApplicationContext(), "接收方或内容", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    ChatManager chatManager = ChatManager.getInstanceFor(connection);
                    //JidCreate.entityBareFrom("用户名@域名");获取用户ID
                    EntityBareJid jid = JidCreate.entityBareFrom(t+"@"+Constants.IP);
                    Chat chat = chatManager.chatWith(jid);
                    chat.send(c);
                }
                catch (SmackException.NotConnectedException | InterruptedException | XmppStringprepException e) {
                    e.printStackTrace();
                }
                break;
        }

    }

    private void initView() throws XmppStringprepException, UnknownHostException {
        connection=getConnection();
        scrollerPanel = findViewById(R.id.scroll_panel);
        panel = findViewById(R.id.panel);
        account = findViewById(R.id.account);
        password = findViewById(R.id.password);
        to = findViewById(R.id.to);
        content = findViewById(R.id.content);
        login = findViewById(R.id.login);
        logout = findViewById(R.id.logout);
        send = findViewById(R.id.send);
        login.setOnClickListener(this);
        logout.setOnClickListener(this);
        send.setOnClickListener(this);
    }

    private XMPPTCPConnection getConnection() throws XmppStringprepException, UnknownHostException {
        InetAddress address = InetAddress.getByName(Constants.IP);
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(Constants.IP)//设置xmpp域名
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)//安全模式认证
                .setHostAddress(address)
                .setPort(Constants.PORT)
                .setCompressionEnabled(false)
                .setSendPresence(true)
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(config);//连接类
        return connection;
    }
}
