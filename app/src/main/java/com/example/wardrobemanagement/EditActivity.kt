package com.example.wardrobemanagement

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import java.io.File

class EditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit)

        // 接收文件路径
        val imagePath = intent.getStringExtra("image_path") ?: return

        //显示图片
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageURI(Uri.parse(imagePath))

        // 设置删除按钮逻辑
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            File(imagePath).let { file ->
                if (file.exists() && file.delete()) {
                    Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show()
                    Log.d("EditActivity", "File deleted successfully")
                    finish() // 关闭 Activity
                } else {
                    Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show()
                    Log.d("EditActivity", "Failed to delete file")
                }
                // 启动 MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        // 设置类别按钮
        setupButton(R.id.buttonCloth, "cloth_", imagePath)
        setupButton(R.id.buttonPant, "pant_", imagePath)
        setupButton(R.id.buttonShoe, "shoe_", imagePath)

        //返回MainActivity按钮
        val returnButton = findViewById<Button>(R.id.returnButton)
        returnButton.setOnClickListener {
            setResult(Activity.RESULT_OK)  // 设置返回结果
            finish()  // 结束当前活动，返回到 MainActivity

            // 启动 MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupButton(buttonId: Int, prefix: String, currentPath: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            val newFileName = prefix + File(currentPath).name
            val newFile = File(File(currentPath).parent, newFileName)
            val oldFile = File(currentPath)
            if (oldFile.renameTo(newFile)) {
                Toast.makeText(this, "File renamed to $newFileName", Toast.LENGTH_SHORT).show()
                intent.putExtra("image_path", newFile.absolutePath)  // Update the intent with the new file path
                findViewById<ImageView>(R.id.imageView).setImageURI(Uri.fromFile(newFile))
            } else {
                Toast.makeText(this, "Failed to rename file", Toast.LENGTH_SHORT).show()
            }
        }
    }
}