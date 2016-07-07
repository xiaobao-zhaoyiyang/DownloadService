package com.imooc_downloaddemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import adapter.FileListAdapter;
import entities.FileInfo;
import service.DownloadService;

public class MainActivity extends AppCompatActivity{

    private ListView mLvFile;
    private List<FileInfo> mFileList;
    private FileListAdapter mAdapter;
    private String url =
            "http://sw.bos.baidu.com/sw-search-sp/software/3b90c8326dc/kugou_8.0.63.18858_setup.exe";
    private FileInfo fileInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        // 注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_UPDATE);
        filter.addAction(DownloadService.ACTION_FINISH);
        registerReceiver(mReceiver, filter);
    }

    private void initView() {
        mLvFile = (ListView) findViewById(R.id.lvFile);

        mFileList = new ArrayList<>();
        // 创建文件信息
        for (int i = 0; i < 10; i++) {
            fileInfo = new FileInfo(i, url, "kugou_" + i + ".exe", 0, 0);
            mFileList.add(fileInfo);
        }

        mAdapter = new FileListAdapter(this, mFileList);
        mLvFile.setAdapter(mAdapter);
    }
    /**
     * 更新UI的广播接收器
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadService.ACTION_UPDATE.equals(intent.getAction())){
                int finished = intent.getIntExtra("finished", 0);
                int id = intent.getIntExtra("id", 0);
                mAdapter.updateProgress(id, finished);
            }else  if (DownloadService.ACTION_FINISH.equals(intent.getAction())){
                FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
                int id = fileInfo.getId();
                mAdapter.updateProgress(id, 0);
                Toast.makeText(MainActivity.this, mFileList.get(id).getFileName() + "下载完成", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(mReceiver);
    }
}
