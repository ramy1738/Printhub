package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BindingOption
import com.example.model.ColorMode
import com.example.model.DuplexMode
import com.example.model.FitMode
import com.example.model.PageOrientation
import com.example.model.PaperSize
import com.example.model.PrintOptions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrintOptionsCard(
    options: PrintOptions,
    isPhotoMode: Boolean,
    onOptionsChanged: (PrintOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_print_options"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Options d'impression",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Basculer les options"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Copies Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.settings_copies),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (options.copies > 1) {
                                        onOptionsChanged(options.copy(copies = options.copies - 1))
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_copies_minus")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuer", modifier = Modifier.size(18.dp))
                            }

                            Text(
                                text = "${options.copies}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .testTag("text_copies_count")
                            )

                            IconButton(
                                onClick = {
                                    onOptionsChanged(options.copy(copies = options.copies + 1))
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_copies_plus")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Augmenter", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Color Mode Chips
                    Text(
                        text = stringResource(R.string.settings_color_mode),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = options.colorMode == ColorMode.COLOR,
                            onClick = { onOptionsChanged(options.copy(colorMode = ColorMode.COLOR)) },
                            label = { Text(stringResource(R.string.color_full)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ColorLens,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (options.colorMode == ColorMode.COLOR) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_color_mode")
                        )

                        FilterChip(
                            selected = options.colorMode == ColorMode.MONOCHROME,
                            onClick = { onOptionsChanged(options.copy(colorMode = ColorMode.MONOCHROME)) },
                            label = { Text(stringResource(R.string.color_bw)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FormatAlignJustify,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (options.colorMode == ColorMode.MONOCHROME) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_bw_mode")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Paper Size
                    Text(
                        text = stringResource(R.string.settings_paper_size),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sizes = if (isPhotoMode) {
                            listOf(PaperSize.PHOTO_4X6, PaperSize.A4, PaperSize.LETTER, PaperSize.ROLL_80MM)
                        } else {
                            listOf(PaperSize.A4, PaperSize.LETTER, PaperSize.ROLL_80MM, PaperSize.ROLL_58MM, PaperSize.A3)
                        }

                        sizes.forEach { size ->
                            FilterChip(
                                selected = options.paperSize == size,
                                onClick = { onOptionsChanged(options.copy(paperSize = size)) },
                                label = {
                                    Text(
                                        when (size) {
                                            PaperSize.A4 -> stringResource(R.string.paper_a4)
                                            PaperSize.A3 -> stringResource(R.string.paper_a3)
                                            PaperSize.LETTER -> stringResource(R.string.paper_letter)
                                            PaperSize.PHOTO_4X6 -> stringResource(R.string.paper_photo)
                                            PaperSize.ROLL_80MM -> "Ticket 80mm"
                                            PaperSize.ROLL_58MM -> "Ticket 58mm"
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    if (isPhotoMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        // Photo Fit Mode
                        Text(
                            text = stringResource(R.string.settings_fit_mode),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = options.fitMode == FitMode.FIT_PAGE,
                                onClick = { onOptionsChanged(options.copy(fitMode = FitMode.FIT_PAGE)) },
                                label = { Text(stringResource(R.string.fit_page), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = options.fitMode == FitMode.FILL_PAGE,
                                onClick = { onOptionsChanged(options.copy(fitMode = FitMode.FILL_PAGE)) },
                                label = { Text(stringResource(R.string.fill_page), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        // Orientation
                        Text(
                            text = stringResource(R.string.settings_orientation),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = options.orientation == PageOrientation.PORTRAIT,
                                onClick = { onOptionsChanged(options.copy(orientation = PageOrientation.PORTRAIT)) },
                                label = { Text(stringResource(R.string.orient_portrait), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = options.orientation == PageOrientation.LANDSCAPE,
                                onClick = { onOptionsChanged(options.copy(orientation = PageOrientation.LANDSCAPE)) },
                                label = { Text(stringResource(R.string.orient_landscape), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Document specifics: Duplex & Binding
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.settings_duplex),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = options.duplexMode == DuplexMode.SINGLE_SIDED,
                                onClick = { onOptionsChanged(options.copy(duplexMode = DuplexMode.SINGLE_SIDED)) },
                                label = { Text(stringResource(R.string.duplex_single), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = options.duplexMode == DuplexMode.DOUBLE_SIDED,
                                onClick = { onOptionsChanged(options.copy(duplexMode = DuplexMode.DOUBLE_SIDED)) },
                                label = { Text(stringResource(R.string.duplex_double), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.settings_binding),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = options.bindingOption == BindingOption.NONE,
                                onClick = { onOptionsChanged(options.copy(bindingOption = BindingOption.NONE)) },
                                label = { Text(stringResource(R.string.binding_none), fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = options.bindingOption == BindingOption.TOP_LEFT_STAPLE,
                                onClick = { onOptionsChanged(options.copy(bindingOption = BindingOption.TOP_LEFT_STAPLE)) },
                                label = { Text(stringResource(R.string.binding_staple), fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = options.bindingOption == BindingOption.SPIRAL,
                                onClick = { onOptionsChanged(options.copy(bindingOption = BindingOption.SPIRAL)) },
                                label = { Text(stringResource(R.string.binding_spiral), fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}
