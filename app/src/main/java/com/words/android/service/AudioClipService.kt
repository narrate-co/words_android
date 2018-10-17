package com.words.android.service

import android.app.IntentService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException


class AudioClipService: Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {




    companion object {
        const val INTENT_KEY_COMMAND = "KEY_COMMAND"
        const val INTENT_KEY_URL = "KEY_URL"

        const val BROADCAST_AUDIO_STATE_DISPATCH = "audio_state_dispatch_broadcast"
        const val BROADCAST_AUDIO_STATE_EXTRA_STATE = "audio_state_extra_state"
        const val BROADCAST_AUDIO_STATE_EXTRA_URL = "audio_state_extra_url"
        const val BROADCAST_AUDIO_STATE_EXTRA_MESSAGE = "audio_state_extra_message"


        const val TAG = "AudioClipService"
    }

    enum class Command {
        PLAY, STOP, NONE
    }

    enum class AudioStateDispatch {
        LOADING, STOPPED, ERROR, PREPARED, PLAYING
    }


    private var mediaPlayer: MediaPlayer? = null
    private val audioManager: AudioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var currentUrl: String? = null

    private var isPreparing = false
    private var playWhenLoaded = true

    private var networkIsAvailable = true //TODO use a network monitor

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("AudioClipService::onStartCommand")
        val command = Command.valueOf(intent?.getStringExtra(INTENT_KEY_COMMAND) ?: Command.NONE.name)
        when (command) {
            Command.PLAY -> play(intent?.getStringExtra(INTENT_KEY_URL))
            Command.STOP, Command.NONE -> {
                stop()
                destroy()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun play(url: String?) {
        playWhenLoaded = true

        //if there's something playing, stop everything, continue
        if (currentUrl != null) stop()

        //if the given url is null, dispatch an error, destroy
        if (url == null || url.isEmpty()) {
            stop()
            dispatchError(url, "No available pronunciation")
            destroy()
            return
        }

        //handle no network by stopping and dispatching an error, destory
        if (!networkIsAvailable) {
            stop()
            dispatchError(url, "No network available")
            destroy()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = null
        audioManager.abandonAudioFocus(this)
        mediaPlayer = MediaPlayer()
        currentUrl = url
        mediaPlayer?.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        try {
            dispatchLoading(url)
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.setOnPreparedListener(this)
            mediaPlayer?.setOnCompletionListener(this)
            mediaPlayer?.setOnErrorListener(this)
            mediaPlayer?.prepareAsync()
        } catch (e: IOException) {
            e.printStackTrace()
            dispatchError(url, "No pronunciation available")
            stop()
            destroy()
        }

    }

    private fun stop() {
        audioManager.abandonAudioFocus(this)
        mediaPlayer?.release()
        mediaPlayer = null
        dispatchStopped(currentUrl)
        currentUrl = null
    }

    private fun destroy() {
        stopSelf()
    }

    override fun onPrepared(player: MediaPlayer?) {
        println("$TAG::onPrepared")
        val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            dispatchPrepared(currentUrl)
            if (playWhenLoaded) {
                dispatchPlaying(currentUrl)
                mediaPlayer?.start()
            }
        } else {
            dispatchError(currentUrl, "Unable to play pronunciation")
            stop()
            destroy()
        }
    }

    override fun onCompletion(p0: MediaPlayer?) {
        stop()
        destroy()
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        println("$TAG::onError")
        dispatchError(currentUrl, "An error occurred playing pronunciation")
        stop()
        destroy()
        return true
    }


    override fun onAudioFocusChange(p0: Int) { }


    private fun dispatch(dispatch: AudioStateDispatch, url: String?, message: String? = null) {
        val intent = Intent(BROADCAST_AUDIO_STATE_DISPATCH)
        intent.putExtra(BROADCAST_AUDIO_STATE_EXTRA_STATE, dispatch)
        intent.putExtra(BROADCAST_AUDIO_STATE_EXTRA_URL, url ?: "")
        intent.putExtra(BROADCAST_AUDIO_STATE_EXTRA_MESSAGE, message ?: "")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun dispatchLoading(url: String?) {
        println("$TAG::dispatchLoading")
        isPreparing = true
        dispatch(AudioStateDispatch.LOADING, url)
    }

    private fun dispatchStopped(url: String?) {
        println("$TAG::dispatchStopped")
        isPreparing = false
        dispatch(AudioStateDispatch.STOPPED, url)
    }

    private fun dispatchError(url: String?, message: String) {
        println("$TAG::dispatchError")
        dispatch(AudioStateDispatch.ERROR, url, message)
    }

    private fun dispatchPrepared(url: String?) {
        println("$TAG::dispatchPrepared")
        isPreparing = false
        dispatch(AudioStateDispatch.PREPARED, url)
    }

    private fun dispatchPlaying(url: String?) {
        println("$TAG::dispatchPlaying")
        isPreparing = false
        dispatch(AudioStateDispatch.PLAYING, url)
    }

}
