package du.hackathon.com.stpsp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MainActivity extends Activity {
    public static final String TAG = "MainActivity";
    private String input;
    private String buffer;
    private Button send;
    private Socket socket;
    private TextView content;
    private TextView result;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x11) {
                Bundle bundle = msg.getData();
                content.append("server:" + bundle.getString("msg") + "\n");
                Log.d(TAG, "接受到来自服务端的消息了");
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        send = findViewById(R.id.send_bt);
        content = findViewById(R.id.content_tv);
        result = findViewById(R.id.txtResult);
        Toast.makeText(MainActivity.this, getLocalIp(), Toast.LENGTH_SHORT).show();
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                input = et.getText().toString();
                content.append("client:" + input + "\n");
                new MyThread(input).start();
            }
        });
    }

    private class MyThread extends Thread {
        public String str;

        public MyThread(String str) {
            this.str = str;
        }

        @Override
        public void run() {
            Message msg = new Message();
            msg.what = 0x11;
            Bundle bundle = new Bundle();
            bundle.clear();
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress("172.24.116.215", 30000), 1000);
                OutputStream out = socket.getOutputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                buffer = "";
                while ((line = br.readLine()) != null) {
                    buffer = line + buffer;
                }
                out.write(str.getBytes("utf-8"));
                out.flush();
                bundle.putString("msg", buffer.toString());
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                br.close();
                out.close();
                socket.close();
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                bundle.putString("msg", "服务器连接失败！请检查网络是否打开");
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getLocalIp() {
        @SuppressLint("WifiManagerLeak") WifiManager wifiM = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiI = wifiM.getConnectionInfo();
        int ipAddress = wifiI.getIpAddress();
        if (ipAddress == 0) {
            return null;
        }
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }

}
