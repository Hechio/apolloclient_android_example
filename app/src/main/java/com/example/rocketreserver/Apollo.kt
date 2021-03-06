package com.example.rocketreserver

import android.content.Context
import android.os.Looper
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**The singleton is handled here manually for sample purposes. In a real-life application,
 * you would certainly use a dependency injection framework such as Dagger or Koin
 * */

private var instance: ApolloClient? = null

fun apolloClient(context: Context): ApolloClient{
    check(Looper.myLooper() == Looper.getMainLooper()){
        "Only the main thread can get the apolloClient instance"
    }
    if (instance !=null){
        return instance!!
    }

    val sqlNormalizeCacheFactory = SqlNormalizedCacheFactory(context,"rocket_reserver_db")
    val cacheFactory = LruNormalizedCacheFactory(EvictionPolicy.builder().maxSizeBytes(10485760L).build())

    val memoryFirstThenSqlCacheFactory = cacheFactory.chain(sqlNormalizeCacheFactory)


    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthorizationInterceptor(context))
        .build()

   instance = ApolloClient.builder()
        .serverUrl("https://apollo-fullstack-tutorial.herokuapp.com")
       .normalizedCache(memoryFirstThenSqlCacheFactory)
        .subscriptionTransportFactory(WebSocketSubscriptionTransport
            .Factory("wss://apollo-fullstack-tutorial.herokuapp.com/graphql",
            okHttpClient))
        .okHttpClient(okHttpClient)
        .build()
    return instance!!
}

private class AuthorizationInterceptor(val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", User.getToken(context) ?: "")
            .build()
        return chain.proceed(request)
    }

}