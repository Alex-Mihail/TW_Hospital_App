package com.example.proiecttw_android.ui.consultation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proiecttw_android.data.api.ApiClient
import com.example.proiecttw_android.data.api.DoctorProfileDto
import com.example.proiecttw_android.data.api.SpecializationDto
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsultationUiState(
    val specializations: List<SpecializationDto> = emptyList(),
    val selectedSpec: String = "", // "" = global
    val search: String = "",
    val doctors: List<DoctorProfileDto> = emptyList(),
    val loadingGlobal: Boolean = false,
    val error: String? = null
)

class ConsultationViewModel : ViewModel() {

    private val _ui = MutableStateFlow(ConsultationUiState())
    val ui: StateFlow<ConsultationUiState> = _ui

    private var allDoctorsCache: List<DoctorProfileDto>? = null
    private var initialized = false

    private suspend fun applyInitialSearch(q: String) {
        val qLower = q.lowercase()

        val specMatch = _ui.value.specializations.firstOrNull { s ->
            val name = s.name.orEmpty()
            name.lowercase().contains(qLower)
        }

        if (specMatch != null) {
            _ui.update { it.copy(search = "", selectedSpec = specMatch.name.orEmpty(), error = null) }
            loadDoctorsBySpec(specMatch.name.orEmpty())
            return
        }

        _ui.update { it.copy(selectedSpec = "", search = q, error = null) }
        loadAllDoctorsOnce()
    }
    fun init(loadGlobalImmediately: Boolean = true, initialSearch: String? = null) {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            loadSpecializations()

            val q = initialSearch?.trim().orEmpty()
            if (q.isNotBlank()) {
                applyInitialSearch(q)
                return@launch
            }

            if (loadGlobalImmediately) {
                onSelectSpecialization("") // global default
            }
        }
    }


    private suspend fun loadSpecializations() {
        try {
            val resp = ApiClient.specializationApi.getAll()
            val list = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
            val sorted = list.sortedBy { it.name.orEmpty().lowercase() }
            _ui.update { it.copy(specializations = sorted, error = null) }
        } catch (_: Exception) {
            _ui.update { it.copy(error = "Backend indisponibil.", specializations = emptyList()) }
        }
    }

    fun onSelectSpecialization(specName: String) {
        _ui.update { it.copy(selectedSpec = specName, search = "", error = null) }

        viewModelScope.launch {
            if (specName.isBlank()) loadAllDoctorsOnce()
            else loadDoctorsBySpec(specName)
        }
    }

    fun onSearchChange(text: String) {
        _ui.update { it.copy(search = text) }
    }

    fun ensureGlobalLoadedIfNeeded() {
        val s = _ui.value
        if (s.selectedSpec.isBlank() && allDoctorsCache == null) {
            viewModelScope.launch { loadAllDoctorsOnce() }
        }
    }

    private suspend fun loadDoctorsBySpec(spec: String) {
        try {
            val resp = ApiClient.doctorApi.bySpecialization(spec)
            val list = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
            _ui.update { it.copy(doctors = list, error = null) }
        } catch (_: Exception) {
            _ui.update { it.copy(error = "Eroare la încărcarea doctorilor.", doctors = emptyList()) }
        }
    }

    private suspend fun loadAllDoctorsOnce() {
        val cached = allDoctorsCache
        if (cached != null) {
            _ui.update { it.copy(doctors = cached, loadingGlobal = false, error = null) }
            return
        }

        val specs = _ui.value.specializations
        if (specs.isEmpty()) return

        _ui.update { it.copy(loadingGlobal = true, error = null) }

        try {
            val lists = specs.map { s ->
                viewModelScope.async {
                    val name = s.name.orEmpty()
                    if (name.isBlank()) return@async emptyList<DoctorProfileDto>()
                    val resp = ApiClient.doctorApi.bySpecialization(name)
                    if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                }
            }.map { it.await() }

            val flat = lists.flatten()

            val unique = flat
                .filter { it.id != null }
                .associateBy { it.id }
                .values
                .toList()

            allDoctorsCache = unique
            _ui.update { it.copy(doctors = unique, loadingGlobal = false, error = null) }
        } catch (_: Exception) {
            _ui.update { it.copy(loadingGlobal = false, error = "Eroare la încărcare globală.", doctors = emptyList()) }
        }
    }
}
