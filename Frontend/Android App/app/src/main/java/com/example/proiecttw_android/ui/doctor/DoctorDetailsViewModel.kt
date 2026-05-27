package com.example.proiecttw_android.ui.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proiecttw_android.data.api.ApiClient
import com.example.proiecttw_android.data.api.DoctorProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DoctorDetailsUiState(
    val loading: Boolean = false,
    val doctor: DoctorProfileDto? = null,
    val error: String? = null
)

class DoctorDetailsViewModel : ViewModel() {
    private val _ui = MutableStateFlow(DoctorDetailsUiState())
    val ui: StateFlow<DoctorDetailsUiState> = _ui

    fun loadDoctor(id: Long) {
        _ui.update { it.copy(loading = true, error = null, doctor = null) }

        viewModelScope.launch {
            try {
                val resp = ApiClient.doctorApi.getDoctorById(id)

                if (resp.code() == 404) {
                    _ui.update { it.copy(loading = false, error = "Doctorul nu a fost găsit (404).") }
                    return@launch
                }

                if (!resp.isSuccessful) {
                    _ui.update { it.copy(loading = false, error = "Eroare backend: ${resp.code()}") }
                    return@launch
                }

                val body = resp.body()
                _ui.update { it.copy(loading = false, doctor = body, error = null) }
            } catch (_: Exception) {
                _ui.update { it.copy(loading = false, error = "Backend indisponibil / eroare rețea.") }
            }
        }
    }
}
