package com.example.wardrobemanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wardrobemanagement.ui.theme.WardrobeManagementTheme

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat.startActivityForResult
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.flow.collectLatest
import java.io.FileOutputStream


class MainActivity : ComponentActivity() {
    // 创建一个可观察状态来保存最新选中的图片 URI
    private val selectedImageUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建 PhotoComponent 实例并设置监听器
        val photoComponent = PhotoComponent.instance
        photoComponent.setContext(this)

        // 拍照或选择图片后更新
        photoComponent.setOnImageSelectedListener(object : OnImageSelectedListener {
            override fun onImageSelected(imageUri: Uri) {
                // 更新图片 URI 状态
                selectedImageUri.value = imageUri

                setContent {
                    WardrobeManagementTheme {
                        Title("衣橱")
                        MainScreen(selectedImageUri)
                    }
                }
            }
        })

        // 首页MainScreen
        setContent {
            WardrobeManagementTheme {
                Title("衣橱")
                MainScreen(selectedImageUri)
            }
        }
    }


//    // 在 onResume 方法中重新显示图片列表
//    override fun onResume() {
//        super.onResume()
//        // 刷新图片列表
//        // 获取最新的图片列表并更新界面
//        setContent {
//            WardrobeManagementTheme {
//                Title("衣橱")
//                MainScreen(selectedImageUri)
//            }
//        }
//    }
}




@Composable
fun Title(name: String){
    Text(text = name)
}

@Composable
fun Subtitle(name: String){
    Text(text = name)
}


//图片结果（URI路径+操作是否成功）
class PictureResult(val uri: Uri?, val isSuccess: Boolean)

// 选择照片
class SelectPicture : ActivityResultContract<Unit?, PictureResult>() {

    private var context: Context? = null

    // 创建选择图片的 Intent。
    override fun createIntent(context: Context, input: Unit?): Intent {
        this.context = context
        // 创建一个 ACTION_PICK 的 Intent，并设置类型为 image/* 以选择图片
        return Intent(Intent.ACTION_PICK).setType("image/*")
    }

    // 返回选择的照片：一个PictureResult
    override fun parseResult(resultCode: Int, intent: Intent?): PictureResult {
        return PictureResult(intent?.data, resultCode == Activity.RESULT_OK)
    }
}

// 拍照
class TakePhoto : ActivityResultContract<Unit?, PictureResult>() {
    // outUri 用于存储图片的 URI
    var outUri: Uri? = null

    private var imageName: String? = null

    companion object {
        //定义单例的原因是因为拍照返回的时候页面会重新实例takePhoto，导致拍照的uri始终为空
        val instance get() = Helper.obj
    }
    private object Helper {
        val obj = TakePhoto()
    }
    // 创建启动摄像头拍照的 Intent
    override fun createIntent(context: Context, input: Unit?): Intent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // 为照片生成文件并获取 URI
            getFileDirectory(context)?.let {
                outUri = it
                intent.putExtra(MediaStore.EXTRA_OUTPUT, it).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            }
        }

    // 返回拍照结果：一个PictureResult
    override fun parseResult(resultCode: Int, intent: Intent?): PictureResult {
        return PictureResult(outUri, resultCode == Activity.RESULT_OK)
    }

    // 生成图片文件并返回其 URI
    private fun getFileDirectory(context: Context): Uri? {//获取app内部文件夹
        imageName = "${UUID.randomUUID().toString().substring(0, 7)}"
//        val fileFolder = File(context.cacheDir, "test_imgs")
        val fileFolder = context.filesDir.resolve("wardrobe_images")
        if (!fileFolder.exists()) {
            fileFolder.mkdirs()
        }
        val file = File(fileFolder, "${imageName}.jpeg")
        if (!file.exists()) {
            file.createNewFile()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.image.provider", file)
    }
}


// 在 PhotoComponent 中定义一个回调接口
interface OnImageSelectedListener {
    fun onImageSelected(imageUri: Uri)
}

//调用拍照和选择相册的方法
class PhotoComponent() {
    // 定义一个 Context 变量，但不在这里初始化
    private lateinit var context: Context

    private var onImageSelectedListener: OnImageSelectedListener? = null

    fun setOnImageSelectedListener(listener: OnImageSelectedListener) {
        this.onImageSelectedListener = listener
    }

    // 在拍照或选择相册图片后调用该方法，并将图片 Uri 传递给 MainActivity
    private fun notifyImageSelected(imageUri: Uri) {
        onImageSelectedListener?.onImageSelected(imageUri)
    }

