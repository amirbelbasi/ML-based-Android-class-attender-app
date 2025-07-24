package com.example.attender

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.* // Import for layout
import androidx.compose.foundation.lazy.LazyColumn // Import for list
import androidx.compose.foundation.lazy.items // Import for handling items in the list
import androidx.compose.material3.* // Use Material3 for UI components
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.attender.ui.theme.AttenderTheme
//
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.* // for column, row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
//
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attender.ui.theme.AttenderTheme
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.FileWriter
import java.nio.file.Files
import java.util.stream.IntStream.range
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.io.File
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.system.exitProcess

var theFinalRes = -1
var theFlagg = false

var appSwitch = 0

val py = Python.getInstance()
val module = py.getModule( "main" )
val sumFunc = module[ "isSamePerson" ]

var results = arrayOf<Float>()

class MainActivity : ComponentActivity() {
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            result ->
            Log.i("", "Obtainded result: $result")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if( !Python.isStarted() ) {
            Python.start( AndroidPlatform( this ) )
        }

        requestMultiplePermissions.launch(arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE))
        setContent {
            MainApp(applicationContext)
        }
    }
}

@Composable
fun CameraPreviewScreen(navController: NavHostController) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val preview = androidx.camera.core.Preview.Builder().build()
    val previewView = remember {
        PreviewView(context)
    }
    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }
    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageCapture)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        Button(onClick = { captureImage(imageCapture, context, navController) }) {
            Text(text = "عکس بگیر")
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

private fun captureImage(imageCapture: ImageCapture, context: Context, navController: NavHostController) {
    var studentCnt = 0
    val dummy = "/storage/emulated/0/Pictures/Students"
    if (File(dummy).exists()){
        studentCnt = File(dummy).listFiles().size
    }
    var name = "$studentCnt.jpg"
    if (appSwitch == 2){
        name = "Tmp.jpg"
    }
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        if (appSwitch == 1) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Students")
        }
        else if (appSwitch == 2) {
            val dummy = "/storage/emulated/0/Pictures/Tmp"
            if (File(dummy).exists()){
                File(dummy+"/Tmp.jpg").delete()
            }
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Tmp")
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                println("Successs")
                if (appSwitch == 2) {
                    val nStudents = File("/storage/emulated/0/Pictures/Students").listFiles().size
                    val Students = File("/storage/emulated/0/Pictures/Students").listFiles()?.map { it.absolutePath }?.toTypedArray()
                    //Students?.forEach { println(it) } ?: println("Directory not found or is not a directory")
                    val dir1 = "/storage/emulated/0/Pictures/Tmp/Tmp.jpg"
                    //while (i < nStudents){
                    //val dir0 = "/storage/emulated/0/Pictures/Students/$i.jpg"
                    //Log.i("Start1:", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    val res = sumFunc?.call(Students, dir1)
                    println(res)
                    //Log.i("End1:"+res, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    //if (res != null) {
                    //    results = results.plus(res.toFloat())
                    //}
                    //i += 1
                    //}
                    var i = 0
                    while (i < nStudents){
                        val ress = res?.asList()?.get(i)
                        if (ress != null) {
                            results = results.plus(ress.toFloat())
                        }
                        i += 1
                    }
                    var finalRes = -1
                    i = 0
                    val minn = results.min()
                    while (i < nStudents){
                        //Log.i("Start3:", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                        if (results[i] == minn){
                            finalRes = i
                            break
                        }
                        i += 1
                        //Log.i("End3:", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    }
                    results = arrayOf<Float>()
                    theFinalRes = finalRes
                    theFlagg = true
                    //Log.i("99999999999999999", "$theFinalRes")
                }
                navController.popBackStack()
            }
            override fun onError(exception: ImageCaptureException) {
                println("Failed $exception")
            }
        })
}

data class NameItem(val name: String, var isChecked: Boolean)

@Composable
fun FirstScreen(
    navController: NavController,
    nameItems: List<NameItem>,
    onUpdateNameItems: (List<NameItem>) -> Unit,
    onCheckNthName: (Int) -> Unit
) {
    // State to hold the input field for new names
    var inputName by remember { mutableStateOf("") }

    if(theFlagg){
        theFlagg = false
        onCheckNthName(theFinalRes)
        //Log.i("7777777777777777777", "Something Changed!")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TextField to take name input from the user
        TextField(
            value = inputName,
            onValueChange = { inputName = it },
            label = { Text("نام دانشجو") },
            modifier = Modifier.fillMaxWidth()
        )

        //Spacer(modifier = Modifier.height(8.dp))

        // Button to add the name from the TextField to the list
        Button(
            onClick = {
                if (inputName.isNotBlank()) {
                    onUpdateNameItems(nameItems + NameItem(inputName, false))
                    inputName = "" // Clear the input field
                }
                appSwitch = 1
                navController.navigate("cam")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("اضافه کردن دانشجو")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                appSwitch = 2
                navController.navigate("cam")
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("حضور و غیاب")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn to display the list of names with checkboxes
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(nameItems) { nameItem ->
                NameItemRow(
                    nameItem = nameItem,
                    onCheckedChange = { checked ->
                        // Update the check box state
                        val updatedList = nameItems.map {
                            if (it.name == nameItem.name) it.copy(isChecked = checked) else it
                        }
                        onUpdateNameItems(updatedList)
                    }
                )
            }
        }
    }
}

@Composable
fun NameItemRow(nameItem: NameItem, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = nameItem.name)
        Checkbox(
            checked = nameItem.isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun MainApp(context: Context) {
    val navController = rememberNavController()

    // Load the initial list of names from SharedPreferences
    var nameItems by remember { mutableStateOf(loadNamesFromPrefs(context)) }

    // NavHost defines the navigation graph
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            FirstScreen(navController, nameItems, onUpdateNameItems = { updatedNameItems ->
                nameItems = updatedNameItems
                saveNamesToPrefs(context, nameItems)
            }, onCheckNthName = { n ->
                nameItems = checkNthName(nameItems, n)
                saveNamesToPrefs(context, nameItems)
            })
        }
        composable("cam") { CameraPreviewScreen(navController) }
    }
}

fun checkNthName(nameItems: List<NameItem>, n: Int): List<NameItem> {
    if (n in nameItems.indices) {
        return nameItems.mapIndexed { index, item ->
            if (index == n && !item.isChecked) {
                item.copy(isChecked = true)
            } else {
                item
            }
        }
    }
    return nameItems
}

// Load the list of names from SharedPreferences
fun loadNamesFromPrefs(context: Context): List<NameItem> {
    val prefs = context.getSharedPreferences("name_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("name_list", null)
    return if (json != null) {
        val type = object : TypeToken<List<NameItem>>() {}.type
        Gson().fromJson(json, type)
    } else {
        emptyList() // Return an empty list if no data exists
    }
}

// Save the list of names to SharedPreferences
fun saveNamesToPrefs(context: Context, nameItems: List<NameItem>) {
    val prefs = context.getSharedPreferences("name_prefs", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val json = Gson().toJson(nameItems) // Convert the list to JSON
    editor.putString("name_list", json)
    editor.apply() // Save the data
}