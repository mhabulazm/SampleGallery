import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.mhabulazm.samplegallery.domain.entity.MediaItem
import com.mhabulazm.samplegallery.domain.entity.MediaType
import com.mhabulazm.samplegallery.presentation.viewmodel.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String?,
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val mediaItems = viewModel.getMedia(albumId).collectAsLazyPagingItems()
    val viewMode by viewModel.viewMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Album Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == AlbumViewModel.ViewMode.GRID)
                                Icons.Default.List else Icons.Outlined.Menu,
                            contentDescription = "Toggle view"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Log.d(
                "Gallery Album Details Screen",
                "AlbumDetailScreen() called ${mediaItems.itemCount}"
            )
            when (viewMode) {
                AlbumViewModel.ViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        state = rememberLazyGridState().also { state ->
                            viewModel.saveScrollPosition(state.firstVisibleItemIndex)
                        }
                    ) {

                        items(
                            count = mediaItems.itemCount,
                            key = { index -> /* mediaItems.peek(index)?.name ?: */ index }
                        ) { index ->
                            mediaItems[index]?.let { MediaItem(it) }

                        }
                    }
                }

                AlbumViewModel.ViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(
                            count = mediaItems.itemCount,
                            key = { index -> mediaItems.peek(index)?.id ?: index }) { item ->
                            mediaItems[item]?.let { MediaListItem(it) }

                        }
                    }
                }
            }

            if (mediaItems.loadState.refresh is LoadState.Loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun MediaItem(item: MediaItem) {
    Column(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .size(Size(250, 250))
                .build(),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Text(item.name, style = MaterialTheme.typography.bodyLarge)

        if (item.type == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(48.dp)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun MediaListItem(item: MediaItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.name,
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}