    // 修改拍照的结果处理
    private fun takePhotoResultHandler(context: Context, result: PictureResult) {
        if (result.isSuccess && result.uri != null) {
            val newUri = saveImageToInternalStorage(context, result.uri, "photo_${System.currentTimeMillis()}.jpeg")
            newUri?.let { notifyImageSelected(it) }
        }
    }

    // 修改选择图片的结果处理，接收 Context 作为参数
    private fun selectPictureResultHandler(context: Context, result: PictureResult) {
        if (result.isSuccess && result.uri != null) {
            val newUri = saveImageToInternalStorage(context, result.uri, "selected_${System.currentTimeMillis()}.jpeg")
            newUri?.let { notifyImageSelected(it) }
        }
    }

    // 分别用于选择图片和拍照，并接收结果
    private var openGalleryLauncher: ManagedActivityResultLauncher<Unit?, PictureResult>? = null
    private var takePhotoLauncher: ManagedActivityResultLauncher<Unit?, PictureResult>? = null

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        val instance get() = Helper.obj
    }

    private object Helper {
        val obj = PhotoComponent()
    }

    //监听拍照权限flow
    private val checkCameraPermission =
        MutableSharedFlow<Boolean?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun setCheckCameraPermissionState(value: Boolean?) {
        scope.launch {
            checkCameraPermission.emit(value)
        }
    }

    //相册权限flow
    private val checkGalleryImagePermission =
        MutableSharedFlow<Boolean?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private fun setCheckGalleryPermissionState(value: Boolean?) {
        scope.launch {
            checkGalleryImagePermission.emit(value)
        }
    }

    /**
     * @param galleryCallback 相册结果回调
     * @param graphCallback 拍照结果回调
     * @param permissionRationale 权限拒绝状态回调
     **/
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun Register(
        galleryCallback: (selectResult: PictureResult) -> Unit,
        graphCallback: (graphResult: PictureResult) -> Unit,
        permissionRationale: ((gallery: Boolean) -> Unit)? = null,
    ) {
        val rememberGraphCallback = rememberUpdatedState(newValue = graphCallback)
        val rememberGalleryCallback = rememberUpdatedState(newValue = galleryCallback)

        openGalleryLauncher = rememberLauncherForActivityResult(contract = SelectPicture()) {
            // 传递 Context 给结果处理函数
            selectPictureResultHandler(context, it)
        }
        takePhotoLauncher = rememberLauncherForActivityResult(contract = TakePhoto.instance) {
            // 传递 Context 给结果处理函数
            takePhotoResultHandler(context, it)
        }


        val readGalleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //13以上的权限申请
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        var permissionCameraState by rememberSaveable { mutableStateOf(false) }
        var permissionGalleryState by rememberSaveable { mutableStateOf(false) }
        val permissionList = arrayListOf(
            Manifest.permission.CAMERA,
            readGalleryPermission,
        )
        val galleryPermissionState = rememberPermissionState(readGalleryPermission)
        val cameraPermissionState = rememberMultiplePermissionsState(permissionList)
        LaunchedEffect(Unit) {
            checkCameraPermission.collectLatest {
                permissionCameraState = it == true
                if (it == true) {
                    if (cameraPermissionState.allPermissionsGranted) {
                        setCheckCameraPermissionState(null)
                        takePhotoLauncher?.launch(null)
                    } else if (cameraPermissionState.shouldShowRationale) {
                        setCheckCameraPermissionState(null)
                        permissionRationale?.invoke(false)
                    } else {
                        cameraPermissionState.launchMultiplePermissionRequest()
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            checkGalleryImagePermission.collectLatest {
                permissionGalleryState = it == true
                if (it == true) {
                    if (galleryPermissionState.status.isGranted) {
                        setCheckGalleryPermissionState(null)
                        openGalleryLauncher?.launch(null)
                    } else if (galleryPermissionState.status.shouldShowRationale) {
                        setCheckGalleryPermissionState(null)
                        permissionRationale?.invoke(true)
                    } else {
                        galleryPermissionState.launchPermissionRequest()
                    }
                }
            }
        }
        LaunchedEffect(cameraPermissionState.allPermissionsGranted) {
            if (cameraPermissionState.allPermissionsGranted && permissionCameraState) {
                setCheckCameraPermissionState(null)
                takePhotoLauncher?.launch(null)
            }
        }
        LaunchedEffect(galleryPermissionState.status.isGranted) {
            if (galleryPermissionState.status.isGranted && permissionGalleryState) {
                setCheckGalleryPermissionState(null)
                openGalleryLauncher?.launch(null)
            }
        }
    }

    private fun saveImageToInternalStorage(context: Context, imageUri: Uri, imageName: String): Uri? {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val directory = context.filesDir.resolve("wardrobe_images")
        if (!directory.exists()) directory.mkdirs()

        val prefixedImageName = "cloth_$imageName"

        val file = File(directory, prefixedImageName)
        val outputStream = FileOutputStream(file)
        // 将图片数据复制到输出流，实现文件保存
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        // 返回新文件的 Uri
        val savedUri = Uri.fromFile(file)
        // 通知 MainActivity 更新界面
        notifyImageSelected(savedUri)
        // 返回新文件的 Uri
        return savedUri
    }

    // 提供一个方法来设置 Context
    fun setContext(context: Context) {
        this.context = context
    }

    //选择图片
    fun selectImage() {
        setCheckGalleryPermissionState(true)
    }
    //拍照
    fun takePhoto() {
        setCheckCameraPermissionState(true)
    }
}



@Composable
fun MainScreen(selectedImageUri: MutableState<Uri?>) {
    // 每个类别的id列表
    val context = LocalContext.current
    val clothesImageFiles =  getClothImages(context)
    val pantsImageFiles = getPantImages(context)
    val shoesImageFiles = getShoeImages(context)

    // 调用拍照和相册
    val mediaAction by lazy { PhotoComponent.instance }
    var localImgPath by remember{
        mutableStateOf(Uri.EMPTY)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 拍照和选择图片的按钮
            Button(onClick = mediaAction::takePhoto) { Text("拍照") }
            Button(onClick = mediaAction::selectImage) { Text("选择图片") }
            Spacer(modifier = Modifier.height(24.dp))
            Subtitle("衣服")
            ImageGrid(clothesImageFiles, context)
            Subtitle("裤子")
            ImageGrid(pantsImageFiles, context)
            Subtitle("鞋")
            ImageGrid(shoesImageFiles, context)
            Spacer(modifier = Modifier.height(24.dp))

        }
    }

    // 监听 selectedImageUri 的变化，并在变化时更新图片列表
    LaunchedEffect(selectedImageUri.value) {
        if (selectedImageUri.value != null) {
            // 拍照或选择相册后，将新的图片 URI 添加到列表中
            val updatedClothesImageFiles = clothesImageFiles.toMutableList()
            updatedClothesImageFiles.add(File(selectedImageUri.value!!.path))
            // 更新界面
            // 重新加载列表
        }
    }


    mediaAction.Register(
        galleryCallback = {

            Log.d("MyAppLog", "相册内容${it}")
            if (it.isSuccess) {
                localImgPath = it.uri
            }
        },
        graphCallback = {
            Log.d("MyAppLog", "拍照内容${it.uri}")
            if (it.isSuccess) {
                localImgPath = it.uri
            }
        },
        permissionRationale = {
            //权限拒绝的处理
        },
    )
}

@Composable
fun ImageGrid(files: List<File>, context: Context) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        content = {
            items(files) { file ->
                // 展示图片
                val context = LocalContext.current
                SimpleCloth(file) {
                    ClothClick(file, context)
                }
            }
        }
    )
}

