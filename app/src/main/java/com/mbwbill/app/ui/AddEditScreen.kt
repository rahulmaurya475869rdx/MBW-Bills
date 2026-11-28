package com.mbwbill.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.mbwbill.app.data.Customer
import com.mbwbill.app.util.ImageUtils
import com.mbwbill.app.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    customerId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onScanSerial: () -> Unit,
    scannedSerial: String,
    onScannedConsumed: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    
    // 👇 Multiple Serial Numbers ki list
    var serialList by remember { mutableStateOf(listOf("")) }
    var scanIndex by remember { mutableStateOf(0) }
    
    var dateOfSale by remember { mutableStateOf("") }
    var dateOfInvoice by remember { mutableStateOf("") }
    var billImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var warrantyImages by remember { mutableStateOf<List<String>>(emptyList()) }

    // Full Screen Image Dialog ke liye
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    // Scanner se aaya hua data yahan fill hoga (specific index par)
    LaunchedEffect(scannedSerial) {
        if (scannedSerial.isNotBlank()) {
            val newList = serialList.toMutableList()
            if (scanIndex in newList.indices) {
                newList[scanIndex] = scannedSerial
            } else {
                newList.add(scannedSerial)
            }
            serialList = newList
            onScannedConsumed()
        }
    }

    LaunchedEffect(customerId) {
        if (customerId > 0) {
            viewModel.getById(customerId)?.let { c ->
                name = c.name; address = c.address; mobile = c.mobile
                // Purane comma-separated serials ko list mein split karo
                serialList = c.serialNo.split(",").map { it.trim() }.ifEmpty { listOf("") }
                dateOfSale = c.dateOfSale; dateOfInvoice = c.dateOfInvoice
                billImages = parseList(c.billImages)
                warrantyImages = parseList(c.warrantyImages)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (customerId == 0L) "Naya Customer" else "Edit Customer") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Customer Naam *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it },
                label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = mobile, onValueChange = { mobile = it },
                label = { Text("Mobile Number *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            // 👇 Multiple Serial Number Fields
            Text("Serial No (Max 5)", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            serialList.forEachIndexed { index, serial ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    OutlinedTextField(
                        value = serial,
                        onValueChange = { newVal ->
                            serialList = serialList.toMutableList().also { it[index] = newVal }
                        },
                        label = { Text(if (index == 0) "Serial No 1 *" else "Serial No ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        scanIndex = index
                        onScanSerial()
                    }) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") }
                    
                    if (serialList.size > 1) {
                        IconButton(onClick = {
                            serialList = serialList.toMutableList().also { it.removeAt(index) }
                        }) { Icon(Icons.Default.Delete, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            if (serialList.size < 5) {
                TextButton(onClick = { serialList = serialList + "" }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Another Serial No")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = dateOfSale, onValueChange = { dateOfSale = it },
                label = { Text("Date of Sale (YYYY-MM-DD) *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = dateOfInvoice, onValueChange = { dateOfInvoice = it },
                label = { Text("Date of Invoice (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            ImageListSection(
                label = "Bill Image (1 ya zyada)",
                images = billImages,
                onAdd = { billImages = billImages + it },
                onRemove = { i -> billImages = billImages.toMutableList().also { it.removeAt(i) } },
                onImageClick = { fullScreenImage = it }
            )
            Spacer(Modifier.height(12.dp))

            ImageListSection(
                label = "Warranty Card Images",
                images = warrantyImages,
                onAdd = { warrantyImages = warrantyImages + it },
                onRemove = { i -> warrantyImages = warrantyImages.toMutableList().also { it.removeAt(i) } },
                onImageClick = { fullScreenImage = it }
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val finalSerial = serialList.filter { it.isNotBlank() }.joinToString(", ")
                    if (name.isBlank() || mobile.isBlank() || finalSerial.isBlank()) {
                        Toast.makeText(context, "Naam, Mobile aur Kam se kam ek Serial No bharna zaroori hai!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val c = Customer(
                        id = customerId,
                        name = name.trim(),
                        address = address.trim(),
                        mobile = mobile.trim(),
                        serialNo = finalSerial,
                        dateOfSale = dateOfSale.trim(),
                        dateOfInvoice = dateOfInvoice.trim(),
                        billImages = Gson().toJson(billImages),
                        warrantyImages = Gson().toJson(warrantyImages),
                        synced = false
                    )
                    viewModel.save(c) { 
                        Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                        onBack() 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Save", fontSize = 16.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 👇 Full Screen Image Dialog
    if (fullScreenImage != null) {
        Dialog(onDismissRequest = { fullScreenImage = null }) {
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { fullScreenImage = null }) {
                AsyncImage(
                    model = File(fullScreenImage!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenImage = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ImageListSection(
    label: String,
    images: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val outUri = UCrop.getOutput(result.data!!)
            if (outUri != null) {
                val path = ImageUtils.compressAndSave(context, outUri)
                if (path.isNotEmpty()) onAdd(path) else {
                    Toast.makeText(context, "Image process failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) startCrop(context, uri, cropLauncher)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            startCrop(context, tempCameraUri!!, cropLauncher)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                tempCameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                cameraLauncher.launch(tempCameraUri!!)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Button(onClick = { showSourceDialog = true }) { Text("+ Add") }
        }
        if (images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(images) { i, path ->
                    Box {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier.size(90.dp).clickable { onImageClick(path) }
                        )
                        IconButton(
                            onClick = { onRemove(i) },
                            modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                        ) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Image Source Choose Karein") },
            text = { Text("Camera se photo lein ya gallery se select karein") },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                        tempCameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                        cameraLauncher.launch(tempCameraUri!!)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Camera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    pickLauncher.launch("image/*")
                }) { Text("Gallery") }
            }
        )
    }
}

private fun startCrop(context: Context, sourceUri: Uri, cropLauncher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    try {
        val destFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        val destUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)

        val intent = UCrop.of(sourceUri, destUri)
            .withMaxResultSize(1600, 2000)
            .withOptions(UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
                setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
                setCompressionQuality(85)
            })
            .getIntent(context)
            
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        cropLauncher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Crop error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun parseList(json: String): List<String> {
    return try {
        Gson().fromJson(json, Array<String>::class.java).toList()
    } catch (e: Exception) { emptyList() }
}
