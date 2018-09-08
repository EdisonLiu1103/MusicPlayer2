package com.example.edison.musicplayer;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageButton;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageButton Pre_btn;
    private ImageButton Play_btn;
    private ImageButton Pause_btn;
    private ImageButton Next_btn;
    private ListView list;

    //歌曲列表对象
    private ArrayList<Music> musicArrayList;

    //当前歌曲的序号，下标从0开始
    private int number = 0;

    //播放状态
    private int status;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        findViews();
        registerListeners();
        initMusicList();
        initListView();
        checkMusicFile();
    }

    private void findViews()
    {
        Pre_btn = findViewById(R.id.pre_btn);
        Play_btn = findViewById(R.id.play_btn);
        Pause_btn = findViewById(R.id.pause_btn);
        Next_btn = findViewById(R.id.next_btn);
        list = findViewById(R.id.listView1);
    }

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
                moveNumberToNext();
                /*play(number);
                Play_btn.setBackgroundResource(R.drawable.play);*/
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
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

    /**发送命令，控制音乐播放，参数定义在MusicService类中*/
    private void sendBroadcastOnCommand(int command){
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command",command);
        //根据不同的命令，封装不同的数据
        switch(command){
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number",number);
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