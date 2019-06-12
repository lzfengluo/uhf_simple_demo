package com.speedata.uhf_simple;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;


/**
 * @author 张明_
 * @date 2018/3/15
 */
public class MyApp extends Application {
    /**
     * 单例
     */
    private static MyApp m_application;
    private IUHFService iuhfService;
    public static boolean isOpenDev = false;
    public static boolean isOpenServer = true;

    public static MyApp getInstance() {
        return m_application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("APP", "onCreate");
        m_application = this;
        Context context = getApplicationContext();
        Log.d("UHFService", "MyApp onCreate");
    }

    public IUHFService getIuhfService() {
        return iuhfService;
    }

    public void setIuhfService() {
        try {
            iuhfService = UHFManager.getUHFService(getApplicationContext());
            Log.d("UHFService", "iuhfService初始化: " + iuhfService);
        } catch (Exception e) {
            e.printStackTrace();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    boolean cn = getApplicationContext().getResources().getConfiguration().locale.getCountry().equals("CN");
                    if (cn) {
                        Toast.makeText(getApplicationContext(), "模块不存在", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Module does not exist", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
