package com.gold.kds517.supremetv.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gold.kds517.supremetv.R;
import com.gold.kds517.supremetv.adapter.CategoryAdapter;
import com.gold.kds517.supremetv.adapter.EpgAdapter;
import com.gold.kds517.supremetv.apps.Constants;
import com.gold.kds517.supremetv.apps.MyApp;
import com.gold.kds517.supremetv.dialog.ConnectionDlg;
import com.gold.kds517.supremetv.dialog.PinDlg;
import com.gold.kds517.supremetv.models.EPGChannel;
import com.gold.kds517.supremetv.models.EPGEvent;
import com.gold.kds517.supremetv.models.FullModel;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class TvGuideActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback, IVLCVout.Callback {
    private String TAG=getClass().getSimpleName();
    private EpgAdapter epgAdapter;
    private ImageView current_channel_image;
    private RecyclerView category_recycler_view,epg_recyclerview;
    private TextView duration, title, content, channel_name;
    private int categoryPos=0, channelPos=0, programPos;
    private RelativeLayout ly_surface;
    private CategoryAdapter categoryAdapter;
    public static SurfaceView surfaceView;
    SurfaceView remote_subtitles_surface;
    private SurfaceHolder holder;
    LinearLayout def_lay;
    String ratio;
    String[] resolutions ;
    private int mVideoWidth;
    private int mVideoHeight;
    private EPGChannel selectedEpgChannel;
    private MediaPlayer mMediaPlayer = null;
    private boolean is_data_loaded = false;
    LibVLC libvlc=null;
    private String contentUri;
    private TextView txt_time;
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction()==KeyEvent.ACTION_UP){
            if (event.getKeyCode()==KeyEvent.KEYCODE_BACK){
                if (category_recycler_view.getVisibility()==View.GONE) {
                    category_recycler_view.setVisibility(View.VISIBLE);
//                    epgAdapter.setChannelPos(channelPos);
//                    categoryAdapter.setSelected(categoryPos);
                    category_recycler_view.scrollToPosition(categoryPos);
                    category_recycler_view.requestFocus();
                    category_recycler_view.performClick();
                }
                else finish();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    class CountDownRunner implements Runnable {
        // @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doWork();
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void doWork() {
        runOnUiThread(() -> {
            try {
                txt_time.setText(Constants.date_format.format(new Date()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_guide);
        epg_recyclerview = findViewById(R.id.epg_recyclerview);
        epg_recyclerview.setLayoutManager(new LinearLayoutManager(this));

        category_recycler_view = findViewById(R.id.category_recyclerview);
        category_recycler_view.setLayoutManager(new LinearLayoutManager(this));

        current_channel_image = findViewById(R.id.current_channel_image);
        duration = findViewById(R.id.textView4);
        title = findViewById(R.id.textView7);
        content = findViewById(R.id.textView8);
        channel_name = findViewById(R.id.channel_name);
        def_lay = findViewById(R.id.def_lay);
        surfaceView = findViewById(R.id.surface_view);
        txt_time = findViewById(R.id.txt_time);

        ly_surface = findViewById(R.id.ly_surface);
        ly_surface.setOnClickListener(this);
        holder = surfaceView.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBX_8888);

        remote_subtitles_surface = findViewById(R.id.remote_subtitles_surface);
        remote_subtitles_surface.setZOrderMediaOverlay(true);
        remote_subtitles_surface.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        Thread myThread;
        Runnable runnable = new CountDownRunner();
        myThread = new Thread(runnable);
        myThread.start();
        if (!is_data_loaded){
            new Thread(this::callAllEpg).start();
        }else{
            runOnUiThread(this::setUI);
        }
        FullScreencall();
    }

    public void FullScreencall() {
        //for new api versions.
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void setUI() {
        epgAdapter = new EpgAdapter(MyApp.fullModels_filter.get(0).getChannels(), this, (integer, integer2, epgChannel, epgEvent) -> {
            //onclicklistener
            playVideo(epgChannel);
            setDescription(epgChannel,epgEvent);
            return null;
        }, (i_ch, i_pro, epgChannel, epgEvent) -> {
            //onfocuslistener
            channelPos=i_ch;
            programPos=i_pro;
            setDescription(epgChannel,epgEvent);
            return null;
        });
        epg_recyclerview.setAdapter(epgAdapter);

        ViewGroup.LayoutParams params = ly_surface.getLayoutParams();
        params.height = MyApp.SURFACE_HEIGHT;
        params.width = MyApp.SURFACE_WIDTH;
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        int SCREEN_HEIGHT = displayMetrics.heightPixels;
        int SCREEN_WIDTH = displayMetrics.widthPixels;
        mVideoHeight = displayMetrics.heightPixels;
        mVideoWidth = displayMetrics.widthPixels;
        holder.setFixedSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        ratio = mVideoWidth + ":"+ mVideoHeight;
        resolutions =  new String[]{"16:9", "4:3", ratio};
        Log.e("height", String.valueOf(MyApp.SCREEN_HEIGHT));
        categoryAdapter = new CategoryAdapter(MyApp.live_categories_filter, (categoryModel, position, is_clicked) -> {
            if (!is_clicked) return null;
            if (categoryModel.getId().equals(Constants.xxx_category_id)){
                PinDlg pinDlg = new PinDlg(TvGuideActivity.this, new PinDlg.DlgPinListener() {
                    @Override
                    public void OnYesClick(Dialog dialog, String pin_code) {
                        dialog.dismiss();
                        String pin = (String)MyApp.instance.getPreference().get(Constants.PIN_CODE);
                        if(pin_code.equalsIgnoreCase(pin)){
                            dialog.dismiss();
                            category_recycler_view.setVisibility(View.GONE);
                            categoryPos=position;
                            epgAdapter.setList(MyApp.fullModels_filter.get(position).getChannels());
                            initializeHeader(MyApp.fullModels_filter.get(position));
                            epgAdapter.setChannelPos(0);
                            epg_recyclerview.scrollToPosition(0);
                            epg_recyclerview.performClick();
                        }else {
                            dialog.dismiss();
                            Toast.makeText(TvGuideActivity.this, "Your Pin code was incorrect. Please try again", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void OnCancelClick(Dialog dialog, String pin_code) {
                        dialog.dismiss();
                    }
                });
                pinDlg.show();
            }else {
                category_recycler_view.setVisibility(View.GONE);
                categoryPos=position;
                epgAdapter.setList(MyApp.fullModels_filter.get(position).getChannels());
                initializeHeader(MyApp.fullModels_filter.get(position));
                epgAdapter.setChannelPos(0);
                epg_recyclerview.scrollToPosition(0);
                epg_recyclerview.performClick();
            }
            return null;
        });
        category_recycler_view.setAdapter(categoryAdapter);
        initializeHeader(MyApp.fullModels_filter.get(0));
    }

    private void initializeHeader(FullModel fullModel) {
        Log.e(TAG,"initialize header by changing category");
        if (fullModel.getChannels()==null || fullModel.getChannels().size()==0) {
            setDescription(null,null);
            return;
        }
        playChannel(fullModel.getChannels().get(0));
        if (fullModel.getChannels().get(0).getEvents()==null || fullModel.getChannels().get(0).getEvents().size()==0){
            setDescription(fullModel.getChannels().get(0),null);
            return;
        }
        setDescription(fullModel.getChannels().get(0),fullModel.getChannels().get(0).getEvents().get(Constants.findNowEvent(fullModel.getChannels().get(0).getEvents())));//Constants.findNowEvent(fullModel.getChannels().get(0).getEvents())
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedEpgChannel!=null){
            if (libvlc != null) {
                releaseMediaPlayer();
            }
            holder = surfaceView.getHolder();
            holder.setFormat(PixelFormat.RGBX_8888);
            holder.addCallback(this);
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            final DisplayMetrics displayMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(displayMetrics);
            int SCREEN_HEIGHT = displayMetrics.heightPixels;
            int SCREEN_WIDTH = displayMetrics.widthPixels;
            holder.setFixedSize(SCREEN_WIDTH, SCREEN_HEIGHT);
            mVideoHeight = displayMetrics.heightPixels;
            mVideoWidth = displayMetrics.widthPixels;
            if (!mMediaPlayer.isPlaying())playChannel(selectedEpgChannel);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
    }

    private void playVideo(EPGChannel epgChannel) {
        Log.e(TAG,"Start Video");
        if (selectedEpgChannel!=null && selectedEpgChannel.getName().equals(epgChannel.getName())){
            goVideoActivity(selectedEpgChannel);
        }else {
            playChannel(epgChannel);
        }

    }

    private void playChannel(EPGChannel epgChannel) {
        selectedEpgChannel = epgChannel;
        contentUri = MyApp.instance.getIptvclient().buildLiveStreamURL(MyApp.user, MyApp.pass,
                epgChannel.getStream_id()+"","ts");
        Log.e("url",contentUri);
        if(def_lay.getVisibility()== View.VISIBLE) def_lay.setVisibility(View.GONE);
        releaseMediaPlayer();
        try {

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            options.add("0");//this option is used to show the first subtitle track
            options.add("--subsdec-encoding");

            libvlc = new LibVLC(this, options);

            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);
            mMediaPlayer.setAspectRatio(MyApp.SCREEN_WIDTH+":"+MyApp.SCREEN_HEIGHT);

            // Seting up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(surfaceView);
            if (remote_subtitles_surface != null)
                vout.setSubtitlesView(remote_subtitles_surface);
            vout.setWindowSize(mVideoWidth, mVideoHeight);
            vout.addCallback(this);
            vout.attachViews();


            Media m = new Media(libvlc, Uri.parse(contentUri));
            mMediaPlayer.setMedia(m);
            m.release();
            mMediaPlayer.play();

        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }
    }

    private MediaPlayer.EventListener mPlayerListener = new MediaPlayerListener(this);

    @Override
    public void onSurfacesCreated(IVLCVout ivlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout ivlcVout) {

    }

    private static class MediaPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<TvGuideActivity> mOwner;

        public MediaPlayerListener(TvGuideActivity owner) {
            mOwner = new WeakReference<TvGuideActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            TvGuideActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    player.releaseMediaPlayer();
                    player.onResume();
                    break;
                case MediaPlayer.Event.Playing:
                    break;
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                    break;
                case MediaPlayer.Event.Buffering:
                    break;
                case MediaPlayer.Event.EncounteredError:
                    player.def_lay.setVisibility(View.VISIBLE);
                    break;
                case MediaPlayer.Event.TimeChanged:
                    break;
                case MediaPlayer.Event.PositionChanged:
                    break;
                default:
                    break;
            }
        }
    }

    private void releaseMediaPlayer() {
        if (libvlc == null)
            return;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.removeCallback(this);
            vout.detachViews();
        }
        holder = null;

        libvlc.release();
        libvlc = null;

    }

    private void goVideoActivity(EPGChannel epgChannel) {
        String url = MyApp.instance.getIptvclient().buildLiveStreamURL(MyApp.user, MyApp.pass,
                epgChannel.getStream_id()+"","ts");
        Log.e(getClass().getSimpleName(),url);
        int current_player = (int) MyApp.instance.getPreference().get(Constants.getCurrentPlayer());
        Intent intent;
        switch (current_player){
            case 0:
            default:
                intent = new Intent(this,LivePlayActivity.class);
                break;
            case 1:
                intent = new Intent(this,LiveIjkPlayActivity.class);
                break;
            case 2:
                intent = new Intent(this,LiveExoPlayActivity.class);
                break;
        }
        MyApp.epgChannel = epgChannel;
        intent.putExtra("title",epgChannel.getName());
        intent.putExtra("img",epgChannel.getStream_icon());
        intent.putExtra("url",url);
        intent.putExtra("stream_id",epgChannel.getStream_id());
        intent.putExtra("is_live",true);
        startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    private void setDescription(EPGChannel epgChannel, EPGEvent epgEvent) {
        Log.e(TAG,"initialize header by changing program");
        if (epgEvent!=null){
            Date that_date = new Date();
            that_date.setTime(epgEvent.getStartTime().getTime()+ Constants.SEVER_OFFSET);
            Date end_date = new Date();
            end_date.setTime(epgEvent.getEndTime().getTime()+ Constants.SEVER_OFFSET);
            duration.setText(Constants.dateFormat1.format(that_date)+" - "+Constants.clockFormat.format(end_date));
            title.setText(epgEvent.getTitle());
            content.setText(epgEvent.getDec());
        }else {
            duration.setText("-");
            title.setText(this.getString(R.string.no_information));
            content.setText("");
        }
        if (epgChannel!=null){
            //video play
            if (epgChannel.getStream_icon()!=null && !epgChannel.getStream_icon().equalsIgnoreCase(""))
                Picasso.with(this).load(epgChannel.getStream_icon())
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .networkPolicy(NetworkPolicy.NO_CACHE)
                        .error(R.drawable.icon)
                        .into(current_channel_image);
            else current_channel_image.setImageResource(R.drawable.icon);
            channel_name.setText(epgChannel.getName());
        }else {
            //video stop
            current_channel_image.setImageResource(R.drawable.icon);
            channel_name.setText("");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ly_surface:
                goVideoActivity(selectedEpgChannel);
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }


    private void callAllEpg() {
        try {
            Log.e("BugCheck","getAllEPG start ");
            long startTime = System.nanoTime();
            //api call here
            String inputStream = MyApp.instance.getIptvclient().getAllEPG(MyApp.user,MyApp.pass);
            long endTime = System.nanoTime();

            long MethodeDuration = (endTime - startTime);
            //            Log.e(getClass().getSimpleName(),inputStream);
            Log.e("BugCheck","getAllEPG success "+MethodeDuration);
            if (inputStream==null || inputStream.length()==0) return;
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser parser = null;
            //        Log.e("xml result","received");
            try {
                parser = parserFactory.newSAXParser();
                DefaultHandler handler = new DefaultHandler(){
                    String currentValue = "";
                    boolean currentElement = false;
                    EPGEvent prevEvent=null;
                    EPGEvent currentEvent=null;
                    String channel="";
                    List<EPGChannel> currentChannelList;
                    ArrayList<EPGEvent> epgModels=new ArrayList<>();
                    public void startElement(String uri, String localName,String qName, Attributes attributes) {
                        currentElement = true;
                        currentValue = "";
//                    Log.e("response","received");
                        if(localName.equals("programme")){
//                        Log.e("response","started programs list");
                            currentEvent = new EPGEvent();
                            String start=attributes.getValue(0);
                            String end=attributes.getValue(1);
                            currentEvent.setStart_timestamp(start);//.split(" ")[0]
                            currentEvent.setStop_timestamp(end);//.split(" ")[0]
                            if (!channel.equals(attributes.getValue(2))) {
                                if (currentChannelList !=null && !currentChannelList.isEmpty()) {
                                    Collections.sort(epgModels, new Comparator<EPGEvent>(){
                                        public int compare(EPGEvent o1, EPGEvent o2){
                                            return o1.getStart_timestamp().compareTo(o2.getStart_timestamp());
                                        }
                                    });
                                    for (EPGChannel epgChannel:currentChannelList)
                                        epgChannel.setEvents(epgModels);
                                }
                                epgModels=new ArrayList<>();
                                channel=attributes.getValue(2);
                                currentChannelList =findChannelByid(channel);
                            }
                        }
                    }
                    public void endElement(String uri, String localName, String qName) {
                        currentElement = false;
                        if (localName.equalsIgnoreCase("title"))
                            currentEvent.setTitle(currentValue);
                        else if (localName.equalsIgnoreCase("desc"))
                            currentEvent.setDec(currentValue);
                        else if (localName.equalsIgnoreCase("programme")) {
                            if (currentChannelList !=null && !currentChannelList.isEmpty())
                                currentEvent.setChannel(currentChannelList.get(0));
                            if (prevEvent!=null){
                                currentEvent.setPreviousEvent(prevEvent);
                                prevEvent.setNextEvent(currentEvent);
                            }
                            prevEvent=currentEvent;
                            for (EPGEvent epgEvent:epgModels){
                                if (epgEvent.getTitle().equals(currentEvent.getTitle()) &&
                                        epgEvent.getDec().equals(currentEvent.getDec()) &&
                                        epgEvent.getStart_timestamp().equals(currentEvent.getStart_timestamp()) &&
                                        epgEvent.getStop_timestamp().equals(currentEvent.getStop_timestamp()))
                                    return;
                            }
                            epgModels.add(currentEvent);
                        }
                        else if (localName.equalsIgnoreCase("tv")){
                            //
                            is_data_loaded=true;
                            runOnUiThread(()-> setUI());
                        }
                    }
                    @Override
                    public void characters(char[] ch, int start, int length) {
                        if (currentElement) {
                            currentValue = currentValue +  new String(ch, start, length);
                        }
                    }
                };

                parser.parse(new InputSource(new StringReader(inputStream)),handler);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                ConnectionDlg connectionDlg = new ConnectionDlg(TvGuideActivity.this, new ConnectionDlg.DialogConnectionListener() {
                    @Override
                    public void OnYesClick(Dialog dialog) {
                        dialog.dismiss();
                        new Thread(() -> callAllEpg()).start();
                    }

                    @Override
                    public void OnNoClick(Dialog dialog) {
                        startActivity(new Intent(TvGuideActivity.this, ConnectionErrorActivity.class));
                    }
                },"LOGIN SUCCESSFUL LOADING DATA",null,null);
                connectionDlg.show();
            });
        }
    }

    private List<EPGChannel> findChannelByid(String channel_id){
        List<EPGChannel> channelList = new ArrayList<>();
        Log.e("allfullmodel",MyApp.fullModels.size()+"");
        List<EPGChannel> entireChannels =Constants.getAllFullModel(MyApp.fullModels).getChannels();
        for (EPGChannel epgChannel : entireChannels) {
            if (epgChannel.getId().equals(channel_id)) {
                channelList.add(epgChannel);
            }
        }
        return channelList;
    }
}