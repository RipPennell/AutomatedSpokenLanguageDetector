package rip.thesis.automatedspokenlanguagedetector;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity {

    FloatingActionButton buttonStart, buttonStop, buttonPlayLastRecordAudio;
    TextView displayedText, progressBarText;
    ProgressBar progressBar;
    String audioSavePathInDevice = null;
    String wavFile = null;
    MediaRecorder mediaRecorder ;
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer ;
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public class CountDownTimer extends android.os.CountDownTimer {

        public CountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            progressBar.setMax((int) millisInFuture/100);
            progressBar.setProgress(0);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            progressBar.incrementProgressBy(1);
            String progressText = "Loaded in " + String.format("%.2f", (Math.random() +
                    progressBar.getProgress())/progressBar.getMax()*255) + "MB of 255MB trained model";
//            String progressText = String.valueOf(progressBar.getProgress());
            progressBarText.setText(progressText);
        }

        @Override
        public void onFinish() {
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        buttonStart = findViewById(R.id.button);
        buttonStop = findViewById(R.id.button2);
        buttonPlayLastRecordAudio = findViewById(R.id.button3);
        displayedText = findViewById(R.id.textView);
        progressBarText = findViewById(R.id.textView2);
        progressBar = findViewById(R.id.progressBar);



        buttonStart.setEnabled(false);
        buttonStart.setAlpha(0.25f);
        buttonStop.setEnabled(false);
        buttonStop.setAlpha(0.25f);
        buttonPlayLastRecordAudio.setEnabled(false);
        buttonPlayLastRecordAudio.setAlpha(0.25f);

        displayedText.setVisibility(View.INVISIBLE);
        displayedText.setText("Record someone talking and I'll tell you the language");


        CountDownTimer countDownTimer = new CountDownTimer(15000, 100);
        countDownTimer.start();



        String dirPath = getFilesDir().getAbsolutePath() + "/oneTimeTestDatabase";
        audioSavePathInDevice = dirPath + "/audioFile.m4a";
        wavFile = dirPath + "/wavFile.wav";





        new Thread(() -> {
            File projDir = new File(dirPath);
            if (!projDir.exists()) {
                projDir.mkdirs();

                AssetManager assetManager = getAssets();
                String[] files = null;
                try {
                    files = assetManager.list("");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (files != null) for (String filename : files) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = assetManager.open(filename);
                        File outFile = new File(dirPath, filename);
                        out = new FileOutputStream(outFile);
                        copyFile(in, out);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (! Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }
            Python py = Python.getInstance();
            PyObject pythonScript = py.getModule("PyScriptForApp");
            pythonScript.callAttr("pythonClassify", dirPath + "/EasterEgg.wav",
                    dirPath + "/model.tflite");

            progressBar.post(() -> progressBar.setProgress(100));
            progressBar.post(() -> progressBar.setVisibility(View.INVISIBLE));
            progressBarText.post(() -> progressBarText.setVisibility(View.INVISIBLE));
            displayedText.post(() -> displayedText.setVisibility(View.VISIBLE));
            buttonStart.post(() -> buttonStart.setEnabled(true));
            buttonStart.post(() -> buttonStart.setAlpha(1f));
        }).start();





        buttonStart.setOnClickListener(view -> {

            if(checkPermission()) {



                MediaRecorderReady();

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                buttonStart.setEnabled(false);
                buttonStart.setAlpha(0.25f);
                buttonStop.setEnabled(true);
                buttonStop.setAlpha(1f);
                buttonPlayLastRecordAudio.setEnabled(false);
                buttonPlayLastRecordAudio.setAlpha(0.25f);



//                    Toast.makeText(MainActivity.this, "Recording started",
//                            Toast.LENGTH_LONG).show();
            } else {
                requestPermission();
            }

        });

        buttonStop.setOnClickListener(view -> {
            mediaRecorder.stop();
            buttonStop.setEnabled(false);
            buttonStop.setAlpha(0.25f);
            buttonPlayLastRecordAudio.setEnabled(true);
            buttonPlayLastRecordAudio.setAlpha(1f);
            buttonStart.setEnabled(true);
            buttonStart.setAlpha(1f);

            FFmpeg.execute("-i " + audioSavePathInDevice + " -y " + wavFile);
//            PyObject test = py.getModule("test");
//            PyObject test1 = test.callAttr("helloworld");
//            displayedText.setText(test1.toString());



            //def pythonClassify(tflitePath, filePath):

            Python py = Python.getInstance();
            PyObject pythonScript = py.getModule("PyScriptForApp");
            PyObject pythonClassify = pythonScript.callAttr("pythonClassify", wavFile,
                    dirPath + "/model.tflite");
            displayedText.setText(pythonClassify.toString());

//            Context context = null;
//            try {
//                context = createPackageContext("rip.thesis.automatedspokenlanguagedetector", 0);
//                String modelPath = context.getDataDir().getAbsolutePath() + "/model.tflite";
//
//                Python py = Python.getInstance();
////            PyObject test = py.getModule("test");
////            PyObject test1 = test.callAttr("helloworld");
////            displayedText.setText(test1.toString());
//
//                PyObject pythonScript = py.getModule("PyScriptForApp");
//                //def pythonClassify(tflitePath, filePath):
//                PyObject pythonClassify = pythonScript.callAttr("pythonClassify", wavFile, modelPath);
//                displayedText.setText(pythonClassify.toString());
//            } catch (PackageManager.NameNotFoundException e) {
//                e.printStackTrace();
//            }





//                Toast.makeText(MainActivity.this, "Recording Completed",
//                        Toast.LENGTH_LONG).show();
        });

        buttonPlayLastRecordAudio.setOnClickListener(view -> {

            buttonStop.setEnabled(false);

            mediaPlayer = new MediaPlayer();
            try {
//                mediaPlayer.setDataSource(audioSavePathInDevice);
                mediaPlayer.setDataSource(wavFile);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.start();
//                Toast.makeText(MainActivity.this, "Recording Playing",
//                        Toast.LENGTH_LONG).show();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void MediaRecorderReady(){
        mediaRecorder=new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioSavePathInDevice);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, int[] grantResults) {
        if (requestCode == RequestPermissionCode) {
            if (grantResults.length > 0) {
                boolean StoragePermission = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED;
                boolean RecordPermission = grantResults[1] ==
                        PackageManager.PERMISSION_GRANTED;

                if (StoragePermission && RecordPermission) {
                    Toast.makeText(MainActivity.this, "Permission Granted",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission " +
                            "Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }
}