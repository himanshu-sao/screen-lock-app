package com.touchfreeze.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    // Future: Add ViewModel-specific providers here
    // Example:
    // @Provides
    // @ViewModelScoped
    // fun provideSomeDependency(): SomeDependency {
    //     return SomeDependency()
    // }
}
