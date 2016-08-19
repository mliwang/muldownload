package cn.itcast.down;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.os.Build;

public class MainActivity extends ActionBarActivity {

	private EditText et_url;
	private EditText et_threadcount;
	private LinearLayout ll_probar;
	private String path;
	private int threadcount;
	private int runningthread;
	private List<ProgressBar> probarlists;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et_url = (EditText) findViewById(R.id.et_url);
		et_threadcount = (EditText) findViewById(R.id.et_threadcount);
		ll_probar = (LinearLayout) findViewById(R.id.ll_probar);
		probarlists = new ArrayList<ProgressBar>();

	}

	public void click(View v) {

		path = et_url.getText().toString().trim();
		threadcount = Integer.parseInt(et_threadcount.getText().toString()
				.trim());
		ll_probar.removeAllViews();
		probarlists.clear();// 先清以前的
		// 根据threadcount的值添加进度条的个数
		for (int i = 0; i < threadcount; i++) {
			ProgressBar probar = (ProgressBar) View.inflate(
					getApplicationContext(), R.layout.probar, null);
			probarlists.add(probar);
			ll_probar.addView(probar);

		}
		// 算文件大小需要联网
		new Thread() {
			public void run() {
				try {
					URL url = new URL(path);
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(5000);
					int code = conn.getResponseCode();
					if (code == 200) {
						// 获取服务器文件大小
						int length = conn.getContentLength();
						System.out.println("获取长度成功，长度为" + length);
						runningthread = threadcount;
						// 创建一个和服务器文件一样大小的空间提前把需要的空间申请出来

						RandomAccessFile raf = new RandomAccessFile(
								getFilename(path), "rw");
						raf.setLength(length);
						// 算出每个线程下载的大小
						int blocksize = length / threadcount;
						// 开始为各个线程分配起始终止位置
						for (int i = 0; i < threadcount; i++) {
							int startindext = i * blocksize;
							int endindex = (i + 1) * blocksize - 1;
							// 对于最后一个线程可能多下点
							if (i == (threadcount - 1)) {
								endindex = length - 1;

							}
							// 开启线程开始下载
							DownLoadThread downLoadThread = new DownLoadThread(
									startindext, endindex, i);
							downLoadThread.start();
						}

					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			};
		}.start();
	}

	public String getFilename(String path) {
		int start = path.lastIndexOf("/") + 1;
		return Environment.getExternalStorageDirectory().getPath()+"/"+path.substring(start);

	}

	private class DownLoadThread extends Thread {
		// 通过构造方法把每个线程开始结束位置传过来
		private int startindext;
		private int endindex;
		private int threadId;
		private int pbmax;//存放当前剩余的最大量
		private int pblastPosition;//存放进度条最后一次断点前下载量

		public DownLoadThread(int startindext, int endindex, int threadId) {
			this.endindex = endindex;
			this.startindext = startindext;
			this.threadId = threadId;
		}

		public void run() {

			try {
				pbmax = endindex - startindext;
				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				// 如果中间断过，继续上次的位置继续下 从文件中读取上次下载的位置
				File file = new File(getFilename(path) + threadId + ".txt");
				if (file.exists() && file.length() > 0) {
					FileInputStream fis = new FileInputStream(file);
					BufferedReader buffer = new BufferedReader(
							new InputStreamReader(fis));
					String position = buffer.readLine();
					int lastposition = Integer.parseInt(position);
					pblastPosition = lastposition - startindext;
					startindext = lastposition + 1;
					fis.close();
				} 
				// 设置请求头信息(作用告诉当前线程下载的开始位置和结束位置)
				conn.setRequestProperty("Range", "bytes=" + startindext + "-"
						+ endindex);
				System.out.println("线程————" + threadId
						+ "下载的开始位置和结束位置和最大长度分别是————" + startindext + "___"
						+ endindex + "__" + pbmax);

				int code = conn.getResponseCode();// 200代表获取服务器资源全部成功，206代表请求部分资源
				if (code == 206) {
					// 创建随机读写文件对象
					RandomAccessFile raf = new RandomAccessFile(
							getFilename(path), "rw");
					InputStream in = conn.getInputStream();
					int len = -1;
					// total记录当前线程下载的总大小
					int toatal = 0;
					byte[] buffer = new byte[1024 * 1024];
					while ((len = in.read(buffer)) != -1) {
						raf.write(buffer, 0, len);
						// 实现断点续传，记录断掉的位置
						toatal += len;
						int currentthreadposition = startindext + toatal;
						RandomAccessFile raff = new RandomAccessFile(
								getFilename(path) + threadId + ".txt", "rwd");
						raff.write(String.valueOf(currentthreadposition)
								.getBytes());
						raff.close();
						// 在此处更新进度条可以保证进度条的实时性
						probarlists.get(threadId).setMax(pbmax);

						probarlists.get(threadId).setProgress(
								pblastPosition + toatal);
					}
					raf.close();
					System.out.println("线程id" + threadId + "-----下载完毕");
					synchronized (DownLoadThread.class) {// 加锁避免进程间混乱
						runningthread--;
						if (runningthread == 0) {
							for (int i = 0; i < threadcount; i++) {
								File deletefile = new File(getFilename(path)
										+ i + ".txt");
								deletefile.delete();

							}

						}

					}

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
