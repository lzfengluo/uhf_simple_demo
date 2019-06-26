package com.speedata.uhf_simple;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.speedata.libuhf.bean.SpdInventoryData;
import com.speedata.libuhf.interfaces.OnSpdInventoryListener;
import com.speedata.uhf_simple.adapter.UhfCardBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 接受广播  触发盘点，返回EPC
 *
 * @author My_PC
 */
public class MyService extends Service {

    /**
     * 按设备侧键触发的扫描广播
     */
    public static final String START_SCAN = "com.spd.action.start_uhf";
    public static final String STOP_SCAN = "com.spd.action.stop_uhf";
    public static final String ACTION_SEND_EPC = "com.se4500.onDecodeComplete";
    public static final String UPDATE = "uhf.update";
    private static final String TAG = "UHFService";
    private SoundPool soundPool;
    private int soundId;
    private boolean isStart = false;
    private List<UhfCardBean> uhfCardBeanList = new ArrayList<>();
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (MyApp.isOpenServer) {
                String action = intent.getAction();
                Log.d(TAG, "===rece===action===" + action);
                assert action != null;
                switch (action) {
                    case START_SCAN:
                        //启动超高频扫描
                        if (openDev()) {
                            if (isStart) {
                                //停止盘点
                                MyApp.getInstance().getIuhfService().inventoryStop();
                                isStart = false;
                                uhfCardBeanList.clear();
                                cancelTimer();
                                return;
                            }
                            MyApp.getInstance().getIuhfService().setOnInventoryListener(new OnSpdInventoryListener() {
                                @Override
                                public void getInventoryData(SpdInventoryData var1) {

                                    String epc = var1.getEpc();
                                    if (!epc.isEmpty() && isStart) {
                                        Log.d(TAG, "===inventoryStart===");
                                        int j;
                                        for (j = 0; j < uhfCardBeanList.size(); j++) {
                                            if (var1.epc.equals(uhfCardBeanList.get(j).getEpc())) {
                                                break;
                                            }
                                        }
                                        if (j == uhfCardBeanList.size()) {
                                            uhfCardBeanList.add(new UhfCardBean(epc , 1, var1.rssi, var1.tid));
                                            sendEpc(epc);
                                        }
                                        if (!MyApp.isLoop && !MyApp.isLongDown) {
                                            //停止盘点
                                            MyApp.getInstance().getIuhfService().inventoryStop();
                                            isStart = false;
                                            uhfCardBeanList.clear();
                                        }
                                    }
                                }
                            });
                            MyApp.getInstance().getIuhfService().inventoryStart();
                            isStart = true;
                            if (MyApp.isLoop) {
                                creatTimer();
                            }
                        }
                        break;
                    case STOP_SCAN:
                        if (MyApp.isLongDown) {
                            //停止盘点
                            MyApp.getInstance().getIuhfService().inventoryStop();
                            isStart = false;
                            uhfCardBeanList.clear();
                            cancelTimer();
                            return;
                        }
                        break;
                    case UPDATE:
                        initUHF();
                        break;
                    default:
                        break;
                }
            }
        }
    };

    public MyService() {
    }


    private UhfBinder mBinder = new UhfBinder();

    class UhfBinder extends Binder {

        void initUHF() {
            Log.d(TAG, "initUHF");
            initUHF();
        }

        public int releaseUHF() {
            Log.d("MyService", "getProgress executed");
            return 0;
        }
        //在服务中自定义getProgress()方法，待会活动中调用此方法

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }//普通服务的不同之处，onBind()方法不在打酱油，而是会返回一个实例

    private Timer timer;
    private TimerTask myTimerTask;
    private int mTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "===onCreate===");
        initReceive();
        initUHF();
    }

    private void creatTimer() {
        if (timer == null) {
            if (MyApp.mLoopTime.isEmpty() || MyApp.mLoopTime == null || "".equals(MyApp.mLoopTime)) {
                MyApp.mLoopTime = "0";
            }
            final int loopTime = Integer.parseInt(MyApp.mLoopTime);
            if (loopTime == 0) {
                return;
            }
            timer = new Timer();
            if (myTimerTask != null) {
                myTimerTask.cancel();
            }
            mTime = 0;
            myTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mTime++;
                    if (mTime >= loopTime) {
                        MyApp.getInstance().getIuhfService().inventoryStop();
                        isStart = false;
                        cancelTimer();
                    }
                }
            };
            timer.schedule(myTimerTask, 1000, 1000);
        }
    }

    private void cancelTimer() {
        if (myTimerTask != null) {
            myTimerTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void initUHF() {
        if (soundPool == null) {
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
            soundId = soundPool.load("/system/media/audio/ui/VideoRecord.ogg", 0);
            Log.w("as3992_6C", "id is " + soundId);
        }
        Log.e(TAG, "initUHF");
        if (MyApp.getInstance().getIuhfService() == null) {
            MyApp.getInstance().setIuhfService();
        }
        openDev();
    }

    /**
     * 注册广播
     */
    private void initReceive() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(START_SCAN);
        filter.addAction(STOP_SCAN);
        filter.addAction(UPDATE);
        registerReceiver(receiver, filter);
    }

    private void sendEpc(String epc) {
        soundPool.play(soundId, 1, 1, 0, 0, 1);
        switch (MyApp.mPrefix) {
            case 0:
                epc = "\n" + epc;
                break;
            case 1:
                epc = " " + epc;
                break;
            case 2:
                epc = "\r\n" + epc;
                break;
            case 3:
                epc = "" + epc;
                break;
            default:
                break;
        }
        switch (MyApp.mSuffix) {
            case 0:
                epc = epc + "\n";
                break;
            case 1:
                epc = epc + " ";
                break;
            case 2:
                epc = epc + "\r\n";
                break;
            case 3:
                epc = epc + "";
                break;
            default:
                break;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_SEND_EPC);
        Bundle bundle = new Bundle();
        bundle.putString("se4500", epc);
        intent.putExtras(bundle);
        sendBroadcast(intent);
        Log.d(TAG, "===SendEpc===" + epc);
    }

    /**
     * 上电开串口
     */
    private boolean openDev() {
        if (!MyApp.isOpenDev) {
            final int i = MyApp.getInstance().getIuhfService().openDev();
            if (i != 0) {
                new AlertDialog.Builder(this).setTitle(R.string.DIA_ALERT).setMessage(R.string.DEV_OPEN_ERR).setPositiveButton(R.string.DIA_CHECK, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "===openDev===失败" + i);
                    }
                }).show();
                MyApp.isOpenDev = false;
                return false;
            } else {
                Log.d(TAG, "===openDev===成功");
                MyApp.isOpenDev = true;
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "===onDestroy===");
        soundPool.release();
        unregisterReceiver(receiver);
    }
}
