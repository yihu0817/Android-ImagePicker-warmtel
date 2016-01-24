package com.warmtel.imagepicker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pizidea.imagepicker.AndroidImagePicker;
import com.pizidea.imagepicker.GlideImagePresenter;
import com.pizidea.imagepicker.ImagePresenter;
import com.pizidea.imagepicker.Util;
import com.pizidea.imagepicker.bean.ImageItem;
import com.warmtel.imagepicker.network.UploadUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 说明：主要用于选择文件和上传文件操作
 */
public class UpLoadFileActivity extends Activity implements OnClickListener,
        UploadUtil.OnUploadProcessListener, AndroidImagePicker.OnImagePickCompleteListener {
    private static final String TAG = "uploadImage";
    private final int REQ_IMAGE = 1433;
    private ImagePresenter presenter = new GlideImagePresenter();
    private int screenWidth;
    /***
     * 使用照相机拍照获取图片
     */
    public static final int SELECT_PIC_BY_TACK_PHOTO = 1;
    /***
     * 使用相册中的图片
     */
    public static final int SELECT_PIC_BY_PICK_PHOTO = 2;
    /**
     * 去上传文件
     */
    protected static final int TO_UPLOAD_FILE = 1;
    /**
     * 上传文件响应
     */
    protected static final int UPLOAD_FILE_DONE = 2;  //
    /**
     * 选择文件
     */
    public static final int TO_SELECT_PHOTO = 3;
    /**
     * 上传初始化
     */
    private static final int UPLOAD_INIT_PROCESS = 4;
    /**
     * 上传中
     */
    private static final int UPLOAD_IN_PROCESS = 5;

    private Uri photoUri;

    /***
     * 这里的这个URL是我服务器的javaEE环境URL
     */
    private static String requestURL = "http://192.168.1.102/fileup/p/file!upload";
    //	private static String requestURL = "http://img.epalmpay.cn/order_userpic.php";
    private Button selectButton, uploadButton, camerButton;
    private TextView uploadImageResult;
    private ProgressBar progressBar;

    private ProgressDialog progressDialog;
    private GridView mGridView;
    private SelectAdapter mSelectAdatper;
    private List<ImageItem> mImageList = new ArrayList<>();

    private MyHandler handler = new MyHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_main_layout);
        initView();
        screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        mSelectAdatper = new SelectAdapter(this);
        mGridView.setAdapter(mSelectAdatper);
    }

    /**
     * 初始化数据
     */
    private void initView() {
        selectButton = (Button) this.findViewById(R.id.selectImage);
        uploadButton = (Button) this.findViewById(R.id.uploadImage);
        camerButton = (Button) this.findViewById(R.id.select_camer_Image);
        selectButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        camerButton.setOnClickListener(this);
        uploadImageResult = (TextView) findViewById(R.id.uploadImageResult);
        progressDialog = new ProgressDialog(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        mGridView = (GridView) findViewById(R.id.gridview);
    }

    @Override
    protected void onResume() {
        AndroidImagePicker.getInstance().setOnImagePickCompleteListener(this);
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_camer_Image:
                takePhoto();
                break;
            case R.id.selectImage:
                pickPhoto();
                break;
            case R.id.uploadImage:
                if (mImageList.size() > 0) {
                    handler.sendEmptyMessage(TO_UPLOAD_FILE);
                } else {
                    Toast.makeText(this, "上传的文件路径出错", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 拍照获取图片
     */
    private void takePhoto() {
        //执行拍照前，应该先判断SD卡是否存在
        String SDState = Environment.getExternalStorageState();
        if (SDState.equals(Environment.MEDIA_MOUNTED)) {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//"android.media.action.IMAGE_CAPTURE"
            /***
             * 需要说明一下，以下操作使用照相机拍照，拍照后的图片会存放在相册中的
             * 这里使用的这种方式有一个好处就是获取的图片是拍照后的原图
             * 如果不实用ContentValues存放照片路径的话，拍照后获取的图片为缩略图不清晰
             */
            ContentValues values = new ContentValues();
            photoUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            /**-----------------*/
            startActivityForResult(intent, SELECT_PIC_BY_TACK_PHOTO);
        } else {
            Toast.makeText(this, "内存卡不存在", Toast.LENGTH_LONG).show();
        }
    }

    /***
     * 从相册中取图片
     */
    private void pickPhoto() {
        AndroidImagePicker.getInstance().setSelectMode(AndroidImagePicker.Select_Mode.MODE_MULTI);
//        AndroidImagePicker.getInstance().setShouldShowCamera(true);

        Intent intent = new Intent();
        int requestCode = REQ_IMAGE;
        intent.setClass(this, ImagesGridActivity.class);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onImagePickComplete(List<ImageItem> items) {
        List<ImageItem> imageList = AndroidImagePicker.getInstance().getSelectedImages();
        for (ImageItem item : imageList) {
            Log.e(TAG, "name :" + item.name + ", path :" + item.path);
        }
        mImageList = imageList;
        mSelectAdatper.clear();
        mSelectAdatper.addAll(items);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PIC_BY_TACK_PHOTO) {
            String[] pojo = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.TITLE, MediaStore.Images.Media.SIZE};
            Cursor cursor = getContentResolver().query(photoUri, pojo, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                String picpath = cursor.getString(cursor.getColumnIndexOrThrow(pojo[0]));
                ImageItem item = new ImageItem(
                        picpath
                        , cursor.getString(cursor.getColumnIndexOrThrow(pojo[1]))
                        , cursor.getInt(cursor.getColumnIndexOrThrow(pojo[2])));
                if (picpath != null &&
                        (picpath.endsWith(".png") || picpath.endsWith(".PNG") || picpath.endsWith(".jpg"))) {
                    mImageList.add(item);
                    mSelectAdatper.clear();
                    mSelectAdatper.addAll(mImageList);
                } else {
                    Toast.makeText(this, "选择图片文件不正确", Toast.LENGTH_LONG).show();
                }
                cursor.close();
            }
        }
    }
    /**
     * 上传服务器响应回调
     */
    @Override
    public void onUploadDone(int responseCode, String message) {
        progressDialog.dismiss();
        Message msg = Message.obtain();
        msg.what = UPLOAD_FILE_DONE;
        msg.arg1 = responseCode;
        msg.obj = message;
        handler.sendMessage(msg);
    }

    private void toUploadFile() {
        uploadImageResult.setText("正在上传中...");
        progressDialog.setMessage("正在上传文件...");
        progressDialog.show();
        String fileKey = "img";
        UploadUtil uploadUtil = UploadUtil.getInstance();
        uploadUtil.setOnUploadProcessListener(this);  //设置监听器监听上传状态

        Map<String, String> params = new HashMap<String, String>();
        params.put("orderId", "11111");
        uploadUtil.uploadFile(mImageList, fileKey, requestURL, params);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<UpLoadFileActivity> mActivity;

        public MyHandler(UpLoadFileActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            UpLoadFileActivity upLoadFileActivity = mActivity.get();
            if (upLoadFileActivity != null) {
                switch (msg.what) {
                    case TO_UPLOAD_FILE:
                        upLoadFileActivity.toUploadFile();
                        break;
                    case UPLOAD_INIT_PROCESS:
                        upLoadFileActivity.progressBar.setMax(msg.arg1);
                        break;
                    case UPLOAD_IN_PROCESS:
                        upLoadFileActivity.progressBar.setProgress(msg.arg1);
                        break;
                    case UPLOAD_FILE_DONE:
                        String result = "响应码：" + msg.arg1 + "\n响应信息：" + msg.obj + "\n耗时：" + UploadUtil.getRequestTime() + "秒";
                        upLoadFileActivity.uploadImageResult.setText(result);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void onUploadProcess(int uploadSize) {
        Message msg = Message.obtain();
        msg.what = UPLOAD_IN_PROCESS;
        msg.arg1 = uploadSize;
        handler.sendMessage(msg);
    }

    @Override
    public void initUpload(int fileSize) {
        Message msg = Message.obtain();
        msg.what = UPLOAD_INIT_PROCESS;
        msg.arg1 = fileSize;
        handler.sendMessage(msg);
    }

    @Override
    protected void onDestroy() {
        AndroidImagePicker.getInstance().deleteOnImagePickCompleteListener(this);
        super.onDestroy();
    }

    class SelectAdapter extends ArrayAdapter<ImageItem> {
        public SelectAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageItem item = getItem(position);
            int width = (screenWidth - Util.dp2px(UpLoadFileActivity.this, 10 * 2)) / 3;
            ImageView imageView = new ImageView(UpLoadFileActivity.this);
            imageView.setBackgroundColor(Color.GRAY);
            GridView.LayoutParams params = new AbsListView.LayoutParams(width, width);
            imageView.setLayoutParams(params);
            presenter.onPresentImage(imageView, item.path, width);

            return imageView;
        }

    }
}