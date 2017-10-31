/*
 * Copyright © 2017 Viro Media. All rights reserved.
 */
package com.viro.renderer.jni;

import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

/**
 * SoundField emits environmental sound from every direction. It is the audio
 * equivalent of a skybox or 360 image, providing atmospheric background noise. These sounds respond
 * to the user's head rotation.
 */
public class SoundField implements BaseSound {

    /**
     * Callback interface for responding to {@link SoundField} events.
     */
    public interface Delegate {

        /**
         * Invoked when a {@link SoundField} has finished loading and is ready to play without delay.
         *
         * @param sound The SoundField.
         */
        void onSoundReady(SoundField sound);

        /**
         * Invoked when a {@link SoundField} failed to load.
         *
         * @param error The associated error message.
         */
        void onSoundFail(String error);
    }

    long mNativeRef;
    private boolean mReady = false;
    private float mVolume = 1.0f;
    private boolean mMuted = false;
    private boolean mPaused = true;
    private boolean mLoop = false;
    private Delegate mDelegate;

    /**
     * Construct a new SoundField.
     *
     * @param viroContext The {@link ViroContext} is required to play sounds.
     * @param uri         The URI of the sound. To load the sound from an Android asset, use URI's
     *                    of the form <tt>file:///android_asset/[asset-name]</tt>.
     * @param delegate    {@link Delegate} which can be used to respond to sound loading and
     *                    playback events. May be null.
     */
    public SoundField(ViroContext viroContext, Uri uri, Delegate delegate) {
        mNativeRef = nativeCreateSoundField(uri.toString(), false, viroContext.mNativeRef);
        mDelegate = delegate;
    }

    /**
     * @hide
     */
    public SoundField(String path, ViroContext viroContext, Delegate delegate, boolean local) {
        mNativeRef = nativeCreateSoundField(path, local, viroContext.mNativeRef);
        mDelegate = delegate;
    }

    /**
     * @hide
     */
    public SoundField(SoundData data, ViroContext viroContext,
                      Delegate delegate) {
        mNativeRef = nativeCreateSoundFieldWithData(data.mNativeRef, viroContext.mNativeRef);
        mDelegate = delegate;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } finally {
            super.finalize();
        }
    }

    /**
     * Release native resources associated with this SoundField.
     */
    public void dispose() {
        if (mNativeRef != 0) {
            nativeDestroySoundField(mNativeRef);
            mNativeRef = 0;
        }
    }

    /**
     * Play the SoundField.
     */
    @Override
    public void play() {
        mPaused = false;
        nativePlaySoundField(mNativeRef);
    }

    /**
     * Pause the SoundField.
     */
    @Override
    public void pause() {
        mPaused = true;
        nativePauseSoundField(mNativeRef);
    }

    /**
     * Return true if the SoundField is currently playing. This returns false if the SoundField is loading
     * or paused.
     *
     * @return True if the SoundField is playing.
     */
    public boolean isPlaying() {
        return mReady && !mPaused;
    }

    /**
     * Return true if the SoundField is paused. The SoundField can be played by invoking {@link #play()}.
     *
     * @return True if the SoundField is paused.
     */
    public boolean isPaused() {
        return mPaused;
    }

    /**
     * Return true if the SoundField is loading.
     *
     * @return True if the SoundField is loading.
     */
    public boolean isLoading() {
        return !mReady;
    }

    /**
     * Set the volume to the given value, where 0.0 is mute and 1.0 is the maximum volume. The
     * default is 1.0.
     *
     * @param volume The value between 0.0 and 1.0.
     */
    @Override
    public void setVolume(float volume) {
        mVolume = volume;
        nativeSetVolume(mNativeRef, volume);
    }

    /**
     * Get the volume of the SoundField, between 0.0 and 1.0.
     *
     * @return The volume.
     */
    public float getVolume() {
        return mVolume;
    }

    /**
     * Set to true to mute the SoundField.
     *
     * @param muted True to mute.
     */
    @Override
    public void setMuted(boolean muted) {
        mMuted = true;
        nativeSetMuted(mNativeRef, muted);
    }

    /**
     * Return true if the SoundField is currently muted.
     *
     * @return True if muted.
     */
    public boolean isMuted() {
        return mMuted;
    }

    /**
     * Set to true to make the SoundField automatically loop to the beginning when playback finishes.
     *
     * @param loop True to loop.
     */
    @Override
    public void setLoop(boolean loop) {
        mLoop = loop;
        nativeSetLoop(mNativeRef, loop);
    }

    /**
     * Return true if the SoundField is currently set to loop after finishing playback.
     *
     * @return True if loop is enabled.
     */
    public boolean getLoop() {
        return mLoop;
    }

    /**
     * Set the rotation of this SoundField. This can be used to rotate the direction of sounds
     * coming from the ambisonic sound field with respect to the user.
     *
     * @param rotation The rotation about the X, Y, and Z axes, in a {@link Vector}.
     */
    public void setRotation(Vector rotation) {
        nativeSetRotation(mNativeRef, rotation.x, rotation.y, rotation.z);
    }

    /**
     * Seek to the given point in the SoundField, in seconds.
     *
     * @param seconds The seek position in seconds.
     */
    @Override
    public void seekToTime(float seconds) {
        nativeSeekToTime(mNativeRef, seconds);
    }

    /**
     * Set the {@link Delegate}, which can be used to respond to SoundField loading and
     * playback events.
     *
     * @param delegate The SoundFieldDelegate to use for this SoundField.
     */
    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
        // call the delegate.onSoundReady() if we're already ready.
        if (mReady) {
            delegate.onSoundReady(this);
        }
    }

    /**
     * Get the {@link Delegate} used to receive callbacks for this SoundField.
     *
     * @return The SoundFieldDelegate, or null if none is attached.
     */
    public Delegate getDelegate() {
        return mDelegate;
    }
    /**
     * @hide
     */
    @Override
    public void soundIsReady() {
        mReady = true;
        if (mDelegate != null) {
            mDelegate.onSoundReady(this);
        }
    }

    /**
     * @hide
     */
    @Override
    public void soundDidFinish() {
        // GVR does not support this yet, so this will never be called
    }

    /**
     * @hide
     * @param error
     */
    @Override
    public void soundDidFail(String error) {
        if (mDelegate != null) {
            mDelegate.onSoundFail(error);
        }
    }

    private native long nativeCreateSoundField(String url, boolean local, long renderContextRef);
    private native long nativeCreateSoundFieldWithData(long dataRef, long renderContextRef);
    private native void nativeDestroySoundField(long mNativeRef);
    private native void nativePlaySoundField(long nativeRef);
    private native void nativePauseSoundField(long nativeRef);
    private native void nativeSetVolume(long nativeRef, float volume);
    private native void nativeSetMuted(long mNativeRef, boolean muted);
    private native void nativeSetLoop(long mNativeRef, boolean loop);
    private native void nativeSeekToTime(long mNativeRef, float seconds);
    private native void nativeSetRotation(long mNativeRef, float degreesX, float degreesY, float degreesZ);
}
