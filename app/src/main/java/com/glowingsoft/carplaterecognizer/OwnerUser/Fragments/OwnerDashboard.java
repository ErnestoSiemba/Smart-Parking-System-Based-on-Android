package com.glowingsoft.carplaterecognizer.OwnerUser.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.glowingsoft.carplaterecognizer.DriverUser.Activities.BookingDetails;
import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.HelperClasses.SlotNoInfo;
import com.glowingsoft.carplaterecognizer.OwnerUser.Activities.AddParkingPosition;
import com.glowingsoft.carplaterecognizer.OwnerUser.Activities.AreaHistory;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class OwnerDashboard extends Fragment {


    LinearLayout expandButtonLayout, slotsStatusLayout, historyButtonLayout, slot_individual_list_view;
    TextView availableText, bookedText, amount2Text, amount3Text, amount4Text, slotName, numberPlate;
    TextView messageText, parkingStatusText, slotsStatusText;
    ImageView toastIcon;
    PieChart platforms_chart;
    SlotNoInfo slotNoInfo;

    BasicUtils utils=new BasicUtils();

    boolean dataSet=false;

    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    Button addParkingButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_owner_dashboard, container, false);

        initComponents(root, inflater);
        attachListeners(inflater);

        if(!utils.isNetworkAvailable(getActivity().getApplication())){
            showToast("No network available. Turn on mobile data or WIFI");
        }

        return root;
    }

    private void initComponents(View root, LayoutInflater inflater) {
        addParkingButton = root.findViewById(R.id.add_my_parking_button);

        expandButtonLayout = root.findViewById(R.id.expandCard);
        availableText = expandButtonLayout.findViewById(R.id.availableText);
        bookedText = expandButtonLayout.findViewById(R.id.occupiedText);
        amount2Text = expandButtonLayout.findViewById(R.id.amount2Text);
        amount3Text = expandButtonLayout.findViewById(R.id.amount3Text);
        amount4Text = expandButtonLayout.findViewById(R.id.amount4Text);
        platforms_chart = expandButtonLayout.findViewById(R.id.platforms_chart);
        slotsStatusLayout = root.findViewById(R.id.slotStatus);
        historyButtonLayout = root.findViewById(R.id.historyBtn);
        parkingStatusText = root.findViewById(R.id.parking_status_text);
        slotsStatusText = root.findViewById(R.id.slot_status_text);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

    }
    private void showToast(String message) {
        // inflate the custom toast layout
        View toastView = getLayoutInflater().inflate(R.layout.custom_toast_layout, null);

        // set the message text
        messageText = toastView.findViewById(R.id.toast_text);
        messageText.setText(message);

        // set the app icon
        toastIcon = toastView.findViewById(R.id.toast_icon);
        toastIcon.setImageResource(R.mipmap.ic_launcher);

        // create the toast and set its custom layout
        Toast toast = new Toast(getActivity());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }


    private void attachListeners(final LayoutInflater inflater) {
        addParkingButton.setOnClickListener(view-> {
            startActivity(new Intent(getActivity(), AddParkingPosition.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        historyButtonLayout.setOnClickListener(view-> {
            startActivity(new Intent(getActivity(), AreaHistory.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        firebaseDatabase.getReference().child("ParkingAreas").orderByChild("userID").equalTo(mAuth.getCurrentUser().getUid())
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        ParkingArea parkingArea = snapshot.getValue(ParkingArea.class);
                        setDashboardValues(parkingArea, inflater);
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
                            setDashboardValues(parkingArea, inflater);
                            Log.e("DashboardOwnerFragment","Fetch parking area");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

    }

    private void setDashboardValues(ParkingArea parkingArea, LayoutInflater inflater) {
        parkingStatusText.setText("Status of " + parkingArea.name);
        slotsStatusText.setText("Slots Status of " + parkingArea.name);

        String prepend="Ugx";
        availableText.setText(String.valueOf(parkingArea.availableSlots));
        bookedText.setText(String.valueOf(parkingArea.occupiedSlots));
        amount2Text.setText(prepend.concat(String.valueOf(parkingArea.amount2).concat("/Hr")));
        amount3Text.setText(prepend.concat(String.valueOf(parkingArea.amount3).concat("/Hr")));
        amount4Text.setText(prepend.concat(String.valueOf(parkingArea.amount4).concat("/Hr")));
//        platforms_chart.setUsePercentValues(true);
        Description desc=new Description();
        desc.setText("Details");
        platforms_chart.setDescription(desc);
        List<PieEntry> value=new ArrayList<>();
        value.add(new PieEntry(parkingArea.availableSlots,"Available"));
        value.add(new PieEntry(parkingArea.occupiedSlots,"Occupied"));
        PieDataSet pieDataSet=new PieDataSet(value,"");
        PieData pieData=new PieData(pieDataSet);
        platforms_chart.setData(pieData);
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        if(!dataSet){
            platforms_chart.animateXY(1400,1400);
            dataSet=true;
        }
        platforms_chart.notifyDataSetChanged();
        platforms_chart.invalidate();
        if(slotsStatusLayout.getChildCount() > 0)
            slotsStatusLayout.removeAllViews();
        for(int i=0;i<parkingArea.slotNos.size();i++){
            slot_individual_list_view = (LinearLayout)inflater.inflate(R.layout.include_slot_individual_list_view, null);
            slotName = slot_individual_list_view.findViewById(R.id.slotName);
            numberPlate = slot_individual_list_view.findViewById(R.id.numberPlate);
            slotNoInfo=parkingArea.slotNos.get(i);
            slotName.setText(slotNoInfo.name);
            numberPlate.setText(slotNoInfo.numberPlate);
            slotsStatusLayout.addView(slot_individual_list_view);
            addParkingButton.setVisibility(View.GONE);
        }
    }
}