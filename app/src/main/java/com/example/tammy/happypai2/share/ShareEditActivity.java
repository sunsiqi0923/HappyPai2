package com.example.tammy.happypai2.share;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tammy.happypai2.AskPositionActivity;
import com.example.tammy.happypai2.R;
import com.example.tammy.happypai2.bean.RegisterBean;
import com.example.tammy.happypai2.bean.ShareBean;
import com.example.tammy.happypai2.util.Constants;
import com.example.tammy.happypai2.util.FetchAddressIntentService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.hdl.myhttputils.MyHttpUtils;
import com.hdl.myhttputils.bean.CommCallback;
import com.hdl.myhttputils.bean.StringCallBack;
import com.hdl.myhttputils.utils.FailedMsgUtils;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import java.util.HashMap;
import java.util.Map;

public class ShareEditActivity extends AppCompatActivity implements View.OnClickListener{

    private String path;
    private Bitmap bm;

    private ImageView iv_share_pic;
    private EditText et_content;
    private LinearLayout ll_location, ll_structure, ll_privacy, ll_tag;
    private TextView tv_location,tv_structure,tv_tag,tv_privacy;

    private FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;
    private AddressResultReceiver mResultReceiver;

    String user_id;

    private ProgressDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_edit);

        Intent intent=getIntent();
        path=intent.getStringExtra("path");
        bm = BitmapFactory.decodeFile(path);

        Log.v("path",path);


        Toolbar myToolbar = (Toolbar) findViewById(R.id.share_edit_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        init();

//        File cacheDir = StorageUtils.getOwnCacheDirectory(this, "imageloader/Cache");
        /**
         * 加载图片
         */
        int width = iv_share_pic.getWidth();
        int height = iv_share_pic.getHeight();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .memoryCacheExtraOptions(width, height) // max width, max height，即保存的每个缓存文件的最大长宽
                .threadPoolSize(3) //线程池内加载的数量
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // You can pass your own memory cache implementation/你可以通过自己的内存缓存实现
                .memoryCacheSize(2 * 1024 * 1024) // 内存缓存的最大值
                .diskCacheSize(50 * 1024 * 1024)  // 50 Mb sd卡(本地)缓存的最大值
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                // 由原先的discCache -> diskCache
                .imageDownloader(new BaseImageDownloader(this, 5 * 1000, 30 * 1000)) // connectTimeout (5 s), readTimeout (30 s)超时时间
                .writeDebugLogs() // Remove for release app
                .build();
        //全局初始化此配置
        ImageLoader.getInstance().init(config);
        ImageLoader.getInstance().displayImage("file://" + path, iv_share_pic);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.v("Location", "" + location.getLatitude() + location.getLongitude());

                            mLastLocation = location;
                            startIntentService();
                        }

                    }
                });


    }

    private void init(){
        iv_share_pic = (ImageView)findViewById(R.id.iv_share_pic);
        et_content = (EditText)findViewById(R.id.et_content);
        ll_location = (LinearLayout)findViewById(R.id.ll_location);
        ll_privacy = (LinearLayout)findViewById(R.id.ll_privacy);
        ll_structure = (LinearLayout)findViewById(R.id.ll_structure);
        ll_tag = (LinearLayout)findViewById(R.id.ll_tag);

        tv_location = (TextView)findViewById(R.id.tv_location);
        tv_structure = (TextView)findViewById(R.id.tv_structure);
        tv_privacy = (TextView)findViewById(R.id.tv_privacy);
        tv_tag = (TextView)findViewById(R.id.tv_tag);

        ll_location.setOnClickListener(this);
        ll_privacy.setOnClickListener(this);
        ll_structure.setOnClickListener(this);
        ll_tag.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_share_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                // User chose the "Settings" item, show the app settings UI...
                share();
                return true;

//            case R.id.action_favorite:
//                // User chose the "Favorite" action, mark the current item
//                // as a favorite...
//                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_location:
                Log.v("button_test","ll_location");
                break;
            case R.id.ll_privacy:
                break;
            case R.id.ll_structure:
                break;
            case R.id.ll_tag:
                break;
            default:
                break;
        }
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            Log.v("res", mAddressOutput);

            tv_location.setText(mAddressOutput);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                Log.v("res", "Success");
            }

        }
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        mResultReceiver = new AddressResultReceiver(new Handler());
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    private void share(){
        SharedPreferences sharedPreferences2 =
        getApplicationContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        user_id = sharedPreferences2.getString("user_id", "null");
        Toast.makeText(this,user_id,Toast.LENGTH_SHORT).show();

        String content = et_content.getText().toString();
        String location = tv_location.getText().toString();

        Map<String, Object> params = new HashMap<>();//构造请求的参数
        params.put("action", "share");
        params.put("user_id",user_id);
        params.put("state_text",content);
        params.put("location",location);
        params.put("image",1);

        MyHttpUtils.build()//构建myhttputils
                .url("http://52.41.31.68/api")//请求的url
                .addParams(params)
                .setJavaBean(ShareBean.class)
                .onExecuteByPost(new CommCallback<ShareBean>(){//开始执行，并有一个回调（异步的哦---->直接可以更新ui）
                    @Override
                    public void onSucceed(ShareBean shareBean) {//请求成功之后会调用这个方法----显示结果
                        //Toast.makeText(RegisterActivity.this,registerBean.getUser_id()+"",Toast.LENGTH_SHORT).show();

                        if(shareBean.getState()==0){
                            Toast.makeText(ShareEditActivity.this,shareBean.getImage(),Toast.LENGTH_SHORT).show();
                            upload(shareBean.getImage());

                        }else{
                            Toast.makeText(ShareEditActivity.this,shareBean.getMsg(),Toast.LENGTH_SHORT).show();
                        }

                    }
                    @Override
                    public void onFailed(Throwable throwable) {//请求失败的时候会调用这个方法
                        Toast.makeText(ShareEditActivity.this, FailedMsgUtils.getErrMsgStr(throwable),Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void upload(String key){
        mProgressDialog.show(ShareEditActivity.this, "上传", "上传中...");
        Map<String, Object> params = new HashMap<>();//构造请求的参数
        params.put("action", "upload");
        params.put("user_id",user_id);
        params.put("image",key);
        MyHttpUtils.build()//构建myhttputils
                .url("http://52.41.31.68/api")//请求的url
                .uploadUrl("http://52.41.31.68/api")
                .addParams(params)
                .addFile(path)
                .onExecuteByPost(new StringCallBack() {//开始执行，并有一个回调（异步的哦---->直接可以更新ui）
                    @Override
                    public void onSucceed(String result) {//请求成功之后会调用这个方法
                        Toast.makeText(ShareEditActivity.this,result,Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailed(Throwable throwable) {//请求失败的时候会调用这个方法
                        Toast.makeText(ShareEditActivity.this, FailedMsgUtils.getErrMsgStr(throwable),Toast.LENGTH_SHORT);
                    }
                })
                .onExecuteUpLoad(new CommCallback() {
                    @Override
                    public void onComplete() {
                        mProgressDialog.dismiss();
                        Toast.makeText(ShareEditActivity.this,"上传完成",Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onSucceed(Object o) {
                    }
                    @Override
                    public void onFailed(Throwable throwable) {
                        Toast.makeText(ShareEditActivity.this, FailedMsgUtils.getErrMsgStr(throwable),Toast.LENGTH_SHORT);
                    }
                });
    }

}
