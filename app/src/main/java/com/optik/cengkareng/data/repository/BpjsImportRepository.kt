package com.optik.cengkareng.data.repository

import com.optik.cengkareng.core.utils.Resource
import com.optik.cengkareng.data.remote.api.ApiService
import com.optik.cengkareng.data.remote.model.BpjsCommitRequest
import com.optik.cengkareng.data.remote.model.BpjsCommitResponse
import com.optik.cengkareng.data.remote.model.BpjsParseResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BpjsImportRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * Mengunggah file PDF klaim BPJS ke backend untuk di-parse menjadi staging data
     */
    fun parsePdf(file: File): Flow<Resource<BpjsParseResponse>> = flow {
        emit(Resource.Loading)
        try {
            val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = apiService.parseBpjsPdf(part)

            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    val json = JSONObject(errorBody ?: "")
                    json.optString("message", "Gagal memproses file PDF.")
                } catch (e: Exception) {
                    "Gagal memproses PDF (HTTP ${response.code()})"
                }
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan saat upload PDF."))
        }
    }

    /**
     * Melakukan commit daftar transaksi yang telah ditinjau ke database
     */
    fun commitBpjs(request: BpjsCommitRequest): Flow<Resource<BpjsCommitResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.commitBpjs(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    val json = JSONObject(errorBody ?: "")
                    json.optString("message", "Gagal menyimpan klaim BPJS.")
                } catch (e: Exception) {
                    "Gagal menyimpan data (HTTP ${response.code()})"
                }
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Terjadi kesalahan jaringan saat menyimpan data."))
        }
    }
}
