
package com.monstervoice.a31

import android.app.Activity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.widget.*
import android.view.Gravity
import kotlin.concurrent.thread
import kotlin.math.tanh

class MainActivity : Activity() {
    private var running = false
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var intensity = 1.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 40, 30, 40)
            setBackgroundColor(0xFF151515.toInt())
        }

        val title = TextView(this).apply {
            text = "MONSTER VOICE A31"
            textSize = 26f
            setTextColor(0xFFFF3333.toInt())
            gravity = Gravity.CENTER
        }

        val slider = SeekBar(this).apply {
            max = 100
            progress = 50
            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        bar: SeekBar, value: Int, user: Boolean
                    ) {
                        intensity = 0.5f + value / 50f
                    }
                    override fun onStartTrackingTouch(bar: SeekBar) {}
                    override fun onStopTrackingTouch(bar: SeekBar) {}
                }
            )
        }

        val button = Button(this).apply {
            text = "START MONSTER"
            setOnClickListener {
                if (running) {
                    stopAudio()
                    text = "START MONSTER"
                } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        arrayOf(Manifest.permission.RECORD_AUDIO), 10
                    )
                } else {
                    startAudio()
                    text = "STOP MONSTER"
                }
            }
        }

        layout.addView(title)
        layout.addView(TextView(this).apply {
            text = "MONSTER INTENSITY"
            setTextColor(-1)
        })
        layout.addView(slider)
        layout.addView(button)
        setContentView(layout)
    }

    private fun startAudio() {
        running = true
        thread {
            val rate = 44100
            val min = AudioRecord.getMinBufferSize(
                rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (min <= 0) {
                running = false
                return@thread
            }
            val size = min * 2
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC, rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, size
                )
                player = AudioTrack(
                    AudioManager.STREAM_MUSIC, rate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, size,
                    AudioTrack.MODE_STREAM
                )
                val input = ShortArray(min)
                val output = ShortArray(min)
                recorder?.startRecording()
                player?.play()
                while (running) {
                    val count = recorder?.read(input, 0, min) ?: 0
                    if (count > 0) {
                        val gain = intensity
                        for (i in 0 until count) {
                            val x = input[i].toFloat() / 32768f
                            output[i] = (tanh(x * gain) * 32767)
                                .toInt().toShort()
                        }
                        player?.write(output, 0, count)
                    }
                }
            } catch (_: Exception) {
                running = false
            } finally {
                releaseAudio()
            }
        }
    }

    private fun releaseAudio() {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        try { player?.stop() } catch (_: Exception) {}
        player?.release()
        player = null
    }

    private fun stopAudio() {
        running = false
    }

    override fun onDestroy() {
        stopAudio()
        super.onDestroy()
    }
}
