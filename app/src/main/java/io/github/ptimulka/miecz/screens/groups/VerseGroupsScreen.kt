package io.github.ptimulka.miecz.screens.groups

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptimulka.miecz.R
import io.github.ptimulka.miecz.data.Verse
import io.github.ptimulka.miecz.repositories.MnemonicPicturesRepository
import io.github.ptimulka.miecz.repositories.SectionRepository
import io.github.ptimulka.miecz.repositories.UserProgressRepository
import io.github.ptimulka.miecz.repositories.VersesGroupsRepository
import java.util.regex.Pattern

@Composable
fun VerseGroupsScreen(contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    
    val vm: VerseGroupsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val assetList = context.assets.list("default_mnemonics")?.toSet() ?: emptySet()
                return VerseGroupsViewModel(
                    SectionRepository(context),
                    VersesGroupsRepository(context),
                    UserProgressRepository(context),
                    MnemonicPicturesRepository(context),
                    assetList
                ) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { vm.onEvent(VerseGroupsEvent.UpdateSearch(it)) },
                onClear = { vm.onEvent(VerseGroupsEvent.ClearSearch) }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = contentPadding
            ) {
                if (state.filteredSections.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.verse_groups_sections_header))
                    }
                    items(state.filteredSections) { section ->
                        VerseItem(
                            id = section.id,
                            name = section.name,
                            verses = section.verses,
                            assetNames = section.assetNames,
                            searchQuery = state.searchQuery,
                            sectionId = null,
                            isExpanded = state.expandedId == -section.id,
                            onToggle = { vm.onEvent(VerseGroupsEvent.ToggleExpand(-section.id)) },
                            existingAssets = state.existingAssets,
                            onPreviewImage = { vm.onEvent(VerseGroupsEvent.ShowPreview(it)) }
                        )
                    }
                }

                if (state.filteredGroups.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.verse_groups_groups_header))
                    }
                    items(state.filteredGroups) { group ->
                        val groupAssetNames = state.groupAssetNames[group.id] ?: emptyList()
                        VerseItem(
                            id = group.id,
                            name = group.name,
                            verses = group.verses,
                            assetNames = groupAssetNames,
                            searchQuery = state.searchQuery,
                            sectionId = state.groupToSectionMap[group.id],
                            isExpanded = state.expandedId == group.id,
                            onToggle = { vm.onEvent(VerseGroupsEvent.ToggleExpand(group.id)) },
                            existingAssets = state.existingAssets,
                            onPreviewImage = { vm.onEvent(VerseGroupsEvent.ShowPreview(it)) }
                        )
                    }
                }
            }
        }

        PreviewOverlay(
            bitmap = state.previewImage,
            onDismiss = { vm.onEvent(VerseGroupsEvent.DismissPreview) }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(stringResource(id = R.string.search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }
            },
            singleLine = true
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun PreviewOverlay(
    bitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    bitmap?.let { b ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
            )
        }
    }
}

@Composable
private fun VerseItem(
    id: Int,
    name: String,
    verses: List<Verse>,
    assetNames: List<String>,
    searchQuery: String,
    sectionId: Int?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    existingAssets: Set<String>,
    onPreviewImage: (String) -> Unit
) {
    val headerText = remember(id, searchQuery) {
        buildAnnotatedString {
            append("$id. ")
            append(buildHighlightedText(name, searchQuery))
        }
    }
    val verseAnnotatedTexts = remember(id, searchQuery) {
        verses.map { verse ->
            val sigla = "${verse.book} ${verse.chapter},${verse.number}: "
            val cleanText = verse.text.replace('_', ' ')
            buildAnnotatedString {
                append(buildHighlightedText(sigla, searchQuery, style = SpanStyle(fontWeight = FontWeight.Bold)))
                val parts = cleanText.split('*')
                parts.forEachIndexed { i, part ->
                    val styledPart = buildHighlightedText(part, searchQuery)
                    if (i % 2 == 0) {
                        append(styledPart)
                    } else {
                        withStyle(style = SpanStyle(color = Color.Gray)) { append(styledPart) }
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.game_button_yellow_dark),
                    modifier = Modifier.weight(1f)
                )
                sectionId?.let { sid ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_levels_path),
                            contentDescription = null,
                            tint = colorResource(id = R.color.game_button_yellow_dark),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = sid.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.game_button_yellow_dark)
                        )
                    }
                }
                val rotationAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "rotation"
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
            if (isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    verses.forEachIndexed { index, _ ->
                        val assetName = assetNames.getOrNull(index)
                        val hasImage = assetName != null && existingAssets.contains(assetName)
                        val annotatedText = verseAnnotatedTexts[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = annotatedText,
                                modifier = Modifier.weight(1f).padding(end = 4.dp),
                                fontSize = 13.sp,
                                lineHeight = 16.sp
                            )
                            if (hasImage) {
                                IconButton(
                                    onClick = { onPreviewImage(assetName!!) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_hint),
                                        contentDescription = stringResource(R.string.image_hint),
                                        tint = colorResource(id = R.color.game_button_yellow_dark),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildHighlightedText(
    fullText: String,
    query: String,
    style: SpanStyle = SpanStyle()
): AnnotatedString {
    if (query.isBlank()) {
        return buildAnnotatedString { withStyle(style) { append(fullText) } }
    }
    return buildAnnotatedString {
        withStyle(style) {
            val pattern = Pattern.quote(query)
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            var lastIndex = 0
            regex.findAll(fullText).forEach { matchResult ->
                append(fullText.substring(lastIndex, matchResult.range.first))
                withStyle(style = SpanStyle(background = Color.Yellow)) {
                    append(matchResult.value)
                }
                lastIndex = matchResult.range.last + 1
            }
            if (lastIndex < fullText.length) {
                append(fullText.substring(lastIndex))
            }
        }
    }
}
