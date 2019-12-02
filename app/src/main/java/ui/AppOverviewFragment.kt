package de.binarynoise.appdate

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.binarynoise.appdate.AppOverviewFragmentDirections.Companion.toAddApp
import de.binarynoise.appdate.AppOverviewFragmentDirections.Companion.toAppDetail
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.lang.ref.WeakReference
import kotlinx.android.synthetic.main.app_overview_fragment.appOverview_list as list
import kotlinx.android.synthetic.main.app_overview_fragment.appOverview_refreshLayout as refreshLayout
import kotlinx.android.synthetic.main.app_overview_fragment.floatingActionButton as fab

class AppOverviewFragment : Fragment() {
	init {
		refreshCallback = { b ->
			refreshing = b
			GlobalScope.launch(Main) {
				refreshLayout?.isRefreshing = b
			}
		}
		GlobalScope.launch {
			AppList.load()
			launch { Notifier.initNotificationGroups() }
			launch { AppList.checkForUpdates() }
			launch { Init.checkPermissions() }
			launch { Init.scheduleBackgroundUpdate() }
		}
	}
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return inflater.inflate(R.layout.app_overview_fragment, container, false)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		refreshLayout.setOnRefreshListener { GlobalScope.launch { AppList.checkForUpdates() } }
		refreshLayout.isRefreshing = refreshing
		
		list.adapter = adapter
		
		fab.setOnClickListener(Navigation.createNavigateOnClickListener(toAddApp()))
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		refreshCallback = null
		list.adapter = null
	}
	
	companion object {
		val adapter: AppOverviewListItemAdapter = AppOverviewListItemAdapter()
		private var refreshing = false
		@Volatile
		private var refreshCallback: ((Boolean) -> Unit)? = null
		
		fun setRefreshing(refreshing: Boolean) {
			AppOverviewFragment.refreshing = refreshing
			val refreshCallback = refreshCallback
			refreshCallback?.invoke(refreshing)
		}
	}
	
	data class AppOverviewListItem(val view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.appOverview_list_name)
		val version: TextView = view.findViewById(R.id.appOverview_list_version)
		val icon: ImageView = view.findViewById(R.id.appOverview_list_icon)
	}
	
	class AppOverviewListItemAdapter : RecyclerView.Adapter<AppOverviewListItem>() {
		override fun getItemCount(): Int = AppList.size
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppOverviewListItem =
			AppOverviewListItem(LayoutInflater.from(parent.context).inflate(R.layout.app_overview_list_item_layout, parent, false))
		
		
		override fun onBindViewHolder(myListItem: AppOverviewListItem, position: Int) {
			val app = AppList.forPosition(position)
			
			val ref = WeakReference(myListItem)
			
			adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
				override fun onChanged() {
					ref.get()?.let { updateUI(app, it) }
				}
			})
			
			updateUI(app, myListItem)
		}
		
		fun updateUI(app: App, myListItem: AppOverviewListItem) {
			with(myListItem) {
				
				view.setOnClickListener { view ->
					view.findNavController().navigate(toAppDetail(app.getId()))
				}
				
				name.text = app.installedName
				
				val foregroundColor = getColor(R.color.foreground, myListItem.view.context.theme)
				
				if (app.hasUpdates) {
					version.text = app.updateVersion.toString()
					version.setTextColor(Color.GREEN)
				} else {
					version.text = app.installedVersion.toString()
					version.setTextColor(foregroundColor)
				}
				
				if (app.hasUpdates) {
					version.text = app.updateVersion.toString()
					version.setTextColor(Color.GREEN)
				} else {
					version.text = app.installedVersion.toString()
					version.setTextColor(foregroundColor)
				}
				
				name.setTextColor(if (app.isInstalled()) foregroundColor else Color.RED)
				
				icon.setImageDrawable(app.getIcon())
			}
		}
	}
}

