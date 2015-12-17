package shivan.dbwbl;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class StartClientTask extends AsyncTask<TaskConfig, Void, Void> implements IClosable {
    private TaskConfig mConfig;
    private MediaPlayer mPlayer;
    private Socket mSocket;

    public  StartClientTask(TaskConfig clientConfig) {
        mConfig = clientConfig;
        mPlayer = MediaPlayer.create(mConfig.getContext(), R.raw.click);
    }

    public void play() {
        mPlayer.start();
        Log.d("LIVE", "song started");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mPlayer.isPlaying()) {
                    mConfig.setProgress(mPlayer.getCurrentPosition(), mPlayer.getDuration());

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Log.d("UI", "run: stop update progress thread");
            }
        }).start();
    }

    public void stop() {
        mPlayer.pause();
        Log.d("LIVE", "song started");
    }

    protected Void doInBackground(TaskConfig... params) {
        Log.d("NET", "Starting client - connecting to " + mConfig.getServerIP());
        mConfig.setStatus("Connecting to server...");

        try {
            InetAddress serverAddress = InetAddress.getByName(mConfig.getServerIP());
            mSocket = new Socket(serverAddress, 9090);
            mSocket.setTcpNoDelay(true);

            BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));

            out.write("HI");
            out.newLine();
            out.flush();

            mConfig.setStatus("Conntected");
            
            while (!Thread.currentThread().isInterrupted() && !isCancelled()) {
                try {
                    String message = input.readLine();

                    if (message == null) {
                        Thread.currentThread().interrupt();
                        return null;
                    }

                    Log.d("NET", "message: " + message);

                    if(message.equals("PING")) {
                        out.write("PONG");
                        out.newLine();
                        out.flush();
                    } else if(message.equals("PLAY")) {
                        play();

                        Thread.sleep(500);

                        out.write("SYNC");
                        out.newLine();
                        out.flush();
                        mConfig.setStatus("Playing");
                    } else if(message.equals("STOP")) {
                        stop();
                        Log.d("LIVE", "song stopped");
                        mConfig.setStatus("Stopped");
                    } else {
                        // Must be a sync number
                        int localPosition = mPlayer.getCurrentPosition();
                        int remotePosition = Integer.valueOf(message);
                        int diff = remotePosition - localPosition;
                        Log.d("LIVE", "local player: " + localPosition + ", server: " + remotePosition + ", d="+diff);
                        Log.d("LIVE", "latency: " + mConfig.getLatency());

                        if(Math.abs(diff) > 15) {
                            mPlayer.seekTo(localPosition + diff + mConfig.getLatency());

                            Thread.sleep(1000);

                            out.write("SYNC");
                            out.newLine();
                            out.flush();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Log.d("NET", "Shutting down client");

        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        Log.d("LIVE", "onCancelled: ");

        mPlayer.stop();
    }

    public void close() {
        try {
            if(mSocket != null)
                mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onPostExecute() {
        // TODO: check this.exception
        // TODO: do something with the feed
    }
}

