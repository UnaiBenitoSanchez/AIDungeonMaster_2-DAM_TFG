package com.example.aidungeonmaster.ui.register

import com.example.aidungeonmaster.ui.i18n.FixedTextTranslator
import com.example.aidungeonmaster.ui.i18n.Text

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.aidungeonmaster.R
import com.example.aidungeonmaster.ui.accessibility.VoiceFormAction
import com.example.aidungeonmaster.ui.accessibility.VoiceFormField
import com.example.aidungeonmaster.ui.accessibility.VoiceFormRegistry
import com.example.aidungeonmaster.ui.accessibility.VoiceFormScreen
import com.example.aidungeonmaster.ui.accessibility.VoiceInputType
import com.example.aidungeonmaster.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Ejecuta la lógica de register screen.
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Ejecuta la lógica de submit register.
    fun submitRegister() {
        when {
            displayName.isBlank() -> viewModel.errorMessage = "Introduce un nombre visible."
            username.length < 3 -> viewModel.errorMessage = "El nombre de usuario debe tener al menos 3 caracteres."
            password != confirmPassword -> viewModel.errorMessage = "Las contraseñas no coinciden."
            else -> {
                viewModel.register(
                    email = email,
                    pass = password,
                    displayName = displayName,
                    username = username
                ) {
                    Toast.makeText(
                        context,
                        FixedTextTranslator.translate(
                            context,
                            "¡Revisa tu correo! Te hemos enviado un email de verificación."
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    navController.popBackStack()
                }
            }
        }
    }

    DisposableEffect(email, password, confirmPassword, displayName, username) {
        val registration = VoiceFormRegistry.register(
            VoiceFormScreen(
                screenName = "registro",
                fields = listOf(
                    VoiceFormField(
                        label = "nombre visible",
                        aliases = listOf("nombre visible", "nombre", "nombre completo"),
                        inputType = VoiceInputType.HUMAN_NAME,
                        onValue = { displayName = it }
                    ),
                    VoiceFormField(
                        label = "nombre de usuario",
                        aliases = listOf("nombre de usuario", "usuario", "username"),
                        inputType = VoiceInputType.USERNAME,
                        onValue = { username = it }
                    ),
                    VoiceFormField(
                        label = "correo electrónico",
                        aliases = listOf("correo", "email", "correo electronico", "correo electrónico"),
                        inputType = VoiceInputType.EMAIL,
                        onValue = { email = it }
                    ),
                    VoiceFormField(
                        label = "contraseña",
                        aliases = listOf("contraseña", "contrasena", "password", "clave"),
                        inputType = VoiceInputType.PASSWORD,
                        onValue = { password = it },
                        feedback = { "Contraseña actualizada." }
                    ),
                    VoiceFormField(
                        label = "confirmar contraseña",
                        aliases = listOf(
                            "confirmar contraseña",
                            "confirmar contrasena",
                            "repetir contraseña",
                            "repetir contrasena",
                            "confirmacion",
                            "confirmación"
                        ),
                        inputType = VoiceInputType.PASSWORD,
                        onValue = { confirmPassword = it },
                        feedback = { "Confirmación de contraseña actualizada." }
                    )
                ),
                actions = listOf(
                    VoiceFormAction(
                        label = "crear cuenta",
                        aliases = listOf(
                            "crear cuenta",
                            "crear una cuenta",
                            "registrarme",
                            "forjar destino",
                            "crear aventurero"
                        ),
                        enabled = {
                            email.isNotBlank() &&
                                    password.isNotBlank() &&
                                    confirmPassword.isNotBlank() &&
                                    displayName.isNotBlank() &&
                                    username.isNotBlank()
                        },
                        disabledFeedback = "Faltan campos obligatorios para crear la cuenta.",
                        onRun = { submitRegister() },
                        feedback = "Creando cuenta."
                    )
                )
            )
        )

        onDispose { registration.dispose() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF1A1A1A),
                        Color(0xFF2D2419)
                    )
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_castle),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.15f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_shield),
                    contentDescription = "Escudo",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CREAR AVENTURERO",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Forja tu leyenda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Hoja del Aventurero",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nombre visible") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        placeholder = { Text("Ej: Unai García") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                                .lowercase()
                                .replace(" ", "_")
                                .filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' }
                        },
                        label = { Text("Nombre de usuario") },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        placeholder = { Text("Ej: unai_gm") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Será el identificador para buscarte.") },
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Pergamino Mágico (Email)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Sello Secreto (Contraseña)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                            androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar Sello") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            val description = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                            androidx.compose.material3.IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { submitRegister() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        enabled = email.isNotBlank()
                                && password.isNotBlank()
                                && confirmPassword.isNotBlank()
                                && displayName.isNotBlank()
                                && username.isNotBlank()
                    ) {
                        Text(
                            text = "FORJAR DESTINO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "← Volver al portal",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Al crear una cuenta, aceptas los términos del pacto mágico y las reglas de la taberna",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            viewModel.errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
