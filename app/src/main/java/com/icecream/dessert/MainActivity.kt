package com.icecream.dessert

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.icecream.dessert.ui.theme.DessertTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DessertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun BrowserApp(viewModel: BrowserViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAboutDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state.isWebViewVisible) {
        viewModel.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Top Bar with Search and Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search bar with modern design
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = { Text("Search Hacker News...", color = Color.Gray) },
                trailingIcon = {
                    IconButton(
                        onClick = { 
                            if (searchQuery.isNotEmpty()) {
                                viewModel.searchWeb(searchQuery)
                            }
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                            .background(Color(0xFF2196F3), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotEmpty()) {
                            viewModel.searchWeb(searchQuery)
                        }
                    }
                )
            )

            // Info Button
            IconButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = Color.Red,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Main Content
        if (state.isWebViewVisible && state.currentUrl != null) {
            var webView by remember { mutableStateOf<WebView?>(null) }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        if (webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            viewModel.goBack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = state.currentUrl!!,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { viewModel.closeWebView() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadUrl(state.currentUrl!!)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Search results with loading indicator
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2196F3),
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.results) { result ->
                        WebResultItem(result = result) {
                            viewModel.navigateToUrl(result.url)
                        }
                    }
                }
            }
        }
    }

    // About Dialog
    if (showAboutDialog) {
        Dialog(
            onDismissRequest = { showAboutDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HackerNews Browser",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Built with ❤️ by Architect",
                        color = Color(0xFF2196F3),
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "A modern browser focused on tech news and discussions from Hacker News. Features include:",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BulletPoint("Fast and efficient search")
                        BulletPoint("Clean, modern interface")
                        BulletPoint("Built-in web browser")
                        BulletPoint("Dark theme for comfortable reading")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Got it!",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            color = Color(0xFF2196F3),
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
fun WebResultItem(
    result: WebResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = result.description,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Points
                result.points?.let {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Points",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = it,
                        color = Color(0xFFFFC107),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                    )
                }
                
                // Author
                result.author?.let {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Author",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = it,
                        color = Color(0xFF2196F3),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                    )
                }
                
                // Comments
                result.comments?.let {
                    Text(
                        text = "💬 $it",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}