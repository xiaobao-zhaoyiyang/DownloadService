package service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import entities.FileInfo;

/**
 * Created by yo on 2016/7/6.
 */
public class DownloadService extends Service {

    public static final String DOWNLOAD_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/download/";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_FINISH = "ACTION_FINISH";
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    public static final int MSG_INIT = 0;

    // 下载任务集合
    private Map<Integer, DownloadTask> mTasks =
            new LinkedHashMap<Integer, DownloadTask>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_START.equals(intent.getAction())){
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i("test", "Start: " + fileInfo.toString());
            // 启动初始化线程
            DownloadTask.sExecutorService.execute(new InitThread(fileInfo));
//            new InitThread(fileInfo).start();
        }else if (ACTION_STOP.equals(intent.getAction())){
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i("test", "Stop: " + fileInfo.toString());
           // 暂停下载
            // 从集合中取出下载任务
            DownloadTask task = mTasks.get(fileInfo.getId());
            if (task != null){
                task.isPause = true;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_INIT:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    Log.i("test", fileInfo.toString());
                    // 启动下载任务
                    DownloadTask task  = new DownloadTask(DownloadService.this, fileInfo, 3);
                    task.download();
                    //把下载任务添加到集合中
                    mTasks.put(fileInfo.getId(), task);
                    break;
            }
        }
    };

    /**
     * 初始化线程
     */
    class InitThread extends Thread{
        private FileInfo mFileInfo;
        public InitThread(FileInfo fileInfo){
            this.mFileInfo = fileInfo;
        }
        public void run(){
            HttpURLConnection connection = null;
            RandomAccessFile raf = null;
            try {
                // 链接网络
                URL url = new URL(mFileInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("GET");
                int length = -1;
                if (connection.getResponseCode() == 200){

                    // 获取文件长度
                    length = connection.getContentLength();
                    if (length < 0){
                        return;
                    }
                }
                File dir = new File(DOWNLOAD_PATH);
                if (!dir.exists()){
                    dir.mkdir();
                }
                // 在本地创建文件
                File file = new File(dir, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                //设置文件长度
                raf.setLength(length);
                mFileInfo.setLength(length);
                mHandler.obtainMessage(MSG_INIT, mFileInfo).sendToTarget();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    connection.disconnect();
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
