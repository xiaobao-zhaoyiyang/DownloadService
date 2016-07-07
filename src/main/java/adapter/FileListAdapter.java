package adapter;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.imooc_downloaddemo.R;

import java.util.List;

import entities.FileInfo;
import service.DownloadService;

/**
 * 文件列表适配器
 */
public class FileListAdapter extends BaseAdapter {

    private Context mContext;
    private List<FileInfo> mFileList;

    public FileListAdapter(Context context, List<FileInfo> fileList) {
        this.mContext = context;
        this.mFileList = fileList;
    }

    @Override
    public int getCount() {
        return mFileList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null){
            convertView = View.inflate(mContext, R.layout.listitem, null);
            holder = new ViewHolder();
            holder.tvFile = (TextView) convertView.findViewById(R.id.tvFileName);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.pbProgress);
            holder.btStart = (Button) convertView.findViewById(R.id.btStart);
            holder.btStop = (Button) convertView.findViewById(R.id.btStop);

            holder.progressBar.setMax(100);

            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvFile.setText(mFileList.get(position).getFileName());
        holder.progressBar.setProgress(mFileList.get(position).getFinished());
        holder.btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_start = new Intent(mContext, DownloadService.class);
                intent_start.setAction(DownloadService.ACTION_START);
                intent_start.putExtra("fileInfo", mFileList.get(position));
                mContext.startService(intent_start);
            }
        });
        holder.btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_start = new Intent(mContext, DownloadService.class);
                intent_start.setAction(DownloadService.ACTION_STOP);
                intent_start.putExtra("fileInfo", mFileList.get(position));
                mContext.startService(intent_start);
            }
        });
        return convertView;
    }

    /**
     * 更新列表项中的进度条
     */
    public void updateProgress(int id, int progress){
        FileInfo fileInfo = mFileList.get(id);
        fileInfo.setFinished(progress);
        notifyDataSetChanged();// 重新调用getView()方法
    }

    // 定义为static是保证此类只加载一次以节省内存
    static class ViewHolder{
        TextView tvFile;
        Button btStart;
        Button btStop;
        ProgressBar progressBar;
    }
}
