package com.jose.app.barcodereader.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.jose.app.barcodereader.adapter.ImageAdapter;
import com.jose.app.barcodereader.asynctask.LongRunningTask;
import com.jose.app.barcodereader.MainActivity;
import com.jose.app.barcodereader.util.ProgressHelper;
import com.jose.app.barcodereader.R;
import com.jose.app.barcodereader.asynctask.UploadAsyncTask;
import com.jose.app.barcodereader.databinding.FragmentUploadBinding;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UploadFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {
    private FragmentUploadBinding binding;
    private ImageAdapter mAdapter;
    private List<Bitmap> mPicList = new ArrayList<>();
    private Bitmap mBillImg = null;
    private String mCurImgPath = null;
    private int machineId = -1;
    private boolean isFromBill = false;
    private int mType = 1;
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), new ActivityResultCallback<ScanIntentResult>() {
        @Override
        public void onActivityResult(ScanIntentResult o) {
            if (o.getContents() == null) {
                Toast.makeText(getActivity(), R.string.msg_scan_cancel, Toast.LENGTH_LONG).show();
            } else {
                machineId = Integer.parseInt(o.getContents().trim());
                binding.etBarcodeNum.setText(String.valueOf(machineId));
                Toast.makeText(getActivity(), R.string.msg_scan_success, Toast.LENGTH_LONG).show();
            }
        }
    });

    private final ActivityResultLauncher<Intent> captureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (binding.ivInvoice.getVisibility() == View.GONE) {
                binding.ivInvoice.setVisibility(View.VISIBLE);
            }
            galleryAddPic();
            setPic();
        }
    });

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurImgPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    private void setPic() {

        // Get the dimensions of the bitmap
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(mCurImgPath, bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap bm = BitmapFactory.decodeFile(mCurImgPath, opts);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(mCurImgPath);
            String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            int orientation = orientString != null ? Integer.parseInt(orientString) :  ExifInterface.ORIENTATION_NORMAL;

            int rotationAngle = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

            Matrix matrix = new Matrix();
            matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
            Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);

            if (isFromBill) {
                mBillImg = bitmap;
                binding.ivInvoice.setImageBitmap(bitmap);
            } else {
                mPicList.add(bitmap);
                mAdapter.notifyDataSetChanged();
                if (binding.emptyView.getVisibility() == View.VISIBLE) {
                    binding.emptyView.setVisibility(View.GONE);
                    binding.rlPics.setVisibility(View.VISIBLE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentUploadBinding.inflate(inflater, container, false);
        Bundle bundle = getArguments();
        mType = bundle.getInt("from");
        if (mType == 1) {
            ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.upload_fragment_label_1);
        } else {
            ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.upload_fragment_label_2);
        }
        return binding.getRoot();

    }

    @SuppressLint("NotifyDataSetChanged")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.ivInvoice.setOnClickListener(this);
        binding.btnScan.setOnClickListener(this);
        binding.btnInvImg.setOnClickListener(this);
        binding.btnPicImg.setOnClickListener(this);
        binding.btnUpload.setOnClickListener(this);

        mPicList = new ArrayList<>();
        mAdapter = new ImageAdapter(mPicList, new ImageAdapter.ItemClickListener() {
            @Override
            public void onTouch(int position, Bitmap curImg, ImageView thumbView) {
                showFullImageDialog(curImg);
            }

            @Override
            public void onDelete(boolean isEmptyList) {
                if (isEmptyList) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.rlPics.setVisibility(View.GONE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                    binding.rlPics.setVisibility(View.VISIBLE);
                }
            }
        });
        binding.rlPics.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_invoice) {
            if (mBillImg != null)
                showFullImageDialog(mBillImg);
        } else if (v.getId() == R.id.btn_scan) {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
            options.setPrompt(getString(R.string.label_scan_prompt));
            options.setCameraId(0);  // Use a specific camera of the device
            options.setBeepEnabled(false);
            options.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(options);
        } else if (v.getId() == R.id.btn_inv_img) {
            isFromBill = true;
            dispatchTakePictureIntent();
        } else if (v.getId() == R.id.btn_pic_img) {
            isFromBill = false;
            dispatchTakePictureIntent();
        } else {
            String machine_num_str = binding.etBarcodeNum.getText().toString();
            if (machine_num_str.trim().isEmpty()) {
                Toast.makeText(getActivity(), R.string.msg_err_scan, Toast.LENGTH_LONG).show();
                return;
            }
            machineId = Integer.parseInt(machine_num_str);
            if (mBillImg == null) {
                Toast.makeText(getActivity(), R.string.msg_err_cap_bill, Toast.LENGTH_LONG).show();
                return;
            }
            if (mPicList.size() == 0) {
                Toast.makeText(getActivity(), R.string.msg_err_ref_pic, Toast.LENGTH_LONG).show();
                return;
            }
            if (mPicList.size() < 4) {
                Toast.makeText(getActivity(), R.string.msg_err_limit_photo_team, Toast.LENGTH_LONG).show();
                return;
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("machineId", machineId);
                jsonObject.put("type", mType);
                jsonObject.put("billImgData", getStringImage(mBillImg));
                JSONArray jsonArray = new JSONArray();
                for (Bitmap pic : mPicList) {
                    jsonArray.put(getStringImage(pic));
                }
                jsonObject.put("refImgData", jsonArray);
                ProgressHelper.showDialog(getActivity(), getActivity().getString(R.string.msg_progressbar));
                new UploadAsyncTask().executeAsync(new LongRunningTask(jsonObject.toString()), new UploadAsyncTask.Callback<String>() {
                    @Override
                    public void onComplete(String result) {
                        if (ProgressHelper.isDialogVisible()) {
                            ProgressHelper.dismissDialog();
                        }
                        Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
                        NavHostFragment.findNavController(UploadFragment.this).popBackStack();
                    }

                    @Override
                    public void onFailure() {
                        if (ProgressHelper.isDialogVisible()) {
                            ProgressHelper.dismissDialog();
                        }
                        Toast.makeText(getActivity(), R.string.msg_err_upload, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (JSONException e) {
                if (ProgressHelper.isDialogVisible()) {
                    ProgressHelper.dismissDialog();
                }
                Toast.makeText(getActivity(), R.string.msg_err_request, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView imgView = (ImageView) v;
        float scale;

        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:   // first finger down only
                matrix.set(imgView.getImageMatrix());
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: // first finger lifted

            case MotionEvent.ACTION_POINTER_UP: // second finger lifted

                mode = NONE;
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

                oldDist = spacing(event);
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (mode == DRAG)
                {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                }
                else if (mode == ZOOM)
                {
                    // pinch zooming
                    float newDist = spacing(event);
                    if (newDist > 5f)
                    {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist; // setting the scaling of the
                        // matrix...if scale > 1 means
                        // zoom in...if scale < 1 means
                        // zoom out
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        imgView.setImageMatrix(matrix); // display the transformation on screen

        return true; // indicate event was handled
    }

    private float spacing(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event)
    {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            photoFile = null;
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(getActivity(),
                    getActivity().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            captureLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getActivity(), R.string.msg_err_capture, Toast.LENGTH_LONG).show();
        }
    }

    private File createAppDirectoryInDownloads() {
        File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDirectory = new File(downloadDirectory, "BarcodeApp");
        if (!appDirectory.exists()) {
            boolean directoryCreated = appDirectory.mkdir();
            if (!directoryCreated)
                return null;
        }

        return appDirectory;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File appDirectory = createAppDirectoryInDownloads();
        if (appDirectory != null) {
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    appDirectory      /* directory */
            );
            mCurImgPath = image.getAbsolutePath();
            return image;
        } else {
            return null;
        }
    }

    private String getStringImage(Bitmap bitmap) {
        Bitmap scaledBitmap = getResizedBitmap(bitmap);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imgBytes = baos.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    private Bitmap getResizedBitmap(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        return Bitmap.createScaledBitmap(image, width / 4, height / 4, true);
    }

    private void showFullImageDialog(Bitmap curImg) {
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.setContentView(R.layout.dialog_zoom_image);
        dialog.setCanceledOnTouchOutside(true);

        ImageView thumbView = dialog.findViewById(R.id.expanded_image);
        thumbView.setImageBitmap(curImg);
        thumbView.setScaleType(ImageView.ScaleType.MATRIX);
        final Display display = getActivity().getWindowManager().getDefaultDisplay();
        final float screenWidth = display.getWidth();
        final float screenHeight = display.getHeight();
        RectF drawableRect = new RectF(0, 0, curImg.getWidth(), curImg.getHeight());
        RectF viewRect = new RectF(0, 0, screenWidth, screenHeight);
        final Matrix splashMatrix = new Matrix();
        splashMatrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
        thumbView.setImageMatrix(splashMatrix);
        thumbView.setOnTouchListener(this);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show();
    }

}