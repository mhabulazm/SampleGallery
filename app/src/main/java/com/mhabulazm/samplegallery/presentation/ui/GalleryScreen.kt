package com.mhabulazm.samplegallery.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mhabulazm.samplegallery.domain.entity.Album
import com.mhabulazm.samplegallery.findActivity
import com.mhabulazm.samplegallery.presentation.permissions.PermissionUtils
import com.mhabulazm.samplegallery.presentation.viewmodel.GalleryViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    GalleryPermissionWrapper(content = {
        GalleryContent(navController, viewModel)
    })


}

@Composable
fun GalleryPermissionWrapper(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember { context as ComponentActivity }

    // Define required permissions
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Track permission state
    var allPermissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.any { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        allPermissionsGranted = permissions.any { it.value }
    }

    // Check permissions on first launch
    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    if (allPermissionsGranted) {
//        GalleryContent(navController, viewModel)
        content()
    } else {
        PermissionRationale {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }
}

@Composable
fun GalleryWithPermission(
    navController: NavController,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    val activity = context.findActivity()
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.all { it.value }
    }

    LaunchedEffect(Unit) {
        val requiredPermissions = PermissionUtils.getMediaPermissions()
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            hasPermission = true
        } else {
            hasPermission = false
            permissionLauncher.launch(requiredPermissions)
        }
    }

    if (hasPermission) {
        GalleryContent(navController, viewModel)
    } else {
        PermissionRationale {
            permissionLauncher.launch(PermissionUtils.getMediaPermissions())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryContent(
    navController: NavController,
    viewModel: GalleryViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Gallery") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is GalleryViewModel.GalleryUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is GalleryViewModel.GalleryUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is GalleryViewModel.GalleryUiState.Success -> {
                    AlbumGrid(
                        albums = state.albums,
                        viewMode = viewMode,
                        onAlbumClick = { album ->
                            Log.d("GALLERY", "GalleryContent() called with: album $album")
                            navController.navigate("album/${album.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumGrid(
    albums: List<Album>,
    viewMode: GalleryViewModel.ViewMode,
    onAlbumClick: (Album) -> Unit,
) {
    when (viewMode) {
        GalleryViewModel.ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(albums) { album ->
                    AlbumItem(album, onAlbumClick)
                }
            }
        }

        GalleryViewModel.ViewMode.LIST -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(albums) { album ->
                    AlbumItem(album, onAlbumClick)
                    Divider()
                }
            }
        }
    }
}

@Composable
fun AlbumItem(
    album: Album,
    onClick: (Album) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick(album) }
    ) {
        Column {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                modifier = Modifier.weight(1f),
                contentScale = ContentScale.Crop
            )
            Text(
                text = album.name,
                modifier = Modifier.padding(8.dp)
            )
            Text(
                text = "${album.mediaCount} items",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}