package com.veblen.tesstwo

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    companion object {
        private const val FILE_NAME = "tessdata"
        private const val LANGUAGE_NAME = "chi_sim.traineddata"
        private const val LANGUAGE_FILE_NAME = "chi_sim"
        private const val PERMISSION_REQUEST_CODE = 0
    }

    private lateinit var mProgressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        } else {
            languageIsExists()
        }

        mBtnAddPic?.setOnClickListener {
            PictureSelector.create(this).openGallery(PictureMimeType.ofImage())
                .isCamera(true)
                .loadImageEngine(GlideEngine.createGlideEngine)
                .selectionMode(PictureConfig.SINGLE)
                .forResult(PictureConfig.CHOOSE_REQUEST)
        }

        mProgressDialog = ProgressDialog(this)
        mProgressDialog.setCanceledOnTouchOutside(false)
    }


    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                languageIsExists()
            }
        }
    }

    private fun languageIsExists() {
        val outFile = File(getExternalFilesDir(FILE_NAME), LANGUAGE_NAME)
        if (!outFile.exists()) {
            copyToSdCard(this, LANGUAGE_NAME, outFile)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    if (selectList.isNotEmpty()) {
                        val media = selectList[0]
                        val bitmap = BitmapFactory.decodeFile(media.path)
                        mIvAddPic.setImageBitmap(bitmap)
                        load(bitmap)
                    }
                }
            }
        }
    }

    private fun load(bitmap: Bitmap) {
        val outFile = File(getExternalFilesDir(FILE_NAME), LANGUAGE_NAME)
        if (!outFile.exists()) {
            showMessage("找不到tessdata")
            return
        }
        val baseApi = TessBaseAPI()
        baseApi.setDebug(BuildConfig.DEBUG)
        val path = getExternalFilesDir("")?.absolutePath ?: ""
        if (TextUtils.isEmpty(path)) {
            showMessage("tessdata路径出现错误")
            return
        }
        baseApi.init(path, LANGUAGE_FILE_NAME)
        mProgressDialog.setMessage("文字识别中...")
        mProgressDialog.show()
        Thread(Runnable {
            baseApi.setImage(bitmap)
            val text = baseApi.utF8Text
            runOnUiThread {
                mTvInfo?.text = text
                mProgressDialog.dismiss()
            }
        }).start()

    }

    private fun copyToSdCard(context: Context, name: String, outFile: File) {
        try {
            val inputStream = context.assets.open(name)
            val fos = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var byteCount = inputStream.read(buffer)
            while (byteCount != -1) {
                fos.write(buffer, 0, byteCount)
                byteCount = inputStream.read(buffer)
            }
            fos.flush()
            inputStream.close()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
            showMessage("保存语言出现错误")
        }
    }

    private fun showMessage(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}
