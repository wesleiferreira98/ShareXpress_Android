package org.ferreiratechlab.sharexpress.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.ferreiratechlab.sharexpress.R
interface OnItemLongClickListener {
    fun onItemLongClick(fileName: String)
}

class FileAdapter(
    private val files: MutableList<String>, // Mudei para MutableList para facilitar a remoção de itens
    private val onItemLongClickListener: OnItemLongClickListener
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val progressMap = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.bind(file, progressMap[file])

        holder.itemView.setOnLongClickListener {
            onItemLongClickListener.onItemLongClick(file) // Passe o nome do arquivo diretamente
            true
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }

    fun updateProgress(fileName: String, progress: Int) {
        progressMap[fileName] = progress
        notifyDataSetChanged()
    }

    fun removeItem(fileName: String) {
        val index = files.indexOf(fileName)
        if (index != -1) {
            files.removeAt(index)
            notifyItemRemoved(index)
        }
    }


    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileName: TextView = itemView.findViewById(R.id.file_name)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)

        fun bind(file: String, progress: Int?) {
            fileName.text = file
            progressBar.progress = progress ?: 0
        }
    }
}

