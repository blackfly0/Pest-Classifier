package com.example.pestclassifier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.R.layout.simple_spinner_dropdown_item;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ImageView imageview;
    private ImageView placeHolderIV;
    private Button selectImageButton;
    private Button predictButton,cancelButton;
    private Spinner selectModelSpinner;
    private ProgressBar loadingProgressBar;
    private TextView loadingTextView;

    private int GALLERY = 1, CAMERA = 2;
    private boolean imageLoaded = false;
    boolean isFromGallery, isModelSelected = false;
    private String currentPhotoName,currentPhotoPath;
    StringRequest stringRequest;
    RequestQueue requestQueue;

    // Create a storage reference from our app
    FirebaseStorage storage = FirebaseStorage.getInstance("gs://ip102-models");
    StorageReference storageRef = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestMultiplePermissions();

        setupView();
        setupSpinner();
        setupButtonListeners();

        requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideLoadingView();
        showMainView();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            imageLoaded = true;
            placeHolderIV.setImageBitmap(null);
            if (data != null) {
                isFromGallery = true;
                Uri contentURI = data.getData();
                currentPhotoPath = Constants.getRealPathFromURI(this, contentURI);
                currentPhotoName = Constants.getFileName(contentURI, this);
                setPic();
            }

        } else if (requestCode == CAMERA && resultCode == RESULT_OK) {
            isFromGallery = false;
            placeHolderIV.setImageBitmap(null);
            imageLoaded = true;
            setPic();
        }
    }

    public void  requestMultiplePermissions(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            //Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            //openSettingsDialog();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    private void setupView(){
        predictButton = findViewById(R.id.predictButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        imageview = findViewById(R.id.imageView);
        placeHolderIV = findViewById(R.id.ivPlaceholder);
        selectModelSpinner = findViewById(R.id.selectModelSpinner);
        cancelButton = findViewById(R.id.cancelButton);
        loadingTextView = findViewById(R.id.waitTextView);
        loadingProgressBar = findViewById(R.id.progressBar);

        hideLoadingView();
        showMainView();
    }
    private void setupSpinner(){
        // Spinner Drop down elements
        List<String> modelNames = new ArrayList<String>();
        modelNames.add("Rice");
        modelNames.add("Corn");
        modelNames.add("Wheat");
        modelNames.add("Mango");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, modelNames);
        dataAdapter.setDropDownViewResource(simple_spinner_dropdown_item);

        selectModelSpinner.setAdapter(dataAdapter);
        selectModelSpinner.setAdapter(
                new NothingSelectedSpinnerAdapter(
                        dataAdapter,
                        R.layout.contact_spinner_row_nothing_selected,
                        // R.layout.contact_spinner_nothing_selected_dropdown, // Optional
                        this));

        selectModelSpinner.setPrompt("Select Pretrained Model");
        selectModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0){
                    String item = parent.getItemAtPosition(position).toString();
                    isModelSelected = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });
    }

    private void setupButtonListeners(){
        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPictureDialog();
            }
        });

        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                predictButtonTapped();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestQueue != null) {
                    requestQueue.cancelAll(TAG);
                }
                showMainView();
                hideLoadingView();
            }
        });
    }

    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select photo from gallery",
                "Capture photo from camera" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallary();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }
    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }
    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Log.d(TAG, ex.toString());
        }
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.pestclassifier.fileprovider",
                    photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            intent.putExtra("return-data", true);
            startActivityForResult(intent, CAMERA);
        }
    }

    private void predictButtonTapped(){

        if(!imageLoaded){
            Toast.makeText(this, "Please upload an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isModelSelected) {
            Toast.makeText(this, "Please select a model first!", Toast.LENGTH_SHORT).show();
            return;
        }

//        Location location = getLocationWithCheckNetworkAndGPS(getApplicationContext());
//        if(location == null){
//            Toast.makeText(this, "Could not get location!", Toast.LENGTH_SHORT).show();
//        }
        // Upload the image

        hideMainView();
        showLoadingView();
        uploadImage();
    }

    private void hideMainView(){
        selectImageButton.setVisibility(View.INVISIBLE);
        predictButton.setVisibility(View.INVISIBLE);
        selectModelSpinner.setVisibility(View.INVISIBLE);
    }
    private void showMainView(){
        selectImageButton.setVisibility(View.VISIBLE);
        predictButton.setVisibility(View.VISIBLE);
        selectModelSpinner.setVisibility(View.VISIBLE);
    }

    private void hideLoadingView(){
        cancelButton.setVisibility(View.INVISIBLE);
        loadingProgressBar.setVisibility(View.INVISIBLE);
        loadingTextView.setVisibility(View.INVISIBLE);
    }
    private void showLoadingView(){
        cancelButton.setVisibility(View.VISIBLE);
        loadingProgressBar.setVisibility(View.VISIBLE);
        loadingTextView.setVisibility(View.VISIBLE);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        currentPhotoName = imageFileName;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void uploadImage(){

        final StorageReference ImagesRef = storageRef.child("Test Images/" + currentPhotoName);
        byte[] data = new byte[0];

        Bitmap bitmap;
        Uri photoURI;
        if(isFromGallery){
            photoURI = Uri.fromFile(new File(currentPhotoPath));
        }else {
            File f = new File(currentPhotoPath);
            photoURI = FileProvider.getUriForFile(this,
                    "com.example.plantsimagecollection.fileprovider",
                    f);
        }

        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
            bitmap = Constants.correctOrientationOfBitmap(bitmap, currentPhotoPath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            data = baos.toByteArray();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        String downloadURL = "";
        UploadTask uploadTask = ImagesRef.putBytes(data);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return ImagesRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    //final String downloadURLString = downloadUri.toString();
                    predictImage();
                    Log.d(TAG, "********* download url:" + downloadUri.toString());
                } else {
                    // Handle failures
                    // ...
                }
            }
        });

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

//                imageUploadProgress = (int)progress;
//                uploadProgressTextView.setText("Uploading " +Integer.toString(imageUploadProgress) + "%");
//                if (imageUploadProgress >= 100) {
//                    Toast.makeText(MainActivity.this, "Image Upload Successful!", Toast.LENGTH_SHORT).show();
//                }
                if ((int)progress >= 100) {
                    Toast.makeText(MainActivity.this, "Image Upload Successful!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bitmap = null;
    }

    private void predictImage(){

        String url = "https://us-central1-fyp-sheez.cloudfunctions.net/predict?imageName=" + currentPhotoName;

        // Request a string response from the provided URL.
        stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject obj = new JSONObject(response);
                            Log.d("Response", obj.toString());

                            Intent newIntent = new Intent(getApplicationContext(), ResultsActivity.class);
                            newIntent.putExtra("RESULTS", response);
                            startActivity(newIntent);

                        } catch (Throwable t) {
                            Log.e("My App", "Could not parse malformed JSON");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "********* Error: "+ error.toString());
            }
        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stringRequest.setTag(TAG);
        requestQueue.add(stringRequest);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageview.getWidth();
        int targetH = imageview.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        bitmap = Constants.correctOrientationOfBitmap(bitmap, currentPhotoPath);
        imageview.setImageBitmap(bitmap);
    }

}