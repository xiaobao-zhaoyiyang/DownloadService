package service;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import db.ThreadDAO;
import db.ThreadDAOImp;
import entities.FileInfo;
import entities.ThreadInfo;

/**
 * 下载任务类
 */
public class DownloadTask {
    private Context mContext;
    private FileInfo mFileInfo;
    private ThreadDAO mDAO;

    private int mFinished = 0;

    public boolean isPause = false;

    private int mThreadCount = 1; // 线程数量
    private List<DownloadThread> mThreadList; // 线程集合

    public static ExecutorService sExecutorService =
            Executors.newCachedThreadPool(); // 线程池

    public DownloadTask(Context context, FileInfo fileInfo, int threadCount) {
        this.mContext = context;
        this.mFileInfo = fileInfo;
        this.mThreadCount = threadCount;
        mDAO = new ThreadDAOImp(mContext);
    }

    public void download(){
        // 读取数据库的线程信息
        List<ThreadInfo> threads = mDAO.getThreads(mFileInfo.getUrl());

        if (threads.size() == 0){
            // 获得每个线程下载的长度
            int length = mFileInfo.getLength() / mThreadCount;
            for (int i = 0; i < mThreadCount; i ++){
                // 创建线程信息
                ThreadInfo threadInfo = new ThreadInfo(i, mFileInfo.getUrl(), length * i, length * (i + 1) - 1, 0);
                if (i == mThreadCount - 1){
                    threadInfo.setEnd(mFileInfo.getLength());
                }
                // 添加到线程信息集合中
                threads.add(threadInfo);
                // 插入下载线程信息
                mDAO.insertThread(threadInfo);
            }
        }
        mThreadList = new ArrayList<>();
        // 启动多个线程下载
        for (ThreadInfo info : threads) {
            DownloadThread thread = new DownloadThread(info);
//            thread.start();
            DownloadTask.sExecutorService.execute(thread);
            // 添加线程到集合中
            mThreadList.add(thread);
        }
    }

    /**
     * 判断是否所有线程都执行完毕
     */
    private synchronized void checkAllThreadFinished(){
        boolean allFinished = true;
        // 遍历线程集合，判断线程是否都执行完毕
        for (DownloadThread thread : mThreadList){
            if (!thread.isFinished){
                allFinished = false;
                break;
            }
        }
        if (allFinished){
            // 删除线程信息
            mDAO.deleteThread(mFileInfo.getUrl());
            // 发送广播通知UI下载任务结束
            Intent intent = new Intent(DownloadService.ACTION_FINISH);
            intent.putExtra("fileInfo", mFileInfo);
            mContext.sendBroadcast(intent);
        }
    }

    /**
     * 下载线程
     */
    class DownloadThread extends Thread{
        private ThreadInfo mThreadInfo;
        public boolean isFinished = false; // 线程是否结束
        public DownloadThread(ThreadInfo threadInfo){
            this.mThreadInfo = threadInfo;
        }
        public void run(){
            HttpURLConnection connection = null;
            RandomAccessFile raf = null;
            InputStream input = null;
            try {
                URL url = new URL(mThreadInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("GET");
                // 设置下载位置
                int start = mThreadInfo.getStart() + mThreadInfo.getFinished();
                connection.setRequestProperty("Range", "bytes="+start+"-"+mThreadInfo.getEnd());

                // 设置文件的写入位置
                File file = new File(DownloadService.DOWNLOAD_PATH, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);

                Intent intent = new Intent(DownloadService.ACTION_UPDATE);
                mFinished += mThreadInfo.getFinished();
                // 开始下载
                if (connection.getResponseCode() == 206){
                    // 读取数据
                    input = connection.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while ((len = input.read(buffer)) != -1){
                        // 写入文件
                        raf.write(buffer, 0, len);
                        // 下载进度发送广播给Activity
                        mFinished += len; // 累加整个文件的进度
                        mThreadInfo.setFinished(mThreadInfo.getFinished() + len); // 累加线程的进度
                        if (System.currentTimeMillis() - time > 1000 || mFinished == mFileInfo.getLength()) {
                            int progress = (int) (mFinished * 1.0f * 100 / mFileInfo.getLength());
                            time = System.currentTimeMillis();
                            intent.putExtra("finished", progress);
                            intent.putExtra("id", mFileInfo.getId());
                            mContext.sendBroadcast(intent);
                        }
                        // 暂停时，保存下载进度
                        if (isPause){
                            mDAO.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mThreadInfo.getFinished());
                            return;
                        }
                    }
                    // 表示线程执行完毕
                    isFinished = true;

                    // 检查下载任务是否执行完毕
                    checkAllThreadFinished();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                connection.disconnect();
                try {
                    raf.close();
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
