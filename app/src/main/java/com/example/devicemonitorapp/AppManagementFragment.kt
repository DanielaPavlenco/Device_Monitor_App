package com.example.devicemonitorapp

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable
import java.util.*

class AppManagementFragment : Fragment() {
//Declaratii pentru elementele de UI și de listele de aplicatii
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private lateinit var filterGroup: RadioGroup
    private var appsList: MutableList<App> = mutableListOf()
    private var filteredAppsList: MutableList<App> = mutableListOf()
    private lateinit var appsAdapter: AppsAdapter
    private var currentFilter: String = "all"
    private var isSortAscending: Boolean = true

    //Metoda onCreateView este apelată pentru a crea și configura UI-ul fragmentului
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_management, container, false)

        //Inițializarea e3lementelor de UI
        recyclerView = view.findViewById(R.id.appsManagerRecyclerView)
        swipeRefreshLayout = view.findViewById(R.id.appsManagerSwipeLayout)
        searchView = view.findViewById(R.id.search_view)
        filterGroup = view.findViewById(R.id.filter_group)
        val sortButton: ImageButton = view.findViewById(R.id.sort_button)
        sortButton.setOnClickListener {
            isSortAscending = !isSortAscending
            sortAppsBySize(isSortAscending)
        }

        // Verificarea modului dark si setarea imaginii de fundal corespunzatoare
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        val root = view.findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.appsManagerCl)

        if (isNightMode) {
            root.setBackgroundResource(R.drawable.night2) // Inlocuieste cu drawable-ul tau pentru modul dark
        } else {
            root.setBackgroundResource(R.drawable.bg2) // Inlocuieste cu drawable-ul tau pentru modul zi
        }

        //Configurarea RecycleView, SwipeRefreshLayout, SearchView șo RadioGroup
        setUpRecyclerView()
        setUpSwipeRefreshLayout()
        setUpSearchView()
        setUpFilterGroup()

        //Încarcarea aplicațiilor instalate
        loadInstalledApps()

        return view
    }

    //Configurarea RecycleView
    private fun setUpRecyclerView() {
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, RecyclerView.VERTICAL))
        appsAdapter = AppsAdapter(requireContext(), filteredAppsList)
        recyclerView.adapter = appsAdapter
    }

    //Configurarea SwipeRefreshLayout
    private fun setUpSwipeRefreshLayout() {
        val typedValueAccent = TypedValue()
        requireContext().theme.resolveAttribute(R.attr.colorAccent, typedValueAccent, true)
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), typedValueAccent.resourceId))
        swipeRefreshLayout.setOnRefreshListener { refreshList(currentFilter) }
    }

    //Configurarea SearchView
    private fun setUpSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterApps(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText ?: "")
                return true
            }
        })
    }

    //Configurarea RadioGroup pentru filtrare
    private fun setUpFilterGroup() {
        filterGroup.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.filter_third_party -> "third_party"
                R.id.filter_system -> "system"
                R.id.filter_disabled -> "disabled"
                else -> "all"
            }
            refreshList(currentFilter)
        }
    }

    //Sortarea aplicațiilor dupa dimensiune
    private fun sortAppsBySize(ascending: Boolean) {
        if (ascending) {
            filteredAppsList.sortBy { it.size }
        } else {
            filteredAppsList.sortByDescending { it.size }
        }
        appsAdapter.notifyDataSetChanged()
    }

    //Incarcarea aplicațiilor instalate pe dispozitiv
    private fun loadInstalledApps() {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            val pm: PackageManager = requireContext().packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            appsList.clear()
            appsList.addAll(packages.map { packageInfo ->
                App(
                    label = packageInfo.loadLabel(pm).toString(),
                    packageName = packageInfo.packageName,
                    isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isEnabled = packageInfo.enabled,
                    icon = packageInfo.loadIcon(pm)!!,
                    size = getAppSize(packageInfo.packageName)
                )
            }.sortedBy { it.label.toLowerCase(Locale.getDefault()) })

            launch(Dispatchers.Main) {
                filterApps(searchView.query.toString())
                sortAppsBySize(isSortAscending)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    //Reimprospatarea listei de aplicatii in functie de filtru
    private fun refreshList(filterMode: String) {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            val pm: PackageManager = requireContext().packageManager
            val packages = when (filterMode) {
                "third_party" -> pm.getInstalledApplications(PackageManager.GET_META_DATA).filterNot {
                    (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                }
                "disabled" -> pm.getInstalledApplications(PackageManager.GET_META_DATA).filterNot {
                    it.enabled
                }
                else -> pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }
            appsList.clear()
            appsList.addAll(packages.map { packageInfo ->
                App(
                    label = packageInfo.loadLabel(pm).toString(),
                    packageName = packageInfo.packageName,
                    isSystemApp = (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isEnabled = packageInfo.enabled,
                    icon = packageInfo.loadIcon(pm)!!,
                    size = getAppSize(packageInfo.packageName)
                )
            }.sortedBy { it.label.toLowerCase(Locale.getDefault()) })

            launch(Dispatchers.Main) {
                filterApps(searchView.query.toString())
                sortAppsBySize(isSortAscending)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }


    //Obtinerea dimensiunii aplicatiei
    private fun getAppSize(packageName: String): Long {
        val pm = requireContext().packageManager
        return try {
            val packageInfo = pm.getPackageInfo(packageName, 0)
            val appFile = packageInfo.applicationInfo.sourceDir
            val appSize = File(appFile).length()
            appSize
        } catch (e: PackageManager.NameNotFoundException) {
            0L
        }
    }

    //Filtrarea aplicatiilor in functie de quer-ul de cautare
    private fun filterApps(query: String) {
        filteredAppsList.clear()
        filteredAppsList.addAll(appsList.filter {
            it.label.contains(query, ignoreCase = true)
        })
        appsAdapter.notifyDataSetChanged()
    }

    //Dezinstalarea unei aplicatii
    private fun uninstallApp(app: App) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.warning))
            .setMessage(getString(R.string.confirmation_message))
            .setNegativeButton(R.string.cancelar) { _, _ -> }
            .setPositiveButton(R.string.uninstall) { _, _ ->
                val packageURI = Uri.parse("package:${app.packageName}")
                val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI)
                startActivity(uninstallIntent)
            }
            .show()
    }

    //Definirea clasei de date pentru aplicatii
    private data class App(
        val label: String,
        val packageName: String,
        val isSystemApp: Boolean,
        val isEnabled: Boolean,
        val icon: Drawable,
        val size: Long
    ) : Serializable

    //Adaptor pentru RecycleView
    private inner class AppsAdapter(
        private val context: Context,
        private val dataSet: MutableList<App>
    ) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val appName: TextView = view.findViewById(R.id.app_name)
            val appPackageName: TextView = view.findViewById(R.id.app_package_name)
            val appStatus: TextView = view.findViewById(R.id.app_status)
            val appType: TextView = view.findViewById(R.id.app_type)
            val appIcon: ImageView = view.findViewById(R.id.app_icon)
            val appSize: TextView = view.findViewById(R.id.app_size) // Add this line
            val itemLayout: RelativeLayout = view.findViewById(R.id.app_root_layout)
        }

        //Crearea ViewHolder-ului pentru fiecare element din lista
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.list_item_apps_manager, parent, false)
            return ViewHolder(view)
        }

        //Legarea datelor de elementele din ViewHolder
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = dataSet[position]

            holder.appName.text = app.label
            holder.appPackageName.text = app.packageName
            holder.appStatus.text = if (app.isEnabled) "Enabled" else "Disabled"
            holder.appStatus.setTextColor(if (app.isEnabled) ContextCompat.getColor(context, android.R.color.holo_green_dark) else ContextCompat.getColor(context, android.R.color.holo_red_dark))
            holder.appType.text = if (app.isSystemApp) "System" else "User"
            holder.appIcon.setImageDrawable(app.icon)
            holder.appSize.text = formatFileSize(app.size) // Add this line

            holder.itemLayout.setOnClickListener {
                showPopupMenu(it, app)
            }
        }

        //Afisarea unui meniu popup pentru acțiuni pe aplicatie
        private fun showPopupMenu(view: View, app: App) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.app_management_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_uninstall -> {
                        uninstallApp(app)
                        true
                    }
                    R.id.action_disable -> {
                        if (app.isEnabled) {
                            disablePackage(app.packageName)
                        } else {
                            enablePackage(app.packageName)
                        }
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        //Obtinerea numarului de elemente din lista
        override fun getItemCount(): Int = dataSet.size

        private fun formatFileSize(size: Long): String {
            val kb = size / 1024
            val mb = kb / 1024
            return if (mb > 0) {
                "$mb MB"
            } else {
                "$kb KB"
            }
        }
    }

    //Activarea unui pachet (aplicatie)
    private fun enablePackage(packageName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("su -c pm enable $packageName")
                val output = process.inputStream.bufferedReader().readText()
                val errorOutput = process.errorStream.bufferedReader().readText()
                process.waitFor()
                launch(Dispatchers.Main) {
                    if (process.exitValue() == 0) {
                        Snackbar.make(requireView(), "Package enabled", Snackbar.LENGTH_LONG).show()
                    } else {
                        Log.e("AppManagementFragment", "Failed to enable package: $errorOutput")
                        Toast.makeText(requireContext(), "Failed to enable package", Toast.LENGTH_LONG).show()
                    }
                    refreshList(currentFilter)
                }
            } catch (e: Exception) {
                Log.e("AppManagementFragment", "Error enabling package: $packageName", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error enabling package: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //Dezactivarea pachetului (aplicatie)
    private fun disablePackage(packageName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.warning))
            .setMessage(getString(R.string.confirmation_message))
            .setNegativeButton(R.string.cancelar) { _, _ -> }
            .setPositiveButton(R.string.disable) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val process = Runtime.getRuntime().exec("su -c pm disable-user --user 0 $packageName")
                        val output = process.inputStream.bufferedReader().readText()
                        val errorOutput = process.errorStream.bufferedReader().readText()
                        process.waitFor()
                        launch(Dispatchers.Main) {
                            if (process.exitValue() == 0) {
                                Snackbar.make(requireView(), "Package disabled", Snackbar.LENGTH_LONG).show()
                            } else {
                                Log.e("AppManagementFragment", "Failed to disable package: $errorOutput")
                                Toast.makeText(requireContext(), "Failed to disable package", Toast.LENGTH_LONG).show()
                            }
                            refreshList(currentFilter)
                        }
                    } catch (e: Exception) {
                        Log.e("AppManagementFragment", "Error disabling package: $packageName", e)
                        launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error disabling package: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
    }


}
