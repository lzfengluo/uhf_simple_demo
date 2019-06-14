package com.speedata.uhf_simple;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.serialport.DeviceControlSpd;
import android.serialport.SerialPortSpd;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;
import com.speedata.libuhf.bean.SpdInventoryData;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.uhf_simple.adapter.UhfCardAdapter;
import com.speedata.uhf_simple.adapter.UhfCardBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * uhf简单盘点demo
 *
 * @author zzc
 * @date 2019/05/22
 */
public class MainActivity extends Activity {

    private TextView tvEpcTotalNum;
    private ImageView ivUhfSet;
    private Button btnInvent, btnClean;
    private ListView lvEpc;
    private UhfCardAdapter uhfCardAdapter;
    private List<UhfCardBean> uhfCardBeanList = new ArrayList<>();
    private IUHFService iuhfService;
    private DeviceControlSpd deviceControlSpd;
    private SerialPortSpd serialPortSpd;
    private int fd;
    private SoundPool soundPool;
    private int soundId;
    private boolean inSearch = false;
    private int num = 0;
    private CheckBox checkBoxServer;
    public static final String START_SCAN = "com.spd.action.start_uhf";
    public static final String STOP_SCAN = "com.spd.action.stop_uhf";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!MyApp.isOpenServer) {
                String action = intent.getAction();
                assert action != null;
                switch (action) {
                    case START_SCAN:
                        //启动超高频扫描
                        if (!openDev()) {
                            if (inSearch) {
                                return;
                            }
                            startInvent();
                        }
                        break;
                    case STOP_SCAN:
                        if (inSearch) {
                            stopInvent();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //强制为竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        MyApp.getInstance().setIuhfService();
        iuhfService = MyApp.getInstance().getIuhfService();
        setContentView(R.layout.activity_main);
        initView();
        initUHF();
        initReceive();
    }

    private void initView() {
        tvEpcTotalNum = findViewById(R.id.tv_epc_total);
        ivUhfSet = findViewById(R.id.iv_uhf_set);
        ivUhfSet.setOnClickListener(new Click());
        btnClean = findViewById(R.id.btn_clean);
        btnClean.setOnClickListener(new Click());
        btnInvent = findViewById(R.id.btn_invent);
        btnInvent.setOnClickListener(new Click());
        lvEpc = findViewById(R.id.lv_epc);
        checkBoxServer = findViewById(R.id.checkbox_server);

    }
    /**
     * 注册广播
     */
    private void initReceive() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(START_SCAN);
        filter.addAction(STOP_SCAN);
        registerReceiver(receiver, filter);
    }

    private void initUHF() {
        try {
            if (iuhfService != null) {
                if (openDev()) {
                    return;
                }
                startService(new Intent(this,MyService.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //上电 开串口
        try {
            deviceControlSpd = new DeviceControlSpd("MAIN", 93);
            deviceControlSpd.PowerOnDevice();
            serialPortSpd = new SerialPortSpd();
            serialPortSpd.OpenSerial("/dev/ttyMT1", 9600);
//            serialPortSpd.OpenSerial("/dev/ttyUSB0", 9600);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //初始化回调监听
        iuhfService.setOnInventoryListener(new OnSpdInventoryListener() {
            @Override
            public void getInventoryData(SpdInventoryData var1) {
                handler.sendMessage(handler.obtainMessage(1, var1));
            }
        });

        // 加载适配器
        uhfCardAdapter = new UhfCardAdapter(this, R.layout.item_epc_list, uhfCardBeanList);
        lvEpc.setAdapter(uhfCardAdapter);
        //初始化声音线程池
        initSoundPool();
    }

    public void initSoundPool() {
        if (soundPool == null) {
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
            soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
            Log.w("zzc", "id is " + soundId);
        }
    }

    public class Click implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v == btnClean) {
                num = 0;
                tvEpcTotalNum.setText(num + "");
                uhfCardBeanList.clear();
                uhfCardAdapter.notifyDataSetChanged();
            } else if (v == btnInvent) {
                if (!inSearch) {
                    startInvent();
                } else {
                    stopInvent();
                }
            } else if (v == ivUhfSet) {
                setAntennaPower();
            }
        }
    }

    private void startInvent() {
        iuhfService.selectCard(1, "", false);
        iuhfService.inventoryStart();
        inSearch = true;
        btnClean.setEnabled(false);
        ivUhfSet.setEnabled(false);
        btnInvent.setText(R.string.uhf_stop_btn);
    }

    private void stopInvent() {
        iuhfService.inventoryStop();
        inSearch = false;
        btnClean.setEnabled(true);
        ivUhfSet.setEnabled(true);
        btnInvent.setText(R.string.uhf_start_btn);
    }

    private void setAntennaPower() {
        final EditText view1 = new EditText(this);
        view1.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.dialog_title_setFreq)).setView(view1).setPositiveButton(getResources().getString(R.string.dialog_pos_set), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int num = Integer.parseInt(view1.getText().toString());
                if (num > 33) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.toast_tips1), Toast.LENGTH_LONG).show();
                    return;
                }
                int result = iuhfService.setAntennaPower(num);
                if (result == 0) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.toast_success), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.toast_failed), Toast.LENGTH_LONG).show();
                }
            }
        }).setNegativeButton(getResources().getString(R.string.dialog_nag_cancel), null).show();
    }

    /**
     * 上电开串口
     *
     * @return 成功返回false
     */
    private boolean openDev() {
        if (iuhfService.openDev() != 0) {
            new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    finish();
                }
            }).show();
            return true;
        }
        return false;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    SpdInventoryData var1 = (SpdInventoryData) msg.obj;
                    int j;
                    for (j = 0; j < uhfCardBeanList.size(); j++) {
                        if (var1.epc.equals(uhfCardBeanList.get(j).getEpc())) {
                            uhfCardBeanList.get(j).setRssi(var1.rssi);
                            break;
                        }
                    }
                    if (j == uhfCardBeanList.size()) {
                        num++;
                        uhfCardBeanList.add(new UhfCardBean(var1.epc, num, var1.rssi, var1.tid));
                        soundPool.play(soundId, 1, 1, 0, 0, 1);

                        if (serialPortSpd != null) {
                            //获取句柄
                            fd = serialPortSpd.getFd();
                            String epcStr = var1.epc;
                            byte[] str = epcStr.getBytes();
                            //发送数据
                            serialPortSpd.WriteSerialByte(fd, str);
                            SystemClock.sleep(50);
                            //发送回车
//                            byte enter = 0x1b;
                            byte[] enter = new byte[1];
                            enter[0] = 0x1b;
                            serialPortSpd.WriteSerialByte(fd, enter);
                            SystemClock.sleep(50);
                        }
                        tvEpcTotalNum.setText(num + "");
                        uhfCardAdapter.notifyDataSetChanged();
                    }
                    break;
                default:
                    break;
            }

        }
    };

    private void sendUpddateService() {
        Intent intent = new Intent();
        intent.setAction("uhf.update");
        this.sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openDev();
        //初始化声音线程
        initSoundPool();
        MyApp.isOpenServer = false;
    }

    @Override
    protected void onPause() {
        MyApp.isOpenServer = true;
        if (iuhfService != null) {
            //更新回调
            sendUpddateService();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (serialPortSpd != null) {
            fd = serialPortSpd.getFd();
            serialPortSpd.CloseSerial(fd);
        }
        try {
            deviceControlSpd.PowerOffDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (checkBoxServer.isChecked()){
            stopService(new Intent(this,MyService.class));
            if (iuhfService != null) {
                if (inSearch) {
                    iuhfService.inventoryStop();
                    inSearch = false;
                }
                iuhfService.closeDev();
            }
        }
        if (soundPool != null) {
            soundPool.release();
        }
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
