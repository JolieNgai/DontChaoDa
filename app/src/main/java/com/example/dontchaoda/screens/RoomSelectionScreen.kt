package com.example.dontchaoda.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dontchaoda.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RoomSelectionScreen(
    navController: NavController,
    viewModel: AppViewModel,
    onRoomSelected: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var roomToRename by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Select a Room") }
            )
        },
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add New Room") },
                containerColor = Color(0xFFFF5722),
                contentColor = Color.White
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.rooms) { room ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onRoomSelected(room) },
                            onLongClick = {
                                roomToRename = room
                                showRenameDialog = true
                            }
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = room,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Add Room Dialog
        if (showAddDialog) {
            RoomNameDialog(
                title = "Add New Room",
                initialValue = "",
                onConfirm = { name ->
                    viewModel.addRoom(name)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        // Rename Room Dialog
        if (showRenameDialog) {
            RoomNameDialog(
                title = "Rename Room",
                initialValue = roomToRename,
                onConfirm = { newName ->
                    viewModel.renameRoom(roomToRename, newName)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }
    }
}

@Composable
fun RoomNameDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Room Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}