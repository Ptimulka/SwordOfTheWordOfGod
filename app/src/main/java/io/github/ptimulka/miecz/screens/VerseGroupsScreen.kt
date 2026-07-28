package io.github.ptimulka.miecz.screens

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val allVerseGroups = remember { VersesGroupsRepository(context).loadVerseGroups() }
    val allSections = remember { SectionRepository(context).loadInitialSections() }

    val userProgressRepository = remember { UserProgressRepository(context) }

    val groupToSectionMap = remember(userProgressRepository.getCustomSectionsCount()) {
        val count = userProgressRepository.getCustomSectionsCount()
        (1..count).flatMap { index ->
            val sectionId = 5 + index - 1
            userProgressRepository.getCustomSectionGroups(sectionId)?.let { (groupId1, groupId2) ->
                listOf(groupId1 to sectionId, groupId2 to sectionId)
            } ?: emptyList()
        }.toMap()
    }

    // Single AssetManager.list() call — O(1) existence checks everywhere, no bitmap I/O during scroll
    val existingAssets = remember {
        context.assets.list("default_mnemonics")?.toHashSet() ?: hashSetOf()
    }
    val repository = remember { MnemonicPicturesRepository(context) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var previewedImage by remember { mutableStateOf<Bitmap?>(null) }
    var expandedGroupId by rememberSaveable { mutableStateOf<Int?>(null) }

    val filteredVerseGroups = remember(searchQuery, allVerseGroups) {
        if (searchQuery.isBlank()) {
            allVerseGroups
        } else {
            allVerseGroups.filter { group ->
                val query = searchQuery.trim()
                group.name.contains(query, ignoreCase = true) ||
                        group.verses.any { verse ->
                            val sigla = "${verse.book} ${verse.chapter},${verse.number}"
                            val cleanText = verse.text.replace('_', ' ').replace("*", "")
                            sigla.contains(query, ignoreCase = true) ||
                                    cleanText.contains(query, ignoreCase = true)
                        }
            }
        }
    }

    val filteredSections = remember(searchQuery, allSections) {
        if (searchQuery.isBlank()) {
            allSections
        } else {
            allSections.filter { section ->
                val query = searchQuery.trim()
                section.name.contains(query, ignoreCase = true) ||
                        section.verses.any { verse ->
                            val sigla = "${verse.book} ${verse.chapter},${verse.number}"
                            val cleanText = verse.text.replace('_', ' ').replace("*", "")
                            sigla.contains(query, ignoreCase = true) ||
                                    cleanText.contains(query, ignoreCase = true)
                        }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
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
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = contentPadding
        ) {
            item {
                Text(
                    text = stringResource(R.string.verse_groups_sections_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            items(filteredSections) { section ->
                VerseItem(
                    id = section.id,
                    name = section.name,
                    verses = section.verses,
                    assetNames = section.assetNames,
                    searchQuery = searchQuery,
                    sectionId = null,
                    isExpanded = expandedGroupId == -section.id,
                    onToggle = {
                        expandedGroupId = if (expandedGroupId == -section.id) null else -section.id
                    },
                    existingAssets = existingAssets,
                    repository = repository,
                    onPreviewImage = { previewedImage = it }
                )
            }
            item {
                Text(
                    text = stringResource(R.string.verse_groups_groups_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            items(filteredVerseGroups) { group ->
                val groupAssetNames = remember(group.id) {
                    group.verses.mapIndexed { index, verse ->
                        "group%03d_%d_%s%d-%s.webp".format(
                            group.id, index + 1, verse.book, verse.chapter,
                            verse.number.replace(".", "-")
                        )
                    }
                }
                VerseItem(
                    id = group.id,
                    name = group.name,
                    verses = group.verses,
                    assetNames = groupAssetNames,
                    searchQuery = searchQuery,
                    sectionId = groupToSectionMap[group.id],
                    isExpanded = expandedGroupId == group.id,
                    onToggle = {
                        expandedGroupId = if (expandedGroupId == group.id) null else group.id
                    },
                    existingAssets = existingAssets,
                    repository = repository,
                    onPreviewImage = { previewedImage = it }
                )
            }
        }
    }

    // Fullscreen image preview overlay — covers entire screen
    previewedImage?.let { bitmap ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { previewedImage = null },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
            )
        }
    }
    } // end outer Box
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
    existingAssets: HashSet<String>,
    repository: MnemonicPicturesRepository,
    onPreviewImage: (Bitmap) -> Unit
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
                                    onClick = {
                                        repository.loadDefaultPicture(assetName!!)?.let { onPreviewImage(it) }
                                    },
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
