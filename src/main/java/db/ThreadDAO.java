package db;

import java.util.List;

import entities.ThreadInfo;

/**
 * 数据访问接口
 */
public interface ThreadDAO {
    /**
     * 插入线程信息
     * @param threadInfo   34004600
     *                     2147483647
     */
    public void insertThread(ThreadInfo threadInfo);

    /**
     * 删除线程
     * @param url
     */
    public void deleteThread(String url);

    /**
     * 更新线程下载进度
     * @param url
     * @param thread_id
     * @param finished
     */
    public void updateThread(String url, int thread_id, int finished);

    /**
     * 查询文件的线程信息
     * @param url
     * @return
     */
    public List<ThreadInfo> getThreads(String url);

    /**
     * 判断线程信息是够存在
     * @param url
     * @return
     */
    public boolean isExists(String url, int thread_id);
}
