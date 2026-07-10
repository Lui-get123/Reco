package com.example.repartocobro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repartocobro.ui.components.AccentButton
import com.example.repartocobro.ui.theme.*
import com.example.repartocobro.viewmodel.AppUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    state: AppUiState,
    onClose: () -> Unit,
    onAddProduct: (String, Int) -> Unit,
    onDeleteProduct: (Int) -> Unit,
    onAddRoute: (String, Int) -> Unit,
    onDeleteRoute: (Int) -> Unit,
    onAddStore: (String, Int) -> Unit,
    onDeleteStore: (Int) -> Unit
) {
    var currentTab by remember { mutableStateOf(0) } // 0: Products, 1: Routes, 2: Stores

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración", fontWeight = FontWeight.Bold, color = Graphite) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver", tint = Graphite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteSurface)
            )
        },
        containerColor = Bone
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = currentTab, containerColor = WhiteSurface) {
                Tab(selected = currentTab == 0, onClick = { currentTab = 0 }) {
                    Text("Productos", modifier = Modifier.padding(16.dp), color = Graphite, fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = currentTab == 1, onClick = { currentTab = 1 }) {
                    Text("Rutas", modifier = Modifier.padding(16.dp), color = Graphite, fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = currentTab == 2, onClick = { currentTab = 2 }) {
                    Text("Tiendas", modifier = Modifier.padding(16.dp), color = Graphite, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(Modifier.weight(1f)) {
                when (currentTab) {
                    0 -> ProductsTab(state, onAddProduct, onDeleteProduct)
                    1 -> RoutesTab(state, onAddRoute, onDeleteRoute)
                    2 -> StoresTab(state, onAddStore, onDeleteStore)
                }
            }
        }
    }
}

@Composable
fun ProductsTab(state: AppUiState, onAdd: (String, Int) -> Unit, onDelete: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nuevo Producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio base") })
                }
            },
            confirmButton = {
                AccentButton("Guardar", onClick = {
                    val p = price.toIntOrNull() ?: 0
                    if (name.isNotBlank() && p > 0) {
                        onAdd(name, p)
                        showDialog = false
                    }
                })
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Graphite) {
                Icon(Icons.Rounded.Add, contentDescription = "Agregar", tint = WhiteSurface)
            }
        },
        containerColor = Color.Transparent
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.allProducts) { product ->
                ListItem(
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(WhiteSurface),
                    headlineContent = { Text(product.name, fontWeight = FontWeight.Bold, color = Graphite) },
                    supportingContent = { Text("Precio: $${product.price}") },
                    trailingContent = {
                        IconButton(onClick = { onDelete(product.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = StatusError)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesTab(state: AppUiState, onAdd: (String, Int) -> Unit, onDelete: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var selectedCollectorId by remember { mutableStateOf(state.collectors.firstOrNull()?.id ?: 1) }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva Ruta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la ruta") })
                    
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = state.collectors.find { it.id == selectedCollectorId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cobrador") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            state.collectors.forEach { collector ->
                                DropdownMenuItem(
                                    text = { Text(collector.name) },
                                    onClick = { selectedCollectorId = collector.id; expanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AccentButton("Guardar", onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, selectedCollectorId)
                        showDialog = false
                    }
                })
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Graphite) {
                Icon(Icons.Rounded.Add, contentDescription = "Agregar", tint = WhiteSurface)
            }
        },
        containerColor = Color.Transparent
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.allRoutes) { route ->
                val collectorName = state.collectors.find { it.id == route.collectorId }?.name ?: "Desconocido"
                ListItem(
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(WhiteSurface),
                    headlineContent = { Text(route.name, fontWeight = FontWeight.Bold, color = Graphite) },
                    supportingContent = { Text("Cobrador: $collectorName") },
                    trailingContent = {
                        IconButton(onClick = { onDelete(route.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = StatusError)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoresTab(state: AppUiState, onAdd: (String, Int) -> Unit, onDelete: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var selectedRouteId by remember { mutableStateOf(state.allRoutes.firstOrNull()?.id ?: 1) }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Nueva Tienda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la tienda") })
                    
                    if (state.allRoutes.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = state.allRoutes.find { it.id == selectedRouteId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ruta") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                state.allRoutes.forEach { route ->
                                    DropdownMenuItem(
                                        text = { Text(route.name) },
                                        onClick = { selectedRouteId = route.id; expanded = false }
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Crea una ruta primero", color = StatusError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                AccentButton("Guardar", onClick = {
                    if (name.isNotBlank() && state.allRoutes.isNotEmpty()) {
                        onAdd(name, selectedRouteId)
                        showDialog = false
                    }
                })
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = Graphite) {
                Icon(Icons.Rounded.Add, contentDescription = "Agregar", tint = WhiteSurface)
            }
        },
        containerColor = Color.Transparent
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.allStoresAdmin) { store ->
                val routeName = state.allRoutes.find { it.id == store.routeId }?.name ?: "Desconocido"
                ListItem(
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(WhiteSurface),
                    headlineContent = { Text(store.name, fontWeight = FontWeight.Bold, color = Graphite) },
                    supportingContent = { Text("Ruta: $routeName") },
                    trailingContent = {
                        IconButton(onClick = { onDelete(store.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = StatusError)
                        }
                    }
                )
            }
        }
    }
}
