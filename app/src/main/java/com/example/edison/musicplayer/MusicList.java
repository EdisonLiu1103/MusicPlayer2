package com.example.edison.musicplayer;

import java.util.ArrayList;

/**
 * MusicLit类采用单一实例，
 * 只能通过getMusicList方法获取、共享
 * 唯一的ArrayList<Music>对象
 */
public class MusicList {

    private static ArrayList<Music> musicArray = new ArrayList<Music>();
    private MusicList(){}
    public static ArrayList<Music> getMusicList()
    {
        return musicArray;
    }
}
