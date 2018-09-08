package com.example.edison.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.security.PublicKey;
import java.security.spec.ECField;

import static android.support.v4.media.session.MediaControllerCompatApi21.TransportControls.play;

public class MusicService extends Service{

        //播放控制命令，标识操作
    public static final int COMMAND_UNKNOWN = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE =1;
    public static final int COMMAND_STOP = 2;
    public static final int COMMAND_RESUME = 3;
    public static final int COMMAND_PREVIOUS = 4;
    public static final int COMMAND_NEXT = 5;
    public static final int COMMAND_CHECK_IS_PLAYING = 6;
    public static final int COMMAND_SEEK_TO = 7;

    //播放状态
    public static final int STATUS_PLAYING = 0;
    public static final int STATUS_PAUSED = 1;
    public static final int STATUS_STOPPED = 2;
    public static final int STATUS_COMPLETED = 3;

    //广播标识
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";


    //歌曲序号，从0开始
    private int number = 0;
    private int status;

    //媒体播放类
    private MediaPlayer player = new MediaPlayer();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**内部类，接收广播命令，并执行操作*/
    abstract class CommandReceiver extends BroadcastReceiver{
        public void onReceiver(Context context, Intent intent){
            //获取命令
            int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
            //执行命令
            switch (command){
                case COMMAND_PLAY:
                    number = intent.getIntExtra("number", 0);
                    play(number);
                    break;
                case COMMAND_PREVIOUS:
                    moveNumberToPrevious();
                    break;
                case COMMAND_NEXT:
                    moveNumberToNext();
                    break;
                case COMMAND_PAUSE:
                    pause();
                    break;
                case COMMAND_STOP:
                    stop();
                    break;
                case COMMAND_RESUME:
                    resume();
                    break;
                case COMMAND_CHECK_IS_PLAYING:
                    break;
                case COMMAND_UNKNOWN:
                default:
                    break;
            }
        }
    }

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

    /**选择下一首*/
    private void moveNumberToNext(){
        //判断是否到达了列表底端
        if((number) == MusicList.getMusicList().size()-1){
            Toast.makeText(MusicService.this,"已经到达列表底端", Toast.LENGTH_LONG).show();
        }else {
            ++number;
            play(number);
        }
    }

    /**播放音乐*/
    private void play(int number){
        //停止当前播放
        if(player != null && player.isPlaying()){
            player.stop();
        }
        load(number);
        player.start();
        status = MusicService.STATUS_PAUSED;
    }

    /**暂停音乐*/
    private void pause(){
        if(player.isPlaying()){
            player.pause();
            status = MusicService.STATUS_PAUSED;
        }
    }

    /**停止音乐*/
    private void stop(){
        if(status != MusicService.STATUS_STOPPED){
            player.stop();
        }
    }

    /**恢复播放（暂停后）*/
    private void resume(){
        player.start();
        status = MusicService.STATUS_PLAYING;

    }

    /**重新播放（播放完成之后）*/
    private void replay(){
        player.start();
        status = MusicService.STATUS_PLAYING;
    }
}
