
package com.pennywiseai.tracker.di

import android.content.Context
import com.pennywiseai.tracker.data.currency.CurrencyConversionService
import com.pennywiseai.tracker.data.currency.ExchangeRateProvider
import com.pennywiseai.tracker.data.currency.ExchangeRateProviderFactory
import com.pennywiseai.tracker.data.database.dao.ExchangeRateDao
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.data.repository.SpendSenseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Hilt module that provides application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    /**
     * Provides the ExchangeRateProvider implementation.
     *
     * @return ExchangeRateProvider for fetching exchange rates
     */
    @Provides
    @Singleton
    fun provideExchangeRateProvider(): ExchangeRateProvider {
        return ExchangeRateProviderFactory.createProvider()
    }

    /**
     * Provides the CurrencyConversionService.
     *
     * @param exchangeRateDao Database access for exchange rates
     * @param exchangeRateProvider Provider for fetching rates from API
     * @param userPreferencesRepository User preferences for base currency
     * @return CurrencyConversionService for currency conversion operations
     */
    @Provides
    @Singleton
    fun provideCurrencyConversionService(
        exchangeRateDao: ExchangeRateDao,
        exchangeRateProvider: ExchangeRateProvider,
        userPreferencesRepository: UserPreferencesRepository
    ): CurrencyConversionService {
        return CurrencyConversionService(
            exchangeRateDao = exchangeRateDao,
            exchangeRateProvider = exchangeRateProvider,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    /**
     * Provides the SpendSenseRepository for SpendSense dashboard operations.
     *
     * @param context Application context for SMS reading and database access
     * @return SpendSenseRepository for managing SpendSense functionality
     */
    @Provides
    @Singleton
    fun provideSpendSenseRepository(
        @ApplicationContext context: Context
    ): SpendSenseRepository {
        return SpendSenseRepository(context = context)
    }
}
