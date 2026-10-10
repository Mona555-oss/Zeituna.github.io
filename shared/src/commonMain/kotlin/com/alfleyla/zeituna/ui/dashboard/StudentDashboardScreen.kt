package com.alfleyla.zeituna.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfleyla.zeituna.profile.ProfileViewModel
import com.alfleyla.zeituna.theme.TurquoisePrimary
import com.alfleyla.zeituna.theme.TurquoiseDark
import org.jetbrains.compose.resources.painterResource
import silentspace.shared.generated.resources.Res
import silentspace.shared.generated.resources.sub_icon
import silentspace.shared.generated.resources.sub_sub_icon

@Composable
fun StudentDashboardScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onBuyClick: () -> Unit,
    onPackageClick: (com.alfleyla.zeituna.data.models.Booking) -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val activePackages by viewModel.activePackages.collectAsState()
    val upcomingLessons by viewModel.upcomingLessons.collectAsState()
    val completedLessons by viewModel.completedLessons.collectAsState()
    val unscheduledLessons by viewModel.unscheduledLessons.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Dialog States
    val showReasonInput by viewModel.showReasonInput.collectAsState()
    val showUnscheduleWarning by viewModel.showUnscheduleWarning.collectAsState()
    val unscheduleMessage by viewModel.unscheduleMessage.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = TurquoisePrimary,
                contentPadding = PaddingValues(horizontal = 8.dp),
                elevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(onClick = {
                                showMenu = false
                                onNavigateToMaterials()
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TurquoisePrimary)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Materials")
                                }
                            }
                            DropdownMenuItem(onClick = {
                                showMenu = false
                                onNavigateToAccount()
                            }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TurquoisePrimary)
                                    Spacer(Modifier.width(12.dp))
                                    Text("My Account")
                                }
                            }
                        }
                    }

                    Image(
                        painter = painterResource(Res.drawable.sub_icon),
                        contentDescription = null,
                        modifier = Modifier.height(40.dp).align(Alignment.Center)
                    )

                    TextButton(
                        onClick = onNavigateToAccount,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("My Account", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Branded User Info Card
                Card(
                    elevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.sub_sub_icon),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).padding(end = 16.dp)
                        )
                        Column {
                            Text(
                                text = profile?.full_name ?: "User Name",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark
                            )
                            Text(
                                text = profile?.email ?: "user@example.com",
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Buy Button
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = TurquoisePrimary),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text("Book a Lesson / Course", color = Color.White, fontSize = 16.sp)
                }

                // Collapsible Sections
                CollapsibleSection(title = "Booked Courses") {
                    if (activePackages.isEmpty()) {
                        Text("No active Courses", modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        activePackages.forEach { pkg ->
                            PackageItem(pkg, onClick = { onPackageClick(pkg) })
                        }
                    }
                }

                CollapsibleSection(title = "Upcoming Lessons", initiallyExpanded = true) {
                    if (upcomingLessons.isEmpty()) {
                        Text("No upcoming lessons", modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        upcomingLessons.forEach { lesson ->
                            LessonItem(
                                event = lesson,
                                showUnschedule = true,
                                onUnscheduleClick = { viewModel.requestUnschedule(lesson) }
                            )
                        }
                    }
                }

                CollapsibleSection(title = "Completed Lessons") {
                    if (completedLessons.isEmpty()) {
                        Text("No completed lessons", modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        completedLessons.forEach { lesson ->
                            LessonItem(event = lesson)
                        }
                    }
                }

                CollapsibleSection(title = "Unscheduled Lessons") {
                    if (unscheduledLessons.isEmpty()) {
                        Text("No unscheduled lessons", modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        unscheduledLessons.forEach { lesson ->
                            LessonItem(event = lesson)
                        }
                    }
                }

                // Logout Button (at bottom)
                Button(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = TurquoisePrimary),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text("Logout", color = Color.White, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(64.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            
            // Dialogs
            showReasonInput?.let { lesson ->
                UnscheduleReasonDialog(
                    onDismiss = { viewModel.clearReasonInput() },
                    onConfirm = { reason ->
                        viewModel.performUnschedule(lesson, reason)
                        viewModel.clearReasonInput()
                    }
                )
            }
            
            if (showUnscheduleWarning) {
                UnscheduleWarningDialog(
                    message = "Lessons canceled with less than 20 hours notice cannot be rescheduled and will be marked as completed.",
                    onConfirm = { 
                        viewModel.clearUnscheduleWarning()
                    }
                )
            }
            
            unscheduleMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearUnscheduleMessage() },
                    title = { Text("Success") },
                    text = { Text(msg) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearUnscheduleMessage() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
