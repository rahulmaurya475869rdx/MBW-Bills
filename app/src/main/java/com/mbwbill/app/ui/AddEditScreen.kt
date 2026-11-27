package com.mbwbill.app.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var serialNo by remember { mutableStateOf("") }
    var dateOfSale by remember { mutableStateOf("") }
    var dateOfInvoice by remember { mutableStateOf("") }
    var billImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var warrantyImages by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(customerId) {
        if (customerId > 0) {
            viewModel.getById(customerId)?.let { c ->
                name = c.name; address = c.address; mobile = c.mobile
                serialNo = c.serialNo; dateOfSale = c.dateOfSale; dateOfInvoice = c.dateOfInvoice
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
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = serialNo,
                    onValueChange = { serialNo = it },
                    label = { Text("Serial No *") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val options = GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .enableAutoZoom()
                        .build()
                    GmsBarcodeScanning.getClient(context, options)
                        .startScan()
                        .addOnSuccessListener { barcode ->
                            barcode.rawValue?.let { serialNo = it }
                        }
                }) { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") }
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
                onRemove = { i -> billImages = billImages.toMutableList().also { it.removeAt(i) } }
            )
            Spacer(Modifier.height(12.dp))

            ImageListSection(
                label = "Warranty Card Images",
                images = warrantyImages,
                onAdd = { warrantyImages = warrantyImages + it },
                onRemove = { i -> warrantyImages = warrantyImages.toMutableList().also { it.removeAt(i) } }
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (name.isBlank() || mobile.isBlank() || serialNo.isBlank()) return@Button
                    val c = Customer(
                        id = customerId,
                        name = name.trim(),
                        address = address.trim(),
                        mobile = mobile.trim(),
                        serialNo = serialNo.trim(),
                        dateOfSale = dateOfSale.trim(),
                        dateOfInvoice = dateOfInvoice.trim(),
                        billImages = Gson().toJson(billImages),
                        warrantyImages = Gson().toJson(warrantyImages),
                        synced = false
                    )
                    viewModel.save(c) { onBack() }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Save", fontSize = 16.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ImageListSection(
    label: String,
    images: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit
) {
    val context = LocalContext.current
    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val outUri = UCrop.getOutput(result.data!!)
            if (outUri != null) {
                val path = ImageUtils.compressAndSave(context, outUri)
                if (path.isNotEmpty()) onAdd(path)
            }
        }
    }
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val dest = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
            val intent = UCrop.of(uri, Uri.fromFile(dest))
                .withMaxResultSize(1600, 2000)
                .withOptions(UCrop.Options().apply {
                    setFreeStyleCropEnabled(true)
                    setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
                    setCompressionQuality(85)
                })
                .getIntent(context)
            cropLauncher.launch(intent)
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Button(onClick = { pickLauncher.launch("image/*") }) { Text("+ Add") }
        }
        if (images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(images) { i, path ->
                    Box {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier.size(90.dp)
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
}

private fun parseList(json: String): List<String> {
    return try {
        Gson().fromJson(json, Array<String>::class.java).toList()
    } catch (e: Exception) { emptyList() }
}
