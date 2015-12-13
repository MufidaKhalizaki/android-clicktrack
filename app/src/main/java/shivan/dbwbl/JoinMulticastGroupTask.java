package shivan.dbwbl;

import android.os.AsyncTask;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by Shivan on 12.12.2015.
 */
public class JoinMulticastGroupTask extends AsyncTask<String, Void, Void> {
    protected Void doInBackground(String... urls) {
        Log.d("NET", "doInBackground: HUEHUEHUE");

        try {
            InetAddress multicastAddress = InetAddress.getByName("224.2.76.24");
            MulticastSocket ms = new MulticastSocket(5500);
            ms.joinGroup(multicastAddress);

            // while (true)
            {
                DatagramPacket dp;

                Log.d("NET", "sending...");
                byte[] message = new byte[1024];
                message = "huehuehuehue :D:D".getBytes();
                dp = new DatagramPacket(message, message.length, multicastAddress, 5500);
                ms.send(dp);
                Log.d("NET", "sent");

                for (int i = 0; i < 2; ++i) {
                    Log.d("NET", "receiving...");
                    byte[] receivedMessage = new byte[1024];
                    dp = new DatagramPacket(receivedMessage, receivedMessage.length, multicastAddress, 5500);
                    ms.receive(dp);
                    Log.d("NET", "received: " + receivedMessage);
                }
            }

            ms.leaveGroup(multicastAddress);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute() {
        // TODO: check this.exception
        // TODO: do something with the feed
    }
}
