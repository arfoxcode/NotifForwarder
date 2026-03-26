package com.notifforwarder.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppFilterActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnSave: Button
    private lateinit var switchFilterMode: Switch
    private lateinit var tvFilterMode: TextView

    private val allApps = mutableListOf<AppItem>()
    private val filteredApps = mutableListOf<AppItem>()
    private val selectedPackages = mutableSetOf<String>()
    private lateinit var adapter: AppAdapter

    data class AppItem(
        val name: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_filter)

        supportActionBar?.title = "Filter Aplikasi"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        etSearch       = findViewById(R.id.etSearch)
        recyclerView   = findViewById(R.id.recyclerView)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        btnSave        = findViewById(R.id.btnSaveFilter)
        switchFilterMode = findViewById(R.id.switchFilterMode)
        tvFilterMode   = findViewById(R.id.tvFilterMode)

        // Load saved state
        val prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE)
        val saved = prefs.getStringSet("allowed_packages", emptySet()) ?: emptySet()
        val filterEnabled = prefs.getBoolean("filter_enabled", false)
        selectedPackages.addAll(saved)
        switchFilterMode.isChecked = filterEnabled

        updateFilterModeLabel()
        switchFilterMode.setOnCheckedChangeListener { _, _ -> updateFilterModeLabel() }

        // Setup RecyclerView
        adapter = AppAdapter(filteredApps, selectedPackages) { updateSelectedCount() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Load installed apps di background
        loadInstalledApps()

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterList(s.toString())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSave.setOnClickListener {
            prefs.edit()
                .putStringSet("allowed_packages", selectedPackages)
                .putBoolean("filter_enabled", switchFilterMode.isChecked)
                .apply()
            Toast.makeText(this, "✅ Filter disimpan!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadInstalledApps() {
        Thread {
            val pm = packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val apps = packages
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // hanya app user
                .map { info ->
                    AppItem(
                        name = pm.getApplicationLabel(info).toString(),
                        packageName = info.packageName,
                        icon = try { pm.getApplicationIcon(info.packageName) } catch (e: Exception) { null }
                    )
                }
                .sortedBy { it.name.lowercase() }

            runOnUiThread {
                allApps.clear()
                allApps.addAll(apps)
                filteredApps.clear()
                filteredApps.addAll(apps)
                adapter.notifyDataSetChanged()
                updateSelectedCount()
            }
        }.start()
    }

    private fun filterList(query: String) {
        filteredApps.clear()
        if (query.isEmpty()) {
            filteredApps.addAll(allApps)
        } else {
            filteredApps.addAll(allApps.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectedCount() {
        tvSelectedCount.text = "${selectedPackages.size} app dipilih"
    }

    private fun updateFilterModeLabel() {
        if (switchFilterMode.isChecked) {
            tvFilterMode.text = "Mode: Hanya app yang dipilih ✅"
            tvFilterMode.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvFilterMode.text = "Mode: Semua app (filter nonaktif)"
            tvFilterMode.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

// ── RecyclerView Adapter ─────────────────────────────────────────────────────

class AppAdapter(
    private val apps: List<AppFilterActivity.AppItem>,
    private val selected: MutableSet<String>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView   = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView    = view.findViewById(R.id.tvAppName)
        val tvPkg: TextView     = view.findViewById(R.id.tvPackageName)
        val checkbox: CheckBox  = view.findViewById(R.id.checkboxApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.tvName.text = app.name
        holder.tvPkg.text = app.packageName
        holder.ivIcon.setImageDrawable(app.icon)
        holder.checkbox.isChecked = app.packageName in selected

        val toggle = {
            if (app.packageName in selected) {
                selected.remove(app.packageName)
                holder.checkbox.isChecked = false
            } else {
                selected.add(app.packageName)
                holder.checkbox.isChecked = true
            }
            onChanged()
        }

        holder.checkbox.setOnClickListener { toggle() }
        holder.itemView.setOnClickListener { toggle() }
    }

    override fun getItemCount() = apps.size
}
