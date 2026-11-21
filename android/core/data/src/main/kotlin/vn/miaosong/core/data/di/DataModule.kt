package vn.miaosong.core.data.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import vn.miaosong.core.data.local.CartDao
import vn.miaosong.core.data.local.MiaoSongDatabase
import vn.miaosong.core.data.repository.AuthRepositoryImpl
import vn.miaosong.core.data.repository.CartRepositoryImpl
import vn.miaosong.core.data.repository.StoreRepositoryImpl
import vn.miaosong.core.domain.AuthRepository
import vn.miaosong.core.domain.CartRepository
import vn.miaosong.core.domain.StoreRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataProvideModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MiaoSongDatabase =
        Room.databaseBuilder(context, MiaoSongDatabase::class.java, "miaosong.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCartDao(db: MiaoSongDatabase): CartDao = db.cartDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("asia-southeast1")
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {
    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository
}
