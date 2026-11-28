package com.mbwbill.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.*
import com.mbwbill.app.sync.SyncWorker
import com.mbwbill.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onScan: () -> Unit // 👈 Naya parameter
) {
    val context = LocalContext.current
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var syncing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MBW Bill", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        enabled = !syncing,
                        onClick = {
                            syncing = true
                            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                                .setConstraints(
                                    Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED).build()
                                ).build()
                            WorkManager.getInstance(context).enqueue(req)
                            WorkManager.getInstance(context)
                                .getWorkInfoByIdLiveData(req.id)
                                .observeForever { info ->
                                    if (info != null && info.state.isFinished) syncing = false
                                }
                        }
                    ) {
                        if (syncing) CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                        ) else Icon(Icons.Default.Sync, contentDescription = "Sync Now")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("Naam, mobile ya serial no search karo") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    // 👇 Search bar me Scanner Icon
                    IconButton(onClick = onScan) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Search", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true
            )

            if (customers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Koi customer nahi hai. + button dabao.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(customers, key = { it.id }) { c ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { onOpen(c.id) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    if (!c.synced) {
                                        Text("●", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                    } else {
                                        Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                    }
                                }
                                Text("📱 ${c.mobile}", fontSize = 14.sp)
                                Text("🔢 ${c.serialNo}", fontSize = 14.sp)
                                if (c.dateOfSale.isNotBlank()) Text("📅 ${c.dateOfSale}", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
