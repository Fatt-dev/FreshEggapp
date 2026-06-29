package com.fresh.egg

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Kontainer Layar
    private lateinit var layoutSplash: View
    private lateinit var layoutOnboarding: View
    private lateinit var layoutScan: View
    private lateinit var layoutResultOverlay: View

    // Elemen Scan Screen
    private lateinit var containerImagePicker: FrameLayout
    private lateinit var imgSelectedEgg: ImageView
    private lateinit var layoutDefaultPlus: View
    private lateinit var btnAnalyzeNow: AppCompatButton
    
    // Elemen Result Screen
    private lateinit var imgResultStatusIcon: ImageView
    private lateinit var txtResultMessage: TextView
    private lateinit var btnBackToHome: AppCompatButton

    // Data Status
    private var selectedBitmap: Bitmap? = null
    private var cameraPhotoUri: Uri? = null
    private var classifier: Classifier? = null
    
    // Executor untuk inferensi di thread latar belakang
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Launcher untuk mengambil foto kamera (Modern API)
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                processAndDisplayImage(uri)
            }
        } else {
            Toast.makeText(this, "Batal mengambil foto kamera", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk memilih gambar galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            processAndDisplayImage(it)
        }
    }

    // Launcher untuk meminta izin kamera
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Izin kamera ditolak. Aktifkan di pengaturan HP untuk mengambil foto.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Classifier
        classifier = Classifier(this)

        // Binding Elemen UI
        layoutSplash = findViewById(R.id.layout_splash)
        layoutOnboarding = findViewById(R.id.layout_onboarding)
        layoutScan = findViewById(R.id.layout_scan)
        layoutResultOverlay = findViewById(R.id.layout_result_overlay)

        containerImagePicker = findViewById(R.id.container_image_picker)
        imgSelectedEgg = findViewById(R.id.img_selected_egg)
        layoutDefaultPlus = findViewById(R.id.layout_default_plus)
        btnAnalyzeNow = findViewById(R.id.btn_analyze_now)

        imgResultStatusIcon = findViewById(R.id.img_result_status_icon)
        txtResultMessage = findViewById(R.id.txt_result_message)
        btnBackToHome = findViewById(R.id.btn_back_to_home)

        // ================= ALUR NAVIGASI & EVENT LISTENER =================

        // Layar 1 -> Layar 2 (Ketuk Splash Screen)
        layoutSplash.setOnClickListener {
            layoutSplash.visibility = View.GONE
            layoutOnboarding.visibility = View.VISIBLE
        }

        // Layar 2 -> Layar 3 (Klik START SCANNING)
        findViewById<View>(R.id.btn_start_scanning).setOnClickListener {
            layoutOnboarding.visibility = View.GONE
            layoutScan.visibility = View.VISIBLE
        }

        // Layar 3: Klik Box Gambar -> Buka Dialog Kamera / Galeri
        containerImagePicker.setOnClickListener {
            showImagePickerDialog()
        }

        // Layar 3: Klik ANALISIS SEKARANG
        btnAnalyzeNow.setOnClickListener {
            val bitmap = selectedBitmap
            if (bitmap == null) {
                Toast.makeText(this, "Silakan pilih foto telur terlebih dahulu", Toast.LENGTH_SHORT).show()
            } else {
                runImageAnalysis(bitmap)
            }
        }

        // Layar 4/5: Klik KEMBALI KE BERANDA (Reset dan kembali ke Layar 2)
        btnBackToHome.setOnClickListener {
            resetScanState()
            layoutResultOverlay.visibility = View.GONE
            layoutOnboarding.visibility = View.VISIBLE
        }
    }

    // Menampilkan dialog pilihan Ambil Foto (Kamera) vs Pilih Galeri
    private fun showImagePickerDialog() {
        val options = arrayOf("Ambil Foto (Kamera)", "Pilih dari Galeri")
        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Foto Telur")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> pickImageLauncher.launch("image/*")
                }
                dialog.dismiss()
            }
            .show()
    }

    // Memeriksa izin kamera
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Membuka Kamera bawaan sistem
    private fun launchCamera() {
        val photoUri = createImageUri()
        if (photoUri != null) {
            cameraPhotoUri = photoUri
            takePictureLauncher.launch(photoUri)
        } else {
            Toast.makeText(this, "Gagal menyiapkan berkas penyimpanan foto", Toast.LENGTH_SHORT).show()
        }
    }

    // Membuat URI tempat penyimpanan foto kamera
    private fun createImageUri(): Uri? {
        return try {
            val imagesDir = File(filesDir, "Pictures")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val file = File(imagesDir, "temp_egg_photo.jpg")
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Memuat gambar dari Uri ke Bitmap, memperbaiki orientasi, dan menampilkan di UI
    private fun processAndDisplayImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Perbaiki rotasi jika foto terbalik/miring
                val rotatedBitmap = rotateImageIfRequired(bitmap, uri)
                selectedBitmap = rotatedBitmap
                
                // Tampilkan ke image view
                imgSelectedEgg.setImageBitmap(rotatedBitmap)
                imgSelectedEgg.visibility = View.VISIBLE
                layoutDefaultPlus.visibility = View.GONE
            } else {
                Toast.makeText(this, "Gagal mengurai file gambar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Kesalahan saat memuat gambar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Memutar balik gambar jika diperlukan berdasarkan tag EXIF
    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(selectedImage) ?: return img
        try {
            val ei = ExifInterface(inputStream)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
                else -> img
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try { inputStream.close() } catch (ex: Exception) {}
            return img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        if (rotatedImg != img) {
            img.recycle()
        }
        return rotatedImg
    }

    // Melakukan klasifikasi gambar pada thread latar belakang (agar UI tidak macet)
    private fun runImageAnalysis(bitmap: Bitmap) {
        val activeClassifier = classifier
        if (activeClassifier == null) {
            Toast.makeText(this, "Model analisis belum siap", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan loading/proses sederhana jika diperlukan
        Toast.makeText(this, "Menganalisis telur...", Toast.LENGTH_SHORT).show()

        backgroundExecutor.execute {
            val result = activeClassifier.classify(bitmap)
            runOnUiThread {
                if (result != null) {
                    showAnalysisResult(result.first)
                } else {
                    Toast.makeText(this@MainActivity, "Analisis gagal dilakukan", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Menampilkan hasil analisis telur (Layar 4 & 5)
    private fun showAnalysisResult(className: String) {
        if (className == "segar") {
            // Layar 4: Segar -> Ikon Centang, Pesan Aman
            imgResultStatusIcon.setImageResource(R.drawable.ic_success_check)
            txtResultMessage.setText(R.string.msg_safe)
        } else {
            // Layar 5: Busuk -> Ikon Silang, Pesan Tidak Aman
            imgResultStatusIcon.setImageResource(R.drawable.ic_error_cross)
            txtResultMessage.setText(R.string.msg_unsafe)
        }
        
        // Munculkan overlay hasil
        layoutResultOverlay.visibility = View.VISIBLE
    }

    // Mengosongkan data foto pada layar Scan
    private fun resetScanState() {
        selectedBitmap = null
        cameraPhotoUri = null
        imgSelectedEgg.visibility = View.GONE
        imgSelectedEgg.setImageDrawable(null)
        layoutDefaultPlus.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
    }
}
