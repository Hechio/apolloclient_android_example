package com.example.rocketreserver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.example.rocketreserver.databinding.LaunchListFragmentBinding
import kotlinx.coroutines.channels.Channel
import java.nio.channels.Channels

class LaunchListFragment : Fragment() {
    private lateinit var binding: LaunchListFragmentBinding
    private val launches = mutableListOf<LaunchListQuery.Launch>()
    val mAdapter = LaunchListAdapter(launches)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LaunchListFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenResumed {

            binding.launches.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = mAdapter
            }
            val channel = Channel<Unit>(Channel.CONFLATED)
            channel.offer(Unit)
            mAdapter.onEndOfListReached = {
                channel.offer(Unit)
            }
            var cursor: String? = null
            for (item in channel){
                val response = try {
                    apolloClient(requireContext())
                        .query(LaunchListQuery(cursor = Input.fromNullable(cursor)))
                        .toBuilder().responseFetcher(ApolloResponseFetchers.CACHE_FIRST)
                        .build().await()
                }catch (e: ApolloException){
                    Log.d("LaunchList", "Failure", e)
                    return@launchWhenResumed
                }
                Log.d("LaunchList", "Success ${response.data}")
                val newLaunches = response.data?.launches?.launches?.filterNotNull()
                if (newLaunches != null){
                    launches.addAll(newLaunches)
                    mAdapter.notifyDataSetChanged()
                }
                cursor = response.data?.launches?.cursor

                if (response.data?.launches?.hasMore != true){
                    break
                }

            }

            mAdapter.onEndOfListReached = null
            channel.close()

        }
        mAdapter.onItemClicked = {
            findNavController().navigate(
                LaunchListFragmentDirections.openLaunchDetails(launchId = it.id))
        }
    }
}