import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mhabulazm.samplegallery.presentation.ui.GalleryScreen

@Composable
fun GalleryNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "gallery"
    ) {
        composable("gallery") {
            GalleryScreen(navController)
        }
        composable(
            route = "album/{albumId}",
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            AlbumDetailScreen(
                albumId = backStackEntry.arguments?.getString("albumId"),
                navController = navController
            )
        }
    }
}