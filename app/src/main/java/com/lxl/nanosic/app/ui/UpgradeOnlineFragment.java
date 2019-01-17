package com.lxl.nanosic.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.lxl.nanosic.app.L;
import com.lxl.nanosic.app.R;
import com.lxl.nanosic.app.Utils;
import com.lxl.nanosic.app.okhttp.CallBackUtil;
import com.lxl.nanosic.app.okhttp.OkhttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;

public class UpgradeOnlineFragment extends DialogFragment implements View.OnClickListener {
    @BindView(R.id.et_DownloadFile)
    EditText mDownloadFile;
    @BindView(R.id.BTN_NEXT)
    Button mBtnNext;
    @BindView(R.id.BTN_CLOSE)
    ImageButton mBtnClose;
    @BindView(R.id.loading)
    ProgressBar loading;

    private Context mContext=null;
    private String mProjectId=null;

    /**handler消息类型定义*/
    public final int SSL_SENDMSG_GET_LIST = 1;      //获取下载地址
    public final int SSL_SENDMSG_DOWNLOAD_FILE = 2; //下载文件

    @Override
    public void onAttach(Activity activity) {
        mContext = activity;
        super.onAttach(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.online_upgrade_layout, null);
        ButterKnife.bind(this, view);
        initView();
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onDestroy() {
        mContext = null;
        L.d("UpgradeOnlineFragment : onDestroy");
        super.onDestroy();
    }

    private void initView() {
        mDownloadFile.addTextChangedListener(adapter);
        // 自动填充之前输入的值
        String preference = Utils.getPreferences(mContext,"project_download");
        if(preference != null){
            mDownloadFile.setText(preference);
        }

        mBtnNext.setOnClickListener(this);
        mBtnClose.setOnClickListener(this);
    }

    private TextWatcherAdapter adapter = new TextWatcherAdapter() {
        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() >= 6 && s.toString().matches("^[a-z]{2}[0-9]*$")) {
                mBtnNext.setEnabled(true);
                mProjectId = s.toString();
                Utils.savePreferences(mContext,"project_download", mProjectId);
            } else {
                mBtnNext.setEnabled(false);
            }
        }
    };

    // 展示下载进度图标
    private void showLoadingIcon(boolean state) {
        mBtnNext.setEnabled(!state); // 下载时关闭按键
        if(loading!=null){
            loading.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void CheckPhoneSuccess(String msg) {
        //已经注册
        loading.setVisibility(View.GONE);
    }

    private void CheckPhoneFail(String msg) {
        //没有注册
        loading.setVisibility(View.GONE);
    }

    private void onNetworkError(String msg) {
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.BTN_NEXT:

                // 防止短时间连续点击产生多个DialogFragment
                /*
                if(Utils.isFastDoubleClick()){
                    L.w("Click too fast!!!");
                    return;
                }
                */

                // 显示进度图标
                showLoadingIcon(true);

                // 检测到合理项目名则发送服务器访问消息，获取下载路径
                sendHttpsMessage(SSL_SENDMSG_GET_LIST, mProjectId);

                break;
            case R.id.BTN_CLOSE:
                dismiss();
                break;
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
            //String str2 = msg.getData().getString("text2");
            final String msgInte = str1;// + str2;

            try {
                switch (msgType) {
                    case SSL_SENDMSG_GET_LIST:
                        // 服务器地址
                        String serverURL = "https://47.98.206.54/app/LookUpVersList.ashx";

                        // 发送json格式数据，获取下载路径
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("Mark", msgInte);
                        L.i( "Update Project ID : " + msgInte);

                        // 回调获取执行结果
                        OkhttpUtil.okHttpPostJson(serverURL, jsonObject.toString(1), new CallBackUtil.CallBackString() {
                            @Override
                            public void onFailure(Call call, Exception e) {
                                L.e( "error:" + e.getMessage());
                                showLoadingIcon(false);// 关闭进度图标

                                //如果界面已经关闭则直接返回
                                if(mContext==null) return;

                                String mainText = getResources().getString(R.string.Text_view_error_code_title);
                                String subText = getResources().getString(R.string.Toast_view_network_error);
                                Utils.ToastShow(mContext, Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL, mainText, subText);
                            }

                            @Override
                            public void onResponse(String response) {
                                L.d("Post ---> " + response);
                                try {
                                    JSONObject jsObj = new JSONObject(response); //转换成json对象
                                    JSONArray jsArray = jsObj.getJSONArray("list"); //取出版本列表

                                    showLoadingIcon(false); // 关闭进度图标

                                    //如果界面已经关闭则直接返回
                                    if(mContext==null) return;

                                    // 显示列表选择界面
                                    UpgradeLocalFragment fragment_Download = UpgradeLocalFragment.newInstance("Server", msgInte, jsArray);
                                    fragment_Download.show(getFragmentManager(), "DownLoad");

                                } catch (JSONException e) {
                                    L.e("Get list error: " + e.getMessage());
                                    showLoadingIcon(false);

                                    //如果界面已经关闭则直接返回
                                    if(mContext==null) return;

                                    String mainText = getResources().getString(R.string.Text_view_error_code_title);
                                    String subText = getResources().getString(R.string.Toast_view_invalid_project);
                                    Utils.ToastShow(mContext , Toast.LENGTH_SHORT, Gravity.CENTER_HORIZONTAL, mainText, subText);
                                }
                            }
                        });
                        break;

                    case SSL_SENDMSG_DOWNLOAD_FILE:

                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }
    });

    /**发送消息*/
    private void sendHttpsMessage(int msgType, String message)
    {
        Message VendorMessage = new Message();
        //消息类型
        VendorMessage.what = msgType;
        //消息内容
        Bundle bundle = new Bundle();
        bundle.putString("text1",message);  //往Bundle中存放数据
        //bundle.putString("text2"," - by client");  //后面可增加参数
        VendorMessage.setData(bundle);  //mes利用Bundle传递数据
        //发送消息
        mHttpsHandler.sendMessage(VendorMessage);
    }
}
