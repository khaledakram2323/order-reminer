package com.example.orderreminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.orderreminders.adapter.OrderAdapter
import com.example.orderreminders.utils.BrandingHelper
import com.example.orderreminders.viewmodel.OrderViewModel

class ArchiveFragment : Fragment() {

    private lateinit var viewModel: OrderViewModel
    private lateinit var adapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BrandingHelper.applyBranding(requireContext(), view)

        viewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerCompletedOrders)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrderAdapter(
            onDoneClick = { order ->
                if (order.isCompleted) {
                    viewModel.undoDone(order)
                } else {
                    viewModel.markDone(order)
                }
            },
            onDeleteClick = { order ->
                viewModel.deleteOrder(order)
            }
        )

        recyclerView.adapter = adapter

        viewModel.completedOrders.asLiveData().observe(viewLifecycleOwner) { orders ->
            adapter.submitList(orders)
        }
    }
}
