package com.example.recipscanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var textViewResult: EditText
    private lateinit var editTextPortions: EditText
    private var originalText: String = ""

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                imageView.setImageBitmap(imageBitmap)
                recognizeText(imageBitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonScan = findViewById<Button>(R.id.btnScanRecipe)
        val buttonCalc = findViewById<Button>(R.id.btnCalculate)
        val buttonSave = findViewById<Button>(R.id.btnSave)
        val buttonSavedList = findViewById<Button>(R.id.btnSavedList)

        imageView = findViewById(R.id.imageViewPhoto)
        textViewResult = findViewById(R.id.textViewResult)
        editTextPortions = findViewById(R.id.editTextPortions)

        if (savedInstanceState != null) {
            originalText = savedInstanceState.getString("originalText", "")
            if (originalText.isNotEmpty()) {
                textViewResult.setText(originalText)
            }
        }

        buttonScan.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            getContent.launch(takePictureIntent)
        }

        buttonCalc.setOnClickListener {
            calculatePortions()
        }

        buttonSave.setOnClickListener {
            saveRecipeToMemory()
        }

        buttonSavedList.setOnClickListener {
            val intent = Intent(this, SavedRecipesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("originalText", textViewResult.text.toString())
    }

    private fun recognizeText(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                originalText = visionText.text
                textViewResult.setText(originalText)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculatePortions() {
        val portionsStr = editTextPortions.text.toString()
        if (portionsStr.isEmpty()) {
            Toast.makeText(this, "Введите количество порций", Toast.LENGTH_SHORT).show()
            return
        }

        val newPortions = portionsStr.toIntOrNull() ?: return
        val originalPortions = 4
        val ratio = newPortions.toDouble() / originalPortions.toDouble()

        val baseRecipe = "Мука: 200 г\nСахар: 150 г\nМолоко: 100 мл\nСоль: 5 г"
        val ingredients = baseRecipe.split("\n")

        val stringBuilder = StringBuilder()
        stringBuilder.append("Новый расчет (на $newPortions порции):\n\n")

        for (item in ingredients) {
            val parts = item.split(": ")
            if (parts.size == 2) {
                val name = parts[0]
                val valueStr = parts[1].replace(" г", "").replace(" мл", "")
                val value = valueStr.toDoubleOrNull() ?: continue
                val unit = if (parts[1].contains("г")) "г" else "мл"

                val newValue = value * ratio
                val formattedValue = if (newValue % 1.0 == 0.0) newValue.toInt().toString() else String.format("%.1f", newValue)

                stringBuilder.append("$name: $formattedValue $unit\n")
            }
        }

        textViewResult.setText(stringBuilder.toString())
    }

    private fun saveRecipeToMemory() {
        val textToSave = textViewResult.text.toString()
        if (textToSave.isEmpty() || textToSave == "Здесь появится текст...") {
            Toast.makeText(this, "Нечего сохранять", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("recipes", MODE_PRIVATE)
        val oldData = prefs.getString("saved_list", "") ?: ""

        val newData = if (oldData.isEmpty()) {
            textToSave
        } else {
            "$oldData|||$textToSave"
        }

        prefs.edit().putString("saved_list", newData).apply()
        Toast.makeText(this, "Рецепт успешно сохранен!", Toast.LENGTH_SHORT).show()
    }
}