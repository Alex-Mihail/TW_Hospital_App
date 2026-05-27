package com.example.proiecttw_android

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.*
import com.example.proiecttw_android.ui.theme.ProiectTW_AndroidTheme
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.proiecttw_android.data.api.ApiClient
import com.example.proiecttw_android.data.datastore.SessionStore
import com.example.proiecttw_android.data.datastore.StoredUser
import com.example.proiecttw_android.data.models.LoginRequest
import com.example.proiecttw_android.ui.HomeScreen
import com.example.proiecttw_android.ui.LoginScreen
import com.example.proiecttw_android.ui.UserUi
import androidx.compose.runtime.remember
import com.example.proiecttw_android.ui.AccountScreen
import com.example.proiecttw_android.ui.EditAccountScreen
import com.example.proiecttw_android.ui.SignUpScreen
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.proiecttw_android.ui.ResetPasswordScreen
import com.example.proiecttw_android.ui.consultation.ConsultationScreen
import com.example.proiecttw_android.ui.doctor.DoctorDetailsScreen
import com.example.proiecttw_android.ui.appointments.AppointmentsScreen
import com.example.proiecttw_android.ui.booking.BookingScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            ComposeView(this).apply {
                setContent {
                    ProiectTW_AndroidTheme {
                        AppNav()
                    }
                }
            }
        )
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()

    val ctx = LocalContext.current
    val store = remember { SessionStore(ctx) }
    val storedUser by store.userFlow.collectAsState(initial = null)

    val userUi: UserUi? = storedUser?.let {
        UserUi(
            id = it.id,
            role = it.role,
            username = it.username,
            firstName = it.firstName,
            lastName = it.lastName
        )
    }

    NavHost(navController = nav, startDestination = "home") {

        composable("home") {
            HomeScreen(navController = nav, user = userUi)
        }

        composable("login") {
            LoginScreen(
                navController = nav,
                onLogin = { identifier, password ->
                    withContext(Dispatchers.IO) {
                        try {
                            val req = LoginRequest(identifier = identifier, password = password)

                            val patientResp = ApiClient.authApi.loginPatient(req)

                            val finalResp = when {
                                patientResp.isSuccessful -> patientResp
                                patientResp.code() == 401 -> ApiClient.authApi.loginDoctor(req)
                                else -> {
                                    return@withContext Result.failure(
                                        IllegalStateException("Eroare server (${patientResp.code()}).")
                                    )
                                }
                            }

                            if (finalResp.code() == 401) {
                                return@withContext Result.failure(
                                    IllegalStateException("Date de autentificare invalide.")
                                )
                            }

                            if (!finalResp.isSuccessful) {
                                return@withContext Result.failure(
                                    IllegalStateException("Eroare server (${finalResp.code()}).")
                                )
                            }

                            val body = finalResp.body()
                                ?: return@withContext Result.failure(
                                    IllegalStateException("Răspuns invalid (body lipsă).")
                                )

                            val id = body.id
                                ?: return@withContext Result.failure(
                                    IllegalStateException("Răspuns invalid (id lipsă).")
                                )

                            val role = (body.role ?: "UNKNOWN").uppercase()

                            val user = StoredUser(
                                id = id,
                                role = role,
                                username = body.username ?: identifier,
                                firstName = body.firstName.orEmpty(),
                                lastName = body.lastName.orEmpty()
                            )

                            store.saveUser(user)
                            Result.success(Unit)
                        } catch (_: Exception) {
                            Result.failure(IllegalStateException("Backend indisponibil."))
                        }
                    }
                }
            )
        }

        composable("signup") {
            SignUpScreen(navController = nav)
        }

        composable("account") {
            AccountScreen(navController = nav, user = userUi, sessionStore = store)
        }

        composable("account/edit") {
            EditAccountScreen(navController = nav, user = userUi, sessionStore = store)
        }

        composable("consultation") {
            ConsultationScreen(navController = nav, user = userUi, searchArg = null)
        }

        composable("consultation/{search}") { backStackEntry ->
            val q = backStackEntry.arguments?.getString("search")
            ConsultationScreen(navController = nav, user = userUi, searchArg = q)
        }
        composable("reset-password") { ResetPasswordScreen(navController = nav) }

        composable(
            route = "doctor-details/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            DoctorDetailsScreen(navController = nav, doctorId = id)
        }

        composable("appointments") {
            AppointmentsScreen(
                navController = nav,
                user = userUi
            )
        }

        composable(
            route = "booking/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.LongType })
        ) { backStack ->
            val doctorId = backStack.arguments?.getLong("doctorId") ?: return@composable
            BookingScreen(navController = nav, user = userUi, doctorId = doctorId)
        }
    }
}
