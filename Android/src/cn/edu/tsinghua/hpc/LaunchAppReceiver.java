package cn.edu.tsinghua.hpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LaunchAppReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		String packageName = intent.getExtras().getString("package");
		if (packageName != null) {
			Intent in = new Intent(context, LaunchAppService.class);
			in.putExtra("package", intent.getExtras().getString("package"));
			context.startService(in);
		}

	}

}
