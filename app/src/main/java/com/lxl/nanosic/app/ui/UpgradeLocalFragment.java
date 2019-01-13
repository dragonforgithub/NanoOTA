package com.lxl.nanosic.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lxl.nanosic.app.R;
import com.lxl.nanosic.app.UpgradeActivity;
import com.lxl.nanosic.app.Utils;
import com.lxl.nanosic.app.ble.BroadcastAction;
import com.lxl.nanosic.app.L;
import com.lxl.nanosic.app.okhttp.CallBackUtil;
import com.lxl.nanosic.app.okhttp.OkhttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;

public class UpgradeLocalFragment extends DialogFragment
		implements View.OnClickListener {

	@BindView(R.id.Close_Frame)
	ImageButton btn_close;

	@BindView(R.id.tv_otaBin)
	TextView tv_ota_bin;

	@BindView(R.id.UpgradeFileSpinner)
	Spinner spinner_UpgradeFile;

	@BindView(R.id.Project_ID)
	EditText project_id;

	@BindView(R.id.Start_Upgrade)
	Button btn_start;

	@BindView(R.id.Delete_File)
	Button btn_delete;

	@BindView(R.id.DownLoading)
	ProgressBar downloading;

	private static boolean isUiStarted=false;   // UI是否已经启动

	/**handler消息类型定义*/
	public final int SSL_SENDMSG_GET_LIST = 1;      //获取下载地址
	public final int SSL_SENDMSG_DOWNLOAD_FILE = 2; //下载文件

	private List<String> list_bin=new ArrayList<String>();
	private static JSONArray downloadList=null;

	private Context mContext;

	private String mUpgradeMode=null;
	private String mUpgradeFile=null;
	private String mPrjId=null;
	private String mBinVersion=null;
	private String mDownLoadURL=null;

	final private String mSDCardPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/nanosic_ota/";
	private String mProjectSelect = null;

	public static UpgradeLocalFragment newInstance(String type, String projectID, JSONArray jsArray) {
		UpgradeLocalFragment mFragment = new UpgradeLocalFragment();

		if(jsArray != null){
			downloadList = jsArray;
		}

		Bundle bundle = new Bundle();
		bundle.putString("mode", type);
		bundle.putString("id", projectID);
		mFragment.setArguments(bundle);
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
		View view = inflater.inflate(R.layout.local_upgrade_layout, null);
		ButterKnife.bind(this, view);
		initView();
		builder.setView(view);
		return builder.create();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()){
			case R.id.Close_Frame:
				dismiss();
				break;

			case R.id.Start_Upgrade:
				if(mUpgradeMode.equals("Local")) {

					if(mUpgradeFile != null){
						//dismiss(); //退出选择界面
						StartUpgradeActivity(); // 启动升级UI

						// 发送广播告知路径
						L.i("===Send file path broadcast,Upgrade file : " + mUpgradeFile);
						BroadcastAction.sendBroadcast(mContext, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
								BroadcastAction.BROADCAST_CONTENT_UPGRADE_FILE_PATH,
								mUpgradeFile);
					}else{
						Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,"未选择文件！",null);
					}

				}else if(mUpgradeMode.equals("Server")) {

					showDownLoading(true); // 显示进度图标
					L.d("下载升级文件 ---> " + mDownLoadURL);
					sendHttpsMessage(SSL_SENDMSG_DOWNLOAD_FILE, mDownLoadURL, mPrjId);
				}
				break;

			case R.id.Delete_File:
				if(mUpgradeFile != null){
					DeleteFile(mUpgradeFile);
					Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,"删除:",mUpgradeFile);
					listLocalUpgradeFile(mProjectSelect, mSDCardPath); // 扫描本地文件更新选择列表
				}else{
					Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,"未选择文件！",null);
				}
				break;
		}
	}

	private void showDownLoading(boolean state) {
		if(downloading!=null){
			downloading.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
		}
	}

	private void initView(){

		L.d("Local fragment initView..."+mProjectSelect);
		mProjectSelect = null;

		// 添加监听
		btn_close.setOnClickListener(this);
		btn_start.setOnClickListener(this);
		btn_delete.setOnClickListener(this);

		mPrjId = getArguments().getString("id"); // 获取项目编号
		mUpgradeMode = getArguments().getString("mode"); // 根据升级模式更新列表界面
		if(mUpgradeMode.equals("Local")) {
			// 添加文本输入框监听
			project_id.addTextChangedListener(fileAdapter);

			// 自动填充之前输入的值
			String preference = Utils.getPreferences(mContext,"project_filtrate");
			if(preference != null){
				project_id.setText(preference);
			}
			project_id.setVisibility(View.VISIBLE);

			// 显示删除键，添加监听
			btn_delete.setVisibility(View.VISIBLE);
			btn_delete.setOnClickListener(this);

			// 显示筛选文本框
			tv_ota_bin.setVisibility(View.VISIBLE);

			L.i("Local upgrade mode...");
			listLocalUpgradeFile(mProjectSelect, mSDCardPath); // 扫描本地文件更新选择列表
		}
		else if(mUpgradeMode.equals("Server")) {
			L.i("Online upgrade mode...");
			listServerUpgradeFile(downloadList); // 根据服务器的回传值更新列表
		}
	}

	// 检测输入的项目编号
	private TextWatcherAdapter fileAdapter = new TextWatcherAdapter() {
		@Override
		public void afterTextChanged(Editable s) {
		mProjectSelect = s.toString();
		listLocalUpgradeFile(mProjectSelect, mSDCardPath); // 更新选择列表
		Utils.savePreferences(mContext,"project_filtrate", mProjectSelect);
		}
	};

	/*使用服务器回复的数据更新列表*/
	private void listServerUpgradeFile(JSONArray jsArray) {

		// 在线升级则根据回传值更新选择列表
		for (int i = 0; i < jsArray.length(); i++) {
			try {
				JSONObject verList = (JSONObject) jsArray.get(i);
				list_bin.add(verList.getString("version"));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		//设置下拉列表的风格
		ArrayAdapter<String> adapter_res =new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, list_bin);
		spinner_UpgradeFile.setAdapter(adapter_res);

		//設置背景顏色
		//spinner_UpgradeFile.setBackgroundColor(getResources().getColor(R.color.featureTitleColor));

		//添加事件Spinner事件监听
		spinner_UpgradeFile.setOnItemSelectedListener(new SpinnerSelectedListener());
	}

	/*扫描本机目录下的升级文件*/
	private void listLocalUpgradeFile(String ProjectId, String FilePath) {

		// 清除之前选择的文件
		mUpgradeFile = null;

		// 清空之前列表
		list_bin.clear();

		// 扫描文件
		File file = new File(FilePath);
		if(file.exists()){
			File[] files = file.listFiles();

			for (int i = files.length-1; i >= 0; i--) {
				if (files[i].isFile()) {
					String filename = files[i].getName();
					// 获取.bin格式文件，如果指定项目编号则进行筛选
					if(filename.endsWith(".bin") && (ProjectId==null || filename.startsWith(ProjectId))){
						//String filePath = files[i].getAbsolutePath();
						//L.i("files[" + i + "].getAbsolutePath() = " + filePath);
						list_bin.add(filename);
					}
				} else if (files[i].isDirectory()) {
					FilePath = files[i].getAbsolutePath();
					listLocalUpgradeFile(ProjectId, FilePath);
				}
			}
		}else {
			file.mkdirs();
			L.e(FilePath+":not exist,then mkdirs.");
		}

        //设置下拉列表的风格
        ArrayAdapter<String> adapter_res =new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, list_bin);

        //将adapter 添加到spinner中
		spinner_UpgradeFile.setAdapter(adapter_res);

        //設置背景顏色
        //spinner_upgrade.setBackgroundColor(getResources().getColor(R.color.featureTitleColor));

        //添加事件Spinner事件监听
		spinner_UpgradeFile.setOnItemSelectedListener(new SpinnerSelectedListener());
	}

	//播放列表事件监听---------------------------------------------
	class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

			TextView tv = (TextView)arg1;
			if(tv != null){
				tv.setTextColor(getResources().getColor(R.color.cpb_yellow_dark));    //设置颜色
				tv.setTextSize(16.0f);    //设置字体大小
				//tv.setGravity(Gravity.CENTER_HORIZONTAL);   //设置居中（异常）
			}

			switch (arg0.getId()) {
				case R.id.UpgradeFileSpinner:
					// 选择的版本号
					mBinVersion = list_bin.get(arg2); // 参数含义 ：在线是版本号，本地是路径
					L.d("mBinVersion = "+mBinVersion);
					// 判断根据当前模式(在线or本地升级)
					if(mUpgradeMode.equals("Local")) {
						// 本地升级文件路径，mBinVersion为rc1661_remote_119.bin
						mUpgradeFile = mSDCardPath+mBinVersion;

					}else if(mUpgradeMode.equals("Server")) {
						//下载路径，例如："https://47.98.206.54/rc1888/101/remote.bin"，mBinVersion为119
						mDownLoadURL = "https://47.98.206.54/app/"+mPrjId+"/"+mBinVersion+"/remote.bin";
					}
					break;
				default:
					L.e("select error!");
					break;
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			L.w( "onNothingSelected!");
		}
	}

	// 处理https消息更新UI
	private Handler mHttpsHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			//msg传递过来的参数类型
			int msgType = msg.what;
			//msg传递过来的参数内容
			String str1 = msg.getData().getString("text1");
			String str2 = msg.getData().getString("text2");

			//final String downloadURL = "https://47.98.206.54/rc1888/101/remote.bin";
			final String downloadURL = str1;
			final String projectID = str2;

			switch (msgType) {
				case SSL_SENDMSG_GET_LIST:
					break;

				case SSL_SENDMSG_DOWNLOAD_FILE:
                    L.d("DownloadFile(URL) ---> " + downloadURL);
					//设置本地保存路径
					String fileName = projectID+"_remote_"+mBinVersion+".bin";
                    // 回调获取执行结果
					OkhttpUtil.okHttpDownloadFile(downloadURL, new CallBackUtil.CallBackFile(mContext, mSDCardPath, fileName) {
						@Override
						public void onFailure(Call call, Exception e) {
							L.e( "DownloadFile error:" + e);
							Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,"错误:","文件下载失败！");
							showDownLoading(false);
						}

						@Override
						public void onResponse(String response) {
							L.d("DownloadFile(String) ---> " + response);
							showDownLoading(false);
						}

						@Override
						public void onResponse(File response) {
							L.d("DownloadFile(File) ---> " + response);
							showDownLoading(false);

							if(response != null){
                                //发送广播包给BLE service，告知本地升级文件路径
                                L.i("===Send file path broadcast : " + response.toString());
                                BroadcastAction.sendBroadcast(mContext, BroadcastAction.BROADCAST_SERVICE_REC_ACTION_REMOTE_UPGRADE,
                                        BroadcastAction.BROADCAST_CONTENT_UPGRADE_FILE_PATH,
                                        response.toString());

                                //退出选择界面
                                dismiss();

                                // 启动升级UI
                                StartUpgradeActivity();
                            } else{
                                Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL,"错误:","无效的升级文件！");
                            }
						}
					});

					break;
			}
			return false;
		}
	});

	/**发送消息*/
	private void sendHttpsMessage(int msgType, String message ,String version)
	{
		Message VendorMessage = new Message();
		//消息类型
		VendorMessage.what = msgType;
		//消息内容
		Bundle bundle = new Bundle();
		bundle.putString("text1",message);  //往Bundle中存放数据
		bundle.putString("text2",version);
		VendorMessage.setData(bundle);  	//message利用Bundle传递数据
		//发送消息
		mHttpsHandler.sendMessage(VendorMessage);
	}

	// 启动升级UI
	private void StartUpgradeActivity(){
		if(!isUiStarted){
			Intent upgradeUiIntent = new Intent(mContext, UpgradeActivity.class);  // 要启动的Activity
			upgradeUiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(upgradeUiIntent);
		}
		L.i("=== Start UI : "+isUiStarted);
	}

	//删除文件
	private void DeleteFile(String filePath){
		File file = new File(filePath);
		if(file.isFile()){
			file.delete();
		}
		file.exists();
	}
}
