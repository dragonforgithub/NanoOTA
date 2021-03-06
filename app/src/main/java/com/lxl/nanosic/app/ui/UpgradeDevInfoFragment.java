package com.lxl.nanosic.app.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.lxl.nanosic.app.L;
import com.lxl.nanosic.app.R;
import com.lxl.nanosic.app.ble.BroadcastAction;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UpgradeDevInfoFragment extends DialogFragment implements View.OnClickListener {

	@BindView(R.id.BTN_CLOSE)
	ImageButton mBtn_Close;
	@BindView(R.id.textView_Remote_Info)
	TextView TextView_RemoteInfo;
	@BindView(R.id.textView_Remote_Version_Power)
	TextView TextView_RemoteVerPower;
	@BindView(R.id.textView_Operation_Permission)
	TextView TextView_OperatePermission;

	private Context mContext=null;

	private String RemoteMac=null,RemoteName=null;
	private String RemoteVidPid=null,RemoteProtocol=null;
	private int RemoteSfVer=0x0,RemotePower=0x0;

	// 所需蓝牙和存储权限
	private String[] BlePermissions = {
			Manifest.permission.BLUETOOTH,                      /*  允许程序连接配对过的蓝牙设备  */
			Manifest.permission.BLUETOOTH_ADMIN,                /*  允许程序进行发现和配对新的蓝牙设备  */
			Manifest.permission.BLUETOOTH_PRIVILEGED,           /*  允许应用程序配对蓝牙设备，而无需用户交互  */
	};
	private String[] StoragePermissions = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
	};

	public static UpgradeDevInfoFragment newInstance() {
		UpgradeDevInfoFragment mFragment = new UpgradeDevInfoFragment();
		return mFragment;
	}

	@Override
	public void onAttach(Activity activity) {
		mContext = activity;
		super.onAttach(activity);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.show_dev_information, null);
		ButterKnife.bind(this, view);

		// 注册广播
		RegisterBroadcastReceiver();

		// 发送初始化广播
		BroadcastAction.sendBroadcast(mContext, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_BLUETOOTH,
				BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_INIT);

		builder.setView(view);

		// 更新文本信息
		updateTextView();

		// 设置关闭按钮监听
		mBtn_Close.setOnClickListener(this);

		return builder.create();
	}

	@Override
	public void onDestroy() {
		// 注销广播
		UnRegisterBroadcastReceiver();
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.BTN_CLOSE:
				dismiss();
				break;
		}
	}

	/** 更新文本信息 */
	private void updateTextView(){

		L.i("Update device information ...");

		String sTempString;
		sTempString = getResources().getString(R.string.Text_view_remote_info_name);
		sTempString += RemoteName;

		sTempString += "      "+getResources().getString(R.string.Text_view_remote_protocol)+RemoteProtocol;

		sTempString += "\n";
		sTempString += getResources().getString(R.string.Text_view_remote_info_mac);
		sTempString += RemoteMac;

		sTempString += "      VID/PID:"+RemoteVidPid;

		TextView_RemoteInfo.setText(sTempString);
		sTempString = getResources().getString(R.string.Text_view_remote_ver);
		if(RemoteSfVer > 0)
			sTempString += String.format("0x%04X", RemoteSfVer);
		sTempString += "\n";
		sTempString += getResources().getString(R.string.Text_view_remote_power);
		if(RemotePower > 0)
			sTempString += String.format("%dmV", RemotePower);
		TextView_RemoteVerPower.setText(sTempString);
		sTempString = getResources().getString(R.string.Text_view_operation_permission);
		TextView_OperatePermission.setText(sTempString);

		//获取当前Android设备的权限状态
		sTempString = getResources().getString(R.string.Text_view_operation_permission);
        sTempString += "\n";
		if(hasPermission(mContext, BlePermissions[0]))
		{
			L.w("BLUETOOTH permission are normal.");
			sTempString += "            BLUETOOTH";
			sTempString += "\n";
		}else{
			L.e("No BLUETOOTH permission.");
		}

		if(hasPermission(mContext, BlePermissions[1]))
		{
			L.w("BLUETOOTH_ADMIN permissions are normal.");
			sTempString += "            BLUETOOTH_ADMIN";
			sTempString += "\n";
		}else{
			L.e("No BLUETOOTH_ADMIN permission.");
		}

		if(hasPermission(mContext, BlePermissions[2]))
		{
			L.w("BLUETOOTH_PRIVILEGED permissions are normal.");
			sTempString += "            BLUETOOTH_PRIVILEGED";
			sTempString += "\n";
		}else{
			L.e("No BLUETOOTH_PRIVILEGED permission.");
		}

		if(hasPermission(mContext, StoragePermissions[0]))
		{
			L.w("READ_EXTERNAL_STORAGE permission are normal.");
			sTempString += "            READ_EXTERNAL_STORAGE";
			sTempString += "\n";
		}else{
			L.e("No READ_EXTERNAL_STORAGE permission.");
		}

		if(hasPermission(mContext, StoragePermissions[1]))
		{
			L.w("WRITE_EXTERNAL_STORAGE permissions are normal.");
			sTempString += "            WRITE_EXTERNAL_STORAGE";
			sTempString += "\n";
		}else{
			L.e("No WRITE_EXTERNAL_STORAGE permission.");
		}
		TextView_OperatePermission.setText(sTempString);
	}

	// 检测权限
	private boolean hasPermission(Context context, String permission){
		int perm = context.checkCallingOrSelfPermission(permission);
		return perm == PackageManager.PERMISSION_GRANTED;
	}

	// 注册广播
	private void RegisterBroadcastReceiver(){
		IntentFilter activityFilter = new IntentFilter();
		activityFilter.addAction(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH);
		activityFilter.addAction(BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE);
		mContext.registerReceiver(DevInfoReceiver, activityFilter);
		L.d("Register DevInfoReceiver");
	}

	// 注销广播
	private void UnRegisterBroadcastReceiver(){
		try {
			if(DevInfoReceiver != null) {
				mContext.unregisterReceiver(DevInfoReceiver);
				L.d("Unregister DevInfoReceiver");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** 处理收到的广播 */
	private BroadcastReceiver DevInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int ibroad_value = 0x00;
			String sbroad_value = null, sbroad_aux_val = null,sTempString;
			final String action = intent.getAction();
			String value_type = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_TYPE);
			if (value_type.equals(BroadcastAction.BROADCAST_VALUE_STRING)) {
				sbroad_value = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING);
			} else if (value_type.equals(BroadcastAction.BROADCAST_VALUE_INT)) {
				ibroad_value = intent.getIntExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_INT, 0);
			} else if (value_type.equals(BroadcastAction.BROADCAST_VALUE_STRING_INT)) {
				sbroad_value = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING);
				ibroad_value = intent.getIntExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_INT, 0);
			} else if (value_type.equals(BroadcastAction.BROADCAST_VALUE_STRING_STRING)) {
				sbroad_value = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING);
				sbroad_aux_val = intent.getStringExtra(BroadcastAction.BROADCAST_VALUE_CONTENT_STRING_AUX);
			}

			if (BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_BLUETOOTH.equals(action))
			{
				if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_INFO)) {

				} else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_CONNECTED)) {
					// Gatt connected
					RemoteMac = sbroad_aux_val;
					L.i("Receive broadcast,Ble Address :" + RemoteMac);
					updateTextView();

				} else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_DISCOVERED)) {
					// Gatt discovered
					RemoteName = sbroad_aux_val;
					L.i("Receive broadcast,the device is connected successfully.The name is:" + RemoteName);
					updateTextView();

					// 发送广播获取遥控版本和电量
					BroadcastAction.sendBroadcast(mContext, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
							BroadcastAction.BROADCAST_CONTENT_REMOTE_INFO);

				} else  if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_GATT_DISCONNECTED)) {
					// Gatt dis connected
					String BleAddress = sbroad_aux_val;
					L.i("Receive broadcast,Ble disconnected :" + BleAddress);
					updateTextView();

				} else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_DEV_PROTOCOL)) {
					// 遥控器OTA协议
					RemoteProtocol = sbroad_aux_val;
					L.i("Receive broadcast,the device ota protocol:" + RemoteProtocol);
					updateTextView();

				} else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_BLUETOOTH_DEV_VIDPID)) {
					// 遥控器VID PID
					RemoteVidPid = "0x"+sbroad_aux_val;
					L.i("Receive broadcast,the device ota vid pid:" + RemoteVidPid);
					updateTextView();
				}
			}
			else if (BroadcastAction.BROADCAST_SERVICE_SEND_ACTION_REMOTE_UPGRADE.equals(action))
			{
				if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_REMOTE_VERSION)) {
					// 遥控器版本
					RemoteSfVer = ibroad_value;
					L.i("Receive broadcast,the device software version:" + String.format("0x%04X,", RemoteSfVer));
					updateTextView();

				} else if (sbroad_value.equals(BroadcastAction.BROADCAST_CONTENT_REMOTE_POWER)) {
					// 遥控器电量
					RemotePower = ibroad_value;
					L.i("Receive broadcast,the device software version:" + String.format("%d,", RemotePower));
					updateTextView();
				}
			}
		}
	};
}
