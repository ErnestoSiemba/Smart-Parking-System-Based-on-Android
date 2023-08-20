package com.glowingsoft.carplaterecognizer.DriverUser.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.NumberPlate;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.glowingsoft.carplaterecognizer.utils.adapters.NumberPlateAdapter;
import com.glowingsoft.carplaterecognizer.utils.animations.ViewAnimation;
import com.glowingsoft.carplaterecognizer.utils.dialog.NumberPlatePopUp;
import com.glowingsoft.carplaterecognizer.utils.network.ApiService;
import com.glowingsoft.carplaterecognizer.utils.swipe.SimpleToDeleteCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class DriverScanPlate extends Fragment implements NumberPlatePopUp.NumberPlatePopUpListener{


    FloatingActionButton fabCameraButton,fabTextEditButton,fabAddButton;
    TextView emptyTextView, messageText;
    ImageView toastIcon;
    View backDropView, cameraButtonLayout, textButtonLayout;

    boolean rotate = false;
    Bitmap upload;

    BasicUtils utils = new BasicUtils();
    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    public static final int SCAN_PERMISSION_ALL = 109;
    public static final int CAMERA_REQUEST_CODE = 103;
    public static final int NUMBER_PLATE_POPUP_REQUEST_CODE = 106;


    Map<String, NumberPlate> numberPlatesList = new HashMap<String, NumberPlate>();
    List<String> keys = new ArrayList<String>();
    Map<String, NumberPlate> treeMap;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_driver_scan_plate, container, false);

        initComponents(root);
        attachListeners(root);
        if(!utils.isNetworkAvailable(getActivity().getApplication())){
            showToast("No network available. Turn on mobile data or WIFI");
        }
        return root;
    }

    private void initComponents(View root) {
        fabCameraButton = root.findViewById(R.id.fab_camera_button);
        fabTextEditButton = root.findViewById(R.id.fab_text_edit_button);
        fabAddButton = root.findViewById(R.id.fab_add_button);
        emptyTextView = root.findViewById(R.id.empty_text_view);

        backDropView = root.findViewById(R.id.back_drop);
        cameraButtonLayout = root.findViewById(R.id.camera_button_layout);
        textButtonLayout = root.findViewById(R.id.text_button_layout);
        ViewAnimation.initShowOut(cameraButtonLayout);
        ViewAnimation.initShowOut(textButtonLayout);
        backDropView.setVisibility(View.GONE);


        recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        // setting the color of icons in the fab
        int color = ContextCompat.getColor(getContext(), R.color.white);
        ColorStateList colorStateList = ColorStateList.valueOf(color);
        // set the color state list as the tint for the FABs
        fabCameraButton.setImageTintList(colorStateList);
        fabTextEditButton.setImageTintList(colorStateList);
        fabAddButton.setImageTintList(colorStateList);



    }
    private void attachListeners(View root) {
        fabCameraButton.setOnClickListener(view-> {
            toggleFabMode(fabAddButton);
            askCameraPermission();
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        fabTextEditButton.setOnClickListener(view-> {
            toggleFabMode(fabAddButton);
            Bundle args = new Bundle();
            args.putString("numberPlate", "");
            NumberPlatePopUp numberPlateDialog = new NumberPlatePopUp();
            numberPlateDialog.setTargetFragment(DriverScanPlate.this, NUMBER_PLATE_POPUP_REQUEST_CODE);
            numberPlateDialog.setArguments(args);
            numberPlateDialog.show(getParentFragmentManager(), "exampledialog");

        });
        fabAddButton.setOnClickListener(view-> {
            toggleFabMode(view);
        });

        backDropView.setOnClickListener(view-> {
            toggleFabMode(fabAddButton);
        });


        firebaseDatabase.getReference().child("NumberPlates").orderByChild("userID_isDeletedQuery").equalTo(mAuth.getCurrentUser().getUid()+"_0")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        SimpleToDeleteCallback itemTouchHelperCallback=new SimpleToDeleteCallback(getActivity()) {
                            @Override
                            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                                return false;
                            }

                            @Override
                            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                                final int position=viewHolder.getAdapterPosition();
                                final String data = keys.get(position);
                                Snackbar snackbar = Snackbar
                                        .make(recyclerView, "Number Plate Removed", Snackbar.LENGTH_LONG)
                                        .setAction("UNDO", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                keys.add(position, data);
                                                mAdapter.notifyItemInserted(position);
                                                recyclerView.scrollToPosition(position);
                                                firebaseDatabase.getReference().child("NumberPlates").child(data).child("userID_isDeletedQuery")
                                                        .setValue(mAuth.getCurrentUser().getUid()+"_0");
                                                firebaseDatabase.getReference().child("NumberPlates").child(data).child("isDeleted")
                                                        .setValue(0);
                                            }
                                        });
                                snackbar.show();
                                keys.remove(position);
                                mAdapter.notifyItemRemoved(position);
                                firebaseDatabase.getReference().child("NumberPlates").child(data).child("userID_isDeletedQuery")
                                        .setValue(mAuth.getCurrentUser().getUid()+"_1");
                                firebaseDatabase.getReference().child("NumberPlates").child(data).child("isDeleted")
                                        .setValue(1);
                                checkRecyclerViewIsEmpty();
                            }
                        };
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            NumberPlate numberPlate = dataSnapshot.getValue(NumberPlate.class);
                            numberPlatesList.put(dataSnapshot.getKey(),numberPlate);
                            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
                            itemTouchHelper.attachToRecyclerView(recyclerView);
                        }
                        treeMap = new TreeMap<String, NumberPlate>(numberPlatesList);
                        keys.addAll(treeMap.keySet());
                        mAdapter = new NumberPlateAdapter(treeMap,keys);
                        recyclerView.setAdapter(mAdapter);
                        checkRecyclerViewIsEmpty();
                        Log.d(String.valueOf(getActivity().getClass()),"Scan"+String.valueOf(numberPlatesList));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

    }

    private void checkRecyclerViewIsEmpty() {
        if(keys.isEmpty()){
            recyclerView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
        }else{
            recyclerView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.GONE);
        }
    }
    private void toggleFabMode(View view) {
        rotate = ViewAnimation.rotateFab(view, !rotate);
        if (rotate) {
            ViewAnimation.showIn(cameraButtonLayout);
            ViewAnimation.showIn(textButtonLayout);
            backDropView.setVisibility(View.VISIBLE);
        } else {
            ViewAnimation.showOut(cameraButtonLayout);
            ViewAnimation.showOut(textButtonLayout);
            backDropView.setVisibility(View.GONE);
        }

    }

    private void showToast(String message) {
        // inflate the custom toast layout
        View toastView = getLayoutInflater().inflate(R.layout.custom_toast_layout, null);

        // set the message text
        messageText = toastView.findViewById(R.id.toast_text);
        messageText.setText(message);

        // set the app icon
        toastIcon = toastView.findViewById(R.id.toast_icon);
        toastIcon.setImageResource(R.drawable.app_logo);

        // create the toast and set its custom layout
        Toast toast = new Toast(getActivity());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void askCameraPermission() {
        if (!hasPermissions(getActivity(), PERMISSIONS)) {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, SCAN_PERMISSION_ALL);
        }else{
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (!hasPermissions(getActivity(), PERMISSIONS)) {
//            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, AppConstants.SCAN_PERMISSION_ALL);
//        }else{
//            openCamera();
//        }
    }

    private void openCamera() {
        Intent camera=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera, CAMERA_REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            try {
                upload = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                upload.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                String fileName="testimage.jpg";
                final File file = new File(Environment.getExternalStorageDirectory()
                        + File.separator + fileName);
                file.createNewFile();
                FileOutputStream fo = new FileOutputStream(file);
                fo.write(outputStream.toByteArray());
                fo.close();
//                Uri yourUri = Uri.fromFile(file);
                OkHttpClient client = new OkHttpClient.Builder().build();
                ApiService apiService = new Retrofit.Builder().baseUrl("https://api.platerecognizer.com").client(client).build().create(ApiService.class);
                RequestBody reqFile = RequestBody.create(okhttp3.MediaType.parse("image/*"), file);
                MultipartBody.Part body = MultipartBody.Part.createFormData("upload",
                        file.getName(), reqFile);
                RequestBody name = RequestBody.create(MediaType.parse("text/plain"), "upload");
                Call<ResponseBody> req = apiService.postImage(body, name,"Token ".concat(getActivity().getApplicationContext().getString(R.string.platerecognizer_api_key)));
                req.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                        Toast.makeText(getActivity(), response.code() + " ", Toast.LENGTH_SHORT).show();
                        try {                            String resp=response.body().string();
                            Log.i(String.valueOf(getActivity().getClass()),"Response: "+ resp);
                            JSONObject obj = new JSONObject(resp); //response.body().string() fetched only once
                            JSONArray geodata = obj.getJSONArray("results");
                            if(geodata.length() > 0 ){
                                Bundle args = new Bundle();
                                args.putString("numberPlate", geodata.getJSONObject(0).getString("plate"));
                                NumberPlatePopUp numberPlateDialog = new NumberPlatePopUp();
                                numberPlateDialog.setTargetFragment(DriverScanPlate.this, NUMBER_PLATE_POPUP_REQUEST_CODE);
                                numberPlateDialog.setArguments(args);
                                numberPlateDialog.show(getParentFragmentManager(), "exampledialog");
                            }else{
                                showToast("No Number Plate Found!");
                            }
                            Log.e(String.valueOf(getActivity().getClass()),"ImageUploader"+ geodata.getJSONObject(0).getString("plate"));
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                        file.delete();
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        showToast("Request Failed");
                        t.printStackTrace();
                    }
                });

            } catch(Exception e) {
                e.printStackTrace();
            }

            getActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }


        if(requestCode == NUMBER_PLATE_POPUP_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                saveData(data.getStringExtra("vehicleNumber"),data.getIntExtra("wheelerType",4));
//                Toast.makeText(getActivity(), data.getStringExtra("selection"), Toast.LENGTH_SHORT).show();
            }else if (resultCode == Activity.RESULT_CANCELED) {
                //Do Something in case not recieved the data
            }
        }
    }


    private void saveData(String vehicleNumber,int wheelerType) {
        final NumberPlate numberPlate = new NumberPlate(vehicleNumber,wheelerType,0,mAuth.getCurrentUser().getUid(),mAuth.getCurrentUser().getUid()+"_0");
        final String key = firebaseDatabase.getReference("NumberPlates").push().getKey();
        firebaseDatabase.getReference("NumberPlates")
                .child(key)
                .setValue(numberPlate).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            showToast("Number Plate Saved Successfully");
                            numberPlatesList.put(key,numberPlate);
                            treeMap = new TreeMap<String, NumberPlate>(numberPlatesList);
                            keys.add(key);
                            mAdapter = new NumberPlateAdapter(treeMap,keys);
                            recyclerView.setAdapter(mAdapter);
                            checkRecyclerViewIsEmpty();
                        } else {
                            showToast("Failed to add extra details");
                        }
                    }
                });
    }



}