package com.ganadopro.util

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream

/**
 * Centraliza el manejo de fotos de animales.
 *
 * Antes las fotos se guardaban en cacheDir (foto de cámara) o se referenciaban
 * por un content:// URI temporal de la galería (SAF). Ambos casos pueden
 * perderse: cacheDir puede ser limpiado por el sistema en cualquier momento,
 * y muchos proveedores de galería no garantizan que su URI siga siendo válido
 * después de un reinicio, incluso con permiso persistente solicitado.
 *
 * La solución: todas las fotos (de cámara o de galería) terminan copiadas
 * como un archivo propio dentro del almacenamiento interno de la app
 * (filesDir/animal_photos), que solo se borra si el usuario desinstala la app.
 * Se guarda la ruta absoluta de ese archivo en Animal.foto.
 */
object PhotoUtils {

    private const val FOLDER_NAME = "animal_photos"

    private fun photosDir(context: Context): File {
        val dir = File(context.filesDir, FOLDER_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Crea un archivo vacío en almacenamiento interno y devuelve un content:// URI
     * vía FileProvider, apto para pasarle a la cámara (TakePicture necesita un URI
     * al que la app de cámara pueda escribir).
     */
    fun createCameraDestination(context: Context): Pair<File, Uri> {
        val file = File(photosDir(context), "foto_animal_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        return file to uri
    }

    /**
     * Copia el contenido de un URI (típicamente de galería) a un archivo propio
     * en almacenamiento interno. Devuelve la ruta absoluta del archivo copiado,
     * o null si no se pudo leer el origen.
     */
    fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val destino = File(photosDir(context), "foto_animal_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destino).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            destino.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Carga una foto de animal en un ImageView, sin importar si "path" es:
     * - una ruta absoluta de archivo (nuevo formato, a partir de este fix)
     * - un content:// o file:// URI antiguo (compatibilidad con datos previos)
     */
    fun load(imageView: ImageView, path: String?, placeholderResId: Int) {
        val options = RequestOptions()
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .circleCrop()

        if (path.isNullOrBlank()) {
            imageView.setImageResource(placeholderResId)
            return
        }

        val model: Any = if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri.parse(path)
        } else {
            File(path)
        }

        Glide.with(imageView)
            .load(model)
            .apply(options)
            .into(imageView)
    }
}
