package org.ferreiratechlab.sharexpress.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.ferreiratechlab.sharexpress.R

data class FileItem(val name: String, var progress: Int)

class FilesAdapter(private val files: MutableList<FileItem>) : RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.file_item, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = files[position]
        holder.fileName.text = fileItem.name
        holder.progressBar.progress = fileItem.progress
    }

    override fun getItemCount(): Int = files.size

    // Adicione novos arquivos de maneira segura
    fun addFile(file: FileItem) {
        Handler(Looper.getMainLooper()).post {
            if (files.none { it.name == file.name }) {
                files.add(file)
                notifyItemInserted(files.size - 1)
            } else {
                // Atualize o progresso se o arquivo já estiver na lista
                updateProgress(file.name, file.progress)
            }
        }
    }

    // Atualize o progresso do arquivo existente
    fun updateProgress(fileName: String, progress: Int) {
        Log.d("FilesAdapter", "Updating progress for $fileName to $progress")
        val fileItem = files.find { it.name == fileName }
        fileItem?.let {
            it.progress = progress
            Handler(Looper.getMainLooper()).post {
                notifyItemChanged(files.indexOf(it))
            }
        }
    }

}
