package com.college.sportsmeet.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.college.sportsmeet.databinding.ActivityCreateMatchBinding
import com.college.sportsmeet.models.CreateMatchRequest
import com.college.sportsmeet.models.SportData
import com.college.sportsmeet.models.TeamDataFull
import com.college.sportsmeet.network.ApiClient
import com.college.sportsmeet.repository.MatchRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CreateMatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateMatchBinding

    // Using Repo directly here for simplicity in a small Admin scope,
    // but ideally, this would use a ViewModel like the other flows.
    private val repository = MatchRepository(ApiClient.apiService)

    // Maps to store the selected IDs from the Dropdown Names
    private var sportsMap = mapOf<String, Long>()
    private var teamsMap = mapOf<String, Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadDropdownData()

        binding.btnCreateMatch.setOnClickListener {
            submitMatch()
        }
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadDropdownData() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val sportsResult = repository.fetchSports()
            val teamsResult = repository.fetchTeams()

            binding.progressBar.visibility = View.GONE

            if (sportsResult.isSuccess && teamsResult.isSuccess) {
                populateSportsDropdown(sportsResult.getOrNull()?.data ?: emptyList())
                populateTeamsDropdown(teamsResult.getOrNull()?.data ?: emptyList())
            } else {
                Snackbar.make(binding.root, "Failed to load backend data.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") { loadDropdownData() }
                    .show()
            }
        }
    }

    private fun populateSportsDropdown(sports: List<SportData>) {
        // Build map: "Cricket" -> ID
        sportsMap = sports.associate { it.sportName to it.id }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sportsMap.keys.toList())
        binding.autoCompleteSport.setAdapter(adapter)
    }

    private fun populateTeamsDropdown(teams: List<TeamDataFull>) {
        // Build map: "CS (BTech)" -> ID
        teamsMap = teams.associate {
            val branch = it.branchName?.let { b -> " ($b)" } ?: ""
            "${it.teamName}$branch" to it.id
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, teamsMap.keys.toList())
        binding.autoCompleteTeamA.setAdapter(adapter)
        binding.autoCompleteTeamB.setAdapter(adapter)
    }

    private fun submitMatch() {
        val selectedSportName = binding.autoCompleteSport.text.toString()
        val selectedTeamAName = binding.autoCompleteTeamA.text.toString()
        val selectedTeamBName = binding.autoCompleteTeamB.text.toString()
        val startTime = binding.etStartTime.text.toString().trim()

        val sportId = sportsMap[selectedSportName]
        val teamAId = teamsMap[selectedTeamAName]
        val teamBId = teamsMap[selectedTeamBName]

        if (sportId == null || teamAId == null || teamBId == null || startTime.isEmpty()) {
            Snackbar.make(binding.root, "Please fill out all fields correctly.", Snackbar.LENGTH_LONG).show()
            return
        }

        if (teamAId == teamBId) {
            Snackbar.make(binding.root, "Team A and Team B cannot be the same.", Snackbar.LENGTH_LONG).show()
            return
        }

        val request = CreateMatchRequest(sportId, teamAId, teamBId, startTime)

        binding.btnCreateMatch.isEnabled = false
        binding.btnCreateMatch.text = "Creating..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = repository.createMatch(request)

            binding.progressBar.visibility = View.GONE
            binding.btnCreateMatch.isEnabled = true
            binding.btnCreateMatch.text = "Create Match"

            result.onSuccess {
                Snackbar.make(binding.root, "Match created successfully!", Snackbar.LENGTH_SHORT).show()
                finish() // Return to AdminDashboard
            }.onFailure { e ->
                Snackbar.make(binding.root, e.localizedMessage ?: "Failed to create match", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}