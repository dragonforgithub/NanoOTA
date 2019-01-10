package com.lxl.nanosic.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lxl.nanosic.app.L;
import com.lxl.nanosic.app.R;
import com.lxl.nanosic.app.ble.BroadcastAction;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 检测到新版本，提示是否更新
 */
public class SelectDialogFragment extends DialogFragment implements View.OnClickListener {
	@BindView(R.id.tv_hint)
	TextView mTvHint;
	@BindView(R.id.btn_yes)
	Button mBtnYes;
	@BindView(R.id.btn_no)
	Button mBtnNo;

    private Context mContext;
    private static String mTitle=null;

	public static SelectDialogFragment newInstance(String str1, String str2)
	{
		if(str1.equals("OtaApk")){
			mTitle = "检测到新版本，是否下载？";
		}else{
			mTitle = "新版本已下载，是否安装？";
		}
		SelectDialogFragment mFragment = new SelectDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString("name", str1); //projectName
		bundle.putString("version", str2); //versionStr
		mFragment.setArguments(bundle);
		mFragment.setCancelable(false); //不可取消
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
		View view = inflater.inflate(R.layout.select_dialog, null);
		ButterKnife.bind(this, view);
		initView(mTitle); // 初始化界面
		builder.setView(view);
		return builder.create();
	}

	private void initView(String hint){
		mTvHint.setText(hint);
		mBtnYes.setOnClickListener(this);
		mBtnNo.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {

		switch (view.getId()){
			case R.id.btn_yes:
				String prjName = getArguments().getString("name");
				String verStr = getArguments().getString("version");

				L.d("===Send apk download broadcast");
				// 广播通知下载安装包
				BroadcastAction.sendBroadcast(mContext, BroadcastAction.MAIN_UPDATE_APK_DOWNLOAD,
                        prjName,verStr);

				dismiss();
				break;
			case R.id.btn_no:
				dismiss();
				break;
		}
	}
}
