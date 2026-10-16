package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.viewmodel.GradeTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionBoardScreen(viewModel: GradeTrackerViewModel) {
    val generatedImage by viewModel.generatedImage.collectAsState()
    val isImageLoading by viewModel.isImageLoading.collectAsState()
    val imageSizeOption by viewModel.imageSizeOption.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var customPrompt by remember { mutableStateOf("") }
    val sizeOptions = listOf("1K", "2K", "4K")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vision_board_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Academic Vision Board & Badges",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate inspiring high-resolution study motivation art and achievement crests using Gemini 3 Pro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Configuration Card (Model & Size Affordance)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Image Resolution Selection (gemini-3-pro-image-preview)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Size Affordance: 1K, 2K, 4K
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sizeOptions.forEach { size ->
                            val isSelected = imageSizeOption == size
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setImageSizeOption(size) },
                                label = { Text(size, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f).testTag("size_option_$size")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text("Custom Prompt (Leave blank for auto-personalized board)") },
                        placeholder = { Text("e.g., Golden honor roll medal with graduation cap in ${profile?.degreeName ?: "Computer Science"}") },
                        modifier = Modifier.fillMaxWidth().testTag("image_prompt_input"),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Quick Themes
                    Text(
                        text = "Inspiration Presets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                customPrompt = "Cinematic golden diploma scroll with magna cum laude honors medal and university tower library at sunset, 8k"
                            },
                            label = { Text("Honors Diploma", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                customPrompt = "Minimalist aesthetic study desk with glowing GPA 4.0 hologram and code notebooks, cozy warm lighting"
                            },
                            label = { Text("Cozy Study Desk", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.generateVisionArt(customPrompt) },
                        enabled = !isImageLoading,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("generate_image_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
                    ) {
                        if (isImageLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating $imageSizeOption Artwork...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate $imageSizeOption Vision Artwork")
                        }
                    }
                }
            }
        }

        // Generated Image Display Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (generatedImage != null) {
                        Image(
                            bitmap = generatedImage!!.asImageBitmap(),
                            contentDescription = "Academic Vision Board Artwork",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Resolution: $imageSizeOption • Generated with gemini-3-pro-image-preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (isImageLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Rendering in $imageSizeOption quality with Gemini 3 Pro...")
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = AcademicBluePrimary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your vision artwork will appear here.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tap 'Generate Vision Artwork' to visualize your graduation and academic milestones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
