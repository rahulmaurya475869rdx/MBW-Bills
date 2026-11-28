package com.mbwbill.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.mbwbill.app.data.Customer
import com.mbwbill.app.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    customerId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    var customer by remember { mutableStateOf<Customer?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) } // 👈 Image Dialog

    LaunchedEffect(customerId) {
        customer = viewModel.getById(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            )
        }
    ) { padding ->
        customer?.let { c ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoRow("Naam", c.name)
                InfoRow("Mobile", c.mobile)
                InfoRow("Address", c.address)
                // Multiple serial numbers direct display honge
                InfoRow("Serial No", c.serialNo) 
                InfoRow("Date of Sale", c.dateOfSale)
                if (c.dateOfInvoice.isNotBlank()) InfoRow("Date of Invoice", c.dateOfInvoice)
                InfoRow("Sync Status", if (c.synced) "✅ Synced" else "⏳ Not synced")

                Spacer(Modifier.height(12.dp))
                ImageSection("Bill Images", c.billImages, onImageClick = { fullScreenImage = it })
                Spacer(Modifier.height(12.dp))
                ImageSection("Warranty Cards", c.warrantyImages, onImageClick = { fullScreenImage = it })
            }

            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Delete?") },
                    text = { Text("Ye customer ka data delete ho jayega.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.delete(customerId)
                            confirmDelete = false
                            onBack()
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                    }
                )
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    // 👇 Full Screen Image Viewer Dialog
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
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", fontSize = 14.sp, modifier = Modifier.width(120.dp))
        Text(value, fontSize = 14.sp)
    }
}

@Composable
private fun ImageSection(title: String, json: String, onImageClick: (String) -> Unit) {
    val paths = try {
        Gson().fromJson(json, Array<String>::class.java).toList()
    } catch (e: Exception) { emptyList() }

    if (paths.isEmpty()) return
    Text(title, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(paths) { p ->
            AsyncImage(
                model = File(p),
                contentDescription = null,
                modifier = Modifier.size(110.dp).clickable { onImageClick(p) } // 👈 Clickable add kiya
            )
        }
    }
}
