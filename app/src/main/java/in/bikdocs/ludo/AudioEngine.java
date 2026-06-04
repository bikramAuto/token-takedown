package in.bikdocs.ludo;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

public class AudioEngine {
    private static final int SAMPLE_RATE = 22050;
    private static final int TONE_DURATION_SHORT_MS = 40;
    private static final int TONE_DURATION_MEDIUM_MS = 80;
    private static final int TONE_DURATION_LONG_MS = 150;

    private final HandlerThread handlerThread;
    private final Handler audioHandler;
    private final Context context;
    private boolean isMuted = false;

    private SoundPool soundPool;
    private int diceSoundId;
    private int captureSoundId;
    private int moveSoundId;

    public AudioEngine(Context context) {
        this.context = context.getApplicationContext();
        handlerThread = new HandlerThread("AudioEngineThread");
        handlerThread.start();
        audioHandler = new Handler(handlerThread.getLooper());

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();

        diceSoundId = soundPool.load(this.context, R.raw.rpg_dice_rolling, 1);
        captureSoundId = soundPool.load(this.context, R.raw.fahh, 1);
        moveSoundId = soundPool.load(this.context, R.raw.move, 1);
    }

    public void setMuted(boolean muted) {
        this.isMuted = muted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void playRollSound() {
        if (isMuted) return;
        soundPool.play(diceSoundId, 1f, 1f, 0, 0, 1.2f); // 1.2x speed for snappier sound
    }

    public void playStepSound() {
        if (isMuted) return;
        soundPool.play(moveSoundId, 1f, 1f, 0, 0, 1f);
    }

    public void playCaptureSound() {
        if (isMuted) return;
        soundPool.play(captureSoundId, 1f, 1f, 0, 0, 1f);
    }

    public void playSafeSound() {
        if (isMuted) return;
        audioHandler.post(() -> {
            playTone(523, TONE_DURATION_SHORT_MS, 0.4f); // C5
            try { Thread.sleep(TONE_DURATION_SHORT_MS + 20); } catch (InterruptedException ignored) {}
            playTone(659, TONE_DURATION_MEDIUM_MS, 0.4f); // E5
        });
    }

    public void playWinSound() {
        if (isMuted) return;
        audioHandler.post(() -> {
            int[] melody = {523, 659, 784, 1046}; // C5, E5, G5, C6
            int[] durations = {TONE_DURATION_MEDIUM_MS, TONE_DURATION_MEDIUM_MS, TONE_DURATION_MEDIUM_MS, TONE_DURATION_LONG_MS};
            for (int i = 0; i < melody.length; i++) {
                playTone(melody[i], durations[i], 0.5f);
                try {
                    Thread.sleep(durations[i] + 10);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    private void playTone(double frequency, int durationMs, float volume) {
        int count = (int) (SAMPLE_RATE * (durationMs / 1000.0));
        short[] samples = new short[count];
        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i / SAMPLE_RATE) * frequency;
            // Linear fade out to prevent clicks/pops
            double fade = 1.0;
            if (i > count * 0.7) {
                fade = 1.0 - (double) (i - count * 0.7) / (count * 0.3);
            }
            samples[i] = (short) (Math.sin(angle) * Short.MAX_VALUE * volume * fade);
        }

        AudioTrack audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build())
                .setBufferSizeInBytes(count * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();

        audioTrack.write(samples, 0, count);
        audioTrack.play();
        
        // Schedule release after playback finishes to free resource
        audioHandler.postDelayed(audioTrack::release, durationMs + 100);
    }

    public void shutdown() {
        handlerThread.quitSafely();
        if (soundPool != null) {
            soundPool.release();
        }
    }
}
