package com.ray.leica

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val PHOTO_PICKER_REQUEST_CODE = 0x1001
        const val REQUEST_IMAGE_CAPTURE = 0x1002
        const val BAR_HEIGHT = 120
        const val LOGO_RIGHT_MARGIN = 400 //default
        const val LOGO_SIZE = 50
        const val PHONE = "RedMi K58U Dragon Version"
        const val PARAMS = "100mm f/2.4 2/1573 ISO237"
    }

    lateinit var currentPhotoPath: String

    lateinit var currentPhotoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun shoot(v: View) {
        dispatchTakePictureIntent()
    }

    fun gallery(v: View) {
        // Launches photo picker in single-select mode.
        // This means that the user can select one photo or video.
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PHOTO_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            // Handle error
            return
        }
        when (requestCode) {
            PHOTO_PICKER_REQUEST_CODE -> {
                // Get photo picker response for single select.
                val currentUri: Uri? = data?.data
                // Do stuff with the photo/video URI.
                currentUri?.apply {
                    createLeica()
                }
                return
            }
            REQUEST_IMAGE_CAPTURE -> {
                currentPhotoUri.apply {
                    createLeica()
                    //删除源文件
                    File(currentPhotoPath).apply {
                        delete()
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.ray.leica.fileprovider",
                        it
                    )
                    currentPhotoUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun Uri.createLeica() {
        val bitmap =
            BitmapFactory.decodeStream(contentResolver.openInputStream(this))
        var scale = (bitmap.width / 1080f)
        scale = if (scale > 1.2f) scale else 1f
        val logoSize = (LOGO_SIZE * scale).toInt()
        var barHeight = (BAR_HEIGHT * scale).toInt()
        Toast.makeText(
            this@MainActivity,
            "${bitmap.width} * ${bitmap.height} px",
            Toast.LENGTH_SHORT
        ).show()
        val dstBitmap =
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(dstBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.drawRect(
            0f,
            (bitmap.height - barHeight).toFloat(),
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            })
        val textPaint = TextPaint().apply {
            textSize = 30 * scale
            color = Color.BLACK
            isFakeBoldText = true
            style = Paint.Style.FILL_AND_STROKE
        }
        //                    LOGO_RIGHT_MARGIN = textPaint.measureText(PARAMS).toInt() + 26
        val logoRightMargin = (Layout.getDesiredWidth(PARAMS, textPaint) + logoSize + 50).toInt()
        Log.d("leica", "LOGO_RIGHT_MARGIN = $logoRightMargin")

        //图片资源
        val leica =
            BitmapFactory.decodeResource(resources, R.drawable.leica_logo)
        //50*50
        canvas.drawBitmap(
            leica,
            null,
            Rect(
                bitmap.width - logoRightMargin,
                bitmap.height - ((barHeight - logoSize) / 2 + logoSize),
                bitmap.width - (logoRightMargin - logoSize),
                bitmap.height - (barHeight - logoSize) / 2
            ),
            null
        )

        canvas.drawRect(
            Rect(
                Rect(
                    bitmap.width - logoRightMargin + logoSize + 15,
                    bitmap.height - ((barHeight - logoSize) / 2 + logoSize) + 15,
                    bitmap.width - logoRightMargin + logoSize + 16,
                    bitmap.height - (barHeight - logoSize) / 2 - 15
                )
            ),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
            }
        )

        //参数
        canvas.drawText(
            PARAMS,
            bitmap.width - logoRightMargin + logoSize + 26f,
            bitmap.height - barHeight / 2 + textPaint.fontMetrics.run {
                (-top - bottom) / 2
            },
            textPaint
        )

        //机型
        canvas.drawText(
            PHONE,
            30f * scale,
            bitmap.height - barHeight / 2 + textPaint.fontMetrics.run {
                (-top - bottom) / 2
            },
            textPaint
        )
        //保存
        val imageFile = File(cacheDir, "leica_${System.currentTimeMillis()}.jpg")
        dstBitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(imageFile))
        MediaStore.Images.Media.insertImage(
            contentResolver,
            imageFile.absolutePath,
            imageFile.nameWithoutExtension,
            "leica"
        )
    }

}