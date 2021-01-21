
package com.naman14.timber.listeners;

/**
 * Listens for playback changes to send the the fragments bound to this activity
 */
public interface MusicStateListener {

    /**
     * Called when {@link com.naman14.timber.MusicService#REFRESH} is invoked
     */
    void restartLoader();

    /**
     * Called when {@link com.naman14.timber.MusicService#PLAYLIST_CHANGED} is invoked
     */
    void onPlaylistChanged();

    /**
     * Called when {@link com.naman14.timber.MusicService#META_CHANGED} is invoked
     */
    void onMetaChanged();

}
