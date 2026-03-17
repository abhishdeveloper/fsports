package com.college.sportsmeet.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.college.sportsmeet.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupListeners() {
        binding.cardCreateMatch.setOnClickListener {
            // Future implementation: BottomSheetDialogFragment or new Activity
            val intent = Intent(this, CreateMatchActivity::class.java)
            startActivity(intent)
        }

        binding.cardManageMatches.setOnClickListener {
            val intent = Intent(this, ManageMatchesActivity::class.java)
            startActivity(intent)
        }
    }
}