// 点击图片事件，跳转EditActivity，传递文件和文件路径
val ClothClick = { file: File, context: Context ->
    Log.d("ImageGrid", "ClothClick is triggered with file path: ${file.path}")
    val intent = Intent(context, EditActivity::class.java).apply {
        putExtra("image_path", file.absolutePath)  // 发送文件的绝对路径
    }
    context.startActivity(intent)
}

// 把图片和点击绑定在一起
@Composable
fun SimpleCloth(file: File, clickEvent: ()->Unit){
    Image(
        painter = rememberAsyncImagePainter(file),
        contentDescription = null,
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
            .clickable(onClick = clickEvent)
    )
}

// 获得上衣的文件列表
fun getClothImages(context: Context): List<File> {
    val imagesDir = context.filesDir.resolve("wardrobe_images")
    return imagesDir.listFiles { _, name -> name.startsWith("cloth_") }?.toList() ?: emptyList()
}

// 获得裤子的文件列表
fun getPantImages(context: Context): List<File> {
    val imagesDir = context.filesDir.resolve("wardrobe_images")
    return imagesDir.listFiles { _, name -> name.startsWith("pant_") }?.toList() ?: emptyList()
}

// 获得鞋的文件列表
fun getShoeImages(context: Context): List<File> {
    val imagesDir = context.filesDir.resolve("wardrobe_images")
    return imagesDir.listFiles { _, name -> name.startsWith("shoe_") }?.toList() ?: emptyList()
}



