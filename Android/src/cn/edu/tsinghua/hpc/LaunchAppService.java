package cn.edu.tsinghua.hpc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LaunchAppService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public static final int INSTALL_REPLACE_EXISTING = 0x00000002;

	public class DownloadRunnable implements Runnable {
		private String packageName;

		public DownloadRunnable(String packageName) {
			this.packageName = packageName;
		}

		private boolean installPackage(String pkgFile) {
			// TODO checking install status and return results.
			Runtime tr = Runtime.getRuntime();
			Log.d("Diplomat", "Intalling" + pkgFile);
			try {
				Process p = tr.exec("pm install " + pkgFile);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				String lineRead = null;
				Log.d("Diplomat", "STD output of excuting 'pm install "
						+ pkgFile);
				while ((lineRead = reader.readLine()) != null) {
					Log.d("Diplomat", lineRead);
				}

				BufferedReader stdError = new BufferedReader(
						new InputStreamReader(p.getErrorStream()));
				Log.d("Diplomat", "ERR output of excuting 'pm install "
						+ pkgFile);
				while ((lineRead = stdError.readLine()) != null) {
					Log.d("Diplomat", lineRead);
				}
				return true;
			} catch (Exception e) {
				Toast.makeText(LaunchAppService.this, "Install package error!",
						Toast.LENGTH_SHORT).show();
			}
			return false;
		}

		public void run() {
			Log.d("test", "download " + packageName);
			String apkName = ApplicationUtils.getInstance().downloadTApp(
					packageName, packageName, LaunchAppService.this);
			if (apkName != null) {
				if (installPackage(apkName)) {

					PackageManager manager = getPackageManager();

					Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
					mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

					final List<ResolveInfo> apps = manager
							.queryIntentActivities(mainIntent, 0);
					for (ResolveInfo i : apps) {
						if (i.activityInfo.packageName.equals(packageName)) {
							Intent intent = new Intent(Intent.ACTION_MAIN);
							intent.addCategory(Intent.CATEGORY_LAUNCHER);
							intent.setComponent(new ComponentName(
									i.activityInfo.packageName,
									i.activityInfo.name));
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
									| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
							startActivity(intent);

						}
					}

				}
			} else {
				Toast.makeText(LaunchAppService.this,
						"Can not retrieve destination application!",
						Toast.LENGTH_LONG);
			}
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		String packageName = intent.getExtras().getString("package");
		new Thread(new DownloadRunnable(packageName)).start();
	}

}
