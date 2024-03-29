package com.example.sustainableapp.views;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.sustainableapp.R;
import com.example.sustainableapp.controllers.EnergyActionController;
import com.example.sustainableapp.controllers.SustainableActionController;
import com.example.sustainableapp.controllers.UserController;
import com.example.sustainableapp.models.BooVariable;
import com.example.sustainableapp.models.EnergyAction;
import com.example.sustainableapp.models.IntVariable;
import com.example.sustainableapp.models.SustainableAction;
import com.example.sustainableapp.models.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class EnergyActionFragment extends Fragment {
    TextView min_tv, s_tv, shower_points_tv;
    EditText min_etn, s_etn, devices_etn;
    CheckBox shower_cb, bath_cb, noWater_cb;
    Button save_energy_action_b;
    String userID;
    private static IntVariable foundEA;
    static ArrayList<EnergyAction> EAData;
    private static BooVariable actionEdited;
    private static BooVariable badge3Edited;
    private static BooVariable badge0Edited;
    ArrayList<String> errors = new ArrayList<>();
    static ArrayList<SustainableAction> saList = new ArrayList<>();
    private static BooVariable saListReturned;
    SustainableAction sa = new SustainableAction();
    static ArrayList<User> profileData;
    public EnergyActionFragment() {
    }
    public static EnergyActionFragment newInstance() {
        EnergyActionFragment fragment = new EnergyActionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            userID = bundle.getString("userID", "0");
        }
    }
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_energy_action, container, false);
        getUsersSustainableActions();
        UserController uc = new UserController();
        uc.getProfile(userID, "EAFragment");

        actionEdited = new BooVariable();
        actionEdited.setListener(() -> {
            Toast.makeText(view.getContext(),"Sėkmingai išsaugoti pakeitimai",Toast. LENGTH_SHORT).show();
            Objects.requireNonNull(getActivity()).findViewById(R.id.ic_tasks).performClick();
        });
        badge3Edited = new BooVariable();
        badge3Edited.setListener(() -> UserActivity.sendNotification(view.getContext(), "3", "Ženklelis", "Valio! Surinkote maksimalų taškų skaičių būsto srityje pirmą kartą, todėl gaunate ženklelį!", false));

        badge0Edited = new BooVariable();
        badge0Edited.setListener(() -> UserActivity.sendNotification(view.getContext(), "3", "Ženklelis", "Valio! Išsaugojote savo pirmąją užduotį, todėl gaunate ženklelį!", false));

        shower_cb = view.findViewById(R.id.shower_cb);
        shower_cb.setOnClickListener(v -> {
            if (shower_cb.isChecked()) {
                setVisibleShowerFields();
            } else {
                setInvisibleShowerFields();
            }
        });
        noWater_cb = view.findViewById(R.id.noWater_cb);
        noWater_cb.setOnClickListener(v -> {
            if (noWater_cb.isChecked()) {
                setInvisibleShowerAndBathFields();
            } else {
                setVisibleShowerAndBathFields();
            }
        });
        save_energy_action_b = view.findViewById(R.id.save_energy_action_b);
        save_energy_action_b.setOnClickListener(v -> {
            if (sa != null) {
                String showerTime = "0:0";
                int devices = 0;
                boolean shower = shower_cb.isChecked();
                boolean bath = bath_cb.isChecked();
                if (noWater_cb.isChecked()) {
                    bath = false;
                    shower = false;
                }
                else {
                    if (shower_cb.isChecked()) {
                        if (!min_etn.getText().toString().equals("")) {
                            showerTime = min_etn.getText().toString();
                        } else {
                            showerTime = "0";
                        }
                        showerTime = showerTime + ":";
                        if (!s_etn.getText().toString().equals("")) {
                            showerTime = showerTime + s_etn.getText().toString();
                        } else {
                            showerTime = showerTime + "0";
                        }
                    }
                }
                if (devices_etn.getText().toString().length() != 0) {
                    devices = Integer.parseInt(devices_etn.getText().toString());
                }
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");
                Date date = new Date(System.currentTimeMillis());
                String dateStr = formatter.format(date);
                EnergyAction ea = new EnergyAction(sa.getId(), sa.getCategory(), sa.getUserID(), sa.getDateBegin(), sa.getDateEnd(), dateStr, noWater_cb.isChecked(), shower, showerTime, bath, devices);
                EnergyActionController eac = new EnergyActionController();
                errors = (ArrayList<String>) eac.validateEA(ea);
                int howManyErr = 0;
                for (int i = 0; i < errors.size(); i++) {
                    if (!errors.get(i).equals("")) {
                        howManyErr = howManyErr + 1;
                    }
                }
                showErrors();
                if (howManyErr == 0) {
                    eac.updateEnergyActionInDB(ea);
                    eac.checkForBadge3(ea, profileData.get(0));
                }
            }});
        saListReturned = new BooVariable();
        saListReturned.setListener(() -> {
            if (saListReturned.isBoo()) {
                String beginDate = saList.get((saList.size()-1)).getDateBegin();
                String endDate = saList.get((saList.size()-1)).getDateEnd();
                SustainableActionController sac = new SustainableActionController();
                if (sac.isTodayInDates(beginDate, endDate)) {
                    sa = saList.get((saList.size()-1));
                }
            }
        });
        EnergyActionController eac = new EnergyActionController();
        eac.getEAForEAFragment(userID, "EnergyActionFragment");
        foundEA = new IntVariable();
        foundEA.setListener(() -> {
            if (EAData != null) {
                String[] sArr = EAData.get(0).getShowerTime().split(":", 5);
                min_etn.setText(sArr[0]);
                s_etn.setText(sArr[1]);
                if (EAData.get(0).isNoWater()) {
                    noWater_cb.setChecked(true);
                }
                if (noWater_cb.isChecked()) {
                    setInvisibleShowerAndBathFields();
                }
                if (EAData.get(0).isShower()) {
                    shower_cb.setChecked(true);
                    setVisibleShowerFields();
                }
                else {
                    shower_cb.setChecked(false);
                    setInvisibleShowerFields();
                }
                bath_cb.setChecked(EAData.get(0).isBath());
                devices_etn.setText(Integer.toString(EAData.get(0).getDevicesOff()));
            }
        });
        return view;
    }
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        min_etn = Objects.requireNonNull(getView()).findViewById(R.id.min_etn);
        s_etn = getView().findViewById(R.id.s_etn);
        min_tv = getView().findViewById(R.id.min_tv);
        s_tv = getView().findViewById(R.id.s_tv);
        shower_points_tv = getView().findViewById(R.id.shower_points_tv);
        devices_etn = getView().findViewById(R.id.devices_etn);
        save_energy_action_b = getView().findViewById(R.id.save_energy_action_b);
        noWater_cb = getView().findViewById(R.id.noWater_cb);
        bath_cb = getView().findViewById(R.id.bath_cb);
        setInvisibleShowerFields();
    }
    public void setInvisibleShowerAndBathFields() {
        setInvisibleShowerFields();
        shower_cb.setVisibility(View.GONE);
        bath_cb.setVisibility(View.GONE);
    }
    public void setVisibleShowerAndBathFields() {
        setVisibleShowerFields();
        shower_cb.setVisibility(View.VISIBLE);
        bath_cb.setVisibility(View.VISIBLE);
    }
    public void setVisibleShowerFields() {
        if (shower_cb.isChecked()) {
            min_etn.setVisibility(View.VISIBLE);
            s_etn.setVisibility(View.VISIBLE);
            min_tv.setVisibility(View.VISIBLE);
            s_tv.setVisibility(View.VISIBLE);
            shower_points_tv.setVisibility(View.VISIBLE);
        }
        else {
            setInvisibleShowerFields();
        }

    }
    public void setInvisibleShowerFields() {
        min_etn.setVisibility(View.GONE);
        s_etn.setVisibility(View.GONE);
        min_tv.setVisibility(View.GONE);
        s_tv.setVisibility(View.GONE);
        shower_points_tv.setVisibility(View.GONE);
    }
    public void getUsersSustainableActions() {
        SustainableActionController sac = new SustainableActionController();
        String purpose = "EnergyActionFragment";
        sac.getUsersSustainableActions(userID, purpose);
    }
    public static void checkUsersSAFound(ArrayList<SustainableAction> sa) {
        saList = sa;
        saListReturned.setBoo(true);
        saListReturned.getListener().onChange();
    }
    public static void checkUsersSANotFound(ArrayList<SustainableAction> sa) {
        saList = sa;
        saListReturned.setBoo(false);
        saListReturned.getListener().onChange();
    }
    public void showErrors() {
        if (!errors.get(0).equals("")) {
            min_etn.setError(errors.get(0));
        }
        if (!errors.get(1).equals("")) {
            s_etn.setError(errors.get(1));
        }
        if (!errors.get(2).equals("")) {
            devices_etn.setError(errors.get(2));
        }
    }
    public static void checkEAEdited() {
        actionEdited.setBoo(true);
        if (actionEdited.getListener() != null) {
            actionEdited.getListener().onChange();
        }
    }
    public static void checkBadge3Edited() {
        badge3Edited.setBoo(true);
        if (badge3Edited.getListener() != null) {
            badge3Edited.getListener().onChange();
        }
    }
    public static void checkBadge0Edited() {
        badge0Edited.setBoo(true);
        if (badge0Edited.getListener() != null) {
            badge0Edited.getListener().onChange();
        }
    }
    public static void checkEAFound(List<EnergyAction> list) {
        EAData = (ArrayList<EnergyAction>) list;
        if (!list.isEmpty()) {
            foundEA.setID(list.get(0).getId());
            if (foundEA.getListener() != null) {
                foundEA.getListener().onChange();
            }
        }
    }
    public static void checkUserFound(List<User> list) {
        profileData = (ArrayList<User>) list;
    }
}