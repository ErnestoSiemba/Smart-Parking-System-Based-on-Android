package com.glowingsoft.carplaterecognizer.OwnerUser.Fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.BookedSlots;
import com.glowingsoft.carplaterecognizer.HelperClasses.NumberPlate;
import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.HelperClasses.User;
import com.glowingsoft.carplaterecognizer.OwnerUser.Activities.AreaHistory;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.glowingsoft.carplaterecognizer.utils.pdfs.InvoiceGenerator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;


public class OwnerAdd extends Fragment {

    BasicUtils utils = new BasicUtils();
    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    Button submitButton;
    Spinner numberPlateSpinner;
    TextView slotNoText,amountText,wheelerText,endDateText,endTimeText,placeText;

    TextView messageText;
    ImageView toastIcon;
    EditText emailEditText;
    LinearLayout endDateLayout, endTimeLayout, scanButton;
    List<Integer> numberPlateWheeler = new ArrayList<Integer>();
    List<String> numberPlateNumber = new ArrayList<String>();
    User userObj;
    ParkingArea parkingArea;
    BookedSlots bookingSlot=new BookedSlots();
    Calendar calendar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_owner_add, container, false);

        initComponents(root);
        attachListeners();

        if (!utils.isNetworkAvailable(getActivity().getApplication())) {
            showToast("No network available. Turn on mobile data or WIFI");
        }

        defaultSpinnerItems();
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, numberPlateNumber);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numberPlateSpinner.setAdapter(dataAdapter);
        addItemsOnSpinner();
        addListenerOnSpinnerItemSelection();
        return root;
    }

    private void initComponents(View root) {
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        placeText = root.findViewById(R.id.placeText);
        emailEditText = root.findViewById(R.id.email_edit_text);
        numberPlateSpinner = root.findViewById(R.id.vehicleSelect);
        wheelerText = root.findViewById(R.id.wheelerText);
        endDateLayout = root.findViewById(R.id.endDate);
        endTimeLayout = root.findViewById(R.id.endTime);
        scanButton = root.findViewById(R.id.scanBtn);
        endDateText = root.findViewById(R.id.end_date_text);
        endTimeText = root.findViewById(R.id.end_time_text);
        amountText = root.findViewById(R.id.amount_text);
        submitButton = root.findViewById(R.id.submit_button);

        calendar=new GregorianCalendar();
        bookingSlot.startTime=bookingSlot.endTime=bookingSlot.checkoutTime=calendar.getTime();
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
        endTimeText.setText(simpleDateFormat.format(bookingSlot.startTime));
        simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
        endDateText.setText(simpleDateFormat.format(bookingSlot.endTime));
        bookingSlot.readNotification=0;
        bookingSlot.readBookedNotification=0;

    }

    private void attachListeners() {
        endDateLayout.setOnClickListener(view-> {
            showDatePicker(endDateText);
        });

        endTimeLayout.setOnClickListener(view-> {
            showTimePicker(endTimeText);
        });

        submitButton.setOnClickListener(view-> {
            String email = emailEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                showToast("Please fill in the driver's email");
                emailEditText.requestFocus();
            } else if (numberPlateSpinner.getSelectedItemPosition() == 0) {
                showToast("Please select a vehicle");
            } else if (bookingSlot.endTime.equals(bookingSlot.startTime)) {
                showToast("Please set the end time");
            } else if (!bookingSlot.timeDiffValid()) {
                showToast("Less time difference. Should be greater than 15 minutes");
            } else {
                saveData();
            }
        });

        firebaseDatabase.getReference().child("ParkingAreas").orderByChild("userID").equalTo(mAuth.getCurrentUser().getUid())
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        ParkingArea parkingArea = snapshot.getValue(ParkingArea.class);
                        setAddValues(parkingArea,snapshot.getKey());
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        firebaseDatabase.getReference().child("ParkingAreas").orderByChild("userID").equalTo(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            ParkingArea parkingArea = dataSnapshot.getValue(ParkingArea.class);
                            setAddValues(parkingArea,dataSnapshot.getKey());
                            Log.e(String.valueOf(getActivity().getClass()),"Fetch parking area");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

    }

    private void defaultSpinnerItems() {
        numberPlateWheeler.clear();
        numberPlateWheeler.add(0);
        numberPlateNumber.clear();
        numberPlateNumber.add("Select a vehicle");
    }

    private void addItemsOnSpinner() {
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
            @Override
            public void afterTextChanged(Editable et) {
                if(utils.isNetworkAvailable(getActivity().getApplication())){
                    String emailStr=et.toString();
                    firebaseDatabase.getReference().child("Users").orderByChild("email").equalTo(emailStr).limitToFirst(1)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.exists()){
                                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                            userObj=dataSnapshot.getValue(User.class);
                                            if(userObj.isVerified==1){
                                                bookingSlot.userID=dataSnapshot.getKey();
                                                Log.i(String.valueOf(getActivity().getClass()),"UserID: "+bookingSlot.userID);
                                                firebaseDatabase.getReference().child("NumberPlates").orderByChild("userID").equalTo(bookingSlot.userID)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                                                    Log.i(String.valueOf(getActivity().getClass()),dataSnapshot.getKey());
                                                                    NumberPlate numberPlate = dataSnapshot.getValue(NumberPlate.class);
                                                                    if(numberPlate.isDeleted==0){
                                                                        numberPlateWheeler.add(numberPlate.wheelerType);
                                                                        numberPlateNumber.add(numberPlate.numberPlate);
                                                                    }
                                                                }
                                                            }
                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {}
                                                        });
                                            }
                                        }
                                    }else{
                                        defaultSpinnerItems();
//                                    Toast.makeText(getActivity(),"User Doesn't exist",Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }else{
                    showToast("No Network Available");
                }
            }
        });
    }

    public void addListenerOnSpinnerItemSelection() {
        numberPlateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if(position!=0){
                    bookingSlot.numberPlate=numberPlateNumber.get(position);
                    bookingSlot.wheelerType=numberPlateWheeler.get(position);
                    calcRefreshAmount();
                    String wheelerTypeStr=String.valueOf(bookingSlot.wheelerType);
                    wheelerText.setText(wheelerTypeStr);
                    Toast.makeText(getActivity(), String.valueOf(numberPlateSpinner.getSelectedItem())+String.valueOf(position), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void calcRefreshAmount() {
        bookingSlot.calcAmount(parkingArea);
        String amountStr=String.valueOf(bookingSlot.amount);
        amountText.setText(amountStr);
    }

    private void setAddValues(ParkingArea parkingArea,String placeID) {
        this.bookingSlot.placeID=placeID;
        this.parkingArea=parkingArea;
        placeText.setText(parkingArea.name);
    }

    private void showDatePicker(final TextView button) {
        DatePickerDialog.OnDateSetListener dateSetListener=new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, final int date) {
                calendar.set(Calendar.YEAR,year);
                calendar.set(Calendar.MONTH,month);
                calendar.set(Calendar.DAY_OF_MONTH,date);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
                button.setText(simpleDateFormat.format(calendar.getTime()));
                bookingSlot.endTime = bookingSlot.checkoutTime = calendar.getTime();
                calcRefreshAmount();
            }
        };
        DatePickerDialog datePickerDialog=new DatePickerDialog(getActivity(),dateSetListener,calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(final TextView button) {
        TimePickerDialog.OnTimeSetListener timeSetListener= new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY,hour);
                calendar.set(Calendar.MINUTE,minute);
                calendar.set(Calendar.SECOND, 0);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
                bookingSlot.endTime = bookingSlot.checkoutTime = calendar.getTime();
                if(bookingSlot.endTime.after(bookingSlot.startTime)){
                    button.setText(simpleDateFormat.format(calendar.getTime()));
                    bookingSlot.endTime = bookingSlot.checkoutTime = calendar.getTime();
                    calcRefreshAmount();
                }else{
                    bookingSlot.endTime = bookingSlot.checkoutTime = bookingSlot.startTime;
                    showToast("Please select a time after present time");
                }
            }
        };
        TimePickerDialog timePickerDialog=new TimePickerDialog(getActivity(),timeSetListener,calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),false);
        timePickerDialog.show();
    }

    private void saveData() {
        if(utils.isNetworkAvailable(getActivity().getApplication())){
            bookingSlot.notificationID=Math.abs((int)Calendar.getInstance().getTimeInMillis());
            final String key=firebaseDatabase.getReference("BookedSlots").push().getKey();
//            bookingSlot.slotNo="None";
            if(parkingArea.availableSlots>0){
                parkingArea.allocateSpace();
                bookingSlot.slotNo=parkingArea.allocateSlot(bookingSlot.numberPlate);
                firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                firebaseDatabase.getReference("BookedSlots").child(key).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(getActivity(),"Success",Toast.LENGTH_SHORT).show();
                            File file = new File(getActivity().getExternalCacheDir(), File.separator + "invoice.pdf");
                            InvoiceGenerator invoiceGenerator=new InvoiceGenerator(bookingSlot,parkingArea,key,userObj,file);
                            invoiceGenerator.create();
                            invoiceGenerator.uploadFile(getActivity(),getActivity().getApplication());
                        }else{
                            parkingArea.deallocateSpace();
                            parkingArea.deallocateSlot(bookingSlot.slotNo);
                            firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                            showToast("Failed! Slots are full");
                        }
                    }
                });
            }else {
                showToast("Failed! Slots are full");
            }
        }else{
            showToast("No Network Available");
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
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 100);

        // show the toast
        toast.show();

    }

}