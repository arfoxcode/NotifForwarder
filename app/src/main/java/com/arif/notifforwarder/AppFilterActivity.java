package com.arif.notifforwarder;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppFilterActivity extends AppCompatActivity {

    static class AppItem {
        String   name;
        String   packageName;
        Drawable icon;
    }

    private EditText  etSearch;
    private ListView  listView;
    private TextView  tvSelectedCount, tvFilterMode;
    private Button    btnSave;
    private Switch    switchFilterMode;

    private final List<AppItem> allApps  = new ArrayList<>();
    private final List<AppItem> filtered = new ArrayList<>();
    private final Set<String>   selected = new HashSet<>();
    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_filter);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Filter Aplikasi");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etSearch        = findViewById(R.id.etSearch);
        listView        = findViewById(R.id.listView);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvFilterMode    = findViewById(R.id.tvFilterMode);
        btnSave         = findViewById(R.id.btnSaveFilter);
        switchFilterMode = findViewById(R.id.switchFilterMode);

        SharedPreferences prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet("allowed_packages", new HashSet<>());
        if (saved != null) selected.addAll(saved);
        switchFilterMode.setChecked(prefs.getBoolean("filter_enabled", false));

        updateModeLabel();
        switchFilterMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                updateModeLabel();
            }
        });

        adapter = new AppListAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                doToggle(pos, view);
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) { doFilter(s.toString()); }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { doSave(); }
        });

        loadApps();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadApps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PackageManager pm = getPackageManager();
                List<ApplicationInfo> pkgs = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                final List<AppItem> list = new ArrayList<>();
                for (ApplicationInfo info : pkgs) {
                    if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    AppItem item = new AppItem();
                    item.name        = pm.getApplicationLabel(info).toString();
                    item.packageName = info.packageName;
                    try { item.icon = pm.getApplicationIcon(info.packageName); }
                    catch (Exception ignored) {}
                    list.add(item);
                }
                Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        allApps.clear(); allApps.addAll(list);
                        filtered.clear(); filtered.addAll(list);
                        adapter.notifyDataSetChanged();
                        updateCount();
                    }
                });
            }
        }).start();
    }

    private void doFilter(String q) {
        filtered.clear();
        String lower = q.toLowerCase();
        for (AppItem a : allApps) {
            if (q.isEmpty() || a.name.toLowerCase().contains(lower)
                    || a.packageName.toLowerCase().contains(lower)) {
                filtered.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void doToggle(int pos, View row) {
        AppItem app = filtered.get(pos);
        CheckBox cb = row.findViewById(R.id.checkboxApp);
        if (selected.contains(app.packageName)) {
            selected.remove(app.packageName);
            if (cb != null) cb.setChecked(false);
        } else {
            selected.add(app.packageName);
            if (cb != null) cb.setChecked(true);
        }
        updateCount();
    }

    private void doSave() {
        getSharedPreferences("notif_prefs", MODE_PRIVATE).edit()
                .putStringSet("allowed_packages", selected)
                .putBoolean("filter_enabled", switchFilterMode.isChecked())
                .apply();
        Toast.makeText(this, "Filter disimpan!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateCount() {
        tvSelectedCount.setText(selected.size() + " app dipilih");
    }

    private void updateModeLabel() {
        if (switchFilterMode.isChecked()) {
            tvFilterMode.setText("Mode: Hanya app yang dipilih");
            tvFilterMode.setTextColor(0xFF2E7D32);
        } else {
            tvFilterMode.setText("Mode: Semua app (filter nonaktif)");
            tvFilterMode.setTextColor(0xFF888888);
        }
    }

    class AppListAdapter extends BaseAdapter {
        @Override public int    getCount()              { return filtered.size(); }
        @Override public Object getItem(int pos)        { return filtered.get(pos); }
        @Override public long   getItemId(int pos)      { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AppFilterActivity.this)
                        .inflate(R.layout.item_app, parent, false);
            }
            AppItem app  = filtered.get(pos);
            ((ImageView) convertView.findViewById(R.id.ivAppIcon)).setImageDrawable(app.icon);
            ((TextView)  convertView.findViewById(R.id.tvAppName)).setText(app.name);
            ((TextView)  convertView.findViewById(R.id.tvPackageName)).setText(app.packageName);
            ((CheckBox)  convertView.findViewById(R.id.checkboxApp))
                    .setChecked(selected.contains(app.packageName));
            return convertView;
        }
    }
}
