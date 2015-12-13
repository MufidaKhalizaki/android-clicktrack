package shivan.dbwbl;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private static AsyncTask<TaskConfig, Void, Void> task;
    private static boolean mIsServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final SharedPreferences settings = getSharedPreferences("AppPrefs", 0);
        final EditText ipEditText = (EditText) findViewById(R.id.ipEditText);
        final EditText latencyEditText = (EditText) findViewById(R.id.latencyEditText);
        final Button startServerButton = (Button) findViewById(R.id.startServerButton);
        final Button okayButton = (Button) findViewById(R.id.okay_button);
        final Button stopButton = (Button) findViewById(R.id.stop_button);

        ipEditText.setText(settings.getString("SERVER_IP", ""));
        latencyEditText.setText(Integer.toString(settings.getInt("LATENCY", 250)));

        startServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskConfig config = new TaskConfig(
                        MainActivity.this, ipEditText.getText().toString(), Integer.valueOf(latencyEditText.getText().toString())
                );

                MainActivity.task = new StartServerTask(config);
                task.execute();
                mIsServer = true;
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mIsServer) {
                    StartServerTask server = ((StartServerTask) task);
                    if(!server.isPlaying()) {
                        server.play();
                        fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                    } else {
                        server.stop();
                        fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                    }
                } else
                    Snackbar.make(view, "Server läuft nicht, Junge!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
            }
        });

        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("SERVER_IP", ipEditText.getText().toString());
                editor.putInt("LATENCY", Integer.valueOf(latencyEditText.getText().toString()));
                editor.commit();

                TaskConfig config = new TaskConfig(
                        MainActivity.this, ipEditText.getText().toString(), Integer.valueOf(latencyEditText.getText().toString())
                );

                MainActivity.task = new StartClientTask(config);
                task.execute();
                mIsServer = false;
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("LIVE", "onClick: cancelling");
                task.cancel(true);
                ((IClosable) task).close();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
