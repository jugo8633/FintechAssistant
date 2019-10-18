package org.iii.chihlee;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity
{
    private final int MSG_RUN_MAIN = 0;
    private final int MSG_RUN_CHAT = 1;
    
    static final HashMap<Integer, Class<?>> mapActivity = new HashMap<Integer, Class<?>>()
    {{
        put(R.id.imageView_introduce, ActivityIntroduce.class);
        put(R.id.imageView_map, ActivityMap.class);
        put(R.id.imageView_chat, ActivityChat.class);
        put(R.id.imageViewIP, ActivitySetting.class);
    }};
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
        handler.sendEmptyMessageDelayed(MSG_RUN_CHAT, 2000);
    }
    
    @Override
    protected void onPause()
    {
        handler.removeMessages(MSG_RUN_CHAT);
        super.onPause();
    }
    
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_RUN_MAIN:
                    handler.removeMessages(MSG_RUN_MAIN);
                    setContentView(R.layout.activity_main);
                    initMain();
                    break;
                case MSG_RUN_CHAT:
                    handler.removeMessages(MSG_RUN_CHAT);
                    startActivity(new Intent(MainActivity.this, ActivityChat.class));
                    break;
            }
        }
    };
    
    private void initMain()
    {
        for (int ImageViewId : mapActivity.keySet())
            findViewById(ImageViewId).setOnClickListener(viewOnClick);
    }
    
    private View.OnClickListener viewOnClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
           
            startActivity(new Intent(MainActivity.this, mapActivity.get(v.getId())));
        }
    };
    
    
}
