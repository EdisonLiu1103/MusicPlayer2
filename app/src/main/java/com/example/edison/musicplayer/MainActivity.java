package com.example.edison.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ImageButton Pre_btn;
    private ImageButton Play_btn;
    private ImageButton Pause_btn;
    private ImageButton Next_btn;
    private ListView list;
    private TextView text_Current;
    private TextView text_Duration;

    private TextView textView;
    private SeekBar seekBar;
    private RelativeLayout root_Layout;


    //更新进度条的Handler
    private Handler seekBarHandler;

    //当前歌曲的持续时间和当前位置，作用于进度条
    private int duration;
    private int time;

    //进度条控制常量
    private static final int PROGRESS_INCREASE = 0;
    private static final int PROGRESS_PAUSE = 1;
    private static final int PROGRESS_RESET = 2;

    //播放模式常量
    private static final int MODE_LIST_SEQUENCE = 0;
    private static final int MODE_SINGLE_CYCLE = 1;
    private static final int MODE_LIST_CYCLE = 2;
    private int playmode;

    //歌曲列表对象
    private ArrayList<Music> musicArrayList;

    //退出判断标记
    private static Boolean isExit = false;

    //音量控制
    private TextView tv_vol;
    private SeekBar seekbar_vol;

    //当前歌曲的序号，下标从0开始
    private int number = 0;

    //播放状态
    private int status;

    //广播接收器
    private StatusChangedReceiver receiver;

    //睡眠模式相关组件，标识常量
    private ImageView iv_sleep;
    private Timer timer_sleep;
    private static final boolean NOTSLEEP = false;
    private static final boolean ISSLEEP = true;

    //默认的睡眠时间
    private int sleepminute = 20;
    //标记是否打开了睡眠模式
    private static boolean sleepmode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        findViews();
        registerListeners();
        initMusicList();
        initListView();
        checkMusicFile();
        duration = 0;
        time = 0;
        //绑定广播接收器，可以接收广播
        bindStatusChangedReceiver();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        initSeekBarHandler();
        startService(new Intent(this, MusicService.class));
        status = MusicService.COMMAND_STOP;

        //默认播放模式是顺序播放
        playmode = MainActivity.MODE_LIST_SEQUENCE;

        //默认睡眠模式是关闭状态
        sleepmode = MainActivity.NOTSLEEP;
    }



    /**绑定广播接收器*/
    private void bindStatusChangedReceiver(){
        receiver = new StatusChangedReceiver();
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver, filter);
    }

    /**获取显示组件*/
    private void findViews()
    {
        Pre_btn = findViewById(R.id.pre_btn);
        Play_btn = findViewById(R.id.play_btn);
        Pause_btn = findViewById(R.id.pause_btn);
        Next_btn = findViewById(R.id.next_btn);
        list = (ListView)findViewById(R.id.listView1);

        seekBar = (SeekBar)findViewById(R.id.seekBar1);
        text_Current = (TextView)findViewById(R.id.tv1);
        text_Duration = (TextView)findViewById(R.id.tv2);

        textView = (TextView)findViewById(R.id.textView);
        root_Layout = (RelativeLayout)findViewById(R.id.relativeLayout1);

        tv_vol = (TextView)findViewById(R.id.main_tv_volumText);
        seekbar_vol = (SeekBar)findViewById(R.id.main_sb_volumbar);

        iv_sleep = (ImageView)findViewById(R.id.main_iv_sleep);

    }

    /*为显示组件注册监听器*/
    private void registerListeners()
    {
        Pre_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               /* moveNumberToPrevious();
                play(number);
                Play_btn.setBackgroundResource(R.drawable.play);*/
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
            }
        });

        Play_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              /*  if(player != null && player.isPlaying())
                {
                    pause();
                    Play_btn.setBackgroundResource(R.drawable.play);
                }
                else {
                    play(number);
                    Play_btn.setBackgroundResource(R.drawable.play);
                }*/

              switch (status){
                  case MusicService.STATUS_PLAYING:
                      sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                      break;
                  case MusicService.STATUS_PAUSED:
                      sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                  case MusicService.COMMAND_STOP:
                      sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                  default:
                      break;
              }
            }
        });

        Pause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop();
                //Play_btn.setBackgroundResource(R.drawable.play);
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);

            }
        });

        Next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               /* moveNumberToNext();
                play(number);
                Play_btn.setBackgroundResource(R.drawable.play);
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);*/
               if(playmode == MainActivity.MODE_LIST_CYCLE){
                   if(number == musicArrayList.size()-1){
                       number = 0;
                       sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                   }
                   else
                   {
                       sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                   }
               }
               else {
                   sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
               }
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                number = i;
                /*play(number);
                Play_btn.setBackgroundResource(R.drawable.play);*/
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(status != MusicService.STATUS_STOPPED){
                    time = i;
                    //更新文本
                    text_Current.setText(formatTime(time));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
               //进度条暂停移动
                seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(status == MusicService.STATUS_PLAYING){
                    //发送广播给MusicService，执行跳转
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                    //进度条恢复移动
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                }
            }
        });
    }

    /**初始化音乐列表对象*/
    private void initMusicList(){
        musicArrayList = MusicList.getMusicList();

        //避免重复添加音乐
        if(musicArrayList.isEmpty())
        {
            Cursor mMusicCursor = this.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Audio.AudioColumns.TITLE);
            //标题
            int indexTitle = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
            //艺术家
            int indexArtist = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            //总时长
            int indexTotalTime = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
            //路径
            int indexPath = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

            /**通过mMusicCursor游标遍历数据库，并将Music对象加载带ArrayList中*/
            for(mMusicCursor.moveToFirst();!mMusicCursor.isAfterLast(); mMusicCursor.moveToNext()){
                String strTitle = mMusicCursor.getString(indexTitle);
                String strArtist = mMusicCursor.getString(indexArtist);
                String strTotoalTime = mMusicCursor.getString(indexTotalTime);
                String strPath = mMusicCursor.getString(indexPath);

            if(strArtist.equals("<unknown>"))
                strArtist = "无艺术家";
                Music music = new Music(strTitle, strArtist, strPath, strTotoalTime);
                musicArrayList.add(music);
            }

        }
    }

    /**设置适配器并初始化listView*/
    private void initListView()
    {
        List<Map<String, String>> list_map = new ArrayList<Map<String, String>>();
        HashMap<String, String> map;
        SimpleAdapter simpleAdapter;
        for(Music music:musicArrayList)
        {
            map = new HashMap<String, String>();
            map.put("musicName", music.getMusicName());
            map.put("musicArtise", music.getMusicArtist());
            list_map.add(map);
        }

        String[] from = new String[]{"musicName", "musicArtist"};
        int[] to = {R.id.lv_tv_title_item, R.id.lv_tv_artist_item};

        simpleAdapter = new SimpleAdapter(this, list_map, R.layout.listview, from, to);
        list.setAdapter(simpleAdapter);
    }

    /**如果列表没有歌曲，测播放按钮不可用， 并提醒用户*/
    private void checkMusicFile()
    {
        if(musicArrayList.isEmpty())
        {
            Pre_btn.setEnabled(false);
            Play_btn.setEnabled(false);
            Next_btn.setEnabled(false);
            Toast.makeText(getApplicationContext(), "当前没有歌曲文件", Toast.LENGTH_SHORT).show();
        }
        else{
            Pre_btn.setEnabled(true);
            Play_btn.setEnabled(true);
            Next_btn.setEnabled(true);
        }
    }



    private void initSeekBarHandler(){
        seekBarHandler = new Handler(){
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                switch (msg.what){
                    case PROGRESS_INCREASE:
                        if(seekBar.getProgress() < duration){
                            //进度条前进1秒
                            seekBar.setProgress(time);
                            seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                            //修改显示当前进度的文本
                            text_Current.setText(formatTime(time));
                            time += 1000;
                        }
                        break;
                    case PROGRESS_PAUSE:
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        break;
                    case PROGRESS_RESET:
                        //重置进度条界面
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        seekBar.setProgress(0);
                        text_Current.setText("00:00");
                        break;
                }
            }
        };
    }

    /**格式化：毫秒->"mm：ss"*/
    private String formatTime(int msec){
        int minute = (msec / 1000) / 60;
        int second = (msec / 1000) % 60;
        String minuteString;
        String secondString = null;
        if(minute < 10){
            minuteString = "0" + minute;
        }else{
            minuteString ="" + minute;
        }
        if(second < 10){
            secondString = "0" + second;
        }
        return minuteString + "：" + secondString;
    }

    /**发送命令，控制音乐播放，参数定义在MusicService类中*/
    private void sendBroadcastOnCommand(int command){
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command",command);
        //根据不同的命令，封装不同的数据
        switch(command){
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number",number);
                break;
            case MusicService.COMMAND_SEEK_TO:
                intent.putExtra("time",time);
                break;
            case MusicService.COMMAND_PREVIOUS:
            case MusicService.COMMAND_NEXT:
            case MusicService.COMMAND_PAUSE:
            case MusicService.COMMAND_RESUME:
        default:
            break;
        }
        sendBroadcast(intent);
    }

    /**内部类、用于播放器状态更新的接收广播*/
    class StatusChangedReceiver extends BroadcastReceiver{
        public void onReceive(Context context, Intent intent){
            //获取播放器状态
            status = intent.getIntExtra("status", -1);
            //在Activity的标题栏中显示当前正在播放的音乐及艺术及
            String musicName = intent.getStringExtra("musicName");
            String musicArtist = intent.getStringExtra("musicArtist");
            switch(status){
                case MusicService.STATUS_PLAYING:


                    seekBarHandler.removeMessages(PROGRESS_INCREASE);
                    time = intent.getIntExtra("time", 0);
                    duration = intent.getIntExtra("duration", 0);
                    number = intent.getIntExtra("number", number);
                    list.setSelection(number);
                    seekBar.setProgress(time);
                    seekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                    text_Duration.setText(formatTime(duration));
                    Play_btn.setBackgroundResource(R.drawable.pause);

                    //设置Activity的标题栏文字，提示正在播放的歌曲
                    textView.setText(musicArtist + " - " + musicName);
                    //MainActivity.this.setTitle("正在播放" + musicName + "" + musicArtist);

                    break;
                case MusicService.STATUS_PAUSED:
                    seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
                    /**String string = MainActivity.this.getTitle().toString().replace("正在播放","已暂停");
                    MainActivity.this.setTitle(string);**/
                    Play_btn.setBackgroundResource(R.drawable.play);
                    //设置textView文字，提示已经播放的歌曲
                    textView.setText(musicArtist + " - " + musicName);
                case MusicService.STATUS_STOPPED:
                    time = 0;
                    duration = 0;
                    text_Current.setText(formatTime(time));
                    text_Duration.setText(formatTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    textView.setText("");
                    MainActivity.this.setTitle("MusicPlayer");
                    Play_btn.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_COMPLETED:
                    number = intent.getIntExtra("number", 0);
                    if(number == MusicList.getMusicList().size()-1)
                        sendBroadcastOnCommand(MusicService.STATUS_STOPPED);
                    else
                        sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    //sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    //MainActivity.this.setTitle("MusicPlayer");

                    number = intent.getIntExtra("number", 0);
                    if(playmode == MainActivity.MODE_LIST_SEQUENCE)         //顺序模式：到达列表末端时发送停止命令，否则播放下一首
                    {
                        if(number == MusicList.getMusicList().size()-1)
                            sendBroadcastOnCommand(MusicService.STATUS_STOPPED);
                        else
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    }
                    else if(playmode == MainActivity.MODE_SINGLE_CYCLE)       //单曲循环
                            sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    else if(playmode == MainActivity.MODE_LIST_CYCLE){        //列表循环：到达列表末端时，把要播放的音乐设置为第一首，然后发送播放命令
                        if(number == musicArrayList.size()-1){
                            number = 0;
                            sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        }else sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                    }
                    textView.setText("");
                    Play_btn.setBackgroundResource(R.drawable.play);
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    protected void onResume() {

        super.onResume();

        //检查播放器是否正在播放，如果正在播放，以上绑定的接收器会改变UI
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        PropertyBean property = new PropertyBean(MainActivity.this);
        String theme = property.getTheme();

        //设置Activity的主题
        setTheme(theme);
        audio_Control();

        //睡眠模式打开时显示图标，关闭时隐藏图标
        if(sleepmode == MainActivity.ISSLEEP)iv_sleep.setVisibility(View.VISIBLE);
        else iv_sleep.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy(){
        if(status == MusicService.STATUS_STOPPED){
            stopService(new Intent(this, MusicService.class));
        }
        super.onDestroy();
    }

    /**创建菜单*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**处理菜单点击事件*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_theme:
                //显示列表对话框
                new AlertDialog.Builder(this)
                        .setTitle("Choose a theme")
                        .setItems(R.array.theme, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //获取在array.xml中定义的主题名称
                                    //String[] themes = MainActivity.this.getResources().getStringArray(R.array.theme);
                                String theme = PropertyBean.THEMES[i];
                                //设置Activity的主题
                                setTheme(theme);
                                //保存选择的主题
                                PropertyBean property = new PropertyBean(MainActivity.this);
                                property.setAndSaveTheme(theme);
                            }
                        }).show();
                break;
            case R.id.menu_about:
                //显示文本对话框
                new AlertDialog.Builder(MainActivity.this).setTitle("MusicPlayer")
                        .setMessage(R.string.about2).show();
                break;

            case R.id.menu_playmode:
                String[] mode = new String[]{"顺序播放","单曲循环","列表循环"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("播放模式");
                builder.setSingleChoiceItems(mode, playmode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        playmode = i;
                    }
                });
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch(playmode){
                            case 0:
                                playmode = MainActivity.MODE_LIST_SEQUENCE;
                                Toast.makeText(getApplicationContext(), R.string.sequence, Toast.LENGTH_SHORT).show();
                            case 1:
                                playmode = MainActivity.MODE_LIST_CYCLE;
                                Toast.makeText(getApplicationContext(), R.string.listcycle, Toast.LENGTH_SHORT).show();
                            case 2:
                                playmode = MainActivity.MODE_SINGLE_CYCLE;
                                Toast.makeText(getApplicationContext(), R.string.singlecycle, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    }
                });

            case R.id.menu_quit:
                //退出程序
                new AlertDialog.Builder(MainActivity.this).setTitle("提示")
                        .setMessage(R.string.quit_message).setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        System.exit(0);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).show();
                break;

            case R.id.menu_sleep:
                showSleepDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //重写onKeyDown方法
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
       int progress;
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                exitByDoubleClick();
                break;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                progress = seekbar_vol.getProgress();
                if(progress != 0)
                    seekbar_vol.setProgress(progress-1);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                progress = seekbar_vol.getProgress();
                if(progress != 0)
                    seekbar_vol.setProgress(progress + 1);
                return true;
            default:
                break;
        }
        return false;
    }

    private void exitByDoubleClick(){
        Timer timer = null;
        if(isExit == false){
            isExit = true;      //准备退出
            Toast.makeText(this, "再按一次退出程序！", Toast.LENGTH_SHORT).show();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            },2000);        //2秒后会执行 run函数的内容，如果2秒内没有按下返回键，则启动定时器自改isExit的值
        }else
        {
            System.exit(0);
        }
    }

    //编写诶audio——Control方法
    private void audio_Control()
    {
        //获取音量管理器
        final AudioManager audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        //设置当前调整音量大小只是针对媒体音乐
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //设置滑动条最大值
        final int max_progress = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekbar_vol.setMax(max_progress);
        //获取当前音量
        int progress = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekbar_vol.setProgress(progress);
        tv_vol.setText("音量：" + (progress*100/max_progress) + "%");
        seekbar_vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tv_vol.setText("音量：" + (i*100)/(max_progress) + "%");
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private void showSleepDialog(){
        //先用getLayoutInflate().inflate来获取布局，用来获取一个View类对象
        final View userView = this.getLayoutInflater().inflate(R.layout.dialog, null);

        //通过View类的findViewByID放来来获取到相关组件对象
        final TextView tv_minute = (TextView)userView.findViewById(R.id.dialog_tv);
        final Switch switch1 = (Switch)userView.findViewById(R.id.dialog_switch);
        final SeekBar seekbar = (SeekBar)userView.findViewById(R.id.dialog_seekbar);

        tv_minute.setText("睡眠于：" + sleepminute + "分钟");
        //根据当前的睡眠状态来确定switch额状态
        if(sleepmode == MainActivity.ISSLEEP)switch1.setChecked(true);
        seekbar.setMax(60);
        seekbar.setProgress(sleepminute);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sleepminute = i;
                tv_minute.setText("睡眠于" + sleepminute + "分钟");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sleepmode = b;
            }
        });
        //定义定时任务
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.exit(0);
            }
        };

        //定义对话框以及初始化
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("选择睡眠时间（0--60min");
        //设置布局
        dialog.setView(userView);
        //设置取消按钮响应事件
        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        //设置重置按钮响应事件
        dialog.setNeutralButton("重置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(sleepmode == MainActivity.ISSLEEP)
                {
                    timerTask.cancel();
                    timer_sleep.cancel();
                }
                sleepmode = MainActivity.NOTSLEEP;
                sleepminute = 20;
                iv_sleep.setVisibility(View.INVISIBLE);
            }
        });
        //设置确定按钮响应事件
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(sleepmode == MainActivity.ISSLEEP){
                    timer_sleep = new Timer();
                    int time = seekbar.getProgress();
                    //启动任务，time*60*1000毫秒后执行
                    timer_sleep.schedule(timerTask, time*60*1000);
                    iv_sleep.setVisibility(View.VISIBLE);
                }
                else
                {
                    //取消任务
                    timerTask.cancel();
                    if(timer_sleep != null) timer_sleep.cancel();
                    dialogInterface.dismiss();
                    iv_sleep.setVisibility(View.INVISIBLE);
                }
            }
        });
        dialog.show();
    }

    /**设置Activity的主题，包括改变背景图片等*/
    private void setTheme(String theme){
        if("彩色1".equals(theme)){
            root_Layout.setBackgroundResource(R.drawable.color1);
        }else if("彩色2".equals(theme)) {
            root_Layout.setBackgroundResource(R.drawable.color2);
        }else if ("彩色3".equals(theme)){
            root_Layout.setBackgroundResource(R.drawable.color3);
        }else if ("彩色4".equals(theme)){
            root_Layout.setBackgroundResource(R.drawable.color4);
        }else if ("彩色5".equals(theme)){
            root_Layout.setBackgroundResource(R.drawable.color5);
        }
    }

    //媒体播放类
    private MediaPlayer player = new MediaPlayer();

    /**读取音乐文件*/
    private void load(int number){
        try{
            player.reset();
            player.setDataSource(MusicList.getMusicList().get(number).getMusicPath());
            player.prepare();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**播放音乐*/
    private void play(int number){
        //停止当前播放
        if(player != null && player.isPlaying())
        {
            player.stop();
        }
        load(number);
        player.start();
    }

    /**暂停音乐*/
    private void pause(){
        if(player.isPlaying())
        {
            player.pause();
        }
    }

    /**停止播放*/
    private void stop(){
        player.stop();
    }

    /**(暂停后)恢复播放*/
    private void resume(){
        player.start();
    }

    /**重新播放（播放完成之后）*/
    private void replay()
    {
        player.start();
    }

    /**选择下一曲*/
    private void moveNumberToNext(){
        //判断是否到达了列表底端
        if((number) == MusicList.getMusicList().size()-1)
        {
            Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.tip_reach_bottom),Toast.LENGTH_SHORT).show();

        }else{
            ++number;
            play(number);
        }
    }

    /**选择上一曲*/
    private void moveNumberToPrevious(){
        //判断是否达到了列表顶端
        if((number) == 0){
            Toast.makeText(MainActivity.this,MainActivity.this.getString(R.string.tip_reach_top),Toast.LENGTH_SHORT).show();
        }else {
            --number;
            play(number);
        }
    }

}
