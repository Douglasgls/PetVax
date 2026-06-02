package com.caogenda

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.caogenda.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val homeFragment = HomeFragment()
    private val petsFragment = PetsFragment()
    private val agendaFragment = AgendaFragment()
    private val perfilFragment = PerfilFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFragments()
        setupNavigation()
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, perfilFragment, "PROFILE").hide(perfilFragment)
            add(R.id.fragmentContainer, agendaFragment, "AGENDA").hide(agendaFragment)
            add(R.id.fragmentContainer, petsFragment, "PETS").hide(petsFragment)
            add(R.id.fragmentContainer, homeFragment, "HOME")
        }.commit()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_pets -> {
                    switchFragment(petsFragment)
                    true
                }
                R.id.nav_agenda -> {
                    switchFragment(agendaFragment)
                    true
                }
                R.id.nav_profile -> {
                    switchFragment(perfilFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        if (fragment != activeFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit()
            activeFragment = fragment
        }
    }

    fun selectTab(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            navigateToLogin()
        }
    }
}