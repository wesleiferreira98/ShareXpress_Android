package org.ferreiratechlab.sharexpress.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.ferreiratechlab.sharexpress.R

data class FileItemI(
    val fileName: String,
    var progress: Int = 0,
    var estimatedTime: String = "00:00:00"
)

interface OnItemLongClickListener {
    fun onItemLongClick(fileName: String)
}

class FileAdapter(
    private val fileNames: MutableList<String>,
    private val listener: OnItemLongClickListener
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val fileList: MutableList<FileItemI> = fileNames.map { fileName ->
        FileItemI(fileName)
    }.toMutableList()

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.file_name)
        val progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
        val progressPercentage: TextView = view.findViewById(R.id.progress_percentage)
        val estimatedTime: TextView = view.findViewById(R.id.estimated_time)

        init {
            view.setOnLongClickListener {
                listener.onItemLongClick(fileList[adapterPosition].fileName)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = fileList[position]
        holder.fileName.text = fileItem.fileName
        holder.progressBar.progress = fileItem.progress
        holder.progressPercentage.text = "${fileItem.progress}%"
        holder.estimatedTime.text = fileItem.estimatedTime
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    fun removeItem(fileName: String) {
        val index = fileList.indexOfFirst { it.fileName == fileName }
        if (index != -1) {
            fileList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateProgress(fileName: String, progress: Int, estimatedTime: String) {
        val index = fileList.indexOfFirst { it.fileName == fileName }
        if (index != -1) {
            fileList[index].progress = progress
            fileList[index].estimatedTime = estimatedTime
            notifyItemChanged(index)
        }
    }
}
