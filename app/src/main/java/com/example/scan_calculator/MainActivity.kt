package com.example.scan_calculator

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var ambil_gambar: Button
    private lateinit var scan_recognize: Button
    private lateinit var ulangi: Button
    private lateinit var tampilkan_gambar: ImageView
    private lateinit var hasil_recognize: TextView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var selectedImageBitmap: Bitmap
    private var hasil: Double = 0.0
    private var REQUEST_CAMERA: Int = 1
    private var REQUEST_GALLERY: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ambil_gambar = findViewById(R.id.pilih_gambar)
        scan_recognize = findViewById(R.id.scan)
        ulangi = findViewById(R.id.reset)
        tampilkan_gambar = findViewById(R.id.tampilan_gambar)
        hasil_recognize = findViewById(R.id.hasil)

        ambil_gambar.setOnClickListener(this)
        scan_recognize.setOnClickListener(this)
        ulangi.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val item = v?.id
        if (item == R.id.pilih_gambar) {
            captureGambar()
        } else if (item == R.id.scan) {
            if (selectedImageBitmap == null) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Pilih Gambar Terlebih Dahulu")
                    .setNegativeButton("Tutup") { dialog, _ ->
                        dialog.dismiss()
                    }
                val alertDialog = builder.create()
                alertDialog.show()
            } else {
                scanPhoto()
            }
        } else {
            resetAll()
        }
    }

    private fun pesan(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun captureGambar() {
        val options = arrayOf("Kamera", "Galeri", "Batal")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Sumber Gambar")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(android.Manifest.permission.CAMERA),
                            REQUEST_CAMERA
                        )
                    } else {
                        openCamera()
                    }
                }
                1 -> {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                            REQUEST_GALLERY
                        )
                    } else {
                        openGallery()
                    }
                }
                2 -> dialog.dismiss()
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), REQUEST_GALLERY)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CAMERA)
        } else {
            pesan("Kamera tidak tersedia")
        }
    }

    private fun resetAll() {
        tampilkan_gambar.setImageDrawable(resources.getDrawable(R.drawable.pict_photo))
        hasil = 0.0
        hasil_recognize.text = "Hasil = $hasil"
        hasil_recognize.isAllCaps = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                pesan("Izin Kamera ditolak")
            }
        } else if (requestCode == REQUEST_GALLERY) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                pesan("Ijin Galeri ditolak")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                val extras = data?.extras
                if (extras != null && extras.containsKey("data")) {
                    val imageBitmap = extras.get("data") as Bitmap
                    tampilkan_gambar.setImageBitmap(imageBitmap)
                    selectedImageBitmap = imageBitmap
                }
            } else if (requestCode == REQUEST_GALLERY) {
                val imageUri = data?.data
                try {
                    val imageBitmap =
                        MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    tampilkan_gambar.setImageBitmap(imageBitmap)
                    selectedImageBitmap = imageBitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun scanPhoto() {
        progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.setMessage("Memproses....")
        progressDialog.show()

        if (selectedImageBitmap != null) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(selectedImageBitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    val recognizedText = text.text
                    operasiMatematika(recognizedText)
                    progressDialog.dismiss()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    pesan("Gagal melakukan deteksi teks")
                }
        } else {
            pesan("Pilih gambar terlebih dahulu")
        }
    }

    private fun operasiMatematika(teks: String) {
        try {
            hasil = evaluateMathExpression(teks)
            hasil_recognize.text = "Hasil = $hasil"
            hasil_recognize.isAllCaps = true
        } catch (e: Exception) {
            pesan("Terjadi kesalahan saat menghitung operasi matematika: ${e.message}")
        }
    }

    @Throws(Exception::class)
    private fun evaluateMathExpression(expression: String): Double {
        val operators = ArrayList<String>()
        val numbers = ArrayList<Double>()

        val number = StringBuilder()
        for (c in expression.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                number.append(c)
            } else {
                if (number.isNotEmpty()) {
                    numbers.add(number.toString().toDouble())
                    number.setLength(0)
                }

                if (isOperator(c)) {
                    operators.add(c.toString())
                } else {
                    throw Exception("Teks mengandung karakter yang tidak valid")
                }
            }
        }

        if (number.isNotEmpty()) {
            numbers.add(number.toString().toDouble())
        }

        if (numbers.size != operators.size + 1) {
            throw Exception("Operasi matematika tidak valid")
        }

        var result = numbers[0]
        for (i in operators.indices) {
            val operator = operators[i]
            val operand = numbers[i + 1]

            when (operator) {
                "+" -> result += operand
                "-" -> result -= operand
                "*", "x", "X" -> result *= operand
                "/" -> result /= operand
                else -> throw Exception("Operasi matematika tidak valid")
            }
        }
        return result
    }

    private fun isOperator(c: Char): Boolean {
        return c == '+' || c == '-' || c == 'x' || c == '/' || c == '*' || c == 'X'
    }

}