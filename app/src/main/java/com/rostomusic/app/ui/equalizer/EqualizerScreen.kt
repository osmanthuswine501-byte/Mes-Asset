package com.rostomusic.app.ui.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.rostomusic.app.core.design.RostoColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.rostomusic.app.R
import com.rostomusic.app.core.design.EmptyState
import com.rostomusic.app.core.design.Space
import com.rostomusic.app.data.settings.SettingsStore
import com.rostomusic.app.playback.AudioEffects
import com.rostomusic.app.playback.EqBand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class EqualizerUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val preset: String = "flat",
    val bands: List<EqBand> = emptyList(),
    val levels: List<Int> = emptyList(),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    effects: AudioEffects,
    private val settings: SettingsStore,
) : ViewModel() {

    private val draftLevels = MutableStateFlow<List<Int>?>(null)
    private val draftBass = MutableStateFlow<Int?>(null)
    private val draftVirtualizer = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<EqualizerUiState> = combine(
        effects.available,
        effects.bands,
        settings.flow,
        draftLevels,
        draftBass,
        draftVirtualizer,
    ) { values ->
        val available = values[0] as Boolean
        @Suppress("UNCHECKED_CAST")
        val bands = values[1] as List<EqBand>
        val currentSettings = values[2] as com.rostomusic.app.data.settings.Settings
        @Suppress("UNCHECKED_CAST")
        val pendingLevels = values[3] as List<Int>?
        val pendingBass = values[4] as Int?
        val pendingVirtualizer = values[5] as Int?

        EqualizerUiState(
            available = available,
            enabled = currentSettings.eqEnabled,
            preset = currentSettings.eqPreset,
            bands = bands,
            levels = pendingLevels ?: AudioEffects.levelsFor(currentSettings, bands),
            bassBoost = pendingBass ?: currentSettings.bassBoost,
            virtualizer = pendingVirtualizer ?: currentSettings.virtualizer,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EqualizerUiState(),
    )

    fun onEnabledChange(enabled: Boolean) {
        viewModelScope.launch { settings.setEqEnabled(enabled) }
    }

    fun onPresetSelected(name: String) {
        val bands = uiState.value.bands
        if (bands.isEmpty()) return
        draftLevels.value = null
        viewModelScope.launch {
            settings.setEqBands(AudioEffects.presetLevels(name, bands).joinToString(","), name)
        }
    }

    fun onBandValueChange(index: Int, rawValue: Float) {
        val current = uiState.value.levels.toMutableList()
        if (index !in current.indices) return
        current[index] = (rawValue / 50f).roundToInt() * 50
        draftLevels.value = current
    }

    fun onBandValueChangeFinished() {
        val committed = draftLevels.value ?: return
        draftLevels.value = null
        viewModelScope.launch {
            settings.setEqBands(committed.joinToString(","), "custom")
        }
    }

    fun onBassValueChange(rawValue: Float) {
        draftBass.value = rawValue.roundToInt()
    }

    fun onBassValueChangeFinished() {
        val committed = draftBass.value ?: return
        draftBass.value = null
        viewModelScope.launch { settings.setBassBoost(committed) }
    }

    fun onVirtualizerValueChange(rawValue: Float) {
        draftVirtualizer.value = rawValue.roundToInt()
    }

    fun onVirtualizerValueChangeFinished() {
        val committed = draftVirtualizer.value ?: return
        draftVirtualizer.value = null
        viewModelScope.launch { settings.setVirtualizer(committed) }
    }

    fun onResetFlat() {
        val bands = uiState.value.bands
        draftLevels.update { null }
        draftBass.update { null }
        draftVirtualizer.update { null }
        viewModelScope.launch {
            if (bands.isNotEmpty()) {
                settings.setEqBands(AudioEffects.presetLevels("flat", bands).joinToString(","), "flat")
            }
            settings.setBassBoost(0)
            settings.setVirtualizer(0)
        }
    }
}

private val PRESET_LABELS = mapOf(
    "flat" to R.string.eq_flat,
    "bass" to R.string.eq_bass,
    "treble" to R.string.eq_treble,
    "vocal" to R.string.eq_vocal,
    "rock" to R.string.eq_rock,
    "pop" to R.string.eq_pop,
    "jazz" to R.string.eq_jazz,
    "classical" to R.string.eq_classical,
    "electronic" to R.string.eq_electronic,
)

@Composable
fun EqualizerRoute(
    nav: NavController,
    vm: EqualizerViewModel = hiltViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    // rememberSaveable réservé uniquement à un état d'interface éphémère
    var showAdvancedEffects by rememberSaveable { mutableStateOf(true) }

    EqualizerScreen(
        uiState = uiState,
        showAdvancedEffects = showAdvancedEffects,
        onToggleAdvancedEffects = { showAdvancedEffects = !showAdvancedEffects },
        onNavigateBack = { nav.popBackStack() },
        onEnabledChange = vm::onEnabledChange,
        onPresetSelected = vm::onPresetSelected,
        onBandValueChange = vm::onBandValueChange,
        onBandValueChangeFinished = vm::onBandValueChangeFinished,
        onBassValueChange = vm::onBassValueChange,
        onBassValueChangeFinished = vm::onBassValueChangeFinished,
        onVirtualizerValueChange = vm::onVirtualizerValueChange,
        onVirtualizerValueChangeFinished = vm::onVirtualizerValueChangeFinished,
        onResetFlat = vm::onResetFlat,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EqualizerScreen(
    uiState: EqualizerUiState,
    showAdvancedEffects: Boolean,
    onToggleAdvancedEffects: () -> Unit,
    onNavigateBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresetSelected: (String) -> Unit,
    onBandValueChange: (Int, Float) -> Unit,
    onBandValueChangeFinished: () -> Unit,
    onBassValueChange: (Float) -> Unit,
    onBassValueChangeFinished: () -> Unit,
    onVirtualizerValueChange: (Float) -> Unit,
    onVirtualizerValueChangeFinished: () -> Unit,
    onResetFlat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val contentAlpha by animateFloatAsState(
        targetValue = if (uiState.enabled) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "eqContentAlpha",
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.equalizer),
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        maxLines = 2,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    val toggleLabel = stringResource(R.string.eq_enable)
                    Switch(
                        checked = uiState.enabled,
                        onCheckedChange = onEnabledChange,
                        enabled = uiState.available,
                        modifier = Modifier
                            .padding(end = Space.x2)
                            .semantics { contentDescription = toggleLabel },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (uiState.available && uiState.bands.isNotEmpty()) {
                val resetLabel = stringResource(R.string.eq_flat)
                ExtendedFloatingActionButton(
                    onClick = onResetFlat,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = resetLabel
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(
                            text = resetLabel,
                            style = MaterialTheme.typography.labelLargeEmphasized,
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        if (!uiState.available || uiState.bands.isEmpty()) {
            EmptyState(
                icon = R.drawable.ic_sliders_horizontal,
                title = stringResource(R.string.eq_unavailable_title),
                body = stringResource(R.string.eq_unavailable_body),
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .alpha(contentAlpha)
                .padding(bottom = Space.x12 + Space.x6),
            verticalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Space.screen),
                horizontalArrangement = Arrangement.spacedBy(Space.x2),
            ) {
                items(AudioEffects.PRESETS.keys.toList()) { name ->
                    val label = stringResource(PRESET_LABELS.getValue(name))
                    FilterChip(
                        selected = uiState.preset == name,
                        onClick = { onPresetSelected(name) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }

            // Card sans onClick (Règle 1 : contient des éléments interactifs Slider)
            Card(
                modifier = Modifier
                    .padding(horizontal = Space.screen)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(Space.x4)) {
                    Text(
                        text = stringResource(R.string.eq_bands),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(Space.x2))
                    uiState.bands.forEachIndexed { index, band ->
                        val bandLabel = if (band.centerHz >= 1000) {
                            stringResource(R.string.khz_short, band.centerHz / 1000f)
                        } else {
                            stringResource(R.string.hz_short, band.centerHz)
                        }
                        val level = uiState.levels.getOrElse(index) { 0 }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bandLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(60.dp),
                            )
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { onBandValueChange(index, it) },
                                onValueChangeFinished = onBandValueChangeFinished,
                                valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                enabled = uiState.enabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = bandLabel
                                        stateDescription = "%+.1f dB".format(level / 100f)
                                    },
                            )
                            Text(
                                text = "%+.1f".format(level / 100f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(44.dp),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showAdvancedEffects,
                enter = fadeIn(spring(dampingRatio = 0.8f)) +
                    expandVertically(spring(dampingRatio = 0.8f)),
                exit = fadeOut(spring(dampingRatio = 0.8f)) +
                    shrinkVertically(spring(dampingRatio = 0.8f)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.x3)) {
                    EffectCard(
                        title = stringResource(R.string.eq_bass_boost),
                        value = uiState.bassBoost,
                        enabled = uiState.enabled,
                        onValueChange = onBassValueChange,
                        onValueChangeFinished = onBassValueChangeFinished,
                    )
                    EffectCard(
                        title = stringResource(R.string.eq_virtualizer),
                        value = uiState.virtualizer,
                        enabled = uiState.enabled,
                        onValueChange = onVirtualizerValueChange,
                        onValueChangeFinished = onVirtualizerValueChangeFinished,
                    )
                }
            }

            Text(
                text = stringResource(R.string.eq_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Space.screen),
            )
        }
    }
}

@Composable
private fun EffectCard(
    title: String,
    value: Int,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    // Card sans onClick (Règle 1 : contient un Slider interactif)
    Card(
        modifier = Modifier
            .padding(horizontal = Space.screen)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(Space.x4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(value / 10f).roundToInt()} %",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 0f..1000f,
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = title },
            )
        }
    }
}

@Composable
fun switchColors(c: RostoColors): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = c.accent,
    checkedTrackColor = c.accentContainer.takeIf { it != Color.Transparent } ?: c.accent.copy(alpha = 0.35f),
    uncheckedThumbColor = c.textSecondary,
    uncheckedTrackColor = c.surfaceAlt,
)