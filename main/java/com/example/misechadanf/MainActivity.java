package com.example.misechadanf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    TextView TvBluetoothStatus;
    TextView TvReceiveData;
    TextView TvSendAuto;
    TextView TvSendOpen;
    TextView TvSendClose;

    TextView TvMise;
    TextView TvTemp;
    TextView TvHumi;
    TextView TvZero;
    TextView TvWindow;

    Button BtnBluetoothOn;
    Button BtnBluetoothOff;
    Button BtnConnect;
    Button BtnAuto;
    Button BtnOpen;
    Button BtnClose;
    Button BtnSendData;

    SeekBar SeekMise;
    SeekBar SeekTemp;
    SeekBar SeekHumi;

    BluetoothAdapter BluetoothAdapter;
    Set<BluetoothDevice> PairedDevices;
    List<String> ListPairedDevices;

    Handler BluetoothHandler;
    ConnectedBluetoothThread ThreadConnectedBluetooth;
    BluetoothDevice BluetoothDevice;
    BluetoothSocket BluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private static final long MIN_CLICK_INTERVAL = 2000;
    private long mLastClickTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TvBluetoothStatus = (TextView) findViewById(R.id.tvBluetoothStatus);
        TvReceiveData = (TextView) findViewById(R.id.tvReceiveData);
        TvSendAuto = (TextView) findViewById(R.id.tvAuto);
        TvSendOpen = (TextView) findViewById(R.id.tvOpen);
        TvSendClose = (TextView) findViewById(R.id.tvClose);

        TvMise = (TextView) findViewById(R.id.saveMise);
        TvTemp = (TextView) findViewById(R.id.saveTemp);
        TvHumi = (TextView) findViewById(R.id.saveHumi);
        TvZero = (TextView) findViewById(R.id.zeroHumi);
        TvWindow = (TextView) findViewById(R.id.tvWindowData);

        BtnBluetoothOn = (Button) findViewById(R.id.btnBluetoothOn);
        BtnBluetoothOff = (Button) findViewById(R.id.btnBluetoothOff);
        BtnAuto = (Button) findViewById(R.id.btnAuto);
        BtnConnect = (Button) findViewById(R.id.btnConnect);
        BtnOpen = (Button) findViewById(R.id.btnOpen);
        BtnClose = (Button) findViewById(R.id.btnClose);
        BtnSendData = (Button) findViewById(R.id.dataSend);

        SeekMise = (SeekBar) findViewById(R.id.seekMise);
        SeekTemp = (SeekBar) findViewById(R.id.seekTemp);
        SeekHumi = (SeekBar) findViewById(R.id.seekHumi);


        BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        //블루투스 활성화
        BtnBluetoothOn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOn();
            }
        });
        BtnBluetoothOff.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOff();
            }
        });


        //클릭이벤트 발생시 작동
        BtnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });
        BtnAuto.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                //중복클릭 방지하기 위한 변수 설정
                long currentClickTime = SystemClock.uptimeMillis();
                long elapsedTime = currentClickTime - mLastClickTime;
                mLastClickTime = currentClickTime;

                // 중복 클릭인 경우
                if (elapsedTime <= MIN_CLICK_INTERVAL) {
                    return;
                }
                if (ThreadConnectedBluetooth != null) {
                    ThreadConnectedBluetooth.write(TvSendAuto.getText().toString());
                    TvSendAuto.setText("auto");
                    Toast.makeText(getApplicationContext(), "AUTO", Toast.LENGTH_LONG).show();
                    TvWindow.setText("자동");
                }
            }
        });
        BtnOpen.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                long currentClickTime = SystemClock.uptimeMillis();
                long elapsedTime = currentClickTime - mLastClickTime;
                mLastClickTime = currentClickTime;

                // 중복 클릭인 경우
                if (elapsedTime <= MIN_CLICK_INTERVAL) {
                    return;
                }
                if (ThreadConnectedBluetooth != null) {
                    ThreadConnectedBluetooth.write(TvSendOpen.getText().toString());
                    TvSendOpen.setText("op");
                    Toast.makeText(getApplicationContext(), "OPEN", Toast.LENGTH_LONG).show();
                    TvWindow.setText("열림");
                }
            }
        });
        BtnClose.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                long currentClickTime = SystemClock.uptimeMillis();
                long elapsedTime = currentClickTime - mLastClickTime;
                mLastClickTime = currentClickTime;

                // 중복 클릭인 경우
                if (elapsedTime <= MIN_CLICK_INTERVAL) {
                    return;
                }
                if (ThreadConnectedBluetooth != null) {
                    ThreadConnectedBluetooth.write(TvSendClose.getText().toString());
                    TvSendClose.setText("cl");
                    Toast.makeText(getApplicationContext(), "CLOSE", Toast.LENGTH_LONG).show();
                    TvWindow.setText("닫힘");
                }
            }
        });


        //아두이노 전송값 확인
        BluetoothHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == BT_MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    TvReceiveData.setText(readMessage);
                }
            }
        };


        //SeekBar 값 저장
        SeekMise.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TvMise.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        SeekTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TvTemp.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        SeekHumi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TvHumi.setText("" + progress);
                TvZero.setText("0");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        //저장값 전달 부분
        BtnSendData.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                long currentClickTime = SystemClock.uptimeMillis();
                long elapsedTime = currentClickTime - mLastClickTime;
                mLastClickTime = currentClickTime;

                // 중복 클릭인 경우
                if (elapsedTime <= MIN_CLICK_INTERVAL) {
                    return;
                }
                if (ThreadConnectedBluetooth != null) {
                    ThreadConnectedBluetooth.write(TvTemp.getText().toString());
                }
                if (ThreadConnectedBluetooth != null) {
                    ThreadConnectedBluetooth.write(TvMise.getText().toString());
                }
                if (ThreadConnectedBluetooth != null) {
                    ThreadConnectedBluetooth.write(TvHumi.getText().toString());
                }
            }
        });

    }


    //블루투스 활성화 메소드
    void bluetoothOn() {
        if (BluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        } else {
            if (BluetoothAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
                TvBluetoothStatus.setText("활성화");
            } else {
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
        }
    }

    void bluetoothOff() {
        if (BluetoothAdapter.isEnabled()) {
            BluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
            TvBluetoothStatus.setText("비활성화");
        } else {
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                    TvBluetoothStatus.setText("활성화");
                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
                    TvBluetoothStatus.setText("비활성화");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void listPairedDevices() {
        if (BluetoothAdapter.isEnabled()) {//블루투스 활성화
            PairedDevices = BluetoothAdapter.getBondedDevices();
            if (PairedDevices.size() > 0) {//선택할 장치가 많을 경우
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");
                ListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : PairedDevices) {//디바이스들 아이템 리스트에 추가
                    ListPairedDevices.add(device.getName());
                }//아이템 저장
                final CharSequence[] items = ListPairedDevices.toArray(new CharSequence[ListPairedDevices.size()]);
                ListPairedDevices.toArray(new CharSequence[ListPairedDevices.size()]);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {//아이템 선택하면 디바이스 이름 가져오기
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    void connectSelectedDevice(String selectedDeviceName) {//블루투스 연결 메서드
        for (BluetoothDevice tempDevice : PairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                BluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            BluetoothSocket = BluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            BluetoothSocket.connect();
            ThreadConnectedBluetooth = new ConnectedBluetoothThread(BluetoothSocket);
            ThreadConnectedBluetooth.start();
            BluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }


    //생명주기 부분
    @Override
    protected void onStart() {
        super.onStart();
        Log.i("ActivityLife", "onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("ActivityLife", "onRestart()");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //다시 시작되는 지점에 저장
        //bundle 은 맵이라 생각하자 맵은 키와 값을 가짐
        //맵은 세상모든것을 담을수있지만 번들은 불가
        //원래저장된값을 가져와 불러올 수있다.
        //그 저장하는 장소가 Bundle이다.
        //onCreate는 onRestoreInstanceState에서 저장하고
        super.onRestoreInstanceState(savedInstanceState);
        Log.i("ActivityLife", "onRestoreInstanceState()");
        SeekTemp.setProgress(savedInstanceState.getInt("y"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("ActivityLife", "onResume()");
    }

    //저장이 필요한 사항이 저장할 때
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i("AtivityLife", "onSaveInstanceState");
        int progress = SeekTemp.getProgress();
        outState.putInt("y", progress);
    }

    //폰에 있는 Home키 누르면 onPause -> onStop이 뜬다.
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("AtivityLife", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("AtivityLife", "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("AtivityLife", "onDestroy");
    }


    //스레드 처리 부분
    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(1000);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        BluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }


        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
