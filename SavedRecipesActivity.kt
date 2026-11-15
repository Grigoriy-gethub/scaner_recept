package com.example.recipscanner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class SavedRecipesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        val listView = findViewById<ListView>(R.id.listViewSaved)

        val prefs = getSharedPreferences("recipes", MODE_PRIVATE)
        val savedData = prefs.getString("saved_list", "") ?: ""

        val recipes = if (savedData.isEmpty()) listOf("Нет сохраненных рецептов") else savedData.split("|||")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recipes)
        listView.adapter = adapter
    }
}