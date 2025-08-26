package com.example.brainsightai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.brainsightai.ui.theme.BrainSightAITheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainSightAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClassifierScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassifierScreen(
    viewModel: ClassifierViewModel = viewModel(
        factory = ClassifierViewModel.Factory(
            ModelHandler(LocalContext.current)
        )
    )
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
            viewModel.onImageSelected(bitmap)
        }
        showBottomSheet = false
    }

    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let { uri ->
                val bitmap =
                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                viewModel.onImageSelected(bitmap)
            }
        }
        showBottomSheet = false
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "temp_image.jpg")
            cameraImageUri.value = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraImageUri.value?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            viewModel.uiState = uiState.copy(error = "Camera permission denied.")
        }
        showBottomSheet = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("BrainSightAI") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Info",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.preview), // Your logo drawable
                contentDescription = "BrainSightAI Logo",
                modifier = Modifier.size(200.dp) // Adjust size as needed
            )
            Spacer(modifier = Modifier.height(10.dp))

            ElevatedButton(
                onClick = { showBottomSheet = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Choose Image Source")
            }
            Spacer(modifier = Modifier.height(16.dp))

            uiState.selectedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected MRI Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.predictionText,
                style = MaterialTheme.typography.headlineSmall
            )

            if (uiState.selectedBitmap != null) {
                Text(
                    text = "Disclaimer: This is an AI-generated response and should not be used as medical advice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            uiState.error?.let { errorMsg ->
                Text(
                    text = "Error: $errorMsg",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (showInfoDialog) {
            val githubLink = "https://github.com/aymnsk" // Replace with your GitHub link
            val githubLink1 = "https://github.com/buildwithnomi" // Replace with your GitHub link
            val uriHandler = LocalUriHandler.current

            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("About BrainSightAI") },
                text = {
                    val annotatedString = buildAnnotatedString {
                        append("Developed by Noman Khan & Ayman Shaikh\n")
                        append("Version: 1.0\n")
                        append("Credits: PyTorch Mobile, Jetpack Compose\n\n")

                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary, // Link color
                            )
                        ) {
                            append("GitHub: Noman ")
                            pushStringAnnotation(tag = "URL", annotation = githubLink1)
                            pop()
                            append("\n")
                            Spacer(modifier = Modifier.height(4.dp))
                            append("GitHub: Ayman ")
                            pushStringAnnotation(tag = "URL", annotation = githubLink)
                            pop()
                        }
                    }
                    ClickableText(
                        text = annotatedString,
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(
                                tag = "URL",
                                start = offset,
                                end = offset
                            )
                                .firstOrNull()?.let {
                                    uriHandler.openUri(it.item)
                                }
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Image Source",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) -> {
                                    val photoFile = File(context.cacheDir, "temp_image.jpg")
                                    cameraImageUri.value = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    cameraImageUri.value?.let { uri ->
                                        cameraLauncher.launch(uri)
                                    }
                                }

                                else -> {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Camera")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Gallery")
                    }
                }
            }
        }
    }
}
