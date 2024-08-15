package org.ferreiratechlab.sharexpress.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import org.ferreiratechlab.sharexpress.R
import org.ferreiratechlab.sharexpress.data.network.ServerDiscoveryManager

class ServerDiscoveryDialog(
    private val onServerSelected: (String, Int) -> Unit
) : DialogFragment() {

    private val servers = mutableListOf<Pair<String, Int>>()
    private lateinit var serverListAdapter: ArrayAdapter<Pair<String, Int>>
    private val serverDiscoveryManager = ServerDiscoveryManager()
    private val handler by lazy { ContextCompat.getMainExecutor(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the custom layout for the dialog
        val view = inflater.inflate(R.layout.dialog_server_discovery, container, false)
        val listView: ListView = view.findViewById(R.id.server_list_view)

        serverListAdapter = object : ArrayAdapter<Pair<String, Int>>(requireContext(), R.layout.item_server, servers) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_server, parent, false)
                val serverIcon = view.findViewById<ImageView>(R.id.server_icon)
                val serverNameTextView = view.findViewById<TextView>(R.id.server_name)

                // Set the server name
                val (ip, port) = getItem(position)!!
                serverNameTextView.text = "$ip:$port"

                // Optionally, set a different icon or color based on server properties
                serverIcon.setImageResource(R.drawable.baseline_cloud_24) // Replace with the correct drawable if needed

                return view
            }
        }

        listView.adapter = serverListAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val (ip, port) = servers[position]
            onServerSelected(ip, port)
            dismiss()
        }

        // Start server discovery
        serverDiscoveryManager.discoverServers { ip, port ->
            handler.execute {
                Log.d("ServerDiscoveryDialog", "Server found: $ip:$port")
                servers.add(Pair(ip, port))
                serverListAdapter.notifyDataSetChanged()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop server discovery when the dialog is closed
        serverDiscoveryManager.stopDiscovery()
    }
}
