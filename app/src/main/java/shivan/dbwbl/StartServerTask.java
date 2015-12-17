package shivan.dbwbl;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class StartServerTask extends AsyncTask<TaskConfig, Void, Void> implements IClosable {
    private TaskConfig mConfig;
    private MediaPlayer mPlayer;
    private ArrayList<CommunicationThread> mCommunicationThreads;
    private ServerSocket mServerSocket;

    public StartServerTask(TaskConfig config) {
        mConfig = config;
        mPlayer = MediaPlayer.create(mConfig.getContext(), R.raw.click);
        mCommunicationThreads = new ArrayList<>();
    }

    public void play() {
        try {
            for(CommunicationThread ct : mCommunicationThreads) {
                ct.sendPlay();
                Thread.sleep(ct.getAveragePing());
            }

            mPlayer.start();
            Log.d("LIVE", "song started");
            mConfig.setStatus("Playing");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            for(CommunicationThread ct : mCommunicationThreads)
                ct.sendStop();

            mPlayer.pause();
            Log.d("LIVE", "song stopped");
            mConfig.setStatus("Stopped");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    class CommunicationThread implements Runnable {
        private Socket mSocket;
        private BufferedReader mInput;
        private long mAveragePing;

        public CommunicationThread(Socket clientSocket) {
            mSocket = clientSocket;

            try {
                mInput = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendPlay() throws IOException, InterruptedException {
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            out.write("PLAY");
            out.newLine();
            out.flush();
        }

        public void sendStop() throws IOException, InterruptedException {
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            out.write("STOP");
            out.newLine();
            out.flush();
        }

        public long getAveragePing() {
            return mAveragePing;
        }

        public void run() {
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            ArrayList<Long> pings = new ArrayList<>();

            long t0 = 0;
            long t1;
            long dt;
            int numberOfPings = 0;

            mConfig.setStatus("Calibrating latency...");

            while (!Thread.currentThread().isInterrupted() && mSocket.isConnected()) {
                try {
                    String message = mInput.readLine();

                    if (message == null) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    Log.d("NET", "message: " + message);

                    if(message.equals("HI")) {
                        Log.d("NET", "new client connected");

                        t0 = System.currentTimeMillis();

                        out.write("PING");
                        out.newLine();
                        out.flush();

                        numberOfPings++;
                    } else if (message.equals("SYNC")) {
                        if(!mPlayer.isPlaying())
                            Log.d("LIVE", "run: cant sync, not playing");
                        else {
                            // Send the time
                            int msec = mPlayer.getCurrentPosition() + (int) mAveragePing;
                            Log.d("LIVE", "sync: " + mPlayer.getCurrentPosition() + " + " + mAveragePing);
                            out.write(msec + "");
                            out.newLine();
                            out.flush();
                        }
                    } else if (message.equals("PONG")) {
                        if(numberOfPings < 8) {
                            t1 = System.currentTimeMillis();
                            dt = (t1 - t0);
                            pings.add(dt);

                            Log.d("NET", "roundtrip: " + dt);

                            Thread.sleep(100);

                            t0 = System.currentTimeMillis();

                            out.write("PING");
                            out.newLine();
                            out.flush();

                            numberOfPings++;
                        } else {
                            mAveragePing = 0;
                            for(Long l : pings)
                                mAveragePing += l / 2;
                            mAveragePing /= pings.size();
                            Log.d("NET", "average: " + mAveragePing);
                            mConfig.setStatus("Average latency: " + mAveragePing + "ms");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.d("NET", "communication thread shutting down...");
            mCommunicationThreads.remove(this);
            mConfig.setClientsText("" + mCommunicationThreads.size());
        }
    }

    protected Void doInBackground(TaskConfig... params) {
        Log.d("NET", "Starting server...");

        mConfig.setStatus("Starting server...");

        try {
            mServerSocket = new ServerSocket(9090);

            mConfig.setStatus("Listening on " + mServerSocket + "...");

            while (!Thread.currentThread().isInterrupted() && !isCancelled()) {
                try {
                    Socket socket = mServerSocket.accept();
                    socket.setTcpNoDelay(true);
                    Log.d("NET", "new socket accepted: " + socket.getRemoteSocketAddress());

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                    mCommunicationThreads.add(commThread);

                   mConfig.setClientsText("" + mCommunicationThreads.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("NET", "Shutting down server");

        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    public void close() {
        try {
            Log.d("LIVE", "closing server");

            mConfig.setStatus("Stopped");
            mConfig.setClientsText("None");

            mPlayer.stop();

            for(CommunicationThread c : mCommunicationThreads)
                c.close();
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
