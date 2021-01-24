/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 The CyanogenMod Project
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.naman14.timber.utils;

import android.provider.MediaStore;


public final class SortOrder {


    public SortOrder() {
    }

    public interface ArtistSortOrder {

        String ARTIST_A_Z = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;


        String ARTIST_Z_A = ARTIST_A_Z + " DESC";


        String ARTIST_NUMBER_OF_SONGS = MediaStore.Audio.Artists.NUMBER_OF_TRACKS
                + " DESC";


        String ARTIST_NUMBER_OF_ALBUMS = MediaStore.Audio.Artists.NUMBER_OF_ALBUMS
                + " DESC";
    }

    public interface AlbumSortOrder {

        String ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;


        String ALBUM_Z_A = ALBUM_A_Z + " DESC";


        String ALBUM_NUMBER_OF_SONGS = MediaStore.Audio.Albums.NUMBER_OF_SONGS
                + " DESC";


        String ALBUM_ARTIST = MediaStore.Audio.Albums.ARTIST;


        String ALBUM_YEAR = MediaStore.Audio.Albums.FIRST_YEAR + " DESC";

    }


    public interface SongSortOrder {

        String SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;


        String SONG_Z_A = SONG_A_Z + " DESC";


        String SONG_ARTIST = MediaStore.Audio.Media.ARTIST;


        String SONG_ALBUM = MediaStore.Audio.Media.ALBUM;


        String SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC";


        String SONG_DURATION = MediaStore.Audio.Media.DURATION + " DESC";


        String SONG_DATE = MediaStore.Audio.Media.DATE_ADDED + " DESC";


        String SONG_FILENAME = MediaStore.Audio.Media.DATA;
    }


    public interface AlbumSongSortOrder {

        String SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;


        String SONG_Z_A = SONG_A_Z + " DESC";


        String SONG_TRACK_LIST = MediaStore.Audio.Media.TRACK + ", "
                + MediaStore.Audio.Media.DEFAULT_SORT_ORDER;


        String SONG_DURATION = SongSortOrder.SONG_DURATION;


        String SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC";


        String SONG_FILENAME = SongSortOrder.SONG_FILENAME;
    }

    public interface ArtistSongSortOrder {

        String SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;


        String SONG_Z_A = SONG_A_Z + " DESC";


        String SONG_ALBUM = MediaStore.Audio.Media.ALBUM;


        String SONG_YEAR = MediaStore.Audio.Media.YEAR + " DESC";


        String SONG_DURATION = MediaStore.Audio.Media.DURATION + " DESC";


        String SONG_DATE = MediaStore.Audio.Media.DATE_ADDED + " DESC";


        String SONG_FILENAME = SongSortOrder.SONG_FILENAME;
    }


    public interface ArtistAlbumSortOrder {

        String ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;


        String ALBUM_Z_A = ALBUM_A_Z + " DESC";


        String ALBUM_NUMBER_OF_SONGS = MediaStore.Audio.Artists.Albums.NUMBER_OF_SONGS
                + " DESC";


        String ALBUM_YEAR = MediaStore.Audio.Artists.Albums.FIRST_YEAR
                + " DESC";
    }

}
