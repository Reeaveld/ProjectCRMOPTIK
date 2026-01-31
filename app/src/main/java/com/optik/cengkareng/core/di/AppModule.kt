package com.optik.cengkareng.core.di

import android.content.Context
import androidx.room.Room
import com.optik.cengkareng.core.utils.Constants
import com.optik.cengkareng.data.local.AppDatabase
import com.optik.cengkareng.data.local.dao.CustomerDao
import com.optik.cengkareng.data.remote.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // 1. Setup Retrofit (Internet)
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/api/") // IP Emulator ke Localhost Laptop
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    // 2. Setup Room (Database Lokal)
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "optik_cengkareng_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()
}