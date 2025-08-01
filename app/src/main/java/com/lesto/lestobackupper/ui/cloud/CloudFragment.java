package com.lesto.lestobackupper.ui.cloud;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.protobuf.InvalidProtocolBufferException;
import com.lesto.lestobackupper.R;
import com.lesto.lestobackupper.databinding.FragmentCloudBinding;
import com.lesto.lestobackupper.proto.ServerDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CloudFragment extends Fragment {
    private FragmentCloudBinding binding;
    private final List<ServerDescription.ServerInfo> serversList = new ArrayList<>();
    private int defaultColor = Color.BLACK;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCloudBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Spinner spinner = root.findViewById(R.id.cloudSelector);

        EditText editTextName = root.findViewById(R.id.txtName);
        EditText editTextServer = root.findViewById(R.id.txtServer);
        EditText editTextUsername = root.findViewById(R.id.txtUsername);
        EditText editTextPassword = root.findViewById(R.id.txtPassword);

        ColorStateList textColors = editTextName.getTextColors();
        defaultColor = textColors.getDefaultColor();

        trackChanges(editTextName, "");
        trackChanges(editTextServer, "");
        trackChanges(editTextUsername, "");
        trackChanges(editTextPassword, "");

        Context c = getContext();
        if (c != null) {

            loadServerList(c, spinner);

            Button btnDel = root.findViewById(R.id.btnDelete);
            btnDel.setOnClickListener(view -> {
                int position = spinner.getSelectedItemPosition();
                if (position < serversList.size()) {
                    new AlertDialog.Builder(c)
                        .setTitle("Confirm")
                        .setMessage("Delete the setup for '"+serversList.get(position).getName()+"'?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            serversList.remove(position);
                            saveServerList(c);
                            loadServerList(c, spinner);
                            spinner.setSelection(position);
                        })
                        .setNegativeButton("No", null)
                        .show();
                }
            });

            Button btn = root.findViewById(R.id.button);
            btn.setOnClickListener(view -> {
                int position = spinner.getSelectedItemPosition();

                ServerDescription.ServerInfo info = ServerDescription.ServerInfo.newBuilder()
                    .setName(editTextName.getText().toString())
                    .setServer(editTextServer.getText().toString())
                    .setUsername(editTextUsername.getText().toString())
                    .setPassword(editTextPassword.getText().toString())
                    .build();

                if (position >= 0 && position < serversList.size()) {
                    serversList.set(position, info); // same name, replace
                }else{
                    serversList.add(info);
                }
                saveServerList(c);
                loadServerList(c, spinner);

                spinner.setSelection(position);
            });

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    if (position < serversList.size()) {
                        ServerDescription.ServerInfo selected = serversList.get(position);

                        trackChanges(editTextName, selected.getName());
                        trackChanges(editTextServer, selected.getServer());
                        trackChanges(editTextUsername, selected.getUsername());
                        trackChanges(editTextPassword, selected.getPassword());
                    }else {
                        trackChanges(editTextName, "");
                        trackChanges(editTextServer, "");
                        trackChanges(editTextUsername, "");
                        trackChanges(editTextPassword, "");
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        return root;
    }
    private final Map<EditText, TextWatcher> changeWatchers = new HashMap<>();

    private void trackChanges(EditText editText, String originalValue) {
        TextWatcher oldWatcher = changeWatchers.get(editText);
        if (oldWatcher != null) {
            editText.removeTextChangedListener(oldWatcher);
        }

        editText.setText(originalValue);
        editText.setTextColor(defaultColor);

        TextWatcher newWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean changed = !s.toString().equals(originalValue);
                editText.setTextColor(changed ? Color.RED : defaultColor);}
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        };

        changeWatchers.put(editText, newWatcher);
        editText.addTextChangedListener(newWatcher);
    }

    private void saveServerList(Context c) {

        ServerDescription.ServerInfoList serverList = ServerDescription.ServerInfoList
                .newBuilder()
                .addAllServers(serversList)
                .build();

        byte[] bytes = serverList.toByteArray();
        String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);

        SharedPreferences prefs = c.getSharedPreferences("Servers", Context.MODE_PRIVATE);
        prefs.edit().putString("servers", base64).apply();
    }

    private void loadServerList(Context c, Spinner spinner) {
        SharedPreferences prefs = c.getSharedPreferences("Servers", Context.MODE_PRIVATE);
        String base64 = prefs.getString("servers", null);
        Log.d("loadServerList", "Loading servers");
        if (base64 != null) {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            try {
                ServerDescription.ServerInfoList serverList = ServerDescription.ServerInfoList.parseFrom(bytes);
                serversList.clear();
                serversList.addAll(serverList.getServersList());
                List<String> serverNames = new ArrayList<>();
                for (ServerDescription.ServerInfo i : serversList){
                    serverNames.add(i.getName());
                    Log.d("loadServerList", "Loaded server conf " + i.getName());
                }
                serverNames.add("NEW");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, serverNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}