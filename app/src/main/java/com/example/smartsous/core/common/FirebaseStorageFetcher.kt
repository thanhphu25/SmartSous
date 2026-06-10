package com.example.smartsous.core.common

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import okio.buffer
import okio.source

class FirebaseStorageFetcher(
    private val reference: StorageReference,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val streamDownloadTask = reference.stream.await()
        return SourceResult(
            source = ImageSource(
                source = streamDownloadTask.stream.source().buffer(),
                context = options.context
            ),
            mimeType = null,
            dataSource = DataSource.NETWORK
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "gs") return null
            val storage = FirebaseStorage.getInstance()
            val reference = storage.getReferenceFromUrl(data.toString())
            return FirebaseStorageFetcher(reference, options)
        }
    }
}
