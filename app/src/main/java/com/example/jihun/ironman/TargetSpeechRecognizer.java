package com.example.jihun.ironman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;

public class TargetSpeechRecognizer implements RecognitionListener {
    protected static final String TAG = "Ironman.SR";
    protected Intent intent_;
    protected SpeechRecognizer speech_recog_;
    protected Activity parent_activity_;
    protected Listener listener_;
    protected String signal_speech_;
    protected HashSet<String> target_speeches_ = new HashSet<>();

    public interface Listener {
        // called on finish of recognizing with words list that were recognized.
        void onEndListening(String speech);
        // notifying sound level.
        void onRmsChanged(float rmsdB);
    }

    public TargetSpeechRecognizer(Activity parent_activity, Listener listener) {
        parent_activity_ = parent_activity;
        listener_ = listener;

        intent_ = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent_.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                parent_activity_.getPackageName());
        intent_.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent_.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        speech_recog_ = SpeechRecognizer.createSpeechRecognizer(parent_activity_);
        speech_recog_.setRecognitionListener(this);
    }

    public void setTargetSpeech(String signal, String[] speeches) {
        // add a space to make it easier to compare with speech string.
        signal_speech_ = signal + " ";
        for (String speech : speeches) {
            target_speeches_.add(speech);
        }
    }

    public void start() {
        Log.i(TAG, "start listening");
        if (target_speeches_.isEmpty()) {
            Log.w(TAG, "target speech list is empty");
        }
        speech_recog_.startListening(intent_);
    }

    public void stop() {
        if (speech_recog_ != null) {
            speech_recog_.cancel();
        }
        Log.i(TAG, "stop listening");
    }

    public void destroy() {
        speech_recog_.destroy();
    }

    // find target speech from recognized speech results.
    protected String processMatchResult(Bundle results) {
        ArrayList<String> results_in_arraylist =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (results == null) {
            Log.e(TAG, "No voice results");
            return "";
        }
        Log.d(TAG, "Printing matches: ");
        for (String match : results_in_arraylist) {
            Log.d(TAG, match);
        }
        for (String match : results_in_arraylist) {
            if (!match.startsWith(signal_speech_)) {
                continue;
            }
                String command = match.substring(signal_speech_.length());
                if (target_speeches_.contains(command)) {
                    Log.d(TAG, "Target matches: " + command);
                    return command;
            }
        }
        return "";
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // it's too noisy
        //Log.d(TAG, "onRmsChanged: " + rmsdB);
        listener_.onRmsChanged(rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        Log.d(TAG, "onError for speech: " + error);
        listener_.onEndListening("");
    }

    @Override
    public void onResults(Bundle results) {
        String match = processMatchResult(results);
        listener_.onEndListening(match);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }
}
