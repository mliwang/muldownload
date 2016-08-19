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
		probarlists.clear();// ������ǰ��
		// ����threadcount��ֵ��ӽ������ĸ���
		for (int i = 0; i < threadcount; i++) {
			ProgressBar probar = (ProgressBar) View.inflate(
					getApplicationContext(), R.layout.probar, null);
			probarlists.add(probar);
			ll_probar.addView(probar);

		}
		// ���ļ���С��Ҫ����
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
						// ��ȡ�������ļ���С
						int length = conn.getContentLength();
						System.out.println("��ȡ���ȳɹ�������Ϊ" + length);
						runningthread = threadcount;
						// ����һ���ͷ������ļ�һ����С�Ŀռ���ǰ����Ҫ�Ŀռ��������

						RandomAccessFile raf = new RandomAccessFile(
								getFilename(path), "rw");
						raf.setLength(length);
						// ���ÿ���߳����صĴ�С
						int blocksize = length / threadcount;
						// ��ʼΪ�����̷߳�����ʼ��ֹλ��
						for (int i = 0; i < threadcount; i++) {
							int startindext = i * blocksize;
							int endindex = (i + 1) * blocksize - 1;
							// �������һ���߳̿��ܶ��µ�
							if (i == (threadcount - 1)) {
								endindex = length - 1;

							}
							// �����߳̿�ʼ����
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
		// ͨ�����췽����ÿ���߳̿�ʼ����λ�ô�����
		private int startindext;
		private int endindex;
		private int threadId;
		private int pbmax;//��ŵ�ǰʣ��������
		private int pblastPosition;//��Ž��������һ�ζϵ�ǰ������

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
				// ����м�Ϲ��������ϴε�λ�ü����� ���ļ��ж�ȡ�ϴ����ص�λ��
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
				// ��������ͷ��Ϣ(���ø��ߵ�ǰ�߳����صĿ�ʼλ�úͽ���λ��)
				conn.setRequestProperty("Range", "bytes=" + startindext + "-"
						+ endindex);
				System.out.println("�̡߳�������" + threadId
						+ "���صĿ�ʼλ�úͽ���λ�ú���󳤶ȷֱ��ǡ�������" + startindext + "___"
						+ endindex + "__" + pbmax);

				int code = conn.getResponseCode();// 200�����ȡ��������Դȫ���ɹ���206�������󲿷���Դ
				if (code == 206) {
					// ���������д�ļ�����
					RandomAccessFile raf = new RandomAccessFile(
							getFilename(path), "rw");
					InputStream in = conn.getInputStream();
					int len = -1;
					// total��¼��ǰ�߳����ص��ܴ�С
					int toatal = 0;
					byte[] buffer = new byte[1024 * 1024];
					while ((len = in.read(buffer)) != -1) {
						raf.write(buffer, 0, len);
						// ʵ�ֶϵ���������¼�ϵ���λ��
						toatal += len;
						int currentthreadposition = startindext + toatal;
						RandomAccessFile raff = new RandomAccessFile(
								getFilename(path) + threadId + ".txt", "rwd");
						raff.write(String.valueOf(currentthreadposition)
								.getBytes());
						raff.close();
						// �ڴ˴����½��������Ա�֤��������ʵʱ��
						probarlists.get(threadId).setMax(pbmax);

						probarlists.get(threadId).setProgress(
								pblastPosition + toatal);
					}
					raf.close();
					System.out.println("�߳�id" + threadId + "-----�������");
					synchronized (DownLoadThread.class) {// ����������̼����
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
