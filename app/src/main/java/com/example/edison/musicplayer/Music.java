package com.example.edison.musicplayer;

/**Music类，包括歌曲名，艺术家，路径，时长等属性，以及相关的获取方法**/
public class Music {
    private String musicName;
    private String musicArtist;
    private String musicPath;
    private String musicTime;
    private String musicDuration;

    public Music(String musicName, String musicArtist, String musicPath, String musicTime){
        this.musicName = musicName;
        this.musicArtist = musicArtist;
        this.musicPath = musicPath;
        this.musicTime = musicTime;
        this.musicDuration = musicDuration;
    }

    public String getMusicName()
    {
        return this.musicName;
    }
    public String getMusicArtist()
    {
        return this.musicArtist;
    }
    public String getMusicPath()
    {
        return this.musicPath;
    }
    public String getMusicTime()
    {
        return this.musicTime;
    }
    public String getMusicDuration()
    {
        return this.musicDuration;
    }
}
