package org.iii.chihlee;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;

import org.iii.chihlee.datatransfer.Controller;
import org.iii.chihlee.datatransfer.SemanticClient;
import org.iii.more.common.Logs;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ActivityChat extends AppCompatActivity implements TextToSpeech.OnInitListener
{
    private final int MSG_RESP = 666;
    //private final String IP = "140.92.142.22";
    private String IP = "127.0.0.1";
    private final int PORT = 1414;
    private ImageView imageViewRobot;
    private int voiceRecognitionRequestCode = 777;
    TextToSpeech mTTS = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        handlerTimer.sendEmptyMessageDelayed(0, 3000);
    }
    
    private void initChat()
    {
        setContentView(R.layout.activity_chat);
        imageViewRobot = findViewById(R.id.imageViewRobot);
        ImageView imageViewChatBg = findViewById(R.id.imageViewMatrixBg);
    
        RequestManager requestManager = Glide.with(this);
        RequestBuilder requestBuilder = requestManager.load(R.drawable.ku_talk);
        requestBuilder.into(imageViewRobot);
    
        findViewById(R.id.imageViewRobot).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                saySomething("您想要的服務是", 0);
                startVoiceRecognitionActivity();
            }
        });
    
        mTTS = new TextToSpeech(this, this);
    
        findViewById(R.id.imageViewIP).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(ActivityChat.this, ActivitySetting.class));
            }
        });
    }
    
    @Override
    protected void onStart()
    {
        IP = ConfigHandler.restoreIP(this);
        super.onStart();
    }
    
    @Override
    protected void onStop()
    {
        super.onStop();
    }
    
    private void startVoiceRecognitionActivity()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "您想要的服務是");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        startActivityForResult(intent, voiceRecognitionRequestCode);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (requestCode == voiceRecognitionRequestCode && resultCode == Activity.RESULT_OK)
        {
            ArrayList<String> matches =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            // 語音識別會有多個結果，第一個是最精確的
            String text = matches.get(0);
            if (null != text && 0 < text.length())
            {
                Controller.CMP_PACKET respPacket = new Controller.CMP_PACKET();
                send(text, respPacket);
                Logs.showTrace("speech: " + text);
                ((TextView) findViewById(R.id.textViewSpeech)).setText(text);
                
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public void send(JSONObject jsonObject, Controller.CMP_PACKET respPacket)
    {
        Thread thread = new Thread(new SocketSend(IP, PORT, jsonObject.toString(), respPacket,
                handler));
        thread.start();
    }
    
    public void send(String strText, Controller.CMP_PACKET respPacket)
    {
        Thread thread = new Thread(new SocketSend(IP, PORT, strText, respPacket,
                handler));
        thread.start();
    }
    
    private class SocketSend implements Runnable
    {
        String IP;
        int PORT;
        String text;
        Controller.CMP_PACKET resp;
        Handler lh;
        
        SocketSend(final String strIP, final int nPort, final String strBody,
                Controller.CMP_PACKET respPacket, Handler hh)
        {
            IP = strIP;
            PORT = nPort;
            text = strBody;
            resp = respPacket;
            lh = hh;
        }
        
        @Override
        public void run()
        {
            Logs.showTrace("[ActivityChat] SocketSend IP: " + IP + " text: " + text);
            Controller.cmpRequest(IP, PORT, Controller.assistant_request, text, resp);
            Message msg = new Message();
            msg.what = MSG_RESP;
            msg.obj = resp.cmpBody;
            lh.sendMessage(msg);
        }
    }
    
    private void saySomething(String text, int qmode)
    {
        ((TextView) findViewById(R.id.textView_chat_tts)).setText(text);
        if (qmode == 1)
        {
            mTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
        else
        {
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    
    /**
     * tts init callback
     * @param status
     */
    @Override
    public void onInit(int status)
    {
        if (status == TextToSpeech.SUCCESS)
        {
            if (mTTS != null)
            {
                int result = mTTS.setLanguage(Locale.TAIWAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                }
                else
                {
                    imageViewRobot.setVisibility(View.VISIBLE);
                }
            }
        }
        else
        {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_LONG).show();
        }
    }
    
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if (MSG_RESP == msg.what)
            {
                String strResp = (String) msg.obj;
                Logs.showTrace("[Handler] handleMessage CMP Response: " + strResp);
                try
                {
                    saySomething(strResp, 0);
                }
                catch (Exception e)
                {
                    Logs.showError(e.getMessage());
                }
            }
        }
    };
    
    @SuppressLint("HandlerLeak")
    private Handler handlerTimer = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 0:
                    initChat();
                    break;
            }
        }
    };
    
   
